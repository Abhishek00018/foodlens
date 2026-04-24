package com.caltrack.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isDevMode: Boolean = authRepository.isDevMode()

    init {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                _uiState.value = AuthUiState.SignedIn
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email, password)
                .onSuccess { _uiState.value = AuthUiState.SignedIn }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign in failed") }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUp(email, password, name)
                .onSuccess { _uiState.value = AuthUiState.NeedsConfirmation(email) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sign up failed") }
        }
    }

    fun confirmSignUp(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.confirmSignUp(email, code)
                .onSuccess { _uiState.value = AuthUiState.SignedIn }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Confirmation failed") }
        }
    }

    fun skipLogin() {
        if (!isDevMode) return
        viewModelScope.launch {
            prefsStore.saveUserName("Dev User")
            prefsStore.saveTokens("dev-token", "dev-access", "dev-refresh")
            _uiState.value = AuthUiState.SignedIn
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
