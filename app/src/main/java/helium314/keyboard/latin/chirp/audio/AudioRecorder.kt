package helium314.keyboard.latin.chirp.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
        private const val MAX_RECORDING_SECONDS = 120
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private val pcmBuffer = ByteArrayOutputStream()

    @Volatile
    private var stopped = false

    @Volatile
    private var pcmResult: ByteArray? = null

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException("Chirp voice input requires Android 6.0 or newer")
        }
        if (isRecording.getAndSet(true)) {
            Log.w(TAG, "Already recording")
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufSize * 4).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioSource(AUDIO_SOURCE)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                isRecording.set(false)
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            isRecording.set(false)
            return
        }

        pcmBuffer.reset()
        stopped = false
        isReleased.set(false)
        pcmResult = null

        recordingThread = Thread({ recordLoop(bufferSize) }, "audio-recorder").apply { start() }
    }

    private fun recordLoop(bufferSize: Int) {
        val readBuffer = ShortArray(bufferSize / 2)
        val record = audioRecord ?: return

        var totalFrames = 0
        val maxFrames = MAX_RECORDING_SECONDS * SAMPLE_RATE

        try {
            while (isRecording.get() && totalFrames < maxFrames) {
                val shortsRead = record.read(readBuffer, 0, readBuffer.size)
                if (shortsRead <= 0) break

                for (i in 0 until shortsRead) {
                    val sample = readBuffer[i]
                    pcmBuffer.write(sample.toInt() and 0xFF)
                    pcmBuffer.write((sample.toInt() shr 8) and 0xFF)
                }

                totalFrames += shortsRead
                if (stopped) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recording loop", e)
        } finally {
            val result = pcmBuffer.toByteArray()
            pcmResult = result
            pcmBuffer.reset()

            if (isReleased.compareAndSet(false, true)) {
                try { record.stop() } catch (_: Exception) {}
                try { record.release() } catch (_: Exception) {}
            }

            audioRecord = null
            isRecording.set(false)
        }
    }

    fun stop(): ByteArray {
        stopped = true
        isRecording.set(false)
        recordingThread?.join(2000)
        return pcmResult ?: ByteArray(0)
    }

    fun isRecordingActive(): Boolean = isRecording.get()

    fun release() {
        stopped = true
        isRecording.set(false)
        recordingThread?.join(1000)
        audioRecord?.takeIf { isReleased.compareAndSet(false, true) }?.let { record ->
            try { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() } catch (_: Exception) {}
            try { record.release() } catch (_: Exception) {}
            audioRecord = null
        }
    }
}
