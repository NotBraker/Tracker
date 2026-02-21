package com.notbraker.tracker.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult as PlayResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val Context.billingStateStore by preferencesDataStore(name = "billing_state")

class PlayBillingManager(
    private val context: Context
) : BillingManager, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val premiumKey = booleanPreferencesKey("premium_enabled")
    private val debugPremiumKey = booleanPreferencesKey("debug_premium_enabled")
    private val _billingState = MutableStateFlow(BillingUiState(isLoading = true))
    override val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private var isConnected = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connectIfNeeded()
        scope.launch { syncStateFromStore() }
    }

    override suspend fun refreshEntitlements() {
        connectIfNeeded()
        if (!isConnected) {
            _billingState.value = _billingState.value.copy(isLoading = false)
            return
        }
        queryProducts()
        queryPurchasesAndPersist()
        syncStateFromStore()
    }

    override fun startPurchaseFlow(activity: Activity, plan: SubscriptionPlan): BillingResult {
        if (!isConnected) {
            return BillingResult(false, "Billing service unavailable.")
        }
        val details = productDetailsById[plan.productId]
            ?: return BillingResult(false, "Product unavailable for ${plan.name.lowercase()}.")

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                return BillingResult(false, "Subscription offer unavailable.")
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            BillingResult(true, "Purchase flow started.")
        } else {
            BillingResult(false, result.debugMessage.ifBlank { "Unable to start purchase flow." })
        }
    }

    override suspend fun restorePurchases(): BillingResult {
        return if (!isConnected) {
            BillingResult(false, "Billing service unavailable.")
        } else {
            queryPurchasesAndPersist()
            syncStateFromStore()
            BillingResult(true, "Purchases restored.")
        }
    }

    override suspend fun clearPremiumForReset(): BillingResult {
        context.billingStateStore.edit { prefs ->
            prefs[debugPremiumKey] = false
        }
        syncStateFromStore()
        return BillingResult(success = true, message = "Premium cleared for reset.")
    }

    override suspend fun setDebugPremiumOverride(enabled: Boolean): BillingResult {
        context.billingStateStore.edit { prefs ->
            prefs[debugPremiumKey] = enabled
            if (enabled) {
                prefs[premiumKey] = true
            }
        }
        syncStateFromStore()
        return BillingResult(
            success = true,
            message = if (enabled) "Debug premium enabled." else "Debug premium disabled."
        )
    }

    override fun onPurchasesUpdated(result: PlayResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            return
        }
        scope.launch {
            purchases.forEach { purchase ->
                acknowledgeIfNeeded(purchase)
            }
            queryPurchasesAndPersist()
            syncStateFromStore()
        }
    }

    private fun connectIfNeeded() {
        if (isConnected || billingClient.isReady) {
            isConnected = true
            return
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: PlayResult) {
                    isConnected = result.responseCode == BillingClient.BillingResponseCode.OK
                    scope.launch {
                        if (isConnected) {
                            queryProducts()
                            queryPurchasesAndPersist()
                            syncStateFromStore()
                        } else {
                            _billingState.value = _billingState.value.copy(isLoading = false)
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            }
        )
    }

    private suspend fun queryProducts() {
        val subscriptionProducts = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    SubscriptionPlan.MONTHLY.productId,
                    SubscriptionPlan.YEARLY.productId
                ).map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val inAppProducts = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SubscriptionPlan.LIFETIME.productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val subs = queryProductDetails(subscriptionProducts)
        val inApps = queryProductDetails(inAppProducts)
        synchronized(productDetailsById) {
            (subs + inApps).forEach { product -> productDetailsById[product.productId] = product }
        }
    }

    private suspend fun queryPurchasesAndPersist() {
        val subscriptionPurchases = queryPurchasesByType(BillingClient.ProductType.SUBS)
        val inAppPurchases = queryPurchasesByType(BillingClient.ProductType.INAPP)
        val allPurchases = subscriptionPurchases + inAppPurchases
        allPurchases.forEach { purchase -> acknowledgeIfNeeded(purchase) }

        val hasActivePurchase = allPurchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        context.billingStateStore.edit { prefs ->
            prefs[premiumKey] = hasActivePurchase
        }
    }

    private suspend fun queryPurchasesByType(productType: String): List<Purchase> {
        val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { result, list ->
                if (!continuation.isActive) return@queryPurchasesAsync
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(list)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
    }

    private suspend fun queryProductDetails(params: QueryProductDetailsParams): List<ProductDetails> {
        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { result, list ->
                if (!continuation.isActive) return@queryProductDetailsAsync
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(list)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { }
    }

    private suspend fun syncStateFromStore() {
        val prefs = context.billingStateStore.data.first()
        val purchased = prefs[premiumKey] ?: false
        val debugEnabled = prefs[debugPremiumKey] ?: false
        val premium = purchased || debugEnabled
        _billingState.value = BillingUiState(
            isPremium = premium,
            isLoading = false,
            activePlan = if (premium) SubscriptionPlan.YEARLY else null
        )
    }
}
