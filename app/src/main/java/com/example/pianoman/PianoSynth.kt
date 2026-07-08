package com.example.pianoman

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.pow

/**
 * Plays any of the 88 piano keys using the 1st-violin pizzicato samples bundled under
 * assets/Samples/1st Violins (recorded roughly every minor third), pitch-shifting each
 * one (by resampling) to whichever key doesn't have its own recording. Keys far from a
 * recorded note will sound thinner since they're stretched hardest, but most of the
 * playable range sits within a semitone or two of a real recording.
 */
class PianoSynth(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private class RecordedNote(val midi: Int, val sample: PcmSample)

    private val recordedNotes: List<RecordedNote> = loadSamples()
    private val resampledCache = HashMap<Int, PcmSample>()

    private fun loadSamples(): List<RecordedNote> {
        val baseDir = "Samples/1st Violins"
        // The library only recorded these four pitch classes per octave (a minor third apart).
        val recordedPitches = listOf(
            "G" to 3, "A#" to 3,
            "C#" to 4, "E" to 4, "G" to 4, "A#" to 4,
            "C#" to 5, "E" to 5, "G" to 5, "A#" to 5,
            "C#" to 6, "E" to 6, "G" to 6, "A#" to 6
        )
        return recordedPitches.map { (pitchClass, octave) ->
            val fileName = "1st-violins-piz-rr1-${pitchClass.lowercase()}$octave-PB.wav"
            val sample = WavPcmLoader.loadFromAsset(appContext, "$baseDir/$fileName")
            RecordedNote(midiNumber(pitchClass, octave), sample)
        }
    }

    private fun midiNumber(pitchClass: String, octave: Int): Int {
        val pitchClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return 12 * (octave + 1) + pitchClasses.indexOf(pitchClass)
    }

    fun playNote(pitchClass: String, octave: Int) {
        if (recordedNotes.isEmpty()) return
        val targetMidi = midiNumber(pitchClass, octave)

        val pcm = resampledCache.getOrPut(targetMidi) {
            val nearest = recordedNotes.minBy { abs(it.midi - targetMidi) }
            val ratio = 2.0.pow((targetMidi - nearest.midi) / 12.0)
            if (ratio == 1.0) nearest.sample else PcmSample(nearest.sample.sampleRate, resample(nearest.sample.mono, ratio))
        }

        val bytes = ByteArray(pcm.mono.size * 2)
        for (i in pcm.mono.indices) {
            val v = pcm.mono[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(pcm.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bytes.size,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack.write(bytes, 0, bytes.size)
        audioTrack.play()

        val durationMs = (pcm.mono.size * 1000L / pcm.sampleRate) + 100L
        mainHandler.postDelayed({
            runCatching { audioTrack.stop() }
            audioTrack.release()
        }, durationMs)
    }

    private fun resample(input: ShortArray, ratio: Double): ShortArray {
        val outLength = (input.size / ratio).toInt().coerceAtLeast(1)
        val output = ShortArray(outLength)
        val lastIndex = input.size - 1
        for (i in 0 until outLength) {
            val srcPos = i * ratio
            val idx = srcPos.toInt().coerceIn(0, lastIndex)
            val frac = srcPos - idx
            val s0 = input[idx]
            val s1 = input[(idx + 1).coerceAtMost(lastIndex)]
            output[i] = (s0 + (s1 - s0) * frac).toInt().toShort()
        }
        return output
    }
}
