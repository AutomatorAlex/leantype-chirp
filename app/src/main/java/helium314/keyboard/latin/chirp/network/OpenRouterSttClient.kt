package helium314.keyboard.latin.chirp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object OpenRouterSttClient {
    private const val TAG = "OpenRouterSttClient"
    private const val ENDPOINT = "https://openrouter.ai/api/v1/audio/transcriptions"

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(audioBase64: String, apiKey: String, model: String = "google/chirp-3"): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val bodyJson = JSONObject().apply {
                    put("model", model)
                    put("input_audio", JSONObject().apply {
                        put("data", audioBase64)
                        put("format", "wav")
                    })
                }

                val requestBody = bodyJson.toString().toRequestBody(JSON_MEDIA)

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    val safeError = sanitizeErrorBody(errorBody)
                    Log.e(TAG, "STT request failed: HTTP ${response.code} — $safeError")
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $safeError"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val json = JSONObject(responseBody)
                val text = json.optString("text", "").trim()

                if (text.isEmpty()) {
                    return@withContext Result.failure(IOException("Empty transcription result"))
                }

                Log.d(TAG, "Transcription success: ${text.length} chars")
                Result.success(text)
            } catch (e: IOException) {
                Log.e(TAG, "Network error during transcription", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during transcription", e)
                Result.failure(e)
            }
        }

    private fun sanitizeErrorBody(body: String): String {
        if (body.isBlank()) return "unknown error"
        return body
            .replace(Regex("sk-or-[A-Za-z0-9_-]+"), "[redacted]")
            .replace(Regex("Bearer\\s+[A-Za-z0-9._~-]+"), "Bearer [redacted]")
            .take(240)
    }
}
