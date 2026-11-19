package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clean_master_settings"
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val CONFIRM_CLEAR_DATA = booleanPreferencesKey("confirm_clear_data")
    }

    val isDarkTheme: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val confirmClearData: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_CLEAR_DATA] ?: true
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setConfirmClearData(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CONFIRM_CLEAR_DATA] = enabled
        }
    }
}
