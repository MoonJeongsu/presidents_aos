package com.uspresident.speeches.ui.speeches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uspresident.speeches.ads.BannerWaterfallView
import com.uspresident.speeches.data.SpeechSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val displayDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechListScreen(
    presidentName: String,
    speeches: List<SpeechSummary>,
    onBack: () -> Unit,
    onSpeechClick: (SpeechSummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(presidentName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BannerWaterfallView()
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "${speeches.size} speeches · sorted by date",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(speeches, key = { it.id }) { speech ->
                SpeechCard(
                    speech = speech,
                    onClick = { onSpeechClick(speech) },
                )
            }
        }
    }
}

@Composable
private fun SpeechCard(
    speech: SpeechSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            speech.date?.let { date ->
                Text(
                    text = formatDisplayDate(date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = speech.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = if (speech.date != null) 6.dp else 0.dp),
            )
        }
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        LocalDate.parse(isoDate).format(displayDateFormatter)
    } catch (_: DateTimeParseException) {
        isoDate
    }
}
