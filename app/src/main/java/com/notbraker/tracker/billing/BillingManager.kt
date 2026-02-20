package com.notbraker.tracker.billing

import kotlinx.coroutines.flow.StateFlow

data class BillingResult(
    val success: Boolean,
    val message: String
)

interface BillingManager {
    val isPremium: StateFlow<Boolean>
    suspend fun startPurchaseFlow(): BillingResult
    suspend fun restorePurchases(): BillingResult
    suspend fun setPremiumOverride(enabled: Boolean): BillingResult
}
