package com.caltrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.UserPreferencesStore
import com.caltrack.app.data.local.entity.MealEntity
import com.caltrack.app.data.repository.MealRepository
import com.caltrack.app.ui.components.MealItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardUiState(
    val meals: List<MealItem> = emptyList(),
    val caloriesConsumed: Int = 0,
    val proteinConsumed: Int = 0,
    val carbsConsumed: Int = 0,
    val fatConsumed: Int = 0,
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 150,
    val carbsGoal: Int = 250,
    val fatGoal: Int = 65,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val prefsStore: UserPreferencesStore
) : ViewModel() {

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Combine goals from Room (server-synced) with DataStore fallback
    private val goalsFlow = combine(
        mealRepository.getGoal(),
        prefsStore.calorieGoal,
        prefsStore.proteinGoal,
        prefsStore.carbsGoal,
        prefsStore.fatGoal
    ) { roomGoal, calPref, proPref, carbPref, fatPref ->
        GoalsState(
            calorie = roomGoal?.calorieGoal ?: calPref,
            protein = roomGoal?.proteinGoal ?: proPref,
            carbs = roomGoal?.carbsGoal ?: carbPref,
            fat = roomGoal?.fatGoal ?: fatPref
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        mealRepository.getMealsByDate(today),
        goalsFlow
    ) { meals, goals ->
        val items = meals.map { it.toMealItem() }
        DashboardUiState(
            meals = items,
            caloriesConsumed = items.sumOf { it.calories },
            proteinConsumed = items.sumOf { it.protein },
            carbsConsumed = items.sumOf { it.carbs },
            fatConsumed = items.sumOf { it.fat },
            calorieGoal = goals.calorie,
            proteinGoal = goals.protein,
            carbsGoal = goals.carbs,
            fatGoal = goals.fat,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            mealRepository.syncMealsForDate(today)
        }
    }

    fun deleteMeal(meal: MealItem) {
        viewModelScope.launch {
            val localId = meal.id.toLongOrNull() ?: return@launch
            val entity = mealRepository.getMealByLocalId(localId)
            if (entity != null) {
                mealRepository.deleteMealRemote(entity)
            } else {
                mealRepository.deleteMealById(localId)
            }
        }
    }

    private data class GoalsState(
        val calorie: Int, val protein: Int, val carbs: Int, val fat: Int
    )
}

internal fun MealEntity.toMealItem() = MealItem(
    id = id.toString(),
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    imageUri = imageUri ?: imageKey
)
