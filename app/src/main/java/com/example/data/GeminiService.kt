package com.example.data

import com.squareup.moshi.JsonClass
import com.example.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiResponsePart(
    val text: String?
)

@JsonClass(generateAdapter = true)
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiResponseContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateCoachResponse(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    suspend fun getCoachingInsights(
        habitsText: String,
        moodsText: String,
        totalXp: Int,
        level: Int
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Ah! Your AI Coach key is missing. Please set your GEMINI_API_KEY inside the Secrets panel of google AI Studio to activate your personal Coach mentorship!"
        }

        val prompt = """
            You are "Momentum AI Coach", an elite behavioral psychologist, habit loop design expert, and supportive performance coach.
            Analyze the user's progress:
            
            - USER STATS: Level $level, Total earned XP: $totalXp
            - HABITS TRACKED:
            $habitsText
            
            - MOOD LOGS:
            $moodsText
            
            Provide a premium, inspiring, and concise coaching review (around 3 actionable bullet points and a concluding powerful sign-off).
            - Use habit loop science concepts (e.g. cue-routine-reward, friction modeling, identity-based habits, atomic increments).
            - Speak directly, empathetically, and with intense motivation.
            - Format utilizing clear spaced paragraphs and bullet points. Avoid using excessive stars or Markdown that does not match Android display layouts.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        return try {
            val response = api.generateCoachResponse(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Coach is reflecting on your habits. Please try again in a moment."
        } catch (e: Exception) {
            "Connection issue: ${e.localizedMessage ?: "Please confirm your internet connection and AI Coach API keys and try again."}"
        }
    }
}
