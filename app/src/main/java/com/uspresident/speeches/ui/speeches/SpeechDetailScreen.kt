package com.uspresident.speeches.ui.speeches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uspresident.speeches.R
import com.uspresident.speeches.data.SpeechDetail
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val displayDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechDetailScreen(
    speech: SpeechDetail?,
    isLoading: Boolean,
    loadError: String? = null,
    viewModel: SpeechDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(speech?.body) {
        speech?.body?.let { viewModel.setSpeechBody(it) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDailyQuotaIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.selectedIndex) {
        val index = uiState.selectedIndex ?: return@LaunchedEffect
        listState.animateScrollToItem(index + 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.speech_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            speech == null -> {
                Text(
                    text = loadError ?: stringResource(R.string.speech_not_available),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                ) {
                    item {
                        speech.summary.date?.let { date ->
                            Text(
                                text = formatDisplayDate(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = speech.summary.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                        Text(
                            text = speech.summary.presidentName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 16.dp),
                        )

                        FeatureHintBanner(
                            quotaUsed = uiState.quotaUsed,
                            quotaLimit = uiState.quotaLimit,
                            rewardLimitReached = uiState.rewardLimitReached,
                            isGrantingBonus = uiState.isGrantingBonus,
                            bonusMessage = uiState.bonusMessage,
                            onWatchTranslationAd = viewModel::watchAdForBonus,
                            ttsQuotaUsed = uiState.ttsQuotaUsed,
                            ttsQuotaLimit = uiState.ttsQuotaLimit,
                            ttsRewardLimitReached = uiState.ttsRewardLimitReached,
                            isGrantingTtsBonus = uiState.isGrantingTtsBonus,
                            ttsBonusMessage = uiState.ttsBonusMessage,
                            onWatchTtsAd = viewModel::watchAdForTtsBonus,
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(
                        items = uiState.sentences,
                        key = { it.index },
                    ) { sentence ->
                        SentenceBlock(
                            sentence = sentence,
                            isSelected = uiState.selectedIndex == sentence.index,
                            onClick = { viewModel.onSentenceClick(sentence.index) },
                            onSpeakClick = { viewModel.onSpeakClick(sentence.index) },
                            onWatchTranslationAd = viewModel::watchAdForBonus,
                            onWatchTtsAd = viewModel::watchAdForTtsBonus,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHintBanner(
    quotaUsed: Int,
    quotaLimit: Int,
    rewardLimitReached: Boolean,
    isGrantingBonus: Boolean,
    bonusMessage: String?,
    onWatchTranslationAd: () -> Unit,
    ttsQuotaUsed: Int,
    ttsQuotaLimit: Int,
    ttsRewardLimitReached: Boolean,
    isGrantingTtsBonus: Boolean,
    ttsBonusMessage: String?,
    onWatchTtsAd: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.translation_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }

            Text(
                text = stringResource(R.string.translation_quota_status, quotaUsed, quotaLimit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.translation_cached_free),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (quotaUsed >= quotaLimit && !rewardLimitReached) {
                OutlinedButton(
                    onClick = onWatchTranslationAd,
                    enabled = !isGrantingBonus,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    if (isGrantingBonus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.translation_watch_ad))
                }
            }

            if (rewardLimitReached) {
                Text(
                    text = stringResource(R.string.translation_reward_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            bonusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tts_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }

            Text(
                text = stringResource(R.string.tts_quota_status, ttsQuotaUsed, ttsQuotaLimit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.tts_cached_free),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (ttsQuotaUsed >= ttsQuotaLimit && !ttsRewardLimitReached) {
                OutlinedButton(
                    onClick = onWatchTtsAd,
                    enabled = !isGrantingTtsBonus,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    if (isGrantingTtsBonus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.tts_watch_ad))
                }
            }

            if (ttsRewardLimitReached) {
                Text(
                    text = stringResource(R.string.tts_reward_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            ttsBonusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            val resetHint = stringResource(R.string.quota_daily_reset_hint)
            if (resetHint.isNotBlank()) {
                Text(
                    text = resetHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SentenceBlock(
    sentence: SentenceUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onWatchTranslationAd: () -> Unit,
    onWatchTtsAd: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = sentence.text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
            )

            when {
                sentence.isSynthesizing -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }

                else -> {
                    IconButton(onClick = onSpeakClick) {
                        Icon(
                            imageVector = if (sentence.isPlaying) {
                                Icons.Filled.VolumeUp
                            } else {
                                Icons.Outlined.VolumeUp
                            },
                            contentDescription = stringResource(R.string.speak_sentence),
                            tint = if (sentence.isPlaying) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            },
                        )
                    }
                }
            }
        }

        if (sentence.ttsQuotaExceeded) {
            Text(
                text = stringResource(R.string.tts_quota_exceeded),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onWatchTtsAd,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.tts_watch_ad))
            }
        }

        sentence.ttsErrorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (isSelected) {
            when {
                sentence.isTranslating -> {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.translating),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                sentence.quotaExceeded -> {
                    Text(
                        text = stringResource(R.string.translation_quota_exceeded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Button(
                        onClick = onWatchTranslationAd,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.translation_watch_ad))
                    }
                }

                sentence.errorMessage != null -> {
                    Text(
                        text = sentence.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                sentence.translation != null -> {
                    TranslationCard(translation = sentence.translation)
                }
            }
        }
    }
}

@Composable
private fun TranslationCard(translation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.korean_translation_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = translation,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.15f,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        LocalDate.parse(isoDate).format(displayDateFormatter)
    } catch (_: DateTimeParseException) {
        isoDate
    }
}
