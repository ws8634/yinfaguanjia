package com.example.elderlyguardian.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class ScamDetectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scamKeyword = intent.getStringExtra("scam_keyword")
        
        if (scamKeyword != null) {
            speakAlert(context, "您确定要进行包含'$scamKeyword'的操作吗？")
            Log.d("ScamDetectionReceiver", "Detected scam keyword: $scamKeyword")
        }
    }

    private fun speakAlert(context: Context, message: String) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ttsInstance = TextToSpeech(context, null)
                ttsInstance.language = Locale.CHINA
                ttsInstance.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}