package com.example.presentation.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Expense
import com.example.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DayDetailUiState(
    val dateTimeMillis: Long = 0L,
    val expenses: List<Expense> = emptyList(),
    val totalSpend: Double = 0.0,
    val isLoading: Boolean = false
)

sealed interface DayDetailIntent {
    data class Init(val dateTimeMillis: Long) : DayDetailIntent
    data class DeleteExpense(val id: Long) : DayDetailIntent
}

sealed interface DayDetailEffect {
    data class ShowMessage(val message: String) : DayDetailEffect
}

class DayDetailViewModel(
    private val useCases: ExpenseUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(DayDetailUiState())
    val state: StateFlow<DayDetailUiState> = _state.asStateFlow()

    private val _effect = Channel<DayDetailEffect>(Channel.BUFFERED)
    val effect: Flow<DayDetailEffect> = _effect.receiveAsFlow()

    private var flowJob: Job? = null

    fun sendIntent(intent: DayDetailIntent) {
        when (intent) {
            is DayDetailIntent.Init -> {
                _state.update { it.copy(dateTimeMillis = intent.dateTimeMillis) }
                loadDayExpenses(intent.dateTimeMillis)
            }
            is DayDetailIntent.DeleteExpense -> {
                viewModelScope.launch {
                    try {
                        useCases.deleteExpense(intent.id)
                        _effect.send(DayDetailEffect.ShowMessage("Expense deleted successfully"))
                    } catch (e: Exception) {
                        _effect.send(DayDetailEffect.ShowMessage("Failed to delete: ${e.message}"))
                    }
                }
            }
        }
    }

    private fun loadDayExpenses(dateTimeMillis: Long) {
        flowJob?.cancel()
        _state.update { it.copy(isLoading = true) }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        flowJob = viewModelScope.launch {
            useCases.getExpensesByTimeRange(startOfDay, endOfDay)
                .collectLatest { list ->
                    val total = list.sumOf { it.amount }
                    _state.update {
                        it.copy(
                            expenses = list,
                            totalSpend = total,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
