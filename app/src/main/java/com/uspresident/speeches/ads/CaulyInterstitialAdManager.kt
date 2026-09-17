package com.uspresident.speeches.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fsn.cauly.CaulyAdInfoBuilder
import com.fsn.cauly.CaulyInterstitialAd
import com.fsn.cauly.CaulyInterstitialAdListener
import com.uspresident.speeches.R

/**
 * Request-on-show Cauly interstitial helper.
 * No preload. On failure or timeout, continues to the intended action.
 */
class CaulyInterstitialAdManager(
    private val activity: Activity,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var inFlight = false
    private var pendingTimeout: Runnable? = null

    fun showThenNavigate(onNavigate: () -> Unit) {
        requestAndShow(
            onClosed = onNavigate,
            onFailedOrTimeout = onNavigate,
            grantRewardOnClose = false,
            onRewardEarned = {},
        )
    }

    fun showForReward(onRewardEarned: () -> Unit, onFinished: () -> Unit = {}) {
        requestAndShow(
            onClosed = onFinished,
            onFailedOrTimeout = onFinished,
            grantRewardOnClose = true,
            onRewardEarned = onRewardEarned,
        )
    }

    private fun requestAndShow(
        onClosed: () -> Unit,
        onFailedOrTimeout: () -> Unit,
        grantRewardOnClose: Boolean,
        onRewardEarned: () -> Unit,
    ) {
        if (inFlight) {
            Log.d(TAG, "interstitial already in flight; fail-open")
            onFailedOrTimeout()
            return
        }
        if (activity.isFinishing) {
            onFailedOrTimeout()
            return
        }

        inFlight = true
        var completed = false

        fun clearTimeout() {
            pendingTimeout?.let { mainHandler.removeCallbacks(it) }
            pendingTimeout = null
        }

        fun finishOnce(block: () -> Unit) {
            if (completed) return
            completed = true
            inFlight = false
            clearTimeout()
            block()
        }

        val timeoutRunnable = Runnable {
            Log.d(TAG, "interstitial timed out; fail-open")
            finishOnce(onFailedOrTimeout)
        }
        pendingTimeout = timeoutRunnable
        mainHandler.postDelayed(timeoutRunnable, REQUEST_TIMEOUT_MS)

        val appCode = activity.getString(R.string.cauly_app_code)
        val adInfo = CaulyAdInfoBuilder(appCode).build()
        val interstitial = CaulyInterstitialAd()
        interstitial.setAdInfo(adInfo)
        interstitial.setInterstialAdListener(object : CaulyInterstitialAdListener {
            override fun onReceiveInterstitialAd(
                ad: CaulyInterstitialAd,
                isChargeableAd: Boolean,
            ) {
                if (completed) {
                    ad.cancel()
                    return
                }
                Log.d(TAG, "interstitial received chargeable=$isChargeableAd")
                clearTimeout()
                ad.show()
            }

            override fun onFailedToReceiveInterstitialAd(
                ad: CaulyInterstitialAd,
                errorCode: Int,
                errorMsg: String,
            ) {
                Log.d(TAG, "interstitial failed code=$errorCode msg=$errorMsg")
                finishOnce(onFailedOrTimeout)
            }

            override fun onClosedInterstitialAd(ad: CaulyInterstitialAd) {
                Log.d(TAG, "interstitial closed")
                finishOnce {
                    if (grantRewardOnClose) {
                        onRewardEarned()
                    }
                    onClosed()
                }
            }

            override fun onLeaveInterstitialAd(ad: CaulyInterstitialAd) {
                Log.d(TAG, "interstitial leave")
            }

            override fun onClickInterstitialAd(ad: CaulyInterstitialAd) {
                Log.d(TAG, "interstitial click")
            }
        })
        interstitial.requestInterstitialAd(activity)
    }

    companion object {
        private const val TAG = "CaulyInterstitial"
        private const val REQUEST_TIMEOUT_MS = 8_000L
    }
}
