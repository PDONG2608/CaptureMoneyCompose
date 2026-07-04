package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snapspend_settings")

class PreferencesManager(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[darkThemeKey] ?: false
        }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkThemeKey] = isDark
        }
    }
}
