package com.example.elderlyguardian.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.elderlyguardian.receivers.MedicationReminderReceiver
import java.util.*

class MedicationReminderManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    data class Medication(
        val id: String,
        val name: String,
        val dosage: String,
        val time: Calendar,
        val repeatDays: List<Int> // 1-7, 1 = Monday
    )

    /**
     * 检查是否可以设置精确闹钟
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * 打开闹钟权限设置页面
     */
    fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    }

    fun setMedicationReminder(medication: Medication) {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e("MedicationReminderManager", "Cannot schedule exact alarms, permission not granted")
            return
        }

        // 取消旧的提醒
        cancelMedicationReminder(medication.id)

        if (medication.repeatDays.isEmpty()) {
            // 一次性提醒
            setSingleReminder(medication, medication.time)
        } else {
            // 重复提醒 - 为每个重复天设置单独的闹钟
            medication.repeatDays.forEachIndexed { index, day ->
                val nextAlarmTime = getNextAlarmTime(medication.time, day)
                setSingleReminder(medication, nextAlarmTime, index)
            }
        }

        Log.d("MedicationReminderManager", "Set reminder for medication: ${medication.name}")
    }

    private fun setSingleReminder(medication: Medication, alarmTime: Calendar, dayIndex: Int = 0) {
        val timeStr = String.format("%02d:%02d", alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE))
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            putExtra("medication_id", medication.id)
            putExtra("medication_name", medication.name)
            putExtra("medication_dosage", medication.dosage)
            putExtra("alarm_time", timeStr)
            putExtra("time_index", dayIndex)  // 添加时间索引
        }

        // 使用不同的requestCode确保每个闹钟都是独立的
        val requestCode = (medication.id + "_$dayIndex").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+ 使用 setExactAndAllowWhileIdle
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6-11
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                }
                else -> {
                    // Android 6以下
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                }
            }
            Log.d("MedicationReminderManager", "Alarm set for ${medication.name} at ${alarmTime.time}, requestCode=$requestCode")
        } catch (e: SecurityException) {
            Log.e("MedicationReminderManager", "SecurityException: Cannot set alarm", e)
        } catch (e: Exception) {
            Log.e("MedicationReminderManager", "Exception: Cannot set alarm", e)
        }
    }

    fun cancelMedicationReminder(medicationId: String) {
        // 取消所有可能的闹钟（最多7个，对应一周的7天）
        (0..6).forEach { dayIndex ->
            val intent = Intent(context, MedicationReminderReceiver::class.java)
            val requestCode = (medicationId + "_$dayIndex").hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        Log.d("MedicationReminderManager", "Cancelled all reminders for medication: $medicationId")
    }

    private fun getNextAlarmTime(baseTime: Calendar, targetDay: Int): Calendar {
        val currentTime = Calendar.getInstance()
        val nextAlarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, baseTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, baseTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 计算目标星期几与当前星期几的差距
        // targetDay: 1=周一, 7=周日
        // Calendar.DAY_OF_WEEK: 1=周日, 2=周一, ..., 7=周六
        val calendarDayOfWeek = when (targetDay) {
            1 -> Calendar.MONDAY      // 2
            2 -> Calendar.TUESDAY     // 3
            3 -> Calendar.WEDNESDAY   // 4
            4 -> Calendar.THURSDAY    // 5
            5 -> Calendar.FRIDAY      // 6
            6 -> Calendar.SATURDAY    // 7
            7 -> Calendar.SUNDAY      // 1
            else -> Calendar.MONDAY
        }

        nextAlarmTime.set(Calendar.DAY_OF_WEEK, calendarDayOfWeek)

        // 如果目标时间已过，设置为下一周
        if (nextAlarmTime.timeInMillis <= currentTime.timeInMillis) {
            nextAlarmTime.add(Calendar.WEEK_OF_YEAR, 1)
        }

        Log.d("MedicationReminderManager", "Next alarm time: ${nextAlarmTime.time}")
        return nextAlarmTime
    }
}