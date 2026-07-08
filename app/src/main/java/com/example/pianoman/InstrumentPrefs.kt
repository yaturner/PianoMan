package com.example.pianoman

import android.content.Context

/** Persists and enumerates the user's choice of instrument sample pack under assets/Samples. */
object InstrumentPrefs {

    private const val PREFS_NAME = "piano_man_prefs"
    private const val KEY_INSTRUMENT_DIR = "instrument_dir"
    const val SAMPLES_ROOT = "Samples"
    const val DEFAULT_INSTRUMENT = "1st Violins"

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
}
