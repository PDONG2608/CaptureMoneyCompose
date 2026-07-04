package com.example.presentation.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CameraHomeUiState(
    val hasCameraPermission: Boolean = false,
    val todaySpend: Double = 0.0,
    val isFlashOn: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface CameraHomeIntent {
    data class UpdatePermission(val granted: Boolean) : CameraHomeIntent
    data class ToggleFlash(val deviceToggled: Boolean) : CameraHomeIntent
    object LoadTodaySpend : CameraHomeIntent
    data class PhotoCaptured(val path: String) : CameraHomeIntent
    data class CaptureError(val message: String) : CameraHomeIntent
}

sealed interface CameraHomeEffect {
    data class NavigateToReview(val path: String) : CameraHomeEffect
    data class ShowError(val message: String) : CameraHomeEffect
}

class CameraHomeViewModel(
    private val useCases: ExpenseUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(CameraHomeUiState())
    val state: StateFlow<CameraHomeUiState> = _state.asStateFlow()

    private val _effect = Channel<CameraHomeEffect>(Channel.BUFFERED)
    val effect: Flow<CameraHomeEffect> = _effect.receiveAsFlow()

    init {
        sendIntent(CameraHomeIntent.LoadTodaySpend)
    }

    fun sendIntent(intent: CameraHomeIntent) {
        when (intent) {
            is CameraHomeIntent.UpdatePermission -> {
                _state.update { it.copy(hasCameraPermission = intent.granted) }
            }
            is CameraHomeIntent.ToggleFlash -> {
                _state.update { it.copy(isFlashOn = intent.deviceToggled) }
            }
            is CameraHomeIntent.LoadTodaySpend -> {
                loadTodaySpending()
            }
            is CameraHomeIntent.PhotoCaptured -> {
                viewModelScope.launch {
                    _effect.send(CameraHomeEffect.NavigateToReview(intent.path))
                }
            }
            is CameraHomeIntent.CaptureError -> {
                viewModelScope.launch {
                    _effect.send(CameraHomeEffect.ShowError(intent.message))
                }
            }
        }
    }

    private fun loadTodaySpending() {
        _state.update { it.copy(isLoading = true) }
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        viewModelScope.launch {
            useCases.getExpensesByTimeRange(startOfDay, endOfDay)
                .collectLatest { todayExpenses ->
                    val total = todayExpenses.sumOf { it.amount }
                    _state.update { it.copy(todaySpend = total, isLoading = false) }
                }
        }
    }
}
