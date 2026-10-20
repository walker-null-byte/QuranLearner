package com.dillu.quranlearner.cloud

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.compose.auth.ComposeAuth

internal actual fun AuthConfig.configurePlatformAuth() = Unit

// iOS uses the browser (ASWebAuthenticationSession) OAuth fallback. The native
// GoogleSignIn bridge needs extra Xcode setup; see supabase/README.md.
internal actual fun ComposeAuth.Config.configureGoogleNativeLogin() = Unit
