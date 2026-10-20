package com.dillu.quranlearner.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dillu.quranlearner.db.QuranDb
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Progress/result of the last sync attempt, for the Settings screen. */
sealed interface CloudSyncStatus {
    data object Idle : CloudSyncStatus
    data object Syncing : CloudSyncStatus
    data class Success(val pulled: Int, val pushed: Int) : CloudSyncStatus
    data class Error(val message: String) : CloudSyncStatus
}

/**
 * Account state for the Settings screen: Google sign-in via Supabase, plus
 * one automatic sync per account per app run and a manual "Sync now".
 *
 * When the app is built without Supabase keys ([Cloud.isConfigured] is false),
 * everything stays inert and the UI shows a "not set up" hint.
 */
class CloudViewModel(private val db: QuranDb) : ViewModel() {

    private val client = Cloud.client

    val isConfigured: Boolean get() = Cloud.isConfigured

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _status = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Idle)
    val status: StateFlow<CloudSyncStatus> = _status.asStateFlow()

    private val _lastSyncedAtMs = MutableStateFlow(0L)
    val lastSyncedAtMs: StateFlow<Long> = _lastSyncedAtMs.asStateFlow()

    /** The account id already auto-synced in this app run (avoids repeat syncs). */
    private var autoSyncedForAccount: String? = null

    init {
        viewModelScope.launch {
            _lastSyncedAtMs.value = db.getSyncStateLong(QuranDb.SYNC_KEY_LAST_SUCCESS)
        }
        val activeClient = client
        if (activeClient != null) {
            viewModelScope.launch {
                activeClient.auth.sessionStatus.collect { session ->
                    when (session) {
                        is SessionStatus.Authenticated -> {
                            _isSignedIn.value = true
                            _email.value = session.session.user?.email
                            val accountId = session.session.user?.id
                            if (accountId != null && accountId != autoSyncedForAccount) {
                                autoSyncedForAccount = accountId
                                syncNow()
                            }
                        }

                        is SessionStatus.NotAuthenticated -> {
                            _isSignedIn.value = false
                            _email.value = null
                            autoSyncedForAccount = null
                            _status.value = CloudSyncStatus.Idle
                        }

                        else -> Unit // Initializing / RefreshFailure — no UI state change
                    }
                }
            }
        }
    }

    /** Kicks off a two-way sync; ignores the request while one is running. */
    fun syncNow() {
        val activeClient = client ?: return
        if (_status.value is CloudSyncStatus.Syncing) return
        // Set before launching so two rapid taps cannot start two syncs.
        _status.value = CloudSyncStatus.Syncing
        viewModelScope.launch {
            try {
                SyncManager(db, activeClient).syncNow()
                _lastSyncedAtMs.value = db.getSyncStateLong(QuranDb.SYNC_KEY_LAST_SUCCESS)
                _status.value = CloudSyncStatus.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _status.value = CloudSyncStatus.Error(t.message ?: "Sync failed")
            }
        }
    }

    fun signOut() {
        val activeClient = client ?: return
        viewModelScope.launch {
            try {
                activeClient.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _status.value = CloudSyncStatus.Error(t.message ?: "Sign out failed")
            }
        }
    }

    /** Result callback for `rememberSignInWithGoogle`; success is picked up by the session flow. */
    fun onSignInResult(result: NativeSignInResult) {
        _status.value = when (result) {
            is NativeSignInResult.Success -> CloudSyncStatus.Idle
            is NativeSignInResult.ClosedByUser -> CloudSyncStatus.Idle
            is NativeSignInResult.NetworkError -> CloudSyncStatus.Error("Sign-in failed: ${result.message}")
            is NativeSignInResult.Error -> CloudSyncStatus.Error("Sign-in failed: ${result.message}")
        }
    }

    /** "never" / "just now" / "5 min ago" / "14:32 today" / "2026-10-18" for the last successful sync. */
    fun lastSyncedLabel(nowMs: Long = Clock.System.now().toEpochMilliseconds()): String {
        val at = _lastSyncedAtMs.value
        if (at <= 0L) return "never"
        val elapsedMin = (nowMs - at) / 60_000L
        return when {
            elapsedMin < 1L -> "just now"
            elapsedMin < 60L -> "$elapsedMin min ago"
            else -> {
                val zone = TimeZone.currentSystemDefault()
                val then = Instant.fromEpochMilliseconds(at).toLocalDateTime(zone)
                val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
                when (then.date) {
                    today -> "${hhmm(then.hour, then.minute)} today"
                    today.minus(1, DateTimeUnit.DAY) -> "${hhmm(then.hour, then.minute)} yesterday"
                    else -> then.date.toString()
                }
            }
        }
    }

    private fun hhmm(hour: Int, minute: Int): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
