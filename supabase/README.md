# Cloud sync & backup (Supabase + Google sign-in)

QuranLearner can back up learning progress, review schedule, XP, achievements,
settings and favorites to a Supabase project and sync them between devices.
Sign-in is "Sign in with Google" (native account picker on Android, browser
elsewhere). Everything is optional: builds without Supabase keys simply hide
the cloud features.

Data lives in one private table set, scoped per user with row level security —
one account can never see another account's rows. `schema.sql` must be run
once; the rest of this file is the one-time dashboard/app configuration.

---

## What gets synced

| Local data | Server table | Notes |
|---|---|---|
| Settings (reciter, fonts, reminder, XP, …) | `user_settings` | Device-only keys (downloaded surahs, export path) stay local |
| Daily progress | `daily_progress` | |
| Learned ayahs | `ayah_progress` | |
| Achievements | `achievements` | |
| Review schedule (SRS) | `ayah_reviews` | |
| Favorites | `favorites` | Deletions sync via tombstones |

Conflicts use last-write-wins per row. Sync runs automatically when the app
starts (once per account per run) and can be triggered manually from
**Settings → Sync now**.

---

## Setup

### 1. Create the database schema

Supabase dashboard → your project → **SQL Editor** → **New query** → paste the
contents of [`schema.sql`](schema.sql) → **Run**.

That creates the six tables, the `server_updated_at` trigger, indexes and the
row-level-security policies. Re-running is safe.

### 2. Google Cloud OAuth credentials

In [Google Cloud Console](https://console.cloud.google.com/):

1. Create (or pick) a project, then configure the **OAuth consent screen**
   (External is fine; add your own Google account as a test user while the app
   is unverified).
2. **Credentials → Create credentials → OAuth client ID → Web application**
   - Name: e.g. `QuranLearner (Supabase)`
   - Authorized redirect URI:
     `https://<PROJECT_REF>.supabase.co/auth/v1/callback`
     (`<PROJECT_REF>` is your project id; the exact URL is shown on the
     Supabase Google provider page, step 3.)
   - Save the **Client ID** and **Client secret**.
3. **Credentials → Create credentials → OAuth client ID → Android**
   (enables the native account picker on Android)
   - Package name: `com.dillu.quranlearner`
   - SHA-1 certificate fingerprint — get it with:

     ```bash
     JAVA_HOME=/opt/android-studio/jbr ./gradlew signingReport
     ```

     Copy the `SHA1` of the `com.dillu.quranlearner` variant you install
     (the default `debug` one during development). Add one Android client per
     signing key you use (debug keystore, release keystore, other machines).

### 3. Enable the Google provider in Supabase

Supabase dashboard → **Authentication → Providers → Google**:

- Enable the provider.
- Paste the **Web** client ID and secret from step 2.
- Under **Authorized Client IDs** add the **Android** client ID too — without
  it native sign-in fails with "Unacceptable audience in id_token".

### 4. Allow the app redirect URLs

Supabase dashboard → **Authentication → URL Configuration → Redirect URLs**,
add both:

```
quranlearner://login-callback     (Android/iOS browser fallback)
http://localhost:48210            (Desktop sign-in callback)
```

### 5. Configure the app

Append to `local.properties` in the repo root (this file is git-ignored — keys
never get committed):

```properties
supabase.url=https://<PROJECT_REF>.supabase.co
supabase.anonKey=<anon public key>
supabase.googleWebClientId=<web client id>.apps.googleusercontent.com
# optional, default is 48210 — must match the redirect URL above
supabase.desktopRedirectPort=48210
```

- **anon key**: Supabase dashboard → **Settings → API → Project API keys →
  `anon` `public`**. It is safe to ship in an app: row-level security is what
  protects the data.
- The Google **Web** client id is what the app passes to the SDK (it becomes
  the OAuth audience; the Android client id is only relevant to Google's
  servers).

Rebuild and install:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :composeApp:installDebug
```

### 6. Sign in and verify

Open **Settings → Sign In**. After signing in, the card shows your email,
"Last synced …" and a **Sync now** button. To verify the backup, check one of
the tables in the Supabase **Table Editor** — rows appear right after the
first sync.

---

## Platform notes

- **Android** uses the native Google account picker (Credential Manager); if
  Google Play services are unavailable it falls back to a browser flow that
  returns to the app through the `quranlearner://login-callback` deeplink.
- **Desktop** opens the system browser and listens for the OAuth callback on
  `http://localhost:<port>`; the port must be free and listed as a redirect
  URL.
- **iOS** uses the browser flow (`ASWebAuthenticationSession`). The native iOS
  Google sheet is not wired up; the app declares the `quranlearner://` URL
  scheme and completes the login in `iOSApp.swift`. Note that linking the iOS
  app requires the `exportedNativeBridge` Swift package from the
  [supabase-kt plugins repo](https://github.com/supabase-community/supabase-kt-plugins/tree/main/ComposeAuth)
  (its Compose Auth plugin ships a cinterop for it) — add it in Xcode via
  **File → Add Package Dependency → Add Local**, then add GoogleSignIn 9.x.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Settings shows "Not set up in this build" | `supabase.url` / `supabase.anonKey` missing from `local.properties` (rebuild after editing). |
| `redirect_uri_mismatch` from Google | Web client redirect URI must be exactly `https://<PROJECT_REF>.supabase.co/auth/v1/callback`. |
| "Unacceptable audience in id_token" on Android | Add the Android OAuth client id to the provider's **Authorized Client IDs**. |
| Sign-in opens the browser, then nothing | URL Configuration is missing `quranlearner://login-callback` (or, on Desktop, `http://localhost:48210`). |
| Sync errors with "relation … does not exist" | `schema.sql` was not run (or was run on a different project). |
| Sync errors with JWT/auth messages | Sign out and sign in again; the session may have expired. |

## Removing cloud data

Delete a row set directly from the Table Editor (they are ordinary tables), or
delete the whole account's data:

```sql
delete from public.user_settings where user_id = 'YOUR-USER-UUID';
-- …same for the other tables
```

or, to remove everything at once, delete the user in
**Authentication → Users** — the foreign keys cascade.
