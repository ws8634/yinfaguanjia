package com.example.elderlyguardian.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.elderlyguardian.services.FallDetectionService
import com.example.elderlyguardian.services.VoiceMonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 启动语音监测服务
            val voiceIntent = Intent(context, VoiceMonitoringService::class.java)
            context.startForegroundService(voiceIntent)
            
            // 启动跌倒检测服务
            val fallIntent = Intent(context, FallDetectionService::class.java)
            context.startForegroundService(fallIntent)
        }
    }
}
