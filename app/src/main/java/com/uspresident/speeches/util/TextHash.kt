package com.uspresident.speeches.util

import java.text.BreakIterator
import java.util.Locale

object SentenceSplitter {
    fun split(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val iterator = BreakIterator.getSentenceInstance(Locale.US)
        iterator.setText(text)

        val sentences = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(sentence)
            }
            start = end
            end = iterator.next()
        }
        return sentences
    }
}
