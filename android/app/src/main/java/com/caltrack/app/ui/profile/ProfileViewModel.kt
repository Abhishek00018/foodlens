package com.caltrack.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.data.remote.GoalRequest
import com.caltrack.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 150,
    val carbsGoal: Int = 250,
    val fatGoal: Int = 65,
    val allergies: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    private val _extra = MutableStateFlow(ExtraState())

    val uiState: StateFlow<ProfileUiState> = combine(
        prefsStore.userName,
        prefsStore.userEmail,
        prefsStore.calorieGoal,
        prefsStore.proteinGoal,
        prefsStore.carbsGoal
    ) { name, email, calGoal, proGoal, carbGoal ->
        ProfileUiState(
            name = name ?: "",
            email = email ?: "",
            calorieGoal = calGoal,
            proteinGoal = proGoal,
            carbsGoal = carbGoal
        )
    }.combine(
        combine(prefsStore.fatGoal, prefsStore.allergies, _extra) { fat, allergies, extra ->
            Triple(fat, allergies, extra)
        }
    ) { base, (fat, allergies, extra) ->
        base.copy(
            fatGoal = fat,
            allergies = allergies,
            isSaving = extra.isSaving,
            successMessage = extra.successMessage,
            error = extra.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    /** Fetch latest profile + goals from server and persist to DataStore. */
    fun syncFromServer() {
        viewModelScope.launch {
            userRepository.getProfile().onSuccess { profile ->
                profile.name?.let { prefsStore.saveUserName(it) }
                profile.email?.let { prefsStore.saveUserEmail(it) }
            }
            userRepository.getGoals().onSuccess { goals ->
                prefsStore.saveGoals(goals.calorieGoal, goals.proteinGoal, goals.carbsGoal, goals.fatGoal)
            }
            userRepository.getAllergies().onSuccess { resp ->
                prefsStore.saveAllergies(resp.allergies)
            }
        }
    }

    fun updateGoals(calorie: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            _extra.update { it.copy(isSaving = true, error = null) }
            val result = userRepository.updateGoals(GoalRequest(calorie, protein, carbs, fat))
            result.onSuccess { goals ->
                prefsStore.saveGoals(goals.calorieGoal, goals.proteinGoal, goals.carbsGoal, goals.fatGoal)
                _extra.update { it.copy(isSaving = false, successMessage = "Goals saved!") }
            }.onFailure { e ->
                // Save locally even if server fails
                prefsStore.saveGoals(calorie, protein, carbs, fat)
                _extra.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun updateAllergies(allergies: List<String>) {
        viewModelScope.launch {
            _extra.update { it.copy(isSaving = true, error = null) }
            val result = userRepository.updateAllergies(allergies)
            result.onSuccess {
                prefsStore.saveAllergies(allergies)
                _extra.update { it.copy(isSaving = false, successMessage = "Allergies saved!") }
            }.onFailure { e ->
                prefsStore.saveAllergies(allergies) // persist locally
                _extra.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearMessage() {
        _extra.update { it.copy(successMessage = null, error = null) }
    }

    fun logout() {
        viewModelScope.launch {
            prefsStore.clearAll()
        }
    }

    private data class ExtraState(
        val isSaving: Boolean = false,
        val successMessage: String? = null,
        val error: String? = null
    )
}
