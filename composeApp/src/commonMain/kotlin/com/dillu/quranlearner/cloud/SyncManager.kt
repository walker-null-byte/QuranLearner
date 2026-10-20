package com.dillu.quranlearner.cloud

import com.dillu.quranlearner.db.QuranDb
import com.dillu.quranlearner.db.SyncAchievementRow
import com.dillu.quranlearner.db.SyncAyahProgressRow
import com.dillu.quranlearner.db.SyncFavoriteRow
import com.dillu.quranlearner.db.SyncProgressRow
import com.dillu.quranlearner.db.SyncReviewRow
import com.dillu.quranlearner.db.SyncSettingRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Two-way sync between the local SQLite database and the per-user tables in
 * Supabase (see `supabase/schema.sql`).
 *
 * Conflict handling is last-write-wins per row:
 *  - Pull: rows whose server timestamp (`server_updated_at`, set by a database
 *    trigger) is at or beyond the stored cursor are fetched and applied where
 *    they are newer than the local copy.
 *  - Push: local rows whose `updatedAt` is past the stored watermark are
 *    upserted (plus favorite tombstones as `deleted = true` rows).
 *
 * Order is always pull-then-push so a single sync converges both sides.
 * Pull uses the *server's* clock for its cursor (immune to device clock skew);
 * only conflict resolution itself compares device timestamps.
 */
class SyncManager(private val db: QuranDb, private val client: SupabaseClient) {

    /** Number of rows received from / sent to the server in one sync. */
    data class SyncSummary(val pulled: Int, val pushed: Int)

