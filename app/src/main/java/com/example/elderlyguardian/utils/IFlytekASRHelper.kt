package com.example.elderlyguardian.utils

import android.content.Context
import android.util.Log
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import com.iflytek.sparkchain.core.asr.AudioAttributes

/**
 * 科大讯飞ASR语音识别帮助类
 * 实现真正的讯飞语音识别功能
 */
class IFlytekASRHelper private constructor() {
    private val TAG = "IFlytekASRHelper"
    private var isInitialized = false
    private var asr: ASR? = null
    private var callback: ASRCallback? = null
    
    companion object {
        @Volatile
        private var instance: IFlytekASRHelper? = null
        
        fun getInstance(): IFlytekASRHelper {
            return instance ?: synchronized(this) {
                instance ?: IFlytekASRHelper().also { instance = it }
            }
        }
    }
    
    /**
     * 初始化讯飞SDK
     */
    fun initialize(
        context: Context,
        appId: String = "1fa0e787",
        apiSecret: String = "ODc5MWFjY2MyYjM0MWZjNjA5ODg5NTBl",
        apiKey: String = "eb307f81f5e718939a524b497b1ecf2c"
    ): Boolean {
        if (isInitialized) {
            Log.d(TAG, "SDK already initialized")
            return true
        }
        
        Log.d(TAG, "开始初始化SDK...")
        Log.d(TAG, "APPID: $appId")
        Log.d(TAG, "APIKey: ${apiKey.take(8)}...")
        
        return try {
            val config = SparkChainConfig.builder()
            config.appID(appId)
            config.apiKey(apiKey)
            config.apiSecret(apiSecret)
            
            Log.d(TAG, "调用SparkChain.init...")
            val result = SparkChain.getInst().init(context, config)
            Log.d(TAG, "SparkChain.init返回: $result")
            
            if (result == 0) {
                isInitialized = true
                // 创建ASR实例
                Log.d(TAG, "创建ASR实例...")
                asr = ASR()
                Log.d(TAG, "ASR实例创建成功")
                setupASRParams()
                Log.d(TAG, "ASR参数设置完成")
                Log.d(TAG, "SparkChain SDK和ASR初始化成功")
                true
            } else {
                Log.e(TAG, "SparkChain SDK初始化失败，错误码: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化SDK时发生异常: ${e.message}", e)
            false
        }
    }
    
    /**
     * 设置ASR参数
     */
    private fun setupASRParams() {
        asr?.let { asrInstance ->
            // 设置语言为中文
            asrInstance.language("zh_cn")
            // 设置领域为日常用语
            asrInstance.domain("iat")
            // 设置方言为普通话
            asrInstance.accent("mandarin")
            // 设置VAD后端点检测时间（毫秒）
            asrInstance.vadEos(2000)
            // 设置标点符号
            asrInstance.ptt(true)
        }
    }
    
    // 录音相关
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    /**
     * 开始语音识别
     */
    fun startListening(callback: ASRCallback) {
        if (!isInitialized || asr == null) {
            callback.onError(-1, "ASR not initialized")
            return
        }
        
        this.callback = callback
        
        try {
            // 注册回调
            asr?.registerCallbacks(object : AsrCallbacks {
                override fun onResult(result: ASR.ASRResult?, userData: Any?) {
                    callback.onDebug("onResult回调被触发")
                    result?.let { asrResult ->
                        // 尝试获取识别结果
                        var recognizedText = ""
                        
                        // 方法1: 尝试getBestMatchText
                        try {
                            recognizedText = asrResult.bestMatchText ?: ""
                            callback.onDebug("bestMatchText='$recognizedText'")
                        } catch (e: Exception) {
                            callback.onDebug("bestMatchText获取失败: ${e.message}")
                        }
                        
                        // 方法2: 如果为空，尝试从Transcriptions获取
                        if (recognizedText.isEmpty()) {
                            try {
                                val transcriptions = asrResult.transcriptions
                                callback.onDebug("transcriptions数量: ${transcriptions?.size ?: 0}")
                                
                                if (transcriptions.isNullOrEmpty()) {
                                    callback.onDebug("transcriptions为空!")
                                } else {
                                    transcriptions.forEachIndexed { index, transcription ->
                                        callback.onDebug("transcription[$index] segments数量: ${transcription.segments?.size ?: 0}")
                                        
                                        if (transcription.segments.isNullOrEmpty()) {
                                            callback.onDebug("transcription[$index] segments为空!")
                                        } else {
                                            transcription.segments.forEachIndexed { segIndex, segment ->
                                                val segText = segment.text ?: "null"
                                                callback.onDebug("segment[$segIndex] text='$segText'")
                                                recognizedText += segment.text ?: ""
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                callback.onDebug("transcriptions获取失败: ${e.message}")
                            }
                        }
                        
                        callback.onDebug("最终识别文本='$recognizedText'")
                        callback.onResult(recognizedText)
                    } ?: run {
                        callback.onDebug("错误: result为null")
                        callback.onResult("")
                    }
                }
                
                override fun onError(error: ASR.ASRError?, userData: Any?) {
                    error?.let {
                        callback.onError(it.code, it.errMsg ?: "Unknown error")
                    }
                }
                
                override fun onBeginOfSpeech() {
                    callback.onBeginOfSpeech()
                }
                
                override fun onEndOfSpeech() {
                    callback.onEndOfSpeech()
                }
                
                override fun onRecordVolume(volume: Double, data: Int) {
                    callback.onVolumeChanged(volume)
                }
            })
            
            // 开始识别（不使用AudioAttributes，让SDK自动处理）
            callback.onDebug("启动ASR识别...")
            val result = asr?.start(null)
            if (result != 0) {
                callback.onError(result ?: -1, "Failed to start ASR")
                return
            }
            
            callback.onStart()
            callback.onDebug("ASR启动成功，开始录音...")
            
            // 启动录音线程
            startRecording(callback)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ASR", e)
            callback.onError(-2, e.message ?: "Unknown error")
        }
    }
    
    /**
     * 开始录音并写入ASR
     */
    private fun startRecording(callback: ASRCallback) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                callback.onDebug("录音初始化失败")
                callback.onError(-3, "录音初始化失败")
                return
            }
            
            isRecording = true
            audioRecord?.startRecording()
            callback.onDebug("录音已开始")
            
            // 在后台线程中读取音频数据
            Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // 写入ASR
                        asr?.write(buffer.copyOf(readSize))
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "录音启动失败", e)
            callback.onDebug("录音启动失败: ${e.message}")
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        try {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            asr?.stop(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ASR", e)
        }
    }
    
    /**
     * 取消语音识别
     */
    fun cancelListening() {
        try {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            asr?.stop(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling ASR", e)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            asr = null
            SparkChain.getInst().unInit()
            isInitialized = false
            Log.d(TAG, "ASR and SparkChain SDK released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ASR", e)
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized && asr != null
}

/**
 * ASR回调接口
 */
interface ASRCallback {
    fun onStart()
    fun onResult(result: String)
    fun onError(code: Int, message: String)
    fun onBeginOfSpeech()
    fun onEndOfSpeech()
    fun onVolumeChanged(volume: Double)
    fun onDebug(message: String) {}
}
