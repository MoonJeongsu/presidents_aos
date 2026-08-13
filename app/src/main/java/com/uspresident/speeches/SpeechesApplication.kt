package com.uspresident.speeches

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.uspresident.speeches.data.ClientIdProvider
import com.uspresident.speeches.data.SpeechBodyFetcher
import com.uspresident.speeches.data.SpeechRepository
import com.uspresident.speeches.data.TranslationQuotaStore
import com.uspresident.speeches.data.TranslationRepository
import com.uspresident.speeches.data.TtsQuotaStore
import com.uspresident.speeches.data.TtsRepository
import com.uspresident.speeches.data.local.AppDatabase
import com.uspresident.speeches.translation.CloudTranslationApi
import com.uspresident.speeches.tts.CloudTtsApi

class SpeechesApplication : Application() {

    lateinit var speechRepository: SpeechRepository
        private set

    lateinit var translationRepository: TranslationRepository
        private set

    lateinit var ttsRepository: TtsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        configureAdMobTestDevices()
        MobileAds.initialize(this)

        val speechBodyFetcher = SpeechBodyFetcher(this)
        speechRepository = SpeechRepository(this, speechBodyFetcher)

        val database = AppDatabase.create(this)
        val api = CloudTranslationApi(
            translateUrl = getString(R.string.translate_function_url).trim(),
            grantBonusUrl = getString(R.string.grant_bonus_function_url).trim(),
        )
        translationRepository = TranslationRepository(
            cacheDao = database.translationCacheDao(),
            api = api,
            quotaStore = TranslationQuotaStore(this),
            clientIdProvider = ClientIdProvider(this),
        )

        val ttsApi = CloudTtsApi(
            synthesizeUrl = getString(R.string.synthesize_function_url).trim(),
            grantBonusUrl = getString(R.string.grant_tts_bonus_function_url).trim(),
        )
        ttsRepository = TtsRepository(
            context = this,
            api = ttsApi,
            quotaStore = TtsQuotaStore(this),
            clientIdProvider = ClientIdProvider(this),
        )
    }

    private fun configureAdMobTestDevices() {
        if (!isDebugBuild()) {
            return
        }

        val testDeviceIds = resources.getStringArray(R.array.admob_test_device_ids)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (testDeviceIds.isEmpty()) {
            Log.i(
                TAG,
                "AdMob test devices not configured. After the first ad request, check Logcat for " +
                    "setTestDeviceIds and add the ID to admob_test_device_ids in strings.xml.",
            )
            return
        }

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build(),
        )
        Log.i(TAG, "AdMob test devices registered: ${testDeviceIds.size}")
    }

    private fun isDebugBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    companion object {
        private const val TAG = "SpeechesApplication"
    }
}
