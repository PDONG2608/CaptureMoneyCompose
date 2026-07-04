package com.example.presentation.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseCategory
import com.example.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditExpenseUiState(
    val expenseId: Long = -1L,
    val title: String = "",
    val amount: String = "",
    val category: String = ExpenseCategory.FOOD.displayName,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface AddEditExpenseIntent {
    data class Init(val photoPath: String?, val expenseId: Long) : AddEditExpenseIntent
    data class UpdateTitle(val title: String) : AddEditExpenseIntent
    data class UpdateAmount(val amount: String) : AddEditExpenseIntent
    data class UpdateCategory(val category: String) : AddEditExpenseIntent
    data class UpdateDateTime(val dateTime: Long) : AddEditExpenseIntent
    object SaveExpense : AddEditExpenseIntent
}

sealed interface AddEditExpenseEffect {
    object NavigateBack : AddEditExpenseEffect
    data class ShowError(val message: String) : AddEditExpenseEffect
    data class ShowSuccess(val message: String) : AddEditExpenseEffect
}

class AddEditExpenseViewModel(
    private val useCases: ExpenseUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditExpenseUiState())
    val state: StateFlow<AddEditExpenseUiState> = _state.asStateFlow()

    private val _effect = Channel<AddEditExpenseEffect>(Channel.BUFFERED)
    val effect: Flow<AddEditExpenseEffect> = _effect.receiveAsFlow()

    fun sendIntent(intent: AddEditExpenseIntent) {
        when (intent) {
            is AddEditExpenseIntent.Init -> {
                initialize(intent.photoPath, intent.expenseId)
            }
            is AddEditExpenseIntent.UpdateTitle -> {
                _state.update { it.copy(title = intent.title) }
            }
            is AddEditExpenseIntent.UpdateAmount -> {
                // Filter out non-numeric characters to keep numerical input clean
                val filtered = intent.amount.filter { it.isDigit() }
                _state.update { it.copy(amount = filtered) }
            }
            is AddEditExpenseIntent.UpdateCategory -> {
                _state.update { it.copy(category = intent.category) }
            }
            is AddEditExpenseIntent.UpdateDateTime -> {
                _state.update { it.copy(dateTimeMillis = intent.dateTime) }
            }
            is AddEditExpenseIntent.SaveExpense -> {
                saveExpense()
            }
        }
    }

    private fun initialize(photoPath: String?, expenseId: Long) {
        if (expenseId != -1L) {
            // Load existing expense
            _state.update { it.copy(isLoading = true, isEditing = true) }
            viewModelScope.launch {
                val expense = useCases.getExpenseById(expenseId)
                if (expense != null) {
                    _state.update {
                        it.copy(
                            expenseId = expense.id,
                            title = expense.title,
                            amount = expense.amount.toLong().toString(),
                            category = expense.category,
                            dateTimeMillis = expense.dateTimeMillis,
                            receiptImagePath = expense.receiptImagePath ?: photoPath,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, receiptImagePath = photoPath) }
                }
            }
        } else {
            // Setup new expense with optionally scanned photo path
            _state.update {
                it.copy(
                    receiptImagePath = if (photoPath.isNullOrEmpty()) null else photoPath,
                    isEditing = false
                )
            }
        }
    }

    private fun saveExpense() {
        val currentState = _state.value
        val title = currentState.title.trim()
        val amountStr = currentState.amount.trim()

        if (title.isEmpty()) {
            viewModelScope.launch {
                _effect.send(AddEditExpenseEffect.ShowError("Please enter a title"))
            }
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            viewModelScope.launch {
                _effect.send(AddEditExpenseEffect.ShowError("Please enter a valid amount"))
            }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val expense = Expense(
                id = if (currentState.isEditing) currentState.expenseId else 0,
                title = title,
                amount = amount,
                category = currentState.category,
                dateTimeMillis = currentState.dateTimeMillis,
                receiptImagePath = currentState.receiptImagePath,
                createdAt = if (currentState.isEditing) System.currentTimeMillis() else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            try {
                if (currentState.isEditing) {
                    useCases.updateExpense(expense)
                    _effect.send(AddEditExpenseEffect.ShowSuccess("Expense updated successfully"))
                } else {
                    useCases.addExpense(expense)
                    _effect.send(AddEditExpenseEffect.ShowSuccess("Expense saved successfully"))
                }
                _effect.send(AddEditExpenseEffect.NavigateBack)
            } catch (e: Exception) {
                _effect.send(AddEditExpenseEffect.ShowError("Error saving expense: ${e.message}"))
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
