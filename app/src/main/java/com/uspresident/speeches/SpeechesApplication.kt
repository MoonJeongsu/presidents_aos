package com.uspresident.speeches

import android.app.Application
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
}
