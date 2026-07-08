package com.example.pianoman

import android.content.Context

/** Persists and enumerates the app's user-configurable preferences. */
object AppPrefs {

    private const val PREFS_NAME = "piano_man_prefs"
    private const val KEY_INSTRUMENT_DIR = "instrument_dir"
    private const val KEY_DURATION_SECONDS = "key_duration_seconds"

    const val SAMPLES_ROOT = "Samples"
    const val DEFAULT_INSTRUMENT = "1st Violins"

    /** Selectable caps for how long a tapped note plays before it's cut off. */
    val KEY_DURATION_OPTIONS = listOf(1, 2, 3, 4, 5)
    const val DEFAULT_KEY_DURATION_SECONDS = 2

    fun getInstrument(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTRUMENT_DIR, DEFAULT_INSTRUMENT) ?: DEFAULT_INSTRUMENT
    }

    fun setInstrument(context: Context, instrument: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INSTRUMENT_DIR, instrument)
            .apply()
    }

    /** Every subdirectory of assets/Samples, each a selectable instrument sample pack. */
    fun listInstruments(context: Context): List<String> =
        (context.assets.list(SAMPLES_ROOT) ?: emptyArray()).sorted()

    fun getKeyDurationSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DURATION_SECONDS, DEFAULT_KEY_DURATION_SECONDS)
    }

    fun setKeyDurationSeconds(context: Context, seconds: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DURATION_SECONDS, seconds)
            .apply()
    }
}
