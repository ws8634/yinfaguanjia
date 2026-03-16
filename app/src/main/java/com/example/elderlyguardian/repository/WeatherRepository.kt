package com.example.elderlyguardian.repository

import android.util.Log
import com.example.elderlyguardian.data.WeatherResult
import com.example.elderlyguardian.network.RetrofitClient
import com.example.elderlyguardian.network.WeatherApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {
    private val weatherApi = RetrofitClient.weatherApiService
    private val TAG = "WeatherRepository"
    
    // 模拟数据生成（原有功能）
    fun generateSimulatedWeather(city: String = "北京"): String {
        val conditions = listOf("晴朗", "多云", "阴天", "小雨", "雷阵雨", "晴间多云")
        val temperatures = (15..32).toList()
        val windDirections = listOf("东风", "南风", "西风", "北风", "东南风", "西北风")
        val windLevels = listOf("1级", "2级", "3级", "4级", "5级")
        
        val condition = conditions.random()
        val temp = temperatures.random()
        val windDir = windDirections.random()
        val windLevel = windLevels.random()
        
        return "今天${city}天气${condition}，气温${temp}°C，${windDir}${windLevel}。模拟数据仅供参考。"
    }
    
    // 从API获取真实天气数据
    suspend fun fetchRealWeatherFromApi(city: String = "北京"): WeatherResult {
        return withContext(Dispatchers.IO) {
            try {
                // 获取城市代码
                val locationCode = WeatherApiConfig.CITY_CODES[city] ?: "101010100"
                
                // 调用API
                val response = weatherApi.getCurrentWeather(
                    location = locationCode,
                    apiKey = WeatherApiConfig.API_KEY
                )
                
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    if (weatherData != null && weatherData.code == "200") {
                        val now = weatherData.now
                        WeatherResult.Success(
                            city = city,
                            temperature = now.temp,
                            condition = now.text,
                            windDir = now.windDir,
                            windScale = now.windScale,
                            humidity = now.humidity,
                            updateTime = weatherData.updateTime
                        )
                    } else {
                        WeatherResult.Error("获取天气数据失败：${weatherData?.code}")
                    }
                } else {
                    WeatherResult.Error("网络请求失败：${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取天气数据异常", e)
                WeatherResult.Error("获取天气数据异常：${e.message}")
            }
        }
    }
    
    // 将天气结果格式化为字符串
    fun formatWeatherResult(result: WeatherResult): String {
        return when (result) {
            is WeatherResult.Success -> {
                "今天${result.city}天气${result.condition}，" +
                "气温${result.temperature}°C，" +
                "${result.windDir}${result.windScale}，" +
                "相对湿度${result.humidity}%。"
            }
            is WeatherResult.Error -> {
                "抱歉，无法获取天气信息：${result.message}"
            }
        }
    }
}
