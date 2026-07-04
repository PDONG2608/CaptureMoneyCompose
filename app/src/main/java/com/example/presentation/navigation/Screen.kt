package com.example.presentation.navigation

sealed class Screen(val route: String) {
    object CameraHome : Screen("camera_home")
    
    object ReceiptReview : Screen("receipt_review?photoPath={photoPath}") {
        fun createRoute(photoPath: String): String {
            val encodedPath = java.net.URLEncoder.encode(photoPath, "UTF-8")
            return "receipt_review?photoPath=$encodedPath"
        }
    }
    
    object AddEditExpense : Screen("add_edit_expense?photoPath={photoPath}&expenseId={expenseId}") {
        fun createRoute(photoPath: String? = null, expenseId: Long? = null): String {
            val encodedPath = photoPath?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            val id = expenseId ?: -1L
            return "add_edit_expense?photoPath=$encodedPath&expenseId=$id"
        }
    }
    
    object Calendar : Screen("calendar")
    
    object DayDetail : Screen("day_detail/{dateTimeMillis}") {
        fun createRoute(dateTimeMillis: Long): String = "day_detail/$dateTimeMillis"
    }
    
    object History : Screen("history")
    
    object Settings : Screen("settings")
}
