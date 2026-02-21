package com.notbraker.tracker.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

data class BillingResult(
    val success: Boolean,
    val message: String
)

enum class SubscriptionPlan(val productId: String) {
    MONTHLY("tracker_pro_monthly"),
    YEARLY("tracker_pro_yearly"),
    LIFETIME("tracker_pro_lifetime")
}

data class BillingUiState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val activePlan: SubscriptionPlan? = null,
    val availablePlans: List<SubscriptionPlan> = SubscriptionPlan.entries
)

interface BillingManager {
    val billingState: StateFlow<BillingUiState>
    suspend fun refreshEntitlements()
    fun startPurchaseFlow(activity: Activity, plan: SubscriptionPlan): BillingResult
    suspend fun restorePurchases(): BillingResult
    suspend fun setDebugPremiumOverride(enabled: Boolean): BillingResult
    suspend fun clearPremiumForReset(): BillingResult
}
