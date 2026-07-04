package com.example.data.repository

import com.example.data.local.db.ExpenseDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Expense
import com.example.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)?.toDomain()
    }

    override fun getExpensesByTimeRange(startDayMillis: Long, endDayMillis: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesByTimeRange(startDayMillis, endDayMillis).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }
}
