package helium314.keyboard.latin.chirp

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import android.util.Log

class ChirpVoiceController(private val ime: LatinIME) {

    companion object {
        private const val TAG = "ChirpVoiceController"
    }

    enum class State { IDLE, RECORDING, TRANSCRIBING }

    @Volatile
    private var state = State.IDLE

    private val prefs = ChirpPreferences(ime)
    private val recorder = AudioRecorder(ime)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isEnabled(): Boolean = prefs.isVoiceEnabled()

    fun toggleRecording(): Boolean {
        when (state) {
            State.IDLE -> {
                if (!isEnabled()) return false
                val apiKey = prefs.getApiKey()
                if (apiKey.isBlank()) {
                    toast("Chirp voice error: API key not set")
                    return false
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val granted = ime.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        toast("Chirp voice error: RECORD_AUDIO permission denied")
                        return false
                    }
                }
                state = State.RECORDING
                recorder.start()
                toast("🎙 Listening… tap mic again to stop")
                return true
            }
            State.RECORDING -> {
                state = State.TRANSCRIBING
                toast("Transcribing…")
                scope.launch {
                    val pcmData = withContext(Dispatchers.IO) { recorder.stop() }
                    if (pcmData.isEmpty()) {
                        toast("Chirp voice error: no audio captured")
                        state = State.IDLE
                        return@launch
                    }
                    val wavData = withContext(Dispatchers.IO) { WavEncoder.encode(pcmData) }
                    val base64 = Base64.encodeToString(wavData, Base64.NO_WRAP)
                    val model = prefs.getModel()
                    val apiKey = prefs.getApiKey()

                    val result = OpenRouterSttClient.transcribe(base64, apiKey, model)
                    mainHandler.post {
                        result.onSuccess { text ->
                            val ic = ime.currentInputConnection
                            if (ic != null) {
                                ic.commitText(text, 1)
                                toast("Transcribed ${text.length} chars")
                            } else {
                                toast("Chirp voice error: no input connection")
                            }
                        }.onFailure { e ->
                            toast("Chirp voice error: ${e.message}")
                            Log.e(TAG, "Transcription failed", e)
                        }
                        state = State.IDLE
                    }
                }
                return true
            }
            State.TRANSCRIBING -> {
                toast("Still transcribing…")
                return true
            }
        }
    }

    fun cancel() {
        if (state == State.RECORDING) {
            scope.launch(Dispatchers.IO) { recorder.stop() }
            state = State.IDLE
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
