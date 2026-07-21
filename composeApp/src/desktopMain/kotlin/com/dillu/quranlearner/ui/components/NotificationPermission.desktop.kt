package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionState(onPermissionResult: (Boolean) -> Unit): NotificationPermissionState {
    return remember {
        object : NotificationPermissionState {
            override val hasPermission: Boolean = true
            override fun requestPermission() {
                onPermissionResult(true)
            }
        }
    }
}
