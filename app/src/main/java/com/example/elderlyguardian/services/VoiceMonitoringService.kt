package com.example.elderlyguardian.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.elderlyguardian.MainActivity
import java.util.*

class VoiceMonitoringService : Service() {
    private val CHANNEL_ID = "voice_monitoring_channel"
    private val ALERT_CHANNEL_ID = "scam_alert_channel"
    private val NOTIFICATION_ID = 1
    private val ALERT_NOTIFICATION_ID = 2
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    
    // 扩展诈骗关键词列表
    private val scamKeywords = listOf(
        "转账", "汇款", "银行卡", "验证码", "密码", "投资", "中奖", 
        "保证金", "手续费", "退款", "理赔", "涉嫌", "违法", "法院",
        "公安", "逮捕", "冻结", "安全账户", "紧急", "立即"
    )
    
    // 高风险关键词（需要特别警告）
    private val highRiskKeywords = listOf("转账", "汇款", "验证码", "密码", "安全账户")

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        createNotificationChannel()
        createAlertChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        vibrator?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "语音监测服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "防诈骗提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "检测到可疑关键词时发出警告"
                setSound(alertSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("银发管家")
            .setContentText("语音防诈骗监测运行中...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("VoiceMonitoringService", "Speech recognition not available")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceMonitoringService", "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d("VoiceMonitoringService", "Beginning of speech")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("VoiceMonitoringService", "End of speech")
            }
            override fun onError(error: Int) {
                Log.e("VoiceMonitoringService", "Speech recognition error: $error")
                // 出错后延迟重启监听
                Handler(Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 2000)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    Log.d("VoiceMonitoringService", "Recognized: $recognizedText")
                    checkForScamKeywords(recognizedText)
                }
                // 继续监听
                Handler(Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 500)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val partialText = matches[0]
                    // 实时检查部分结果
                    checkForScamKeywords(partialText, isPartial = true)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        try {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer()
            }
            speechRecognizer?.startListening(recognitionIntent)
            Log.d("VoiceMonitoringService", "Started listening")
        } catch (e: Exception) {
            Log.e("VoiceMonitoringService", "Error starting speech recognition: ${e.message}")
            Handler(Looper.getMainLooper()).postDelayed({
                startListening()
            }, 2000)
        }
    }

    private fun checkForScamKeywords(text: String, isPartial: Boolean = false) {
        val foundHighRisk = highRiskKeywords.filter { text.contains(it) }
        val foundScam = scamKeywords.filter { text.contains(it) }
        
        if (foundHighRisk.isNotEmpty()) {
            // 高风险警告
            val alertMessage = "警告！检测到高风险关键词：${foundHighRisk.joinToString(", ")}。请务必提高警惕，这可能是诈骗电话！"
            triggerHighRiskAlert(alertMessage)
        } else if (foundScam.isNotEmpty() && !isPartial) {
            // 一般风险提醒
            val alertMessage = "提醒：检测到可疑关键词：${foundScam.joinToString(", ")}。请谨慎处理，如有疑问请挂断电话并咨询家人。"
            speakAlert(alertMessage)
            sendScamNotification(foundScam.joinToString(", "))
        }
    }
    
    private fun triggerHighRiskAlert(message: String) {
        // 震动提醒
        vibrateAlert()
        
        // 语音播报
        speakAlert(message)
        
        // 发送高优先级通知
        sendHighRiskNotification(message)
    }
    
    private fun vibrateAlert() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 500, 1000, 500, 1000),
                    -1
                )
                vib.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), -1)
            }
        }
    }

    private fun speakAlert(message: String) {
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun sendScamNotification(keywords: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ 防诈骗提醒")
            .setContentText("检测到可疑关键词：$keywords")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
    
    private fun sendHighRiskNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("🚨 高风险警告！")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID + 1, notification)
    }
}
