package com.example.data.mapper

import com.example.data.local.db.ExpenseEntity
import com.example.domain.model.Expense

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        dateTimeMillis = dateTimeMillis,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        title = title,
        amount = amount,
        category = category,
        dateTimeMillis = dateTimeMillis,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
