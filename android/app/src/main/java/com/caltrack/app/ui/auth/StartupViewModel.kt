package com.caltrack.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.ui.DashboardRoute
import com.caltrack.app.ui.LoginRoute
import com.caltrack.app.ui.OnboardingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    val startDestination: StateFlow<Any?> = combine(
        prefsStore.authToken,
        prefsStore.userName
    ) { token, name ->
        when {
            token.isNullOrBlank() -> LoginRoute
            name.isNullOrBlank() -> OnboardingRoute
            else -> DashboardRoute
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
