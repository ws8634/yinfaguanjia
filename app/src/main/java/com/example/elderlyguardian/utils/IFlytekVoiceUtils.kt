package com.example.elderlyguardian.utils

import android.content.Context
import android.util.Log

/**
 * 科大讯飞SparkChain语音工具类
 * 简化版本，用于SDK初始化和状态检测
 */
class IFlytekVoiceUtils private constructor() {
    private val TAG = "IFlytekVoiceUtils"
    private var isInitialized = false
    
    companion object {
        @Volatile
        private var instance: IFlytekVoiceUtils? = null
        
        fun getInstance(): IFlytekVoiceUtils {
            return instance ?: synchronized(this) {
                instance ?: IFlytekVoiceUtils().also { instance = it }
            }
        }
    }
    
    /**
     * 初始化讯飞SDK
     * 使用反射方式调用，避免编译时依赖问题
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
        
        return try {
            // 使用反射获取SparkChain类
            val sparkChainClass = Class.forName("com.iflytek.sparkchain.core.SparkChain")
            val sparkChainConfigClass = Class.forName("com.iflytek.sparkchain.core.SparkChainConfig")
            
            // 创建配置对象
            val configBuilder = sparkChainConfigClass.getMethod("builder").invoke(null)
            configBuilder.javaClass.getMethod("appID", String::class.java).invoke(configBuilder, appId)
            configBuilder.javaClass.getMethod("apiKey", String::class.java).invoke(configBuilder, apiKey)
            configBuilder.javaClass.getMethod("apiSecret", String::class.java).invoke(configBuilder, apiSecret)
            val config = configBuilder.javaClass.getMethod("build").invoke(configBuilder)
            
            // 获取SparkChain实例并初始化
            val getInstMethod = sparkChainClass.getMethod("getInst")
            val sparkChain = getInstMethod.invoke(null)
            val initMethod = sparkChainClass.getMethod("init", Context::class.java, sparkChainConfigClass)
            val result = initMethod.invoke(sparkChain, context, config) as Int
            
            if (result == 0) {
                isInitialized = true
                Log.d(TAG, "SparkChain SDK initialized successfully")
                true
            } else {
                Log.e(TAG, "SparkChain SDK initialization failed: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SparkChain SDK", e)
            false
        }
    }
    
    /**
     * 发送消息（简化版）
     */
    fun sendMessage(
        message: String,
        callback: IFlytekVoiceCallback
    ) {
        if (!isInitialized) {
            callback.onError(-1, "SDK not initialized")
            return
        }
        
        // 简化实现，返回测试消息
        callback.onResult("讯飞SDK已初始化成功！\n您说：$message\n（大模型对话功能开发中）")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            val sparkChainClass = Class.forName("com.iflytek.sparkchain.core.SparkChain")
            val getInstMethod = sparkChainClass.getMethod("getInst")
            val sparkChain = getInstMethod.invoke(null)
            val unInitMethod = sparkChainClass.getMethod("unInit")
            unInitMethod.invoke(sparkChain)
            isInitialized = false
            Log.d(TAG, "SparkChain SDK released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SDK", e)
        }
    }
    
    /**
     * 检查SDK是否已初始化
     */
    fun isReady(): Boolean = isInitialized
}

/**
 * 讯飞语音回调接口
 */
interface IFlytekVoiceCallback {
    fun onResult(result: String)
    fun onError(code: Int, message: String)
}
