package com.notbraker.tracker.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

fun Context.getOnboardingCompletedFlow(): Flow<Boolean> =
    onboardingDataStore.data.map { prefs -> prefs[ONBOARDING_COMPLETED] ?: false }

suspend fun Context.setOnboardingCompleted(completed: Boolean) {
    onboardingDataStore.edit { it[ONBOARDING_COMPLETED] = completed }
}
