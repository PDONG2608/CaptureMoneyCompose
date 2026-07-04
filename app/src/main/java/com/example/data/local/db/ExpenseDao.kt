package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateTimeMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE dateTimeMillis >= :startDayMillis AND dateTimeMillis <= :endDayMillis ORDER BY dateTimeMillis DESC")
    fun getExpensesByTimeRange(startDayMillis: Long, endDayMillis: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}
