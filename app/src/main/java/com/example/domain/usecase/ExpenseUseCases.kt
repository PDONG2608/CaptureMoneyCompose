package com.example.domain.usecase

import com.example.domain.model.Expense
import com.example.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class AddExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense): Long = repository.insertExpense(expense)
}

class GetExpenseByIdUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long): Expense? = repository.getExpenseById(id)
}

class GetAllExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Expense>> = repository.getAllExpenses()
}

class GetExpensesByTimeRangeUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(startDayMillis: Long, endDayMillis: Long): Flow<List<Expense>> {
        return repository.getExpensesByTimeRange(startDayMillis, endDayMillis)
    }
}

class UpdateExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense) = repository.updateExpense(expense)
}

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteExpenseById(id)
}

data class ExpenseUseCases(
    val addExpense: AddExpenseUseCase,
    val getExpenseById: GetExpenseByIdUseCase,
    val getAllExpenses: GetAllExpensesUseCase,
    val getExpensesByTimeRange: GetExpensesByTimeRangeUseCase,
    val updateExpense: UpdateExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase
)
