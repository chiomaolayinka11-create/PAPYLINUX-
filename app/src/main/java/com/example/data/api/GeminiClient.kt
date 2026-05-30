package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Gemini Request Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

// --- Gemini Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Agent Structured Models ---

@JsonClass(generateAdapter = true)
data class AgentCommand(
    @Json(name = "description") val description: String,
    @Json(name = "cmd") val cmd: String
)

@JsonClass(generateAdapter = true)
data class AgentResponse(
    @Json(name = "explanation") val explanation: String,
    @Json(name = "commands") val commands: List<AgentCommand>
)

// --- Gemini Retrofit API Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Moshi and Retrofit Client ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun askAgent(prompt: String): AgentResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured. Please supply it in the Secrets Panel.")
        }

        val systemInstruction = "You are 'Termux Automation AI Agent', a local shell automation assistant. " +
                "The user will propose a task to run/automate inside their Termux app, including physical phone control requests. " +
                "Your job is to build a sequence of commands to solve the user's issue and explain the procedure clearly. " +
                "You have access to Android's physical phone features through standard Termux API binaries. When appropriate or requested, utilize them: " +
                "\n- Text-To-Speech: `termux-tts-speak \"<text>\"`" +
                "\n- Flashlight/Torch: `termux-torch [on|off]`" +
                "\n- Screen Brightness: `termux-brightness <0-255>`" +
                "\n- Volume Control: `termux-volume [music|ring|alarm|system|notification] <0-15>`" +
                "\n- Device Vibrator: `termux-vibrate -d <duration_ms>`" +
                "\n- Generate Push Notification: `termux-notification -t \"<title>\" -c \"<content>\"`" +
                "\n- Toast Popup: `termux-toast -s \"<message>\"`" +
                "\n- Show Battery Status: `termux-battery-status`" +
                "\n- Contact List: `termux-contact-list`" +
                "\n- GPS Location: `termux-location`" +
                "\n- Set clipboard: `termux-clipboard-set \"<text>\"`" +
                "\n- Get clipboard: `termux-clipboard-get`" +
                "\n- Take Photo: `termux-camera-photo -c <camera_id> <output_path.jpg>`" +
                "\n\nYou must return the response strictly as a JSON object containing an 'explanation' text string and a list of 'commands' representing individual steps. " +
                "Do NOT use any markdown wrap like ```json or prefix/suffix. Just return pure JSON text conforming strictly to: " +
                "{\n" +
                "  \"explanation\": \"String explanation\",\n" +
                "  \"commands\": [\n" +
                "    { \"description\": \"What this command step does\", \"cmd\": \"The exact shell command string\" }\n" +
                "  ]\n" +
                "}"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val response = apiService.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return null

        return try {
            val adapter = moshi.adapter(AgentResponse::class.java)
            adapter.fromJson(responseText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
