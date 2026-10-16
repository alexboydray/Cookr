package com.alexray.recipeapp.data.repository

import com.alexray.recipeapp.data.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

class AuthRepository {

    private val auth get() = Supabase.client.auth

    /** Emits whenever the signed-in state changes, including on app start. */
    val sessionStatus: StateFlow<SessionStatus> get() = auth.sessionStatus

    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() = auth.signOut()

    fun currentUserId(): String? = auth.currentUserOrNull()?.id
}
