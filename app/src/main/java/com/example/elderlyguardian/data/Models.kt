package com.example.elderlyguardian.data

data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String
)

data class Medication(
    val id: String,
    val name: String,
    val dosage: String,           // 每次服用剂量，如 "1 片"
    val frequency: String,        // 服用频率显示文本，如 "每日2次"
    val times: List<String>,      // 服用时间列表
    val startDate: String,
    val endDate: String?,
    val notes: String?,           // 备注
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 10,
    val effect: String? = null,   // 药效/作用，如 "降低血压"
    val duration: String? = null, // 药效持续时间，如 "24 小时"
    // 新增字段用于自定义频率
    val frequencyType: String = "daily", // daily, weekly, custom
    val frequencyValue: Int = 1,         // 每日/每周几次
    val frequencyDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 每周哪几天（1=周一）
    // 今日服用记录（时间索引 -> 是否已服用）
    val todayTaken: Map<Int, Boolean> = emptyMap()
)

data class MedicationRecord(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val takenTime: String,
    val date: String,
    val reminded: Boolean = false
)

data class MedicationReminder(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val scheduledTime: String,
    val reminded: Boolean = false,
    val date: String
)

data class UserSettings(
    val emergencyContacts: List<EmergencyContact>,
    val fallDetectionEnabled: Boolean,
    val voiceMonitoringEnabled: Boolean,
    val voiceCompanionEnabled: Boolean,
    val fontSize: Int,
    val medicationReminderEnabled: Boolean = true,
    val reminderVolume: Int = 80
)
