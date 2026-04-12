package com.caltrack.app.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caltrack.app.data.local.entity.MealEntity
import com.caltrack.app.data.repository.MealRepository
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
import javax.inject.Inject

data class MealDetailUiState(
    val meal: MealItem? = null,
    val allergenWarnings: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MealDetailViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // mealId is passed as the route argument (local Room id as String)
    private val mealId: Long = savedStateHandle.get<String>("mealId")?.toLongOrNull() ?: 0L

    private val _extra = MutableStateFlow(ExtraState())

    val uiState: StateFlow<MealDetailUiState> = combine(
        mealRepository.getMealById(mealId),
        _extra
    ) { entity, extra ->
        if (entity == null) {
            MealDetailUiState(isLoading = false, isDeleted = extra.isDeleted)
        } else {
            MealDetailUiState(
                meal = entity.toMealItem(),
                allergenWarnings = entity.allergenWarnings
                    .split(",")
                    .filter { it.isNotBlank() },
                isLoading = false,
                isDeleting = extra.isDeleting,
                isDeleted = extra.isDeleted,
                error = extra.error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealDetailUiState()
    )

    fun deleteMeal() {
        viewModelScope.launch {
            _extra.update { it.copy(isDeleting = true, error = null) }
            val entity = mealRepository.getMealByLocalId(mealId)
            if (entity != null) {
                val result = mealRepository.deleteMealRemote(entity)
                result.onSuccess {
                    _extra.update { it.copy(isDeleting = false, isDeleted = true) }
                }.onFailure { e ->
                    _extra.update { it.copy(isDeleting = false, error = e.message) }
                }
            } else {
                _extra.update { it.copy(isDeleting = false, isDeleted = true) }
            }
        }
    }

    private data class ExtraState(
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val error: String? = null
    )
}
