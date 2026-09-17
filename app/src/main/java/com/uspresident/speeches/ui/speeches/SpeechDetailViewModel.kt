package com.uspresident.speeches.ui.speeches

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uspresident.speeches.SpeechesApplication
import com.uspresident.speeches.audio.SentenceAudioPlayer
import com.uspresident.speeches.data.BonusGrantResult
import com.uspresident.speeches.data.TranslationRepository
import com.uspresident.speeches.data.TranslationResult
import com.uspresident.speeches.data.TtsBonusGrantResult
import com.uspresident.speeches.data.TtsRepository
import com.uspresident.speeches.data.TtsResult
import com.uspresident.speeches.util.SentenceSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SentenceUiState(
    val index: Int,
    val text: String,
    val translation: String? = null,
    val isTranslating: Boolean = false,
    val errorMessage: String? = null,
    val quotaExceeded: Boolean = false,
    val isSynthesizing: Boolean = false,
    val isPlaying: Boolean = false,
    val ttsErrorMessage: String? = null,
    val ttsQuotaExceeded: Boolean = false,
)

data class SpeechDetailUiState(
    val sentences: List<SentenceUiState> = emptyList(),
    val selectedIndex: Int? = null,
    val quotaUsed: Int = 0,
    val quotaLimit: Int = 40,
    val isGrantingBonus: Boolean = false,
    val bonusMessage: String? = null,
    val rewardLimitReached: Boolean = false,
    val ttsQuotaUsed: Int = 0,
    val ttsQuotaLimit: Int = 40,
    val isGrantingTtsBonus: Boolean = false,
    val ttsBonusMessage: String? = null,
    val ttsRewardLimitReached: Boolean = false,
    val pendingTtsRetryIndex: Int? = null,
)

