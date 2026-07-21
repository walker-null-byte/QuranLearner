package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UserNotifications.*
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents

/**
 * iOS implementation of [NotificationScheduler].
 *
 * Uses UNUserNotificationCenter to schedule a daily repeating trigger.
 */
actual class NotificationScheduler {

    companion object {
        const val REMINDER_ID = "noor_daily_reminder"
    }

    actual fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Request permission (no-ops silently if already granted)
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { _, _ -> }

        // Build the notification content
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
        }

        // Daily trigger at the specified time
        val dateComponents = NSDateComponents().apply {
            setHour(hour.toLong())
            setMinute(minute.toLong())
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents,
            repeats = true,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            REMINDER_ID,
            content,
            trigger,
        )
        center.addNotificationRequest(request) { _ -> }
    }

    actual fun cancelDailyReminder() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_ID))
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler {
    return remember { NotificationScheduler() }
}
