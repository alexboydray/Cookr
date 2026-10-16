# Recipe App

Android app for saving recipes from a pasted URL, planning meals for a week,
and generating a grocery list from the plan. Multi-user: recipes belong to an
account, not a device.

## Stack

| Piece | Choice |
|---|---|
| Client | Android, Kotlin + Jetpack Compose |
| Build | AGP 9.4.0 / Gradle 9.6.0 / JDK 17 |
| Kotlin | 2.4.21 (AGP 9 built-in Kotlin — no `kotlin-android` plugin) |
| Compose | BOM 2026.09.00, Material 3 |
| Backend | Supabase (Postgres + Auth + Storage) |
| Client lib | supabase-kt 3.8.0 on Ktor/OkHttp |
| minSdk / compileSdk / targetSdk | 26 / 37 / 36 |

There is deliberately no DI framework yet. The Supabase client is a single
object; Hilt can come in when there is enough to inject to justify the
KSP/version coupling.

## First-time setup

1. **Supabase project.** Create one, then run `supabase/schema.sql` in the SQL
   editor. Create a public storage bucket named `recipe-images`.
2. **Credentials.** Copy `local.properties.example` to `local.properties` and
   fill in `SUPABASE_URL` and `SUPABASE_ANON_KEY` from Settings → API.
   `local.properties` is git-ignored.
   The anon key is meant to ship in a client — Row Level Security is what
   protects the data. Never put the `service_role` key in this app.
3. **Build.**
   ```
   .\gradlew assembleDebug
   ```
   The APK lands in `app/build/outputs/apk/debug/`.

## Layout

```
app/src/main/java/com/alexray/recipeapp/
  MainActivity.kt
  data/
    SupabaseClient.kt      single client instance
    model/                 @Serializable row models
    repository/            Auth + Recipe data access
  ui/
    theme/                 Compose theme
    auth/                  sign-in / sign-up
    recipes/               recipe list + detail
    navigation/            top-level routes
supabase/
  schema.sql               tables, triggers, RLS policies
```

## Data model notes

`recipe_ingredients` stores both `raw_text` ("2 cloves garlic, minced") and a
parsed split (`quantity`, `unit`, `item`, `note`). The split is what makes
grocery aggregation possible — "2 cloves garlic" plus "1 clove garlic" has to
roll up to 3 — and `raw_text` is what gets displayed and the fallback when a
line won't parse. **Unit normalization has to happen at import time, not when
the list is generated.**

Every table has RLS on. Owned tables check `owner_id = auth.uid()`; child
tables check ownership through their parent row.

## URL import

Parsing runs server-side in a Supabase edge function, not on the device:

1. Client sends the URL.
2. Function normalizes it and checks `source_cache` — a hit returns at once.
3. Miss: fetch the page, try schema.org JSON-LD, then microdata, then an LLM
   call as fallback.
4. Result is cached in `source_cache`, shared across all accounts.
5. Client shows it for confirmation; on save, rows are written under the
   user's `owner_id`.

Server-side because the API key stays off the device, one parse serves every
user, and parser fixes ship without an app update.

The edge function is **not written yet.**

## Build order

- [x] Supabase schema
- [x] Project scaffold, toolchain pinned
- [x] Debug APK builds clean (verified 2026-10-15, ~16 MB)
- [ ] Supabase project created + schema applied
- [ ] Signup / login
- [ ] Recipe list + detail from Supabase
- [ ] URL import edge function + paste-a-URL flow
- [ ] Meal planner (week view)
- [ ] Grocery list generation

## Setup gotchas (hit while getting this to build)

Worth knowing before touching the build files — the Android toolchain moved a
lot in 2026:

- **AGP 9 has built-in Kotlin.** Applying `org.jetbrains.kotlin.android` is now
  an error with the new DSL. It is not in the plugins block on purpose.
- **The SDK DSL changed.** It is `compileSdk { version = release(37) }`, not
  `compileSdk = 37`. Same shape for `minSdk` and `targetSdk` inside
  `defaultConfig`.
- **`compileSdk` must be at least 37.** Several AndroidX artifacts declare
  `minCompileSdk 37`; the build fails at `checkDebugAarMetadata` on 36 with a
  message that reads like a suggestion but is a hard requirement.
- **SDK platforms now have minor versions** — `platforms;android-37.0`, `37.1`,
  `37.2`. `release(37)` resolves to `android-37.0`.
- **Material 3 `TopAppBar` is still experimental**, so screens using it need
  `@OptIn(ExperimentalMaterial3Api::class)`.
- **`sdkmanager` is deprecated** in favour of the new `android` CLI in
  `cmdline-tools/latest/bin`. Use `android sdk install <pkg>` and
  `android sdk list --all`. Pass `--no-metrics` to opt out of usage reporting.
- **Windows file locking** bites on rebuilds: if a Kotlin task fails with
  "Unable to delete directory ... cacheable", run `gradle --stop` and delete
  `app/build`.

## Open questions

- Re-host scraped images in Supabase Storage, or hotlink the source? Re-hosting
  costs storage but survives dead links.
- Offline reading: add a Room cache, or stay online-only for v1?
- `applicationId` is `com.alexray.recipeapp` — change it before any Play
  Store submission if you want a domain you control.
- `isMinifyEnabled` is off for release. Turning it on needs the
  kotlinx.serialization keep rules in `proguard-rules.pro` verified first.
