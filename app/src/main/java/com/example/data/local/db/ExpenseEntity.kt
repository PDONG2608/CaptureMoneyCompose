package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dateTimeMillis: Long,
    val receiptImagePath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
