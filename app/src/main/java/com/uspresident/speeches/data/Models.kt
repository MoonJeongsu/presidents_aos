package com.uspresident.speeches.data

data class President(
    val id: String,
    val name: String,
    val years: String,
)

data class SpeechSummary(
    val id: String,
    val presidentId: String,
    val title: String,
    val date: String?,
    val presidentName: String,
)

data class SpeechDetail(
    val summary: SpeechSummary,
    val body: String,
)
