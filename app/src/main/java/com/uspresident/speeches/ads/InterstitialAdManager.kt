package com.uspresident.speeches.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.uspresident.speeches.R

class InterstitialAdManager(
    private val activity: Activity,
) {
    private var interstitialAd: InterstitialAd? = null
    private var pendingNavigation: (() -> Unit)? = null

    fun loadAd() {
        InterstitialAd.load(
            activity,
            activity.getString(R.string.admob_interstitial_unit_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            },
        )
    }

    fun showThenNavigate(onNavigate: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onNavigate()
            loadAd()
            return
        }

        pendingNavigation = onNavigate
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                pendingNavigation?.invoke()
                pendingNavigation = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                pendingNavigation?.invoke()
                pendingNavigation = null
                loadAd()
            }
        }
        ad.show(activity)
    }
}
