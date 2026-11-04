package com.alexray.cookr.data

import com.alexray.cookr.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * One client for the whole app.
 *
 * No DI framework yet on purpose — there is not enough to inject to justify
 * the KSP/version coupling Hilt brings. Swap this for a Hilt module when
 * there is.
 *
 * The anon key is meant to ship in a client; Row Level Security is what
 * actually protects the data.
 */
object Supabase {

    val configured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