    suspend fun syncNow(): SyncSummary {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Not signed in")

        // The cursors belong to one account; signing into a different account
        // starts a fresh sync so this device's data is backed up there too.
        if (db.getSyncStateString(QuranDb.SYNC_KEY_ACCOUNT) != userId) {
            db.setSyncState(QuranDb.SYNC_KEY_LAST_PULLED, "")
            db.setSyncState(QuranDb.SYNC_KEY_LAST_PUSHED, 0L)
            db.setSyncState(QuranDb.SYNC_KEY_ACCOUNT, userId)
        }

        val startedAt = Clock.System.now().toEpochMilliseconds()
        val pullCursor = db.getSyncStateString(QuranDb.SYNC_KEY_LAST_PULLED)
        val pushWatermark = db.getSyncStateLong(QuranDb.SYNC_KEY_LAST_PUSHED)

        var pulled = 0
        var pushed = 0
        var pulledUpTo: Instant? = null

        fun track(serverTs: String?) {
            if (serverTs.isNullOrBlank()) return
            val parsed = runCatching { Instant.parse(serverTs) }.getOrNull() ?: return
            if (pulledUpTo == null || parsed > pulledUpTo!!) pulledUpTo = parsed
        }

        // ── 1) Pull server changes ──
        val settings = pull<SyncSettingRow>(TABLE_SETTINGS, pullCursor)
        val progress = pull<SyncProgressRow>(TABLE_PROGRESS, pullCursor)
        val ayahProgress = pull<SyncAyahProgressRow>(TABLE_AYAH_PROGRESS, pullCursor)
        val achievements = pull<SyncAchievementRow>(TABLE_ACHIEVEMENTS, pullCursor)
        val reviews = pull<SyncReviewRow>(TABLE_REVIEWS, pullCursor)
        val favorites = pull<SyncFavoriteRow>(TABLE_FAVORITES, pullCursor)

        for (row in settings) track(row.serverUpdatedAt)
        for (row in progress) track(row.serverUpdatedAt)
        for (row in ayahProgress) track(row.serverUpdatedAt)
        for (row in achievements) track(row.serverUpdatedAt)
        for (row in reviews) track(row.serverUpdatedAt)
        for (row in favorites) track(row.serverUpdatedAt)

        // Reaching this point means every table was fetched; only now advance the cursor.
        pulledUpTo?.let { db.setSyncState(QuranDb.SYNC_KEY_LAST_PULLED, it.toString()) }

        db.applySettingsRows(settings)
        db.applyProgressRows(progress)
        db.applyAyahProgressRows(ayahProgress)
        db.applyAchievementRows(achievements)
        db.applyReviewRows(reviews)
        db.applyFavoriteRows(favorites)
        pulled = settings.size + progress.size + ayahProgress.size +
            achievements.size + reviews.size + favorites.size

        // ── 2) Push local changes ──
        val localSettings = db.exportSettingsSince(pushWatermark).map { it.copy(userId = userId) }
        push(TABLE_SETTINGS, localSettings, "user_id,key")

        val localProgress = db.exportProgressSince(pushWatermark).map { it.copy(userId = userId) }
        push(TABLE_PROGRESS, localProgress, "user_id,date")

        val localAyahProgress = db.exportAyahProgressSince(pushWatermark).map { it.copy(userId = userId) }
        push(TABLE_AYAH_PROGRESS, localAyahProgress, "user_id,surah_number,ayah_number")

        val localAchievements = db.exportAchievementsSince(pushWatermark).map { it.copy(userId = userId) }
        push(TABLE_ACHIEVEMENTS, localAchievements, "user_id,id")

        val localReviews = db.exportReviewsSince(pushWatermark).map { it.copy(userId = userId) }
        push(TABLE_REVIEWS, localReviews, "user_id,surah_number,ayah_number")

        val localFavorites = db.exportFavoritesSince(pushWatermark).map { it.copy(userId = userId) }
        // Never upload a row and its own tombstone in the same batch: PostgREST
        // upserts them into one statement and Postgres rejects the duplicate key.
        val pushedFavoriteKeys = localFavorites.map { it.surahNumber to it.ayahNumber }.toSet()
        val tombstones = db.exportFavoriteTombstones()
            .filter { (it.surahNumber to it.ayahNumber) !in pushedFavoriteKeys }
            .map { it.copy(userId = userId) }
        push(TABLE_FAVORITES, localFavorites + tombstones, "user_id,surah_number,ayah_number")
        db.clearFavoriteTombstones(tombstones)

        pushed = localSettings.size + localProgress.size + localAyahProgress.size +
            localAchievements.size + localReviews.size + localFavorites.size + tombstones.size

        // Everything written before this instant is on the server now. The -1 ms
        // guard keeps a row written in the very same millisecond as `startedAt`
        // from being excluded from every future push.
        db.setSyncState(QuranDb.SYNC_KEY_LAST_PUSHED, startedAt - 1)
        db.setSyncState(QuranDb.SYNC_KEY_LAST_SUCCESS, Clock.System.now().toEpochMilliseconds())

        return SyncSummary(pulled = pulled, pushed = pushed)
    }

    private inline suspend fun <reified T : Any> pull(table: String, cursor: String): List<T> =
        if (cursor.isBlank()) {
            client.from(table).select().decodeList<T>()
        } else {
            client.from(table).select {
                filter { gte("server_updated_at", cursor) }
            }.decodeList<T>()
        }

    private inline suspend fun <reified T : Any> push(table: String, rows: List<T>, onConflictColumns: String) {
        if (rows.isEmpty()) return
        for (batch in rows.chunked(PUSH_BATCH_SIZE)) {
            client.from(table).upsert(batch) { onConflict = onConflictColumns }
        }
    }

    private companion object {
        const val TABLE_SETTINGS = "user_settings"
        const val TABLE_PROGRESS = "daily_progress"
        const val TABLE_AYAH_PROGRESS = "ayah_progress"
        const val TABLE_ACHIEVEMENTS = "achievements"
        const val TABLE_REVIEWS = "ayah_reviews"
        const val TABLE_FAVORITES = "favorites"
        const val PUSH_BATCH_SIZE = 500
    }
}
