import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Completes the browser-based Google sign-in flow (quranlearner://login-callback).
                    SupabaseIosKt.handleAuthDeeplink(url: url)
                }
        }
    }
}
