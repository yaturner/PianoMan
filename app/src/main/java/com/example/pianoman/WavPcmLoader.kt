package com.example.pianoman

import android.content.Context
import java.io.InputStream

/** Decoded mono 16-bit PCM audio plus the sample rate it was recorded at. */
data class PcmSample(val sampleRate: Int, val mono: ShortArray)

/** Minimal reader for uncompressed 16-bit PCM WAV files bundled as raw resources or assets. */
object WavPcmLoader {

    fun loadFromRaw(context: Context, resId: Int): PcmSample =
        load(context.resources.openRawResource(resId))

    fun loadFromAsset(context: Context, assetPath: String): PcmSample =
        load(context.assets.open(assetPath))

    private fun load(input: InputStream): PcmSample {
        val bytes = input.use { it.readBytes() }
        require(bytes.size > 44) { "WAV file too small" }

        val channels = readLE16(bytes, 22)
        val sampleRate = readLE32(bytes, 24)
        val bitsPerSample = readLE16(bytes, 34)
        require(bitsPerSample == 16) { "Only 16-bit PCM WAV files are supported" }

        var offset = 12
        var dataOffset = -1
        var dataSize = -1
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = readLE32(bytes, offset + 4)
            if (chunkId == "data") {
                dataOffset = offset + 8
                dataSize = chunkSize
                break
            }
            offset += 8 + chunkSize + (chunkSize and 1)
        }
        require(dataOffset >= 0) { "No data chunk found in WAV data" }

        val frameCount = dataSize / (2 * channels)
        val mono = ShortArray(frameCount)
        var pos = dataOffset
        for (i in 0 until frameCount) {
            var sum = 0
            repeat(channels) {
                val sample = ((bytes[pos + 1].toInt() shl 8) or (bytes[pos].toInt() and 0xFF)).toShort()
                sum += sample
                pos += 2
            }
            mono[i] = (sum / channels).toShort()
        }
        return PcmSample(sampleRate, mono)
    }

    private fun readLE16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readLE32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
