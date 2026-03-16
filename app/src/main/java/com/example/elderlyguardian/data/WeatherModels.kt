package com.example.elderlyguardian.data

// 和风天气 API 响应数据类
data class QWeatherResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: QWeatherNow,
    val refer: QWeatherRefer?
)

data class QWeatherNow(
    val obsTime: String,
    val temp: String,
    val feelsLike: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String,
    val cloud: String,
    val dew: String
)

data class QWeatherRefer(
    val sources: List<String>?,
    val license: List<String>?
)

// 地理位置查询响应
data class QWeatherLocationResponse(
    val code: String,
    val location: List<QWeatherLocation>?,
    val refer: QWeatherRefer?
)

data class QWeatherLocation(
    val name: String,
    val id: String,
    val lat: String,
    val lon: String,
    val adm2: String,
    val adm1: String,
    val country: String,
    val tz: String,
    val utcOffset: String,
    val isDst: String,
    val type: String,
    val rank: String,
    val fxLink: String
)

// 天气数据封装类
sealed class WeatherResult {
    data class Success(
        val city: String,
        val temperature: String,
        val condition: String,
        val windDir: String,
        val windScale: String,
        val humidity: String,
        val updateTime: String
    ) : WeatherResult()
    
    data class Error(val message: String) : WeatherResult()
}
