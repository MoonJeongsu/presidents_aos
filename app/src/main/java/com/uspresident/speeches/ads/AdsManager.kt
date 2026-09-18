package com.uspresident.speeches.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.fsn.cauly.CaulyAdInfoBuilder
import com.fsn.cauly.CaulyInterstitialAd
import com.fsn.cauly.CaulyInterstitialAdListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.uspresident.speeches.BuildConfig

/**
 * Interstitial waterfall: Cauly → Pangle → Unity.
 * Cauly is request-on-show (no preload). Fail/timeout advances to the next network.
 * Does not block UI navigation.
 */
class AdsManager(
    private val activity: Activity,
) : CaulyInterstitialAdListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var loading = false

    /** 0 idle, 1 cauly, 2 pangle, 3 unity */
    @Volatile
    private var network = 0

    @Volatile
    private var rewardMode = false

    @Volatile
    private var adWasShown = false

    private var onRewardEarned: (() -> Unit)? = null
    private var onRewardFinished: (() -> Unit)? = null

    private var interstitialAd: CaulyInterstitialAd? = null
    private var caulyTimeout: Runnable? = null

    fun initialize() {
        AdsSdk.ensureInitialized(activity)
        Log.i(
            TAG,
            "ads ready cauly=${BuildConfig.CAULY_APP_CODE} " +
                "pangle=${BuildConfig.PANGLE_APP_ID}/${BuildConfig.PANGLE_INTERSTITIAL_SLOT_ID} " +
                "pangleBanner=${BuildConfig.PANGLE_BANNER_SLOT_ID} " +
                "unity=${BuildConfig.UNITY_GAME_ID}/${BuildConfig.UNITY_INTERSTITIAL_PLACEMENT_ID} " +
                "unityBanner=${BuildConfig.UNITY_BANNER_PLACEMENT_ID}",
        )
    }

    /** Fire-and-forget interstitial after navigation. */
    fun showInterstitial() {
        startWaterfall(rewardMode = false, onRewardEarned = null, onFinished = null)
    }

    /** Bonus path: grant reward only if an interstitial was shown and closed. */
    fun showForReward(onRewardEarned: () -> Unit, onFinished: () -> Unit = {}) {
        startWaterfall(
            rewardMode = true,
            onRewardEarned = onRewardEarned,
            onFinished = onFinished,
        )
    }

    private fun startWaterfall(
        rewardMode: Boolean,
        onRewardEarned: (() -> Unit)?,
        onFinished: (() -> Unit)?,
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFinished?.invoke()
            return
        }
        if (loading) {
            Log.i(TAG, "request already in flight")
            if (rewardMode) onFinished?.invoke()
            return
        }
        this.rewardMode = rewardMode
        this.onRewardEarned = onRewardEarned
        this.onRewardFinished = onFinished
        adWasShown = false
        loading = true
        network = 1
        requestCauly()
    }

    private fun requestCauly() {
        try {
            val adInfo = CaulyAdInfoBuilder(BuildConfig.CAULY_APP_CODE).build()
            val ad = CaulyInterstitialAd()
            ad.setAdInfo(adInfo)
            ad.setInterstialAdListener(this)
            interstitialAd = ad
            Log.i(TAG, "request cauly")
            scheduleCaulyTimeout()
            ad.requestInterstitialAd(activity)
        } catch (error: Exception) {
            Log.w(TAG, "cauly request failed", error)
            loadPangle("cauly request exception")
        }
    }

    private fun scheduleCaulyTimeout() {
        clearCaulyTimeout()
        val runnable = Runnable {
            Log.w(TAG, "cauly client timeout")
            loadPangle("cauly client timeout")
        }
        caulyTimeout = runnable
        mainHandler.postDelayed(runnable, CAULY_TIMEOUT_MS)
    }

    private fun clearCaulyTimeout() {
        caulyTimeout?.let { mainHandler.removeCallbacks(it) }
        caulyTimeout = null
    }

    override fun onReceiveInterstitialAd(ad: CaulyInterstitialAd, isChargeableAd: Boolean) {
        clearCaulyTimeout()
        Log.i(TAG, "cauly received chargeable=$isChargeableAd")
        if (!canPresent()) {
            ad.cancel()
            finishWaterfall("cauly discarded")
            return
        }
        try {
            ad.show()
            markShown("cauly")
        } catch (error: Exception) {
            Log.w(TAG, "cauly show failed", error)
            loadPangle("cauly show exception")
        }
    }

    override fun onFailedToReceiveInterstitialAd(
        ad: CaulyInterstitialAd,
        errorCode: Int,
        errorMsg: String,
    ) {
        clearCaulyTimeout()
        loadPangle("cauly fail $errorCode $errorMsg")
    }

    override fun onClosedInterstitialAd(ad: CaulyInterstitialAd) {
        Log.i(TAG, "cauly closed")
        completeAfterClose()
    }

    override fun onLeaveInterstitialAd(ad: CaulyInterstitialAd) {
        Log.i(TAG, "cauly left app")
    }

    override fun onClickInterstitialAd(ad: CaulyInterstitialAd) {
        Log.i(TAG, "cauly clicked")
    }

    override fun onTimeout(ad: CaulyInterstitialAd, errorMsg: String) {
        clearCaulyTimeout()
        loadPangle("cauly timeout $errorMsg")
    }

    private fun loadPangle(reason: String) {
        onMain {
            if (!loading || network != 1) return@onMain
            network = 2
            interstitialAd = null
            clearCaulyTimeout()
            Log.w(TAG, "advance pangle: $reason")
            if (!PAGSdk.isInitSuccess()) {
                loadUnity("pangle not initialized")
                return@onMain
            }
            try {
                PAGInterstitialAd.loadAd(
                    BuildConfig.PANGLE_INTERSTITIAL_SLOT_ID,
                    PAGInterstitialRequest(),
                    object : PAGInterstitialAdLoadListener {
                        override fun onError(code: Int, message: String) {
                            loadUnity("pangle fail $code $message")
                        }

                        override fun onAdLoaded(ad: PAGInterstitialAd) {
                            onMain { showPangle(ad) }
                        }
                    },
                )
            } catch (error: Exception) {
                Log.w(TAG, "pangle load exception", error)
                loadUnity("pangle load exception")
            }
        }
    }

    private fun showPangle(ad: PAGInterstitialAd) {
        if (!loading) return
        if (!canPresent()) {
            finishWaterfall("pangle discarded")
            return
        }
        ad.setAdInteractionListener(object : PAGInterstitialAdInteractionListener {
            override fun onAdShowed() {
                Log.i(TAG, "pangle showed")
            }

            override fun onAdClicked() {
                Log.i(TAG, "pangle clicked")
            }

            override fun onAdDismissed() {
                Log.i(TAG, "pangle closed")
                completeAfterClose()
            }
        })
        try {
            ad.show(activity)
            markShown("pangle")
        } catch (error: Exception) {
            Log.w(TAG, "pangle show failed", error)
            loadUnity("pangle show exception")
        }
    }

    private fun loadUnity(reason: String) {
        onMain {
            if (!loading || network != 2) return@onMain
            network = 3
            Log.w(TAG, "advance unity: $reason")
            if (!UnityAds.isInitialized) {
                finishWaterfall("unity not initialized")
                return@onMain
            }
            try {
                UnityAds.load(
                    BuildConfig.UNITY_INTERSTITIAL_PLACEMENT_ID,
                    object : IUnityAdsLoadListener {
                        override fun onUnityAdsAdLoaded(placementId: String) {
                            onMain { showUnity(placementId) }
                        }

                        override fun onUnityAdsFailedToLoad(
                            placementId: String,
                            error: UnityAds.UnityAdsLoadError,
                            message: String,
                        ) {
                            finishWaterfall("unity fail $error $message")
                        }
                    },
                )
            } catch (error: Exception) {
                Log.w(TAG, "unity load exception", error)
                finishWaterfall("unity load exception")
            }
        }
    }

    private fun showUnity(placementId: String) {
        if (!loading) return
        if (!canPresent()) {
            finishWaterfall("unity discarded")
            return
        }
        try {
            UnityAds.show(
                activity,
                placementId,
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        shownPlacementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String,
                    ) {
                        finishWaterfall("unity show fail $error $message")
                    }

                    override fun onUnityAdsShowStart(shownPlacementId: String) {
                        Log.i(TAG, "unity started")
                    }

                    override fun onUnityAdsShowClick(shownPlacementId: String) {
                        Log.i(TAG, "unity clicked")
                    }

                    override fun onUnityAdsShowComplete(
                        shownPlacementId: String,
                        state: UnityAds.UnityAdsShowCompletionState,
                    ) {
                        Log.i(TAG, "unity complete $state")
                        completeAfterClose()
                    }
                },
            )
            markShown("unity")
        } catch (error: Exception) {
            Log.w(TAG, "unity show exception", error)
            finishWaterfall("unity show exception")
        }
    }

    private fun canPresent(): Boolean {
        return !activity.isFinishing && !activity.isDestroyed
    }

    private fun markShown(networkName: String) {
        adWasShown = true
        loading = false
        network = 0
        interstitialAd = null
        Log.i(TAG, "$networkName shown")
    }

    private fun completeAfterClose() {
        onMain {
            val grant = rewardMode && adWasShown
            val earned = onRewardEarned
            val finished = onRewardFinished
            clearRewardCallbacks()
            if (grant) {
                earned?.invoke()
            }
            finished?.invoke()
        }
    }

    private fun finishWaterfall(reason: String) {
        onMain {
            if (!loading && !rewardMode) {
                Log.w(TAG, "waterfall empty (idle): $reason")
                return@onMain
            }
            loading = false
            network = 0
            interstitialAd = null
            clearCaulyTimeout()
            Log.w(TAG, "waterfall empty: $reason")
            val finished = onRewardFinished
            clearRewardCallbacks()
            // All networks failed without a successful show — no bonus.
            finished?.invoke()
        }
    }

    private fun clearRewardCallbacks() {
        rewardMode = false
        adWasShown = false
        onRewardEarned = null
        onRewardFinished = null
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    companion object {
        private const val TAG = "SpeechesAds"
        private const val CAULY_TIMEOUT_MS = 8_000L
    }
}
