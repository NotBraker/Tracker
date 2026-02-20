package com.notbraker.tracker.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.billingDataStore by preferencesDataStore(name = "billing_prefs")

class FakeBillingManager(private val context: Context) : BillingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val premiumKey = booleanPreferencesKey("premium_enabled")
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        scope.launch {
            context.billingDataStore.data
                .map { prefs -> prefs[premiumKey] ?: false }
                .collect { value -> _isPremium.value = value }
        }
    }

    override suspend fun startPurchaseFlow(): BillingResult {
        setPremium(true)
        return BillingResult(success = true, message = "Premium unlocked.")
    }

    override suspend fun restorePurchases(): BillingResult {
        return BillingResult(success = true, message = "Purchases restored.")
    }

    override suspend fun setPremiumOverride(enabled: Boolean): BillingResult {
        setPremium(enabled)
        return BillingResult(
            success = true,
            message = if (enabled) "Premium enabled." else "Premium disabled."
        )
    }

    private suspend fun setPremium(enabled: Boolean) {
        context.billingDataStore.edit { prefs ->
            prefs[premiumKey] = enabled
        }
    }
}
