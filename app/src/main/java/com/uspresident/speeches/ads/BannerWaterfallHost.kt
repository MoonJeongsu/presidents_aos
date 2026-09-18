package com.uspresident.speeches.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.fsn.cauly.CaulyAdInfoBuilder
import com.fsn.cauly.CaulyAdView
import com.fsn.cauly.CaulyAdViewListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import com.uspresident.speeches.BuildConfig

/**
 * Banner waterfall host: Cauly → Pangle → Unity.
 * First network that fills keeps the slot; no concurrent banners.
 */
class BannerWaterfallHost(
    context: Context,
) : FrameLayout(context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val activity: Activity? = context as? Activity

    private var network = 0
    private var destroyed = false
    private var filled = false

    private var caulyView: CaulyAdView? = null
    private var pangleAd: PAGBannerAd? = null
    private var unityBanner: BannerView? = null
    private var caulyTimeout: Runnable? = null

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        minimumHeight = (50 * resources.displayMetrics.density).toInt()
    }

    fun start() {
        if (destroyed || filled || network != 0) return
        AdsSdk.ensureInitialized(context)
        network = 1
        loadCauly()
    }

    fun pause() {
        caulyView?.pause()
    }

    fun resume() {
        caulyView?.resume()
    }

    fun destroyHost() {
        destroyed = true
        clearCaulyTimeout()
        destroyCauly()
        destroyPangle()
        destroyUnity()
        removeAllViews()
    }

    private fun loadCauly() {
        if (destroyed || filled) return
        try {
            val info = CaulyAdInfoBuilder(BuildConfig.CAULY_APP_CODE)
                .bannerHeight(CaulyAdInfoBuilder.FIXED)
                .setBannerSize(320, 50)
                .build()
            val view = CaulyAdView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
                setAdInfo(info)
                setAdViewListener(object : CaulyAdViewListener {
                    override fun onReceiveAd(adView: CaulyAdView, isChargeableAd: Boolean) {
                        clearCaulyTimeout()
                        if (destroyed || filled || network != 1) return
                        filled = true
                        Log.i(TAG, "cauly banner filled chargeable=$isChargeableAd")
                    }

                    override fun onFailedToReceiveAd(
                        adView: CaulyAdView,
                        errorCode: Int,
                        errorMsg: String,
                    ) {
                        clearCaulyTimeout()
                        Log.w(TAG, "cauly banner fail $errorCode $errorMsg")
                        advancePangle("cauly fail $errorCode $errorMsg")
                    }

                    override fun onShowLandingScreen(adView: CaulyAdView) = Unit
                    override fun onCloseLandingScreen(adView: CaulyAdView) = Unit
                    override fun onClickAd(adView: CaulyAdView) = Unit
                })
            }
            caulyView = view
            removeAllViews()
            addView(view)
            Log.i(TAG, "request cauly banner")
            scheduleCaulyTimeout()
        } catch (error: Exception) {
            Log.w(TAG, "cauly banner exception", error)
            advancePangle("cauly exception")
        }
    }

    private fun scheduleCaulyTimeout() {
        clearCaulyTimeout()
        val runnable = Runnable {
            if (destroyed || filled || network != 1) return@Runnable
            Log.w(TAG, "cauly banner timeout")
            advancePangle("cauly timeout")
        }
        caulyTimeout = runnable
        mainHandler.postDelayed(runnable, CAULY_TIMEOUT_MS)
    }

    private fun clearCaulyTimeout() {
        caulyTimeout?.let { mainHandler.removeCallbacks(it) }
        caulyTimeout = null
    }

    private fun advancePangle(reason: String) {
        onMain {
            if (destroyed || filled || network != 1) return@onMain
            network = 2
            destroyCauly()
            Log.w(TAG, "advance pangle banner: $reason")
            loadPangle()
        }
    }

    private fun loadPangle() {
        if (destroyed || filled) return
        val slotId = BuildConfig.PANGLE_BANNER_SLOT_ID.trim()
        if (slotId.isEmpty()) {
            Log.w(TAG, "pangle banner slot empty; skip")
            advanceUnity("pangle banner slot empty")
            return
        }
        if (!PAGSdk.isInitSuccess()) {
            advanceUnity("pangle not initialized")
            return
        }
        try {
            val size = PAGBannerSize.BANNER_W_320_H_50
            val request = PAGBannerRequest(size)
            Log.i(TAG, "request pangle banner slot=$slotId")
            PAGBannerAd.loadAd(
                slotId,
                request,
                object : PAGBannerAdLoadListener {
                    override fun onError(code: Int, message: String) {
                        Log.w(TAG, "pangle banner fail $code $message")
                        advanceUnity("pangle fail $code $message")
                    }

                    override fun onAdLoaded(bannerAd: PAGBannerAd) {
                        onMain { showPangle(bannerAd) }
                    }
                },
            )
        } catch (error: Exception) {
            Log.w(TAG, "pangle banner exception", error)
            advanceUnity("pangle exception")
        }
    }

    private fun showPangle(bannerAd: PAGBannerAd) {
        if (destroyed || filled || network != 2) {
            bannerAd.destroy()
            return
        }
        filled = true
        pangleAd = bannerAd
        bannerAd.setAdInteractionListener(object : PAGBannerAdInteractionListener {
            override fun onAdShowed() {
                Log.i(TAG, "pangle banner showed")
            }

            override fun onAdClicked() {
                Log.i(TAG, "pangle banner clicked")
            }

            override fun onAdDismissed() {
                Log.i(TAG, "pangle banner dismissed")
            }
        })
        removeAllViews()
        val bannerView = bannerAd.bannerView
        addView(
            bannerView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        Log.i(TAG, "pangle banner filled")
    }

    private fun advanceUnity(reason: String) {
        onMain {
            if (destroyed || filled || network != 2) return@onMain
            network = 3
            destroyPangle()
            Log.w(TAG, "advance unity banner: $reason")
            loadUnity()
        }
    }

    private fun loadUnity() {
        if (destroyed || filled) return
        val act = activity
        if (act == null || act.isFinishing) {
            Log.w(TAG, "unity banner skipped: no activity")
            finishEmpty("no activity")
            return
        }
        if (!UnityAds.isInitialized) {
            finishEmpty("unity not initialized")
            return
        }
        try {
            val banner = BannerView(
                act,
                BuildConfig.UNITY_BANNER_PLACEMENT_ID,
                UnityBannerSize(320, 50),
            )
            banner.listener = object : BannerView.Listener() {
                override fun onBannerLoaded(bannerAdView: BannerView) {
                    onMain {
                        if (destroyed || filled || network != 3) return@onMain
                        filled = true
                        Log.i(TAG, "unity banner filled")
                    }
                }

                override fun onBannerShown(bannerAdView: BannerView) {
                    Log.i(TAG, "unity banner shown")
                }

                override fun onBannerClick(bannerAdView: BannerView) {
                    Log.i(TAG, "unity banner clicked")
                }

                override fun onBannerFailedToLoad(
                    bannerAdView: BannerView,
                    errorInfo: BannerErrorInfo,
                ) {
                    Log.w(TAG, "unity banner fail ${errorInfo.errorCode} ${errorInfo.errorMessage}")
                    finishEmpty("unity fail ${errorInfo.errorCode}")
                }

                override fun onBannerLeftApplication(bannerAdView: BannerView) {
                    Log.i(TAG, "unity banner left app")
                }
            }
            unityBanner = banner
            removeAllViews()
            addView(
                banner,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
            )
            Log.i(TAG, "request unity banner")
            banner.load()
        } catch (error: Exception) {
            Log.w(TAG, "unity banner exception", error)
            finishEmpty("unity exception")
        }
    }

    private fun finishEmpty(reason: String) {
        onMain {
            if (destroyed) return@onMain
            network = 0
            removeAllViews()
            Log.w(TAG, "banner waterfall empty: $reason")
        }
    }

    private fun destroyCauly() {
        clearCaulyTimeout()
        caulyView?.destroy()
        caulyView = null
    }

    private fun destroyPangle() {
        pangleAd?.destroy()
        pangleAd = null
    }

    private fun destroyUnity() {
        unityBanner?.listener = null
        unityBanner?.destroy()
        unityBanner = null
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    companion object {
        private const val TAG = "SpeechesBanner"
        private const val CAULY_TIMEOUT_MS = 8_000L
    }
}
