package helium314.keyboard.latin.chirp

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.chirp.audio.AudioRecorder
import helium314.keyboard.latin.chirp.audio.WavEncoder
import helium314.keyboard.latin.chirp.network.OpenRouterSttClient
import helium314.keyboard.latin.chirp.settings.ChirpPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import android.util.Log

class ChirpVoiceController(private val ime: LatinIME) {

    companion object {
        private const val TAG = "ChirpVoiceController"
    }

    interface StateListener {
        fun onStateChanged(state: State)
    }

    enum class State { IDLE, RECORDING, TRANSCRIBING }

    @Volatile
    private var state = State.IDLE

    private var stateListener: StateListener? = null

    fun setStateListener(listener: StateListener?) {
        stateListener = listener
    }

    private val prefs = ChirpPreferences(ime)
    private val recorder = AudioRecorder(ime)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isEnabled(): Boolean = prefs.isVoiceEnabled()

    private fun setState(newState: State) {
        if (state != newState) {
            state = newState
            stateListener?.onStateChanged(newState)
        }
    }

    fun getState(): State = state

    fun toggleRecording(): Boolean {
        Log.d("[DEBUG-CHIRP]", "toggleRecording() called. Current state: $state")
        when (state) {
            State.IDLE -> {
                if (!isEnabled()) {
                    Log.d("[DEBUG-CHIRP]", "Chirp voice is disabled in preferences")
                    return false
                }
                val apiKey = prefs.getApiKey()
                if (apiKey.isBlank()) {
                    Log.e("[DEBUG-CHIRP]", "API key is blank")
                    toast("Chirp voice error: API key not set")
                    return false
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val granted = ime.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        Log.e("[DEBUG-CHIRP]", "RECORD_AUDIO permission denied")
                        toast("Chirp voice error: RECORD_AUDIO permission denied")
                        return false
                    }
                }
                val startTime = System.currentTimeMillis()
                var autoStopped = false
                setState(State.RECORDING)
                recorder.start()
                toast("🎙 Listening… tap mic again to stop")
                scope.launch {
                    val maxDurationMs = AudioRecorder.MAX_RECORDING_SECONDS * 1000L
                    while (recorder.isRecordingActive() && state == State.RECORDING) {
                        delay(200)
                        if (System.currentTimeMillis() - startTime >= maxDurationMs - 500) {
                            autoStopped = true
                        }
                    }
                    Log.d("[DEBUG-CHIRP]", "Recording loop finished. isRecordingActive: ${recorder.isRecordingActive()}, state: $state, autoStopped: $autoStopped")
                    if (state == State.RECORDING) {
                        if (autoStopped) {
                            Log.d("[DEBUG-CHIRP]", "Auto-stopping recording due to max duration limit (50s)")
                            toast("⏱ Max limit reached, transcribing…")
                        }
                        stopAndTranscribe()
                    }
                }
                return true
            }
            State.RECORDING -> {
                Log.d("[DEBUG-CHIRP]", "Stopping recording manually")
                stopAndTranscribe()
                return true
            }
            State.TRANSCRIBING -> {
                Log.d("[DEBUG-CHIRP]", "Tap ignored: already transcribing")
                toast("Still transcribing…")
                return true
            }
        }
    }

    private fun stopAndTranscribe() {
        Log.d("[DEBUG-CHIRP]", "stopAndTranscribe() called. Current state: $state")
        if (state != State.RECORDING) return
        setState(State.TRANSCRIBING)
        toast("Transcribing…")
        scope.launch {
            try {
                Log.d("[DEBUG-CHIRP]", "Stopping recorder and retrieving PCM data")
                val pcmData = withContext(Dispatchers.IO) { recorder.stop() }
                Log.d("[DEBUG-CHIRP]", "PCM data size: ${pcmData.size} bytes")
                if (pcmData.isEmpty()) {
                    Log.e("[DEBUG-CHIRP]", "PCM data is empty")
                    toast("Chirp voice error: no audio captured")
                    return@launch
                }
                Log.d("[DEBUG-CHIRP]", "Encoding PCM to WAV")
                val wavData = withContext(Dispatchers.IO) { WavEncoder.encode(pcmData) }
                Log.d("[DEBUG-CHIRP]", "WAV data size: ${wavData.size} bytes")
                val base64 = Base64.encodeToString(wavData, Base64.NO_WRAP)
                Log.d("[DEBUG-CHIRP]", "Base64 payload size: ${base64.length} chars")
                val model = prefs.getModel()
                val apiKey = prefs.getApiKey()

                Log.d("[DEBUG-CHIRP]", "Sending transcription request to OpenRouter. Model: $model")
                val result = OpenRouterSttClient.transcribe(base64, apiKey, model)
                Log.d("[DEBUG-CHIRP]", "Transcription request completed. Success: ${result.isSuccess}")
                result.onSuccess { text ->
                    Log.d("[DEBUG-CHIRP]", "Transcription success. Text length: ${text.length} chars")
                    try {
                        val cm = ime.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("transcribed text", text))
                        Log.d("[DEBUG-CHIRP]", "Copied transcribed text to clipboard")
                    } catch (e: Exception) {
                        Log.e("[DEBUG-CHIRP]", "Failed to copy to clipboard", e)
                    }
                    val ic = ime.currentInputConnection
                    if (ic != null) {
                        Log.d("[DEBUG-CHIRP]", "Committing text to input connection")
                        ic.commitText(text, 1)
                        toast("Transcribed ${text.length} chars (copied to clipboard)")
                    } else {
                        Log.w("[DEBUG-CHIRP]", "InputConnection is null, cannot commit text")
                        toast("Transcribed: \"$text\" (copied to clipboard)")
                    }
                }.onFailure { e ->
                    Log.e("[DEBUG-CHIRP]", "Transcription failed with exception", e)
                    toast("Chirp voice error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("[DEBUG-CHIRP]", "Unexpected error during transcription flow", e)
                toast("Chirp voice error: ${e.message}")
            } finally {
                Log.d("[DEBUG-CHIRP]", "Resetting state to IDLE")
                setState(State.IDLE)
            }
        }
    }

    fun cancel() {
        if (state == State.RECORDING) {
            scope.launch(Dispatchers.IO) { recorder.stop() }
            setState(State.IDLE)
            toast("Voice input cancelled")
        }
    }

    fun destroy() {
        cancel()
        recorder.release()
        scope.cancel()
    }

    private fun toast(message: String) {
        Toast.makeText(ime, message, Toast.LENGTH_SHORT).show()
    }
}
