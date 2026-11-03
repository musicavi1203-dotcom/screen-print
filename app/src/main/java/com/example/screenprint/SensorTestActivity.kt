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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        infoText = TextView(this).apply {
            textSize = 20f
            setPadding(50, 50, 50, 50)
            text = "הנח את האצבע וגרור אותה על המסך.\nהסתכל אם הלחץ והשטח משתנים."
        }
        
        setContentView(infoText)

        infoText.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    minPressure = 1.0f
                    maxPressure = 0.0f
                    minSize = 1.0f
                    maxSize = 0.0f
                }
                MotionEvent.ACTION_MOVE -> {
                    val pressure = event.pressure
                    val size = event.size

                    if (pressure < minPressure) minPressure = pressure
                    if (pressure > maxPressure) maxPressure = pressure
                    
                    if (size < minSize) minSize = size
                    if (size > maxSize) maxSize = size

                    val isPressureStuck = (maxPressure - minPressure) < 0.01f
                    val isSizeStuck = (maxSize - minSize) < 0.01f

                    infoText.text = """
                        |לחץ (Pressure):
                        |נוכחי: ${"%.3f".format(pressure)}
                        |מינימום: ${"%.3f".format(minPressure)}
                        |מקסימום: ${"%.3f".format(maxPressure)}
                        |האם תקוע? ${if (isPressureStuck) "כן (קבוע)" else "לא (משתנה)"}
                        |
                        |שטח מגע (Size):
                        |נוכחי: ${"%.3f".format(size)}
                        |מינימום: ${"%.3f".format(minSize)}
                        |מקסימום: ${"%.3f".format(maxSize)}
                        |האם תקוע? ${if (isSizeStuck) "כן (קבוע)" else "לא (משתנה)"}
                        |
                        |נקודות היסטוריה: ${event.historySize}
                        |מיקום: X=${event.x.toInt()} Y=${event.y.toInt()}
                    """.trimMargin()
                }
            }
            true
        }
    }
}
