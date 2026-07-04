package com.example.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Expense
import com.example.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarUiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-11
    val expenses: List<Expense> = emptyList(),
    val dailyTotals: Map<Int, Double> = emptyMap(),
    val isLoading: Boolean = false
)

sealed interface CalendarIntent {
    object Init : CalendarIntent
    data class ChangeMonth(val delta: Int) : CalendarIntent
    data class SelectDay(val day: Int) : CalendarIntent
}

sealed interface CalendarEffect {
    data class NavigateToDayDetail(val dateMillis: Long) : CalendarEffect
}

class CalendarViewModel(
    private val useCases: ExpenseUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _effect = Channel<CalendarEffect>(Channel.BUFFERED)
    val effect: Flow<CalendarEffect> = _effect.receiveAsFlow()

    private var flowJob: Job? = null

    init {
        loadMonthExpenses()
    }

    fun sendIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.Init -> {
                // Already initiated
            }
            is CalendarIntent.ChangeMonth -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, _state.value.year)
                    set(Calendar.MONTH, _state.value.month)
                    add(Calendar.MONTH, intent.delta)
                }
                _state.update {
                    it.copy(
                        year = cal.get(Calendar.YEAR),
                        month = cal.get(Calendar.MONTH)
                    )
                }
                loadMonthExpenses()
            }
            is CalendarIntent.SelectDay -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, _state.value.year)
                    set(Calendar.MONTH, _state.value.month)
                    set(Calendar.DAY_OF_MONTH, intent.day)
                    set(Calendar.HOUR_OF_DAY, 12) // Neutral time midday
                }
                viewModelScope.launch {
                    _effect.send(CalendarEffect.NavigateToDayDetail(cal.timeInMillis))
                }
            }
        }
    }

    private fun loadMonthExpenses() {
        flowJob?.cancel()
        _state.update { it.copy(isLoading = true) }

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _state.value.year)
            set(Calendar.MONTH, _state.value.month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfMonth = cal.timeInMillis

        flowJob = viewModelScope.launch {
            useCases.getExpensesByTimeRange(startOfMonth, endOfMonth)
                .collectLatest { list ->
                    val totals = list.groupBy {
                        val expenseCal = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
                        expenseCal.get(Calendar.DAY_OF_MONTH)
                    }.mapValues { entry ->
                        entry.value.sumOf { it.amount }
                    }

                    _state.update {
                        it.copy(
                            expenses = list,
                            dailyTotals = totals,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
