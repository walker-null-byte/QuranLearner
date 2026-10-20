package com.dillu.quranlearner.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Shared Supabase entry point for cloud sync/backup.
 *
 * [client] is `null` when the app has not been configured with a Supabase
 * project (see `supabase/README.md`); every cloud UI path degrades gracefully
 * in that case.
 */
object Cloud {

    /** Deeplink used for OAuth callbacks on Android/iOS (must match the manifest / Info.plist). */
    const val OAUTH_SCHEME = "quranlearner"
    const val OAUTH_HOST = "login-callback"

    val isConfigured: Boolean get() = SupabaseConfig.isConfigured

    val client: SupabaseClient? by lazy { if (isConfigured) createCloudClient() else null }

    private fun createCloudClient(): SupabaseClient =
        createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = OAUTH_SCHEME
                host = OAUTH_HOST
                configurePlatformAuth()
            }
            install(Postgrest)
            install(ComposeAuth) { configureGoogleNativeLogin() }
        }
}

/** Platform hook: the desktop client configures the localhost OAuth callback server. */
internal expect fun AuthConfig.configurePlatformAuth()

/** Platform hook: Android enables native Google sign-in via Credential Manager. */
internal expect fun ComposeAuth.Config.configureGoogleNativeLogin()
