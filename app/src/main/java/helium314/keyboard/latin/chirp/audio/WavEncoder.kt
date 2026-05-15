package helium314.keyboard.latin.chirp.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {

    fun encode(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val bytesPerSample = bitsPerSample / 8
        val byteRate = sampleRate * channels * bytesPerSample
        val blockAlign = channels * bytesPerSample
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        val out = ByteArrayOutputStream(44 + dataSize)
        val buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(fileSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))

        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)
        buf.putShort(1.toShort())
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign.toShort())
        buf.putShort(bitsPerSample.toShort())

        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataSize)

        out.write(buf.array())
        out.write(pcmData)

        return out.toByteArray()
    }
}
