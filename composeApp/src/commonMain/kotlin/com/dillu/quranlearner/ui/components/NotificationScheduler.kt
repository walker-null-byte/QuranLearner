package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable

/**
 * Cross-platform notification scheduler for daily reminders.
 *
 * Platforms implement the scheduling via:
 * - Android: AlarmManager + NotificationCompat
 * - iOS: UNUserNotificationCenter
 * - Desktop/Wasm: No-op stubs
 */
expect class NotificationScheduler {
    /**
     * Schedule a daily notification at the given hour and minute.
     * Replaces any previously scheduled reminder.
     *
     * @param hour   Hour of day (0–23)
     * @param minute Minute of hour (0–59)
     * @param title  Notification title
     * @param body   Notification body text
     */
    fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String)

    /** Cancel the previously scheduled daily reminder. */
    fun cancelDailyReminder()
}

/**
 * Platform-aware Composable factory that remembers a [NotificationScheduler]
 * scoped to the current composition.
 */
@Composable
expect fun rememberNotificationScheduler(): NotificationScheduler
