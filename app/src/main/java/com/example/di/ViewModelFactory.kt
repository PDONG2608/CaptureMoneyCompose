package com.example.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.datastore.PreferencesManager
import com.example.domain.usecase.ExpenseUseCases
import com.example.presentation.addedit.AddEditExpenseViewModel
import com.example.presentation.calendar.CalendarViewModel
import com.example.presentation.camera.CameraHomeViewModel
import com.example.presentation.daydetail.DayDetailViewModel
import com.example.presentation.review.ReceiptReviewViewModel
import com.example.presentation.history.ExpenseHistoryViewModel
import com.example.presentation.settings.SettingsViewModel

class ViewModelFactory(
    private val useCases: ExpenseUseCases,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CameraHomeViewModel::class.java) -> {
                CameraHomeViewModel(useCases) as T
            }
            modelClass.isAssignableFrom(ReceiptReviewViewModel::class.java) -> {
                ReceiptReviewViewModel() as T
            }
            modelClass.isAssignableFrom(AddEditExpenseViewModel::class.java) -> {
                AddEditExpenseViewModel(useCases) as T
            }
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> {
                CalendarViewModel(useCases) as T
            }
            modelClass.isAssignableFrom(DayDetailViewModel::class.java) -> {
                DayDetailViewModel(useCases) as T
            }
            modelClass.isAssignableFrom(ExpenseHistoryViewModel::class.java) -> {
                ExpenseHistoryViewModel(useCases) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(preferencesManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
