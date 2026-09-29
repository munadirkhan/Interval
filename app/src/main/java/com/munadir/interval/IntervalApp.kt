package com.munadir.interval

import android.app.Application
import com.munadir.interval.billing.Billing
import com.munadir.interval.data.CardStore
import com.munadir.interval.data.Settings
import com.munadir.interval.notifications.Notifier
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class IntervalApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Settings.init(this)
        CardStore.init(this)
        CardStore.seedIfEmpty()
        Notifier.createChannel(this)

        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Config.REVENUECAT_API_KEY).build()
        )
        Billing.observe()
    }
}
