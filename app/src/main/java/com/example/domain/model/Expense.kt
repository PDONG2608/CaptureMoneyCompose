package com.example.domain.model

data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dateTimeMillis: Long,
    val receiptImagePath: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Dining"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills & Utilities"),
    OTHERS("Others");

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { 
                it.displayName.equals(value, ignoreCase = true) || 
                it.name.equals(value, ignoreCase = true) 
            } ?: OTHERS
        }
    }
}
