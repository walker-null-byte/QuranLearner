package com.dillu.quranlearner.db

import kotlinx.serialization.Serializable

/**
 * Wire row models for cloud sync backing (Supabase/PostgREST).
 *
 * Property names are intentionally camelCase: the Supabase SDK's default
 * property conversion maps them onto the snake_case column names defined in
 * `supabase/schema.sql` (`updatedAt` -> `updated_at`, `nextReviewDate` ->
 * `next_review_date`, ...). Nullable/default fields are omitted when pushing
 * because the SDK serializes with `encodeDefaults = false`.
 *
 * Conflict rule (last-write-wins): `updatedAt` is the epoch-millisecond
 * timestamp of the edit on the device that owns it. `serverUpdatedAt` is set
 * by a Postgres trigger and only used as the incremental pull cursor — it is
 * never sent when pushing.
 *
 * [deleted] marks tombstones (only favorites are ever deleted by the app).
 */

@Serializable
data class SyncSettingRow(
    val userId: String? = null,
    val key: String,
    val value: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)

@Serializable
data class SyncProgressRow(
    val userId: String? = null,
    val date: String,
    val ayahsLearned: Int,
    val goal: Int,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)

@Serializable
data class SyncAyahProgressRow(
    val userId: String? = null,
    val surahNumber: Int,
    val ayahNumber: Int,
    val learnedAt: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)

@Serializable
data class SyncAchievementRow(
    val userId: String? = null,
    val id: String,
    val unlockedAt: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)

@Serializable
data class SyncReviewRow(
    val userId: String? = null,
    val surahNumber: Int,
    val ayahNumber: Int,
    val nextReviewDate: String,
    val intervalDays: Int,
    val easeFactor: Double,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)

@Serializable
data class SyncFavoriteRow(
    val userId: String? = null,
    val surahNumber: Int,
    val ayahNumber: Int,
    val addedAt: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val serverUpdatedAt: String? = null,
)
