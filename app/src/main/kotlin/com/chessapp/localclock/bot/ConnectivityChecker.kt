package com.chessapp.localclock.bot

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the device currently has a working path to the internet. Checked fresh on every call
 * (not a cached flag) so [BotStrategy] can pick local vs. remote correctly on every single bot
 * move, including a connectivity change that happened mid-game.
 */
fun interface ConnectivityChecker {
    fun isOnline(): Boolean
}

/**
 * Backed by [ConnectivityManager]'s own "validated" signal — Android already does its own
 * captive-portal / real-connectivity detection behind [NetworkCapabilities.NET_CAPABILITY_VALIDATED],
 * so this reflects genuine internet access rather than just "connected to some network," which a
 * Wi-Fi captive portal with no real connectivity would otherwise report as available.
 */
class AndroidConnectivityChecker(context: Context) : ConnectivityChecker {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
