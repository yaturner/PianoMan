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
 * Plays any of the 88 piano keys using the .wav samples bundled under an instrument's
 * directory in assets/Samples (e.g. "1st Violins"), pitch-shifting each one (by resampling)
 * to whichever key doesn't have its own recording. Keys far from a recorded note will sound
 * thinner since they're stretched hardest, but most of the playable range sits within a
 * semitone or two of a real recording.
 */
class PianoSynth(context: Context, instrument: String = AppPrefs.DEFAULT_INSTRUMENT) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private class RecordedNote(val midi: Int, val sample: PcmSample)

    // The pitch immediately preceding "-PB" or the ".wav" extension, e.g. "...a#3-PB.wav" or
    // "...a#3.wav" both yield pitch class "a#" and octave "3".
    private val noteFileNamePattern = Regex("""-([A-Ga-g]#?)(-?\d+)(?=-|\.wav$)""")

    private var recordedNotes: List<RecordedNote> = emptyList()
    private val resampledCache = HashMap<Int, PcmSample>()
    private var maxPlaybackSeconds = AppPrefs.getKeyDurationSeconds(appContext)

    init {
        setInstrument(instrument)
    }

    /** Switches to a different instrument's sample pack, reloading its recordings. */
    fun setInstrument(instrument: String) {
        recordedNotes = loadSamples(instrument)
        resampledCache.clear()
    }

    /** Changes how long (in seconds) a note plays before being cut off. */
    fun setMaxPlaybackSeconds(seconds: Int) {
        maxPlaybackSeconds = seconds
    }

    private fun loadSamples(instrument: String): List<RecordedNote> {
        val dir = "${AppPrefs.SAMPLES_ROOT}/$instrument"
        val fileNames = appContext.assets.list(dir) ?: return emptyList()
        val seenMidi = HashSet<Int>()
        val notes = mutableListOf<RecordedNote>()
        for (fileName in fileNames) {
            if (!fileName.lowercase().endsWith(".wav")) continue
            val match = noteFileNamePattern.find(fileName) ?: continue
            val (pitchClass, octaveText) = match.destructured
            val octave = octaveText.toIntOrNull() ?: continue
            val midi = midiNumber(pitchClass.uppercase(), octave)
            if (!seenMidi.add(midi)) continue // keep the first take of a repeated/round-robin note
            val sample = WavPcmLoader.loadFromAsset(appContext, "$dir/$fileName")
            notes += RecordedNote(midi, sample)
        }
        return notes
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

        val maxFrames = pcm.sampleRate * maxPlaybackSeconds
        val frameCount = minOf(pcm.mono.size, maxFrames)
        val truncated = frameCount < pcm.mono.size
        // A sample cut off mid-waveform pops audibly, so taper the last ~15ms out to silence.
        val fadeFrames = if (truncated) minOf(frameCount, pcm.sampleRate / 65) else 0
        val fadeStart = frameCount - fadeFrames

        val bytes = ByteArray(frameCount * 2)
        for (i in 0 until frameCount) {
            var v = pcm.mono[i].toInt()
            if (i >= fadeStart) v = (v * (frameCount - i) / fadeFrames)
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

        val durationMs = (frameCount * 1000L / pcm.sampleRate) + 100L
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
