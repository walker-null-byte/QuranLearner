package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Desktop no-op stub for [NotificationScheduler]. */
actual class NotificationScheduler {
    actual fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String) {
        // No-op on desktop
    }

    actual fun cancelDailyReminder() {
        // No-op on desktop
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler {
    return remember { NotificationScheduler() }
}
