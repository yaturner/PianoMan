package com.example.pianoman

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A full 88-key piano keyboard (A0..C8) that scrolls horizontally inside a
 * HorizontalScrollView and plays a note wherever it's touched, supporting chords
 * via multitouch.
 */
class PianoKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Key(val pitchClass: String, val octave: Int, val isBlack: Boolean) {
        val rect = RectF()
        val label: String get() = "$pitchClass$octave"
    }

    private val synth = PianoSynth(context)

    private val whiteKeyWidthPx = dp(52f)
    private val blackKeyWidthPx = dp(32f)
    private val blackKeyHeightRatio = 0.6f

    private val whiteKeys = mutableListOf<Key>()
    private val blackKeys = mutableListOf<Key>()
    private var totalWhiteKeys = 0

    // pointerId -> key currently held down, so multiple fingers can each hold a note.
    private val activeTouches = mutableMapOf<Int, Key>()

    private val whiteKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val whiteKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B3D9FF") }
    private val blackKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val blackKeyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4D88C4") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = dp(11f)
        textAlign = Paint.Align.CENTER
    }

    init {
        buildKeys()
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    private fun buildKeys() {
        val chromatic = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val allKeys = mutableListOf<Key>()

        // Octave 0 only has the top three keys of a standard 88-key piano.
        allKeys += Key("A", 0, false)
        allKeys += Key("A#", 0, true)
        allKeys += Key("B", 0, false)
        for (octave in 1..7) {
            for (pitchClass in chromatic) {
                allKeys += Key(pitchClass, octave, pitchClass.endsWith("#"))
            }
        }
        allKeys += Key("C", 8, false)

        var whiteIndex = 0
        for (key in allKeys) {
            if (key.isBlack) {
                blackKeys.add(key)
            } else {
                whiteKeys.add(key)
                whiteIndex++
            }
        }
        totalWhiteKeys = whiteIndex

        // Position keys: each white key gets the next slot; a black key is centered on the
        // boundary between the white keys on either side of it.
        whiteIndex = 0
        for (key in allKeys) {
            if (!key.isBlack) {
                whiteIndex++
            } else {
                val boundaryX = whiteIndex * whiteKeyWidthPx
                key.rect.left = boundaryX - blackKeyWidthPx / 2f
                key.rect.right = boundaryX + blackKeyWidthPx / 2f
            }
        }
        whiteIndex = 0
        for (key in whiteKeys) {
            key.rect.left = whiteIndex * whiteKeyWidthPx
            key.rect.right = (whiteIndex + 1) * whiteKeyWidthPx
            whiteIndex++
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (totalWhiteKeys * whiteKeyWidthPx).toInt()
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val height = if (heightSize > 0) heightSize else dp(240f).toInt()
        setMeasuredDimension(desiredWidth, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        for (key in whiteKeys) {
            key.rect.top = 0f
            key.rect.bottom = h.toFloat()
        }
        for (key in blackKeys) {
            key.rect.top = 0f
            key.rect.bottom = h * blackKeyHeightRatio
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (key in whiteKeys) {
            val pressed = activeTouches.containsValue(key)
            canvas.drawRect(key.rect, if (pressed) whiteKeyPressedPaint else whiteKeyPaint)
            canvas.drawRect(key.rect, borderPaint)
            if (key.pitchClass == "C") {
                canvas.drawText(key.label, key.rect.centerX(), key.rect.bottom - dp(12f), labelPaint)
            }
        }
        for (key in blackKeys) {
            val pressed = activeTouches.containsValue(key)
            canvas.drawRect(key.rect, if (pressed) blackKeyPressedPaint else blackKeyPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                val key = keyAt(event.getX(idx), event.getY(idx))
                if (key != null) {
                    activeTouches[pointerId] = key
                    synth.playNote(key.pitchClass, key.octave)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pointerId = event.getPointerId(idx)
                if (activeTouches.remove(pointerId) != null) invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (activeTouches.isNotEmpty()) {
                    activeTouches.clear()
                    invalidate()
                }
            }
        }
        return true
    }

    private fun keyAt(x: Float, y: Float): Key? {
        for (key in blackKeys) {
            if (key.rect.contains(x, y)) return key
        }
        for (key in whiteKeys) {
            if (key.rect.contains(x, y)) return key
        }
        return null
    }
}
