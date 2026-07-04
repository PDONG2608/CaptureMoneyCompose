package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.datastore.PreferencesManager
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.ExpenseDao
import com.example.data.repository.ExpenseRepositoryImpl
import com.example.domain.repository.ExpenseRepository
import com.example.domain.usecase.*

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "snapspend_db"
        ).fallbackToDestructiveMigration().build()
    }

    val expenseDao: ExpenseDao by lazy {
        database.expenseDao()
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(expenseDao)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    val expenseUseCases: ExpenseUseCases by lazy {
        ExpenseUseCases(
            addExpense = AddExpenseUseCase(expenseRepository),
            getExpenseById = GetExpenseByIdUseCase(expenseRepository),
            getAllExpenses = GetAllExpensesUseCase(expenseRepository),
            getExpensesByTimeRange = GetExpensesByTimeRangeUseCase(expenseRepository),
            updateExpense = UpdateExpenseUseCase(expenseRepository),
            deleteExpense = DeleteExpenseUseCase(expenseRepository)
        )
    }
}
