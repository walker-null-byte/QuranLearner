package com.dillu.quranlearner.ui.components

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import java.util.Calendar

/**
 * Android implementation of [NotificationScheduler].
 *
 * Uses [AlarmManager] for repeating daily alarms and
 * [NotificationCompat] for building the notification.
 */
actual class NotificationScheduler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "noor_daily_reminder"
        const val CHANNEL_NAME = "Daily Reminders"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE = 2001
        const val EXTRA_TITLE = "noor_title"
        const val EXTRA_BODY = "noor_body"
        const val EXTRA_HOUR = "noor_hour"
        const val EXTRA_MINUTE = "noor_minute"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily Quran learning reminders"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    actual fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NoorReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the first trigger — next occurrence of the requested time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    actual fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NoorReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

/**
 * BroadcastReceiver that fires the local notification when the alarm triggers.
 */
class NoorReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE) ?: "Time to Learn"
        val body = intent.getStringExtra(NotificationScheduler.EXTRA_BODY)
            ?: "Continue your Quran journey — even one ayah a day keeps the streak alive! 🌙"

        val mainIntent = Intent(context, com.dillu.quranlearner.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon) // Use app icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Use HIGH priority so it shows immediately
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationScheduler.NOTIFICATION_ID, notification)

        // Reschedule for next day
        val hour = intent.getIntExtra(NotificationScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTE, -1)
        if (hour != -1 && minute != -1) {
            val scheduler = NotificationScheduler(context)
            scheduler.scheduleDailyReminder(hour, minute, title, body)
        }
    }
}

@Composable
actual fun rememberNotificationScheduler(): NotificationScheduler {
    val context = LocalContext.current
    return remember { NotificationScheduler(context.applicationContext) }
}
