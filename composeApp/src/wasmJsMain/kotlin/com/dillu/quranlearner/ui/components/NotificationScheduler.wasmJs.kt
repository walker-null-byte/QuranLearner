package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** WasmJs no-op stub for [NotificationScheduler]. */
actual class NotificationScheduler {
    actual fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String) {
        // No-op on web
    }

    actual fun cancelDailyReminder() {
        // No-op on web
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler {
    return remember { NotificationScheduler() }
}
