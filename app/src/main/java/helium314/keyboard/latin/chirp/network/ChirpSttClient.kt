package helium314.keyboard.latin.chirp.network

import android.util.Base64
import android.util.Log
import helium314.keyboard.latin.chirp.settings.ChirpPreferences.SttProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ChirpSttClient {
    private const val TAG = "ChirpSttClient"
    private const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/audio/transcriptions"
    private const val REQUESTY_ENDPOINT = "https://router.requesty.ai/v1/audio/transcriptions"

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val WAV_MEDIA = "audio/wav".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(
        wavData: ByteArray,
        apiKey: String,
        model: String,
        provider: SttProvider,
    ): Result<String> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        var attempt = 0
        val maxAttempts = 3
        var delayMs = 1000L

        while (attempt < maxAttempts) {
            try {
                Log.d("[DEBUG-CHIRP]", "transcribe() attempt ${attempt + 1}/$maxAttempts. Provider: $provider, WAV size: ${wavData.size} bytes")

                val request = buildRequest(wavData, apiKey, model, provider)
                client.newCall(request).execute().use { response ->
                    Log.d("[DEBUG-CHIRP]", "HTTP response code: ${response.code}")

                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty()
                        val responseCode = response.code
                        Log.e("[DEBUG-CHIRP]", "HTTP error response body: ${sanitizeErrorBody(errorBody)}")

                        // If it's a transient error (429, 408, 500-504), we can retry
                        if (responseCode == 429 || responseCode == 408 || responseCode in 500..504) {
                            val retryAfterHeader = response.header("Retry-After")
                            val retryAfterSeconds = retryAfterHeader?.toLongOrNull()
                            val actualDelay = if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                                retryAfterSeconds * 1000L
                            } else {
                                delayMs
                            }

                            Log.w("[DEBUG-CHIRP]", "STT request failed with transient HTTP $responseCode. Retrying in ${actualDelay}ms... (Attempt ${attempt + 1}/$maxAttempts)")
                            attempt++
                            if (attempt < maxAttempts) {
                                delay(actualDelay)
                                delayMs *= 2 // Exponential backoff
                                continue
                            }
                        }

                        val errorMessage = extractErrorMessage(errorBody)
                        Log.e("[DEBUG-CHIRP]", "STT request failed: HTTP $responseCode — $errorMessage")
                        return@withContext Result.failure(IOException("HTTP $responseCode: $errorMessage"))
                    }

                    val responseBody = response.body?.string()
                    Log.d("[DEBUG-CHIRP]", "HTTP success response body length: ${responseBody?.length ?: 0} chars")
                    if (responseBody == null) {
                        return@withContext Result.failure(IOException("Empty response body"))
                    }

                    val json = JSONObject(responseBody)
                    val text = json.optString("text", "").trim()

                    if (text.isEmpty()) {
                        return@withContext Result.failure(IOException("Empty transcription result"))
                    }

                    Log.d("[DEBUG-CHIRP]", "Transcription success: ${text.length} chars")
                    return@withContext Result.success(text)
                }
            } catch (e: IOException) {
                Log.w("[DEBUG-CHIRP]", "Network error during transcription (Attempt ${attempt + 1}/$maxAttempts)", e)
                lastException = e
                attempt++
                if (attempt < maxAttempts) {
                    delay(delayMs)
                    delayMs *= 2
                }
            } catch (e: Exception) {
                Log.e("[DEBUG-CHIRP]", "Unexpected non-retryable error during transcription", e)
                return@withContext Result.failure(e)
            }
        }

        Result.failure(lastException ?: IOException("Failed after $maxAttempts attempts"))
    }

    private fun buildRequest(wavData: ByteArray, apiKey: String, model: String, provider: SttProvider): Request =
        when (provider) {
            SttProvider.OPENROUTER -> buildOpenRouterRequest(wavData, apiKey, model)
            SttProvider.REQUESTY -> buildRequestyRequest(wavData, apiKey, model)
        }

    private fun buildOpenRouterRequest(wavData: ByteArray, apiKey: String, model: String): Request {
        val audioBase64 = Base64.encodeToString(wavData, Base64.NO_WRAP)
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("input_audio", JSONObject().apply {
                put("data", audioBase64)
                put("format", "wav")
            })
        }

        return Request.Builder()
            .url(OPENROUTER_ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
            .build()
    }

    private fun buildRequestyRequest(wavData: ByteArray, apiKey: String, model: String): Request {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("file", "audio.wav", wavData.toRequestBody(WAV_MEDIA))
            .build()

        return Request.Builder()
            .url(REQUESTY_ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return "unknown error"
        try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            if (errorObj != null) {
                val message = errorObj.optString("message")
                if (!message.isNullOrBlank()) {
                    return message
                }
            }
        } catch (e: Exception) {
            // Not a valid JSON or different format, fallback to sanitized body
        }
        return sanitizeErrorBody(body)
    }

    private fun sanitizeErrorBody(body: String): String {
        if (body.isBlank()) return "unknown error"
        return body
            .replace(Regex("sk-or-[A-Za-z0-9_-]+"), "[redacted]")
            .replace(Regex("Bearer\\s+[A-Za-z0-9._~-]+"), "Bearer [redacted]")
            .take(240)
    }
}
