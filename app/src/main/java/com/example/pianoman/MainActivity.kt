package com.example.pianoman

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private lateinit var pianoKeyboard: PianoKeyboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        pianoKeyboard = findViewById(R.id.pianoKeyboard)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_preferences) {
            showInstrumentDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showInstrumentDialog() {
        val instruments = InstrumentPrefs.listInstruments(this)
        if (instruments.isEmpty()) return
        val current = InstrumentPrefs.getInstrument(this)
        val checkedIndex = instruments.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.instrument)
            .setSingleChoiceItems(instruments.toTypedArray(), checkedIndex) { dialog, which ->
                val selected = instruments[which]
                InstrumentPrefs.setInstrument(this, selected)
                pianoKeyboard.setInstrument(selected)
                dialog.dismiss()
            }
            .show()
    }
}
