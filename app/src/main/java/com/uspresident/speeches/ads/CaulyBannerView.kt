package com.uspresident.speeches.ads

import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fsn.cauly.CaulyAdInfoBuilder
import com.fsn.cauly.CaulyAdView
import com.fsn.cauly.CaulyAdViewListener
import com.uspresident.speeches.R

@Composable
fun CaulyBannerView(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var adView by remember { mutableStateOf<CaulyAdView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView?.resume()
                Lifecycle.Event.ON_PAUSE -> adView?.pause()
                Lifecycle.Event.ON_DESTROY -> adView?.destroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView?.destroy()
            adView = null
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            val appCode = ctx.getString(R.string.cauly_app_code)
            val info = CaulyAdInfoBuilder(appCode)
                .bannerHeight(CaulyAdInfoBuilder.FIXED)
                .setBannerSize(320, 50)
                .build()

            CaulyAdView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
                setAdInfo(info)
                setAdViewListener(object : CaulyAdViewListener {
                    override fun onReceiveAd(view: CaulyAdView, isChargeableAd: Boolean) {
                        Log.d(TAG, "banner received chargeable=$isChargeableAd")
                    }

                    override fun onFailedToReceiveAd(
                        view: CaulyAdView,
                        errorCode: Int,
                        errorMsg: String,
                    ) {
                        Log.d(TAG, "banner failed code=$errorCode msg=$errorMsg")
                    }

                    override fun onShowLandingScreen(view: CaulyAdView) = Unit

                    override fun onCloseLandingScreen(view: CaulyAdView) = Unit

                    override fun onClickAd(view: CaulyAdView) = Unit
                })
                adView = this
            }
        },
    )
}

private const val TAG = "CaulyBanner"
