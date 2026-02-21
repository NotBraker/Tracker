package com.notbraker.tracker.onboarding

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val THEME_MODE = stringPreferencesKey("theme_mode")

fun Context.getThemeModeFlow(): Flow<String> =
    themeDataStore.data.map { prefs -> prefs[THEME_MODE] ?: "system" }

suspend fun Context.setThemeMode(mode: String) {
    themeDataStore.edit { it[THEME_MODE] = mode }
}
