package com.dillu.quranlearner.cloud

import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

/**
 * Completes the browser-based Google sign-in flow on iOS when the system opens
 * the `quranlearner://login-callback` URL. Wire this from SwiftUI (see
 * `iosApp/iosApp/iOSApp.swift`) via `.onOpenURL { SupabaseIosKt.handleAuthDeeplink(url: $0) }`.
 *
 * Note: the native Google sheet is not wired on iOS; the Supabase SDK falls
 * back to browser-based OAuth there (ASWebAuthenticationSession).
 */
fun handleAuthDeeplink(url: NSURL) {
    if (url.scheme != Cloud.OAUTH_SCHEME) return
    Cloud.client?.handleDeeplinks(url)
}