class SpeechDetailViewModel(
    application: Application,
    private val onWatchRewardedAd: (onRewardEarned: () -> Unit, onFinished: () -> Unit) -> Unit,
) : AndroidViewModel(application) {

    private val translationRepository: TranslationRepository =
        (application as SpeechesApplication).translationRepository

    private val ttsRepository: TtsRepository =
        (application as SpeechesApplication).ttsRepository

    private val audioPlayer = SentenceAudioPlayer()

    private val _uiState = MutableStateFlow(
        SpeechDetailUiState(
            quotaUsed = translationRepository.getCachedQuotaUsed(),
            quotaLimit = translationRepository.getCachedQuotaLimit(),
            ttsQuotaUsed = ttsRepository.getCachedQuotaUsed(),
            ttsQuotaLimit = ttsRepository.getCachedQuotaLimit(),
        ),
    )
    val uiState: StateFlow<SpeechDetailUiState> = _uiState.asStateFlow()

    fun setSpeechBody(body: String) {
        audioPlayer.stop()
        val translationReset = translationRepository.refreshDailyQuotaIfNeeded()
        val ttsReset = ttsRepository.refreshDailyQuotaIfNeeded()
        val sentences = SentenceSplitter.split(body).mapIndexed { index, text ->
            SentenceUiState(index = index, text = text)
        }
        _uiState.update {
            it.copy(
                sentences = sentences,
                selectedIndex = null,
                quotaUsed = translationRepository.getCachedQuotaUsed(),
                quotaLimit = translationRepository.getCachedQuotaLimit(),
                ttsQuotaUsed = ttsRepository.getCachedQuotaUsed(),
                ttsQuotaLimit = ttsRepository.getCachedQuotaLimit(),
                rewardLimitReached = if (translationReset) false else it.rewardLimitReached,
                ttsRewardLimitReached = if (ttsReset) false else it.ttsRewardLimitReached,
                pendingTtsRetryIndex = null,
            )
        }
    }

    fun refreshDailyQuotaIfNeeded() {
        val translationReset = translationRepository.refreshDailyQuotaIfNeeded()
        val ttsReset = ttsRepository.refreshDailyQuotaIfNeeded()
        if (!translationReset && !ttsReset) {
            return
        }
        _uiState.update { state ->
            state.copy(
                quotaUsed = translationRepository.getCachedQuotaUsed(),
                quotaLimit = translationRepository.getCachedQuotaLimit(),
                ttsQuotaUsed = ttsRepository.getCachedQuotaUsed(),
                ttsQuotaLimit = ttsRepository.getCachedQuotaLimit(),
                rewardLimitReached = if (translationReset) false else state.rewardLimitReached,
                ttsRewardLimitReached = if (ttsReset) false else state.ttsRewardLimitReached,
                sentences = state.sentences.map { sentence ->
                    sentence.copy(
                        quotaExceeded = if (translationReset) false else sentence.quotaExceeded,
                        ttsQuotaExceeded = if (ttsReset) false else sentence.ttsQuotaExceeded,
                    )
                },
            )
        }
    }

    fun onSentenceClick(index: Int) {
        val current = _uiState.value
        val sentence = current.sentences.getOrNull(index) ?: return

        if (current.selectedIndex == index && sentence.translation != null) {
            _uiState.update { it.copy(selectedIndex = null) }
            return
        }

        _uiState.update { it.copy(selectedIndex = index, bonusMessage = null) }

        if (sentence.translation != null || sentence.isTranslating) {
            return
        }

        viewModelScope.launch {
            updateSentence(index) {
                it.copy(
                    isTranslating = true,
                    errorMessage = null,
                    quotaExceeded = false,
                )
            }

            when (val result = translationRepository.translate(sentence.text)) {
                is TranslationResult.Success -> {
                    updateSentence(index) {
                        it.copy(
                            translation = result.translatedText,
                            isTranslating = false,
                            errorMessage = null,
                            quotaExceeded = false,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            quotaUsed = result.quotaUsed,
                            quotaLimit = result.quotaLimit,
                        )
                    }
                }

                is TranslationResult.QuotaExceeded -> {
                    updateSentence(index) {
                        it.copy(
                            isTranslating = false,
                            quotaExceeded = true,
                            errorMessage = null,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            quotaUsed = result.quotaUsed,
                            quotaLimit = result.quotaLimit,
                        )
                    }
                }

                is TranslationResult.Error -> {
                    updateSentence(index) {
                        it.copy(
                            isTranslating = false,
                            errorMessage = result.message,
                            quotaExceeded = false,
                        )
                    }
                }
            }
        }
    }

    fun onSpeakClick(index: Int) {
        val sentence = _uiState.value.sentences.getOrNull(index) ?: return

        if (sentence.isPlaying) {
            stopPlayback()
            return
        }

        if (sentence.isSynthesizing) {
            return
        }

        stopPlayback()

        viewModelScope.launch {
            updateSentence(index) {
                it.copy(
                    isSynthesizing = true,
                    ttsErrorMessage = null,
                    ttsQuotaExceeded = false,
                    isPlaying = false,
                )
            }

            when (val result = ttsRepository.synthesize(sentence.text)) {
                is TtsResult.Success -> {
                    _uiState.update {
                        it.copy(
                            ttsQuotaUsed = result.quotaUsed,
                            ttsQuotaLimit = result.quotaLimit,
                            pendingTtsRetryIndex = null,
                        )
                    }
                    updateSentence(index) {
                        it.copy(isSynthesizing = false, isPlaying = true)
                    }
                    audioPlayer.play(result.audioFile) {
                        updateSentence(index) { state ->
                            if (state.isPlaying) state.copy(isPlaying = false) else state
                        }
                    }
                }

                is TtsResult.QuotaExceeded -> {
                    updateSentence(index) {
                        it.copy(
                            isSynthesizing = false,
                            ttsQuotaExceeded = true,
                            ttsErrorMessage = null,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            ttsQuotaUsed = result.quotaUsed,
                            ttsQuotaLimit = result.quotaLimit,
                            pendingTtsRetryIndex = index,
                        )
                    }
                }

                is TtsResult.Error -> {
                    updateSentence(index) {
                        it.copy(
                            isSynthesizing = false,
                            ttsErrorMessage = result.message,
                            ttsQuotaExceeded = false,
                        )
                    }
                }
            }
        }
    }

    fun watchAdForBonus() {
        if (_uiState.value.isGrantingBonus || _uiState.value.rewardLimitReached) {
            return
        }

        onWatchRewardedAd(
            { grantTranslationBonusAfterReward() },
            {},
        )
    }

    fun watchAdForTtsBonus() {
        if (_uiState.value.isGrantingTtsBonus || _uiState.value.ttsRewardLimitReached) {
            return
        }

        onWatchRewardedAd(
            { grantTtsBonusAfterReward() },
            {},
        )
    }

    private fun grantTranslationBonusAfterReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGrantingBonus = true, bonusMessage = null) }

            when (val result = translationRepository.grantRewardBonus()) {
                is BonusGrantResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGrantingBonus = false,
                            quotaUsed = result.quotaUsed,
                            quotaLimit = result.quotaLimit,
                            bonusMessage = null,
                            rewardLimitReached = false,
                        )
                    }
                    retrySelectedSentenceIfNeeded()
                }

                is BonusGrantResult.RewardLimitReached -> {
                    _uiState.update {
                        it.copy(
                            isGrantingBonus = false,
                            quotaUsed = result.quotaUsed,
                            quotaLimit = result.quotaLimit,
                            rewardLimitReached = true,
                        )
                    }
                }

                is BonusGrantResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGrantingBonus = false,
                            bonusMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun grantTtsBonusAfterReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGrantingTtsBonus = true, ttsBonusMessage = null) }

            when (val result = ttsRepository.grantRewardBonus()) {
                is TtsBonusGrantResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGrantingTtsBonus = false,
                            ttsQuotaUsed = result.quotaUsed,
                            ttsQuotaLimit = result.quotaLimit,
                            ttsBonusMessage = null,
                            ttsRewardLimitReached = false,
                        )
                    }
                    retryPendingTtsIfNeeded()
                }

                is TtsBonusGrantResult.RewardLimitReached -> {
                    _uiState.update {
                        it.copy(
                            isGrantingTtsBonus = false,
                            ttsQuotaUsed = result.quotaUsed,
                            ttsQuotaLimit = result.quotaLimit,
                            ttsRewardLimitReached = true,
                        )
                    }
                }

                is TtsBonusGrantResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGrantingTtsBonus = false,
                            ttsBonusMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun retrySelectedSentenceIfNeeded() {
        val selectedIndex = _uiState.value.selectedIndex ?: return
        val sentence = _uiState.value.sentences.getOrNull(selectedIndex) ?: return
        if (sentence.translation != null || sentence.isTranslating || !sentence.quotaExceeded) {
            return
        }
        onSentenceClick(selectedIndex)
    }

    private fun retryPendingTtsIfNeeded() {
        val pendingIndex = _uiState.value.pendingTtsRetryIndex ?: return
        val sentence = _uiState.value.sentences.getOrNull(pendingIndex) ?: return
        if (sentence.isSynthesizing || sentence.isPlaying || !sentence.ttsQuotaExceeded) {
            return
        }
        onSpeakClick(pendingIndex)
    }

    private fun stopPlayback() {
        audioPlayer.stop()
        _uiState.update { state ->
            state.copy(
                sentences = state.sentences.map { sentence ->
                    if (sentence.isPlaying) sentence.copy(isPlaying = false) else sentence
                },
            )
        }
    }

    private fun updateSentence(index: Int, transform: (SentenceUiState) -> SentenceUiState) {
        _uiState.update { state ->
            state.copy(
                sentences = state.sentences.map { sentence ->
                    if (sentence.index == index) transform(sentence) else sentence
                },
            )
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
