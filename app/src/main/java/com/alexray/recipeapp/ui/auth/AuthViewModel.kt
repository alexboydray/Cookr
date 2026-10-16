package com.alexray.recipeapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexray.recipeapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val canSubmit: Boolean
        get() = !busy && email.contains("@") && password.length >= 6
}

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun toggleMode() = _state.update {
        it.copy(isSignUp = !it.isSignUp, error = null, notice = null)
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                if (s.isSignUp) {
                    repo.signUp(s.email, s.password)
                    _state.update {
                        it.copy(
                            busy = false,
                            notice = "Account created. Check your email if confirmation is on.",
                        )
                    }
                } else {
                    repo.signIn(s.email, s.password)
                    _state.update { it.copy(busy = false) }
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(busy = false, error = t.message ?: "Something went wrong")
                }
            }
        }
    }
}
