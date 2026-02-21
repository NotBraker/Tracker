package com.notbraker.tracker.data.template

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.templateDataStore by preferencesDataStore(name = "template_prefs")
private val HIDDEN_TEMPLATE_IDS = stringSetPreferencesKey("hidden_template_ids")

fun Context.getHiddenTemplateIdsFlow(): Flow<Set<String>> =
    templateDataStore.data.map { prefs -> prefs[HIDDEN_TEMPLATE_IDS] ?: emptySet() }

suspend fun Context.addHiddenTemplateId(templateId: String) {
    templateDataStore.edit { prefs ->
        val current = prefs[HIDDEN_TEMPLATE_IDS] ?: emptySet()
        prefs[HIDDEN_TEMPLATE_IDS] = current + templateId
    }
}

suspend fun Context.removeHiddenTemplateId(templateId: String) {
    templateDataStore.edit { prefs ->
        val current = prefs[HIDDEN_TEMPLATE_IDS] ?: emptySet()
        prefs[HIDDEN_TEMPLATE_IDS] = current - templateId
    }
}

suspend fun Context.clearHiddenTemplateIds() {
    templateDataStore.edit { it.remove(HIDDEN_TEMPLATE_IDS) }
}
