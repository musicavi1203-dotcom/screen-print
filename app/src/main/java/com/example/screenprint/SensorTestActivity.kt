package com.example.screenprint

import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SensorTestActivity : AppCompatActivity() {

    private lateinit var infoText: TextView
    
    private var minPressure = 1.0f
    private var maxPressure = 0.0f
    private var minSize = 1.0f
    private var maxSize = 0.0f
    
    private var minMajor = 1.0f
    private var maxMajor = 0.0f
    private var minMinor = 1.0f
    private var maxMinor = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        infoText = TextView(this).apply {
            textSize = 16f
            setPadding(50, 50, 50, 50)
            text = "הנח את האצבע וגרור אותה על המסך.\nהסתכל אם touchMajor ו-touchMinor משתנים."
        }
        
        setContentView(infoText)

        infoText.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // איפוס הערכים בכל תחילת נגיעה חדשה
                    minPressure = event.pressure; maxPressure = event.pressure
                    minSize = event.size; maxSize = event.size
                    minMajor = event.getTouchMajor(); maxMajor = event.getTouchMajor()
                    minMinor = event.getTouchMinor(); maxMinor = event.getTouchMinor()
                }
                MotionEvent.ACTION_MOVE -> {
                    val pressure = event.pressure
                    val size = event.size
                    val major = event.getTouchMajor()
                    val minor = event.getTouchMinor()

                    // עדכון מינימום ומקסימום
                    if (pressure < minPressure) minPressure = pressure
                    if (pressure > maxPressure) maxPressure = pressure
                    
                    if (size < minSize) minSize = size
                    if (size > maxSize) maxSize = size
                    
                    if (major < minMajor) minMajor = major
                    if (major > maxMajor) maxMajor = major
                    
                    if (minor < minMinor) minMinor = minor
                    if (minor > maxMinor) maxMinor = minor

                    // בדיקה האם הערכים תקועים (הפרש קטן מאוד)
                    val isPressureStuck = (maxPressure - minPressure) < 0.01f
                    val isSizeStuck = (maxSize - minSize) < 0.01f
                    val isMajorStuck = (maxMajor - minMajor) < 0.01f
                    val isMinorStuck = (maxMinor - minMinor) < 0.01f

                    infoText.text = """
                        |--- לחץ (Pressure) ---
                        |נוכחי: ${"%.3f".format(pressure)} | תקוע? ${if (isPressureStuck) "כן" else "לא"}
                        |טווח: ${"%.3f".format(minPressure)} - ${"%.3f".format(maxPressure)}
                        |
                        |--- שטח כללי (Size) ---
                        |נוכחי: ${"%.3f".format(size)} | תקוע? ${if (isSizeStuck) "כן" else "לא"}
                        |טווח: ${"%.3f".format(minSize)} - ${"%.3f".format(maxSize)}
                        |
                        |--- ציר ארוך (Touch Major) ---
                        |נוכחי: ${"%.3f".format(major)} | תקוע? ${if (isMajorStuck) "כן" else "לא"}
                        |טווח: ${"%.3f".format(minMajor)} - ${"%.3f".format(maxMajor)}
                        |
                        |--- ציר קצר (Touch Minor) ---
                        |נוכחי: ${"%.3f".format(minor)} | תקוע? ${if (isMinorStuck) "כן" else "לא"}
                        |טווח: ${"%.3f".format(minMinor)} - ${"%.3f".format(maxMinor)}
                        |
                        |מיקום: X=${event.x.toInt()} Y=${event.y.toInt()}
                    """.trimMargin()
                }
            }
            true
        }
    }
}
