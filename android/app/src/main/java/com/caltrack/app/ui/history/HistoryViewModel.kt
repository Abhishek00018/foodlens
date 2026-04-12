package com.caltrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.repository.MealRepository
import com.caltrack.app.ui.components.DayCalorie
import com.caltrack.app.ui.components.MealItem
import com.caltrack.app.ui.dashboard.toMealItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val mealsByDate: Map<String, List<MealItem>> = emptyMap(),
    val weeklyCalories: List<DayCalorie> = emptyList(),
    val calorieGoal: Int = 2000,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    // Load 7-day window ending today
    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6)
    private val startDate = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val endDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val uiState: StateFlow<HistoryUiState> = combine(
        mealRepository.getMealsForDateRange(startDate, endDate),
        _isRefreshing
    ) { meals, isRefreshing ->
        // Group meals by date label
        val grouped = meals
            .groupBy { it.date }
            .mapKeys { (date, _) -> formatDateLabel(date) }
            .mapValues { (_, entities) -> entities.map { it.toMealItem() } }

        // Build 7-day calorie bars — fill gaps with 0
        val calorieMap = meals.groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.calories } }
        val weeklyCalories = (0..6).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .take(3)
            DayCalorie(label, calorieMap[date.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: 0)
        }.reversed()

        HistoryUiState(
            mealsByDate = grouped,
            weeklyCalories = weeklyCalories,
            isLoading = false,
            isRefreshing = isRefreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            // Sync each day in range from server
            var current = weekStart
            while (!current.isAfter(today)) {
                mealRepository.syncMealsForDate(current.format(DateTimeFormatter.ISO_LOCAL_DATE))
                current = current.plusDays(1)
            }
            _isRefreshing.update { false }
        }
    }

    private fun formatDateLabel(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            }
        } catch (e: Exception) {
            isoDate
        }
    }
}
