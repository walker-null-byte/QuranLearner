package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable

interface NotificationPermissionState {
    val hasPermission: Boolean
    fun requestPermission()
}

@Composable
expect fun rememberNotificationPermissionState(onPermissionResult: (Boolean) -> Unit = {}): NotificationPermissionState
