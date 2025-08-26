package com.example.zamankumandasi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.MobileAds

object AdManager {
    private const val TAG = "AdManager"
    // Google test interstitial ad unit ID. Replace with your own in prod.
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var lastShownAtMs: Long = 0
    private var loadInProgress = false
    @Volatile private var isPremiumUser: Boolean = false

    // Frequency cap: at most once every 60 seconds
    private const val MIN_INTERVAL_MS = 60_000L

    fun init(context: Context) {
        // Safe to call multiple times
        MobileAds.initialize(context)
        preload(context)
    }

    fun setPremium(enabled: Boolean) {
        isPremiumUser = enabled
        if (enabled) {
            // Drop any loaded ad to prevent accidental display
            interstitialAd = null
        }
    }

    fun preload(context: Context) {
    if (isPremiumUser) return
        if (loadInProgress || interstitialAd != null) return
        loadInProgress = true
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadInProgress = false
                    Log.d(TAG, "Interstitial loaded")
                    setFullScreenCallbacks()
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    Log.w(TAG, "Failed to load interstitial: ${error.message}")
                    interstitialAd = null
                    loadInProgress = false
                }
            }
        )
    }

    private fun setFullScreenCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Failed to show interstitial: ${adError.message}")
                interstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown")
                lastShownAtMs = System.currentTimeMillis()
                interstitialAd = null
            }
        }
    }

    fun maybeShowInterstitial(activity: Activity, force: Boolean = false) {
    if (isPremiumUser) return
        val now = System.currentTimeMillis()
        if (!force && (now - lastShownAtMs) < MIN_INTERVAL_MS) {
            // Too soon; ensure a preload exists for later
            if (interstitialAd == null) preload(activity)
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
        } else {
            preload(activity)
        }
    }
}
