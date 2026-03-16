package com.example.elderlyguardian.services

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderlyguardian.MainActivity
import com.example.elderlyguardian.utils.MedicationStorage
import java.text.SimpleDateFormat
import java.util.*

class MedicationAlarmActivity : ComponentActivity() {
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var isSpeaking = false
    private val handler = Handler(Looper.getMainLooper())
    private var medicationId: String = ""
    private var medicationName: String = "药品"
    private var alarmTime: String = ""
    private var timeIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MedicationAlarmActivity", "onCreate called")

        medicationId = intent.getStringExtra("medicationId") ?: ""
        medicationName = intent.getStringExtra("medicationName") ?: "药品"
        alarmTime = intent.getStringExtra("alarmTime") ?: ""
        timeIndex = intent.getIntExtra("timeIndex", 0)
        
        Log.d("MedicationAlarmActivity", "medicationId: $medicationId, medicationName: $medicationName, alarmTime: $alarmTime, timeIndex: $timeIndex")

        // 设置全屏显示，即使在锁屏状态下
        setupLockScreenFlags()

        // 显示界面
        setContent {
            MaterialTheme {
                MedicationAlarmScreen(
                    medicationName = medicationName,
                    alarmTime = alarmTime,
                    onTake = {
                        onTakeMedication()
                    },
                    onDismiss = {
                        onDismissAlarm()
                    }
                )
            }
        }

        // 延迟初始化TTS和震动，避免阻塞UI渲染
        handler.postDelayed({
            initTextToSpeech(medicationName)
            startVibration()
        }, 500)
    }

    private fun setupLockScreenFlags() {
        // 添加所有可能的锁屏显示标志
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Android 8.1+ 使用新API
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Android 10+ 额外设置
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.addFlags(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }

        // 请求解锁屏幕
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguardManager.isKeyguardLocked) {
                try {
                    val localKeyguardLock = keyguardManager.newKeyguardLock("MedicationAlarm")
                    localKeyguardLock.disableKeyguard()
                    Log.d("MedicationAlarmActivity", "已请求解锁屏幕")
                } catch (e: Exception) {
                    Log.e("MedicationAlarmActivity", "解锁屏幕失败: ${e.message}")
                }
            }
        }
    }

    private fun initTextToSpeech(medicationName: String) {
        try {
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.CHINA
                    isSpeaking = true
                    startContinuousSpeaking(medicationName)
                } else {
                    Log.e("MedicationAlarmActivity", "TTS初始化失败: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmActivity", "TTS初始化异常: ${e.message}")
        }
    }

    /**
     * 连续播报直到用户操作
     */
    private fun startContinuousSpeaking(medicationName: String) {
        val message = "该吃$medicationName 了，记得按时服药哦"
        
        // 立即播报一次
        speakMessage(message)
        
        // 每5秒重复播报
        val speakRunnable = object : Runnable {
            override fun run() {
                if (isSpeaking) {
                    speakMessage(message)
                    handler.postDelayed(this, 5000)
                }
            }
        }
        handler.postDelayed(speakRunnable, 5000)
    }

    private fun speakMessage(message: String) {
        try {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            Log.e("MedicationAlarmActivity", "语音播报失败: ${e.message}")
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let { vib ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000),
                        0 // 重复
                    )
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000), 0)
                }
            }
        } catch (e: Exception) {
            Log.e("MedicationAlarmActivity", "震动失败: ${e.message}")
        }
    }

    private fun onTakeMedication() {
        Log.d("MedicationAlarmActivity", "点击已服用按钮")
        // 先停止闹钟和返回，再更新状态（避免阻塞UI）
        stopAlarm()
        returnToApp()
        
        // 后台更新用药记录的服用状态
        if (medicationId.isNotEmpty()) {
            try {
                updateMedicationTakenStatus()
            } catch (e: Exception) {
                Log.e("MedicationAlarmActivity", "更新服用状态失败: ${e.message}")
            }
        }
    }

    /**
     * 更新用药记录的服用状态
     */
    private fun updateMedicationTakenStatus() {
        val storage = MedicationStorage(this)
        val medications = storage.loadMedications().toMutableList()
        val index = medications.indexOfFirst { it.id == medicationId }
        
        if (index != -1) {
            val medication = medications[index]
            // 更新对应时间的服用状态
            val updatedTodayTaken = medication.todayTaken.toMutableMap()
            updatedTodayTaken[timeIndex] = true
            
            // 创建更新后的用药记录
            val updatedMedication = medication.copy(
                todayTaken = updatedTodayTaken
            )
            
            medications[index] = updatedMedication
            storage.saveMedications(medications)
            
            Log.d("MedicationAlarmActivity", "已更新服用状态: medicationId=$medicationId, timeIndex=$timeIndex")
        }
    }

    private fun onDismissAlarm() {
        Log.d("MedicationAlarmActivity", "点击稍后按钮")
        stopAlarm()
        returnToApp()
    }

    /**
     * 返回到应用主界面
     */
    private fun returnToApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_alarm", true)
        }
        startActivity(intent)
        finish()
    }

    private fun stopAlarm() {
        isSpeaking = false
        handler.removeCallbacksAndMessages(null)
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e("MedicationAlarmActivity", "停止TTS失败: ${e.message}")
        }
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("MedicationAlarmActivity", "停止震动失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    override fun onBackPressed() {
        // 禁止返回键，必须点击按钮
    }
}

@Composable
fun MedicationAlarmScreen(
    medicationName: String,
    alarmTime: String,
    onTake: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF6B35))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：图标和时间放在一行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 药罐图标
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = "用药提醒",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 当前时间
                Text(
                    text = currentTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 中部：提醒标题和药品信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 提醒标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⏰",
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "用药时间到了！",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 药品信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = medicationName,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "记得按时服药哦",
                            fontSize = 28.sp,
                            color = Color(0xFF666666)
                        )

                        if (alarmTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "预定时间: $alarmTime",
                                fontSize = 22.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
            }

            // 底部：操作按钮
            Column(
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 稍后提醒按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "稍后",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 已服用按钮
                    Button(
                        onClick = onTake,
                        modifier = Modifier
                            .weight(1.8f)
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "已服用",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 提示文字
                Text(
                    text = "银发管家提醒您注意身体健康",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}