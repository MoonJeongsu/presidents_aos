package com.uspresident.speeches.data

import android.content.Context
import org.json.JSONArray

class SpeechRepository(
    context: Context,
    private val speechBodyFetcher: SpeechBodyFetcher,
) {

    private val appContext = context.applicationContext
    private val presidents: List<President>
    private val speechesByPresident: Map<String, List<SpeechSummary>>

    init {
        val presidentJson = readAsset("presidents.json")
        val speechIndexJson = readAsset("speeches_index.json")

        presidents = parsePresidents(presidentJson)
        speechesByPresident = parseSpeechIndex(speechIndexJson)
            .groupBy { it.presidentId }
            .mapValues { (_, speeches) ->
                speeches.sortedWith(
                    compareBy<SpeechSummary> { it.date ?: "9999-12-31" }
                        .thenBy { it.title.lowercase() },
                )
            }
    }

    fun getPresidents(): List<President> = presidents

    fun getSpeeches(presidentId: String): List<SpeechSummary> {
        return speechesByPresident[presidentId].orEmpty()
    }

    fun getPresident(presidentId: String): President? {
        return presidents.firstOrNull { it.id == presidentId }
    }

    fun getSpeechSummary(speechId: String): SpeechSummary? {
        return speechesByPresident.values
            .asSequence()
            .flatMap { it.asSequence() }
            .firstOrNull { it.id == speechId }
    }

    fun getSpeechDetail(speechId: String): SpeechDetail? {
        val summary = getSpeechSummary(speechId) ?: return null
        val bodyResult = speechBodyFetcher.fetchBody(speechId)
        val body = bodyResult.getOrNull() ?: return null
        return SpeechDetail(summary = summary, body = body)
    }

    fun getSpeechDetailError(speechId: String): String? {
        if (getSpeechSummary(speechId) == null) return null
        return speechBodyFetcher.fetchBody(speechId).exceptionOrNull()?.message
    }

    private fun readAsset(path: String): String {
        return appContext.assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun parsePresidents(json: String): List<President> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    President(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        years = item.optString("years"),
                    ),
                )
            }
        }
    }

    private fun parseSpeechIndex(json: String): List<SpeechSummary> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SpeechSummary(
                        id = item.getString("id"),
                        presidentId = item.getString("presidentId"),
                        title = item.getString("title"),
                        date = item.optString("date").takeIf { it.isNotBlank() },
                        presidentName = item.getString("presidentName"),
                    ),
                )
            }
        }
    }
}
