package com.caltrack.app.ui.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object SignedIn : AuthUiState()
    data class NeedsConfirmation(val email: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
