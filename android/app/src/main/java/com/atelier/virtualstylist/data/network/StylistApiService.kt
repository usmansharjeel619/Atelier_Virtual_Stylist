package com.atelier.virtualstylist.data.network

import android.content.Context
import com.atelier.virtualstylist.BuildConfig
import com.atelier.virtualstylist.data.model.AnalyzeRequest
import com.atelier.virtualstylist.data.model.EditFlatLayRequest
import com.atelier.virtualstylist.data.model.FlatLayImageResponse
import com.atelier.virtualstylist.data.model.GarmentAnalysis
import com.atelier.virtualstylist.data.model.GenerateFlatLayRequest
import com.atelier.virtualstylist.data.model.InsightRequest
import com.atelier.virtualstylist.data.model.QuickTipsResponse
import com.atelier.virtualstylist.data.model.StyleInsight
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class HealthResponse(val ok: Boolean = false)

object ServerConfigStore {
    private const val PREFS = "atelier_server_prefs"
    private const val KEY_URL = "api_base_url"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_URL, null)?.trim()
        return if (!saved.isNullOrBlank()) normalizeUrl(saved) else normalizeUrl(BuildConfig.API_BASE_URL)
    }

    fun setBaseUrl(context: Context, rawUrl: String): String {
        val clean = normalizeUrl(rawUrl)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, clean)
            .apply()
        return clean
    }

    fun resetBaseUrl(context: Context): String {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_URL)
            .apply()
        return normalizeUrl(BuildConfig.API_BASE_URL)
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().ifBlank { BuildConfig.API_BASE_URL }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}

interface StylistApiService {

    @GET("/api/health")
    suspend fun healthCheck(): Response<HealthResponse>

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
        private val gson: Gson = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()

        private val jsonGuardInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            // Catch auth redirects (e.g., AI Studio /__cookie_check.html) or HTML error pages
            if (response.code in 300..399) {
                val loc = response.header("Location") ?: ""
                response.close()
                val msg = if (loc.contains("__cookie_check") || loc.contains("accounts.google")) {
                    "This dev URL requires browser sign-in. In AI Studio click Deploy / Share App, then paste your public URL under Server in the top bar."
                } else {
                    "Backend redirected (${response.code}) instead of returning JSON. Verify your public Server URL in the top bar."
                }
                return@Interceptor syntheticJsonError(request, 502, msg)
            }

            val body = response.body
            val contentType = response.header("Content-Type")?.lowercase().orEmpty()
            val raw = body?.string().orEmpty()
            val trimmed = raw.trimStart()

            val looksLikeHtml = contentType.contains("text/html") ||
                trimmed.startsWith("<") ||
                trimmed.startsWith("<!doctype", ignoreCase = true)

            if (looksLikeHtml || (trimmed.isEmpty() && !response.isSuccessful)) {
                val msg = when {
                    response.code == 404 ->
                        "Backend returned 404 Not Found. Make sure you clicked Deploy / Share App in AI Studio or update the Server URL in the top bar."
                    raw.contains("__cookie_check", ignoreCase = true) || raw.contains("accounts.google", ignoreCase = true) ->
                        "Dev preview URL requires browser cookies. Publish/Share your app in AI Studio and paste that public URL under Server in the top bar."
                    else ->
                        "Server returned an HTML page (HTTP ${response.code}) instead of JSON. Check your Server URL in the top bar."
                }
                return@Interceptor syntheticJsonError(request, 502, msg)
            }

            val mediaType = (contentType.ifBlank { "application/json; charset=utf-8" }).toMediaType()
            response.newBuilder()
                .body(raw.toResponseBody(mediaType))
                .build()
        }

        private fun syntheticJsonError(
            request: okhttp3.Request,
            code: Int,
            message: String
        ): okhttp3.Response {
            val payload = gson.toJson(mapOf("error" to message))
            return okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Bad Gateway")
                .header("Content-Type", "application/json")
                .body(payload.toResponseBody("application/json; charset=utf-8".toMediaType()))
                .build()
        }

        fun create(baseUrl: String = BuildConfig.API_BASE_URL): StylistApiService {
            val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(jsonGuardInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            return Retrofit.Builder()
                .baseUrl(normalizedBase)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(StylistApiService::class.java)
        }
    }
}
