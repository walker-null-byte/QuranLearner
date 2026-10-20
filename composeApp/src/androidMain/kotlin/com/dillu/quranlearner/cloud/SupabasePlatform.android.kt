package com.dillu.quranlearner.cloud

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin

internal actual fun AuthConfig.configurePlatformAuth() = Unit

internal actual fun ComposeAuth.Config.configureGoogleNativeLogin() {
    // Native Google sign-in (Credential Manager) needs the *web* OAuth client id.
    if (SupabaseConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
        googleNativeLogin(serverClientId = SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
    }
}
