package com.example.domain.repository

import com.example.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun getExpenseById(id: Long): Expense?
    fun getExpensesByTimeRange(startDayMillis: Long, endDayMillis: Long): Flow<List<Expense>>
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpenseById(id: Long)
}
