package com.example.elderlyguardian.utils

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class VoiceRecognitionHelper(private val context: Context) {
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val voiceData = mutableListOf<Float>()

    fun startRecording() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            audioRecord?.startRecording()
            isRecording = true
            voiceData.clear()

            Thread {
                val buffer = ShortArray(BUFFER_SIZE)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (read > 0) {
                        for (i in 0 until read) {
                            voiceData.add(buffer[i].toFloat())
                        }
                    }
                }
            }.start()
            Log.d("VoiceRecognitionHelper", "Recording started")
        } catch (e: Exception) {
            Log.e("VoiceRecognitionHelper", "Error starting recording: ${e.message}")
        }
    }

    fun stopRecording(): FloatArray {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d("VoiceRecognitionHelper", "Recording stopped, data size: ${voiceData.size}")
        return voiceData.toFloatArray()
    }

    fun extractFeatures(audioData: FloatArray): FloatArray {
        val features = FloatArray(128)
        for (i in features.indices) {
            features[i] = if (i < audioData.size) audioData[i] else 0f
        }
        return features
    }

    fun close() {
        isRecording = false
        audioRecord?.release()
        audioRecord = null
    }
}