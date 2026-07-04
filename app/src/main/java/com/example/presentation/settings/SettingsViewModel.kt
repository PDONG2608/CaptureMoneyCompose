package com.example.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.datastore.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = preferencesManager.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkTheme(isDark)
        }
    }
}
