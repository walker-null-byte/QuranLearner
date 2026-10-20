package com.dillu.quranlearner.cloud

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.compose.auth.ComposeAuth

internal actual fun AuthConfig.configurePlatformAuth() {
    // OAuth on desktop runs through the SDK's built-in localhost callback server;
    // pin the port so it can be allow-listed in the Supabase dashboard.
    httpCallbackConfig { httpPort = SupabaseConfig.DESKTOP_REDIRECT_PORT }
}

internal actual fun ComposeAuth.Config.configureGoogleNativeLogin() = Unit
