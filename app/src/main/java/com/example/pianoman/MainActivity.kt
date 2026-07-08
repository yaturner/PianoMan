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
        when (item.itemId) {
            R.id.action_preferences -> {
                showPreferencesDialog()
                return true
            }
            R.id.action_about -> {
                showAboutDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPreferencesDialog() {
        val entries = arrayOf(getString(R.string.instrument), getString(R.string.key_duration))
        AlertDialog.Builder(this)
            .setTitle(R.string.preferences)
            .setItems(entries) { _, which ->
                when (which) {
                    0 -> showInstrumentDialog()
                    1 -> showKeyDurationDialog()
                }
            }
            .show()
    }

    private fun showInstrumentDialog() {
        val instruments = AppPrefs.listInstruments(this)
        if (instruments.isEmpty()) return
        val current = AppPrefs.getInstrument(this)
        val checkedIndex = instruments.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.instrument)
            .setSingleChoiceItems(instruments.toTypedArray(), checkedIndex) { dialog, which ->
                val selected = instruments[which]
                AppPrefs.setInstrument(this, selected)
                pianoKeyboard.setInstrument(selected)
                dialog.dismiss()
            }
            .show()
    }

    private fun showKeyDurationDialog() {
        val options = AppPrefs.KEY_DURATION_OPTIONS
        val labels = options.map { seconds -> resources.getQuantityString(R.plurals.seconds, seconds, seconds) }
        val current = AppPrefs.getKeyDurationSeconds(this)
        val checkedIndex = options.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.key_duration)
            .setSingleChoiceItems(labels.toTypedArray(), checkedIndex) { dialog, which ->
                val selected = options[which]
                AppPrefs.setKeyDurationSeconds(this, selected)
                pianoKeyboard.setKeyDurationSeconds(selected)
                dialog.dismiss()
            }
            .show()
    }

    private fun showAboutDialog() {
        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        val message = getString(R.string.about_message, versionName ?: "")
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
