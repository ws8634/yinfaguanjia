package com.example.elderlyguardian.network

import com.example.elderlyguardian.data.QWeatherLocationResponse
import com.example.elderlyguardian.data.QWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    // 实时天气查询
    @GET("v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String = "101010100", // 默认北京
        @Query("key") apiKey: String
    ): Response<QWeatherResponse>
    
    // 地理位置查询
    @GET("v2/city/lookup")
    suspend fun lookupCity(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<QWeatherLocationResponse>
}

// API配置
object WeatherApiConfig {
    const val BASE_URL = "https://p25ctuftah.re.qweatherapi.com/"
    const val API_KEY = "c4524877fcb443e09800f312b09810ec"
    
    // 城市代码映射（常用城市）
    val CITY_CODES = mapOf(
        "北京" to "101010100",
        "上海" to "101020100",
        "广州" to "101280101",
        "深圳" to "101280601",
        "杭州" to "101210101",
        "南京" to "101190101",
        "成都" to "101270101",
        "武汉" to "101200101",
        "西安" to "101110101",
        "重庆" to "101040100"
    )
}
