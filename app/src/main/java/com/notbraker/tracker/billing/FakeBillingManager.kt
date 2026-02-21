package com.notbraker.tracker.billing

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
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
    private val _billingState = MutableStateFlow(BillingUiState())
    override val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    init {
        scope.launch {
            context.billingDataStore.data
                .map { prefs -> prefs[premiumKey] ?: false }
                .collect { premium ->
                    _billingState.value = _billingState.value.copy(
                        isPremium = premium,
                        activePlan = if (premium) SubscriptionPlan.YEARLY else null
                    )
                }
        }
    }

    override suspend fun refreshEntitlements() {
        // State is continuously mirrored from DataStore.
    }

    override fun startPurchaseFlow(activity: Activity, plan: SubscriptionPlan): BillingResult {
        return if (!isDebuggable()) {
            BillingResult(success = false, message = "Debug purchase adapter disabled in release builds.")
        } else {
            scope.launch { setPremium(true) }
            BillingResult(success = true, message = "Debug premium enabled for ${plan.name.lowercase()}.")
        }
    }

    override suspend fun restorePurchases(): BillingResult {
        return BillingResult(success = true, message = "Debug restore complete.")
    }

    override suspend fun setDebugPremiumOverride(enabled: Boolean): BillingResult {
        if (!isDebuggable()) {
            return BillingResult(success = false, message = "Debug override unavailable.")
        }
        setPremium(enabled)
        _billingState.value = _billingState.value.copy(
            isPremium = enabled,
            activePlan = if (enabled) SubscriptionPlan.YEARLY else null
        )
        return BillingResult(success = true, message = if (enabled) "Debug premium enabled." else "Debug premium disabled.")
    }

    override suspend fun clearPremiumForReset(): BillingResult {
        setPremium(false)
        _billingState.value = _billingState.value.copy(isPremium = false, activePlan = null)
        return BillingResult(success = true, message = "Premium cleared for reset.")
    }

    private suspend fun setPremium(enabled: Boolean) {
        context.billingDataStore.edit { prefs ->
            prefs[premiumKey] = enabled
        }
    }

    private fun isDebuggable(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
