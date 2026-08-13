package com.uspresident.speeches.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.uspresident.speeches.R

class RewardedAdManager(
    private val activity: Activity,
) {
    private var rewardedAd: RewardedAd? = null
    private var pendingReward: (() -> Unit)? = null
    private var pendingDismiss: (() -> Unit)? = null

    fun loadAd() {
        RewardedAd.load(
            activity,
            activity.getString(R.string.admob_rewarded_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            },
        )
    }

    fun showForReward(onRewardEarned: () -> Unit, onFinished: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onFinished()
            loadAd()
            return
        }

        pendingReward = onRewardEarned
        pendingDismiss = onFinished

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                pendingDismiss?.invoke()
                pendingDismiss = null
                pendingReward = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                pendingDismiss?.invoke()
                pendingDismiss = null
                pendingReward = null
                loadAd()
            }
        }

        ad.show(activity) { _ ->
            pendingReward?.invoke()
            pendingReward = null
        }
    }
}
