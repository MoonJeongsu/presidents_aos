package com.uspresident.speeches.ads

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.fsn.cauly.Logger
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.uspresident.speeches.BuildConfig

/** Shared Pangle / Unity initialization for interstitial and banner. */
object AdsSdk {
    private const val TAG = "SpeechesAdsSdk"

    fun ensureInitialized(context: Context) {
        if (BuildConfig.DEBUG) {
            Logger.setLogLevel(Logger.LogLevel.Info)
        }
        initPangle(context.applicationContext)
        initUnity(context.applicationContext)
    }

    private fun initPangle(appContext: Context) {
        if (PAGSdk.isInitSuccess()) return
        try {
            val config = PAGConfig.Builder()
                .appId(BuildConfig.PANGLE_APP_ID)
                .debugLog(BuildConfig.DEBUG)
                .build()
            PAGSdk.init(
                appContext,
                config,
                object : PAGSdk.PAGInitCallback {
                    override fun success() {
                        Log.i(TAG, "pangle init ok")
                    }

                    override fun fail(code: Int, msg: String) {
                        Log.w(TAG, "pangle init failed: $code $msg")
                    }
                },
            )
        } catch (error: Exception) {
            Log.w(TAG, "pangle init exception", error)
        }
    }

    private fun initUnity(appContext: Context) {
        if (UnityAds.isInitialized) return
        try {
            UnityAds.initialize(
                appContext,
                BuildConfig.UNITY_GAME_ID,
                BuildConfig.DEBUG,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Log.i(TAG, "unity init ok")
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError,
                        message: String,
                    ) {
                        Log.w(TAG, "unity init failed: $error $message")
                    }
                },
            )
        } catch (error: Exception) {
            Log.w(TAG, "unity init exception", error)
        }
    }
}
