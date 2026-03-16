package com.example.elderlyguardian.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.elderlyguardian.MainActivity
import java.util.*

class FallDetectionService : Service(), SensorEventListener {
    private val CHANNEL_ID = "fall_detection_channel"
    private val NOTIFICATION_ID = 2
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var textToSpeech: TextToSpeech? = null
    private val FALL_THRESHOLD = 25.0f
    private var lastFallTime: Long = 0
    private val MIN_TIME_BETWEEN_FALLS = 10000L
    private val emergencyContacts = mutableListOf("10086")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeSensors()
        initializeTextToSpeech()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        textToSpeech?.shutdown()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "跌倒检测服务",
                NotificationManager.IMPORTANCE_LOW
            )
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
            .setContentText("跌倒检测中...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            detectFall(acceleration)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectFall(acceleration: Float) {
        val currentTime = System.currentTimeMillis()
        if (acceleration > FALL_THRESHOLD) {
            if (currentTime - lastFallTime > MIN_TIME_BETWEEN_FALLS) {
                lastFallTime = currentTime
                handleFallEvent()
            }
        }
    }

    private fun handleFallEvent() {
        speakAlert("您还好吗？请说'我没事'确认安全")
        Handler(Looper.getMainLooper()).postDelayed({
            sendEmergencyAlert()
        }, 15000)
    }

    private fun speakAlert(message: String) {
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun sendEmergencyAlert() {
        val location = getCurrentLocation()
        val locationText = if (location != null) {
            "位置：${location.latitude}, ${location.longitude}"
        } else {
            "位置：未知"
        }

        val smsManager = SmsManager.getDefault()
        val message = "紧急求助：老人可能跌倒！$locationText"
        for (contact in emergencyContacts) {
            try {
                smsManager.sendTextMessage(contact, null, message, null, null)
            } catch (e: Exception) {
                Log.e("FallDetectionService", "Error sending SMS: ${e.message}")
            }
        }

        if (emergencyContacts.isNotEmpty()) {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = android.net.Uri.parse("tel:${emergencyContacts[0]}")
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(callIntent)
            } catch (e: Exception) {
                Log.e("FallDetectionService", "Error making call: ${e.message}")
            }
        }
    }

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            } catch (e: SecurityException) {
                Log.e("FallDetectionService", "Error getting location: ${e.message}")
            }
        }
        return bestLocation
    }
}