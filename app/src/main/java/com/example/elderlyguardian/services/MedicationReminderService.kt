package com.example.elderlyguardian.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.elderlyguardian.MainActivity
import com.example.elderlyguardian.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MedicationReminderService : Service() {
    private var textToSpeech: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val checkInterval = 30000L // 30秒检查一次
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "medication_reminder_channel"
        const val CHANNEL_NAME = "用药提醒"
        const val NOTIFICATION_ID = 1001
        const val ALARM_CHANNEL_ID = "medication_alarm_channel"
        const val ALARM_CHANNEL_NAME = "用药闹钟提醒"
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        createNotificationChannel()
        createAlarmChannel()
        initTextToSpeech()
        acquireWakeLock()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 开始定时检查
        startReminderCheck()

        return START_STICKY
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MedicationReminder::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10分钟
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用药提醒后台服务"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createAlarmChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用药闹钟式提醒"
                setSound(alarmSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
                setBypassDnd(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openMedication", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("银发管家")
            .setContentText("用药提醒服务运行中")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startReminderCheck() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkAndTriggerReminders()
                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    private fun checkAndTriggerReminders() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

        // 只在整点或半点附近检查，避免频繁检查
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        if (currentMinute !in listOf(0, 1, 30, 31)) {
            return
        }

        // 检查预设的用药时间
        checkMedicationTime("08:00", currentTime, "降压药", currentDate)
        checkMedicationTime("12:00", currentTime, "钙片", currentDate)
        checkMedicationTime("18:00", currentTime, "降糖药", currentDate)
    }

    private fun checkMedicationTime(scheduledTime: String, currentTime: String, medicationName: String, date: String) {
        val scheduled = parseTime(scheduledTime)
        val current = parseTime(currentTime)

        // 如果在预定时间的前后1分钟内
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(
            kotlin.math.abs(current.time - scheduled.time)
        )

        if (diffMinutes <= 1) {
            // 检查今天是否已经提醒过
            val sharedPrefs = getSharedPreferences("medication_prefs", Context.MODE_PRIVATE)
            val reminderKey = "reminded_${medicationName}_${date}_${scheduledTime}"

            if (!sharedPrefs.getBoolean(reminderKey, false)) {
                // 标记为已提醒
                sharedPrefs.edit().putBoolean(reminderKey, true).apply()

                // 发送闹钟式提醒
                sendAlarmReminder(medicationName, scheduledTime)
            }
        }
    }

    private fun parseTime(timeStr: String): Date {
        return SimpleDateFormat("HH:mm", Locale.CHINA).parse(timeStr) ?: Date()
    }

    private fun sendAlarmReminder(medicationName: String, time: String) {
        // 唤醒屏幕
        wakeUpScreen()
        
        // 震动提醒
        startVibration()

        // 语音播报 - 多次播报确保听到
        val message = "该吃$medicationName 了，记得按时服药哦"
        repeat(3) { index ->
            handler.postDelayed({
                textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            }, index * 5000L) // 每5秒播报一次
        }

        // 发送闹钟式通知
        sendAlarmNotification(medicationName, time)
        
        // 启动全屏提醒Activity
        launchAlarmActivity(medicationName, time)
    }

    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            "MedicationReminder::AlarmWakeLock"
        )
        wakeLock.acquire(30 * 1000L) // 保持唤醒30秒
    }

    private fun startVibration() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000),
                    -1
                )
                vib.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000), -1)
            }
        }
    }

    private fun sendAlarmNotification(medicationName: String, time: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("openMedication", true)
            putExtra("medicationName", medicationName)
            putExtra("alarmTime", time)
            putExtra("isAlarm", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, medicationName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 添加服用动作按钮
        val takeIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_TAKE_MEDICATION"
            putExtra("medicationName", medicationName)
        }
        val takePendingIntent = PendingIntent.getActivity(
            this, medicationName.hashCode() + 1, takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("⏰ 用药时间到了！")
            .setContentText("该吃 $medicationName 了，记得按时服药哦")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "已服用", takePendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000))
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(medicationName.hashCode(), notification)
    }

    private fun launchAlarmActivity(medicationName: String, time: String) {
        val alarmIntent = Intent(this, MedicationAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("medicationName", medicationName)
            putExtra("alarmTime", time)
        }
        startActivity(alarmIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        vibrator?.cancel()
    }
}
