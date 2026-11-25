<p align="center">
  <img src="docs/banner.svg" alt="Cookr" width="100%">
</p>

<p align="center">
  <img alt="Status: in development" src="https://img.shields.io/badge/Status-In_development-F08A3C?style=for-the-badge">
  <img alt="Android 8 or newer" src="https://img.shields.io/badge/Android-8%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
</p>

---

### Tired of scrolling past someone's life story to get to the recipe, then writing the grocery list by hand?

**That's what Cookr is for.** Paste a recipe's link and Cookr keeps just the recipe: the ingredients and the steps. Plan the week's meals from your saved recipes, and Cookr adds up everything you need into one grocery list ("2 cloves garlic" plus "1 clove garlic" becomes 3).

> [!IMPORTANT]
> Cookr is still being built and there's no download yet. The list below is what it's for; the progress list further down shows what's done.

## ✨ What it does

| | |
|---|---|
| 🔗 **Save from a link** | Paste a recipe's address and Cookr pulls out the ingredients and steps. |
| 📅 **Plan the week** | Put recipes on the days you'll cook them. |
| 🛒 **One grocery list** | Every ingredient for the week, added up and in one place. |
| 👤 **Your account, any device** | Recipes belong to your account, not your phone, so they follow you. |
| 🔒 **Private by default** | Only you can see your recipes and plans. |

## 🚧 Progress

- [x] Database design
- [x] App set up, and a test build that installs
- [ ] Sign up and log in
- [ ] Recipe list and recipe page
- [ ] Save a recipe from a link
- [ ] Meal planner (week view)
- [ ] Grocery list

## ❓ FAQ

<details>
<summary><b>When can I try it?</b></summary>

Once sign-in, recipes and link import work, a download will appear on this page. Until then there's nothing to install.
</details>

<details>
<summary><b>Will it work with any recipe site?</b></summary>

Most recipe sites describe their recipes in a standard format that Cookr can read. For the ones that don't, it has a fallback that reads the page itself. Each link is read once and shared, so popular recipes import instantly.
</details>

## 🛠️ For developers

<details>
<summary>How it's built, and how to build it</summary>

- Android app in Kotlin and Jetpack Compose (Material 3), with Supabase (Postgres, Auth and Storage) as the backend through supabase-kt.
- `supabase/schema.sql` holds the tables, triggers and Row Level Security rules: every table checks that the row belongs to the signed-in user.
- Recipe links are to be read by a Supabase edge function, not on the phone: it tries the page's schema.org JSON-LD, then microdata, then an AI fallback, and caches the result for every account. (Not written yet.)
- Ingredients keep both the original line and a split into quantity, unit, item and note; the split is what lets the grocery list add things up.

**Build it**

1. Create a Supabase project, run `supabase/schema.sql` in its SQL editor, and add a public storage bucket named `recipe-images`.
2. Copy `local.properties.example` to `local.properties` and fill in `SUPABASE_URL` and `SUPABASE_ANON_KEY` (Settings → API). Never put the `service_role` key in the app.
3. Run `.\gradlew assembleDebug`. The APK lands in `app/build/outputs/apk/debug/`.

**Toolchain notes:** AGP 9 has Kotlin built in (no `kotlin-android` plugin), the SDK is set with `compileSdk { version = release(37) }`, and `compileSdk` must be at least 37 because several AndroidX libraries require it. On Windows, if a rebuild fails with "Unable to delete directory", run `gradle --stop` and delete `app/build`.
</details>
