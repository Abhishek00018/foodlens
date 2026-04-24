package com.caltrack.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.data.remote.GoalRequest
import com.caltrack.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val ALLERGEN_LIST = listOf(
    "Lactose", "Gluten", "Peanuts", "Tree Nuts", "Shellfish",
    "Soy", "Eggs", "Fish", "Sesame", "Wheat"
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefsStore: UserPreferencesStore,
    private val userRepository: UserRepository
) : ViewModel() {

    data class OnboardingState(
        val currentPage: Int = 0,
        val name: String = "",
        val calorieGoal: Int = 2000,
        val proteinGoal: Int = 150,
        val carbsGoal: Int = 250,
        val fatGoal: Int = 65,
        val selectedAllergies: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val isComplete: Boolean = false
    )

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun setCalorieGoal(cal: Int) = _state.update { it.copy(calorieGoal = cal) }
    fun setProteinGoal(p: Int) = _state.update { it.copy(proteinGoal = p) }
    fun setCarbsGoal(c: Int) = _state.update { it.copy(carbsGoal = c) }
    fun setFatGoal(f: Int) = _state.update { it.copy(fatGoal = f) }

    fun toggleAllergen(allergen: String) = _state.update { current ->
        val updated = if (allergen in current.selectedAllergies) {
            current.selectedAllergies - allergen
        } else {
            current.selectedAllergies + allergen
        }
        current.copy(selectedAllergies = updated)
    }

    fun nextPage() = _state.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(2)) }
    fun prevPage() = _state.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }

    fun complete() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val s = _state.value
            prefsStore.saveUserName(s.name)
            prefsStore.saveGoals(s.calorieGoal, s.proteinGoal, s.carbsGoal, s.fatGoal)
            prefsStore.saveAllergies(s.selectedAllergies.toList())

            // Best-effort API sync — don't fail onboarding on network error
            runCatching {
                userRepository.updateGoals(
                    GoalRequest(
                        calorieGoal = s.calorieGoal,
                        proteinGoal = s.proteinGoal,
                        carbsGoal = s.carbsGoal,
                        fatGoal = s.fatGoal
                    )
                )
                userRepository.updateAllergies(s.selectedAllergies.toList())
            }

            _state.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
}
