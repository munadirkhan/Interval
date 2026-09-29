package com.munadir.interval.billing

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single source of truth for whether the user has paid.
 *
 * Every gated feature reads [isPro] and nothing else. Purchase checks scattered through the
 * UI are how an app ends up with one screen that thinks you paid and another that does not.
 *
 * Pro means "any active entitlement" rather than a hardcoded identifier. This app sells one
 * thing, so the two are equivalent, and this way renaming the entitlement in the RevenueCat
 * dashboard cannot silently lock a paying user out.
 */
object Billing {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** True once RevenueCat has answered, so the UI does not flash the wrong state. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun observe() {
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> apply(info) }
    }

    private fun apply(info: CustomerInfo) {
        _isPro.value = info.entitlements.active.isNotEmpty()
        _loaded.value = true
    }

    /**
     * Ties purchases to the local profile instead of the install.
     *
     * Without this, RevenueCat generates an anonymous id per install and a reinstall looks like
     * a brand-new customer. The profile id is a random UUID with no personal data in it.
     */
    suspend fun identify(profileId: String) {
        runCatching { Purchases.sharedInstance.awaitLogIn(profileId) }
            .onSuccess { apply(it.customerInfo) }
    }

    suspend fun refresh() {
        runCatching { Purchases.sharedInstance.awaitCustomerInfo() }
            .onSuccess { apply(it) }
            .onFailure { _loaded.value = true }
    }

    /** The "default" offering configured in the RevenueCat dashboard. */
    suspend fun currentOffering(): Offering? =
        runCatching { Purchases.sharedInstance.awaitOfferings().current }.getOrNull()

    /**
     * Returns null on success, or a message worth showing the user.
     *
     * Cancelling also returns null: backing out of a purchase is a normal thing to do, not
     * an error to interrupt someone with.
     */
    suspend fun purchase(activity: Activity, pkg: Package): String? {
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, pkg).build()
            )
            apply(result.customerInfo)
            null
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) null else (e.message ?: "Purchase failed.")
        } catch (e: Exception) {
            e.message ?: "Purchase failed."
        }
    }

    suspend fun restore(): String? {
        return try {
            apply(Purchases.sharedInstance.awaitRestore())
            null
        } catch (e: Exception) {
            e.message ?: "Nothing to restore."
        }
    }
}
