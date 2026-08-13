package com.uspresident.speeches.audio

import android.media.MediaPlayer
import java.io.File

class SentenceAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(file: File, onComplete: () -> Unit) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                stop()
                onComplete()
            }
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.run {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (_: Exception) {
            }
            release()
        }
        mediaPlayer = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}
