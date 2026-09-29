package com.munadir.interval

/**
 * App-wide constants.
 *
 * REVENUECAT_API_KEY is a RevenueCat **Test Store** key. It can only ever produce
 * simulated purchases -- no real payment method is involved and no money moves -- so
 * it is safe to commit and lets anyone clone this repo and exercise the paywall
 * immediately. A production build would read a platform key from local.properties instead.
 */
object Config {
    const val REVENUECAT_API_KEY = "test_yKkXiGIsdpVLlqDgGrdfdKZZujC"

    /** Cards a free user may hold at once. Past this, the paywall appears. */
    const val FREE_CARD_LIMIT = 5
}
