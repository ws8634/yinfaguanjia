package com.example.elderlyguardian.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.elderlyguardian.services.MedicationAlarmActivity

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medication_id")
        val medicationName = intent.getStringExtra("medication_name")
        val alarmTime = intent.getStringExtra("alarm_time")
        val timeIndex = intent.getIntExtra("time_index", 0)

        if (medicationName != null && medicationId != null) {
            Log.d("MedicationReminderReceiver", "收到闹钟提醒: $medicationName, 时间: $alarmTime, 索引: $timeIndex")

            // 启动闹钟Activity显示提醒界面
            val alarmIntent = Intent(context, MedicationAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("medicationId", medicationId)
                putExtra("medicationName", medicationName)
                putExtra("alarmTime", alarmTime ?: "")
                putExtra("timeIndex", timeIndex)
            }
            
            try {
                context.startActivity(alarmIntent)
                Log.d("MedicationReminderReceiver", "成功启动闹钟界面")
            } catch (e: Exception) {
                Log.e("MedicationReminderReceiver", "启动闹钟界面失败: ${e.message}")
            }
        }
    }
}