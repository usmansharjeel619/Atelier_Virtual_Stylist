package com.atelier.virtualstylist.data.network

import com.atelier.virtualstylist.BuildConfig
import com.atelier.virtualstylist.data.model.AnalyzeRequest
import com.atelier.virtualstylist.data.model.EditFlatLayRequest
import com.atelier.virtualstylist.data.model.FlatLayImageResponse
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.data.model.GenerateFlatLayRequest
import com.atelier.virtualstylist.data.model.InsightRequest
import com.atelier.virtualstylist.data.model.QuickTipsResponse
import com.atelier.virtualstylist.data.model.StyleInsight
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface StylistApiService {

    @POST("/api/stylist/analyze")
    suspend fun analyzeGarment(@Body body: AnalyzeRequest): Response<GarmentAnalysis>

    @POST("/api/stylist/style-insight")
    suspend fun fetchStyleInsight(@Body body: InsightRequest): Response<StyleInsight>

    @POST("/api/stylist/quick-tips")
    suspend fun fetchQuickTips(@Body body: InsightRequest): Response<QuickTipsResponse>

    @POST("/api/stylist/generate-flatlay")
    suspend fun generateFlatLay(@Body body: GenerateFlatLayRequest): Response<FlatLayImageResponse>

    @POST("/api/stylist/edit-flatlay")
    suspend fun editFlatLay(@Body body: EditFlatLayRequest): Response<FlatLayImageResponse>

    companion object {
        fun create(baseUrl: String = BuildConfig.API_BASE_URL): StylistApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            return Retrofit.Builder()
                .baseUrl(normalizedBase)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StylistApiService::class.java)
        }
    }
}
