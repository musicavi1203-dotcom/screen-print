package com.example.screenprint

import android.view.MotionEvent
import kotlin.math.sqrt

data class BiometricProfile(
    val avgPressure: Float,
    val pressureVariance: Float,
    val avgSize: Float,
    val sizeVariance: Float,
    val avgVelocity: Float,
    val duration: Long,
    val positionDelta: Float
)

object BiometricEngine {

    fun extractProfile(events: List<MotionEvent>): BiometricProfile? {
        if (events.size < 3) return null

        var totalPressure = 0f
        var totalSize = 0f
        var totalVelocity = 0f
        var samplesCount = 0

        val startEvent = events.first()
        val endEvent = events.last()
        val duration = endEvent.eventTime - startEvent.eventTime

        var prevX = startEvent.x
        var prevY = startEvent.y
        var prevTime = startEvent.eventTime

        for (event in events) {
            val historySize = event.historySize
            val loops = if (historySize > 0) historySize else 1

            for (i in 0 until loops) {
                val pressure = if (historySize > 0) event.getHistoricalPressure(i) else event.pressure
                val size = if (historySize > 0) event.getHistoricalSize(i) else event.size
                val x = if (historySize > 0) event.getHistoricalX(i) else event.x
                val y = if (historySize > 0) event.getHistoricalY(i) else event.y
                val time = if (historySize > 0) event.getHistoricalEventTime(i) else event.eventTime

                totalPressure += pressure
                totalSize += size

                val dx = x - prevX
                val dy = y - prevY
                val dt = (time - prevTime).toFloat().coerceAtLeast(1f)
                totalVelocity += (sqrt(dx * dx + dy * dy) / dt) * 1000f
                
                prevX = x
                prevY = y
                prevTime = time
                samplesCount++
            }
        }

        if (samplesCount == 0) return null

        val avgPressure = totalPressure / samplesCount
        val avgSize = totalSize / samplesCount
        val avgVelocity = totalVelocity / samplesCount

        var pressureVar = 0f
        var sizeVar = 0f
        for (event in events) {
            val historySize = event.historySize
            val loops = if (historySize > 0) historySize else 1
            for (i in 0 until loops) {
                val p = if (historySize > 0) event.getHistoricalPressure(i) else event.pressure
                val s = if (historySize > 0) event.getHistoricalSize(i) else event.size
                pressureVar += (p - avgPressure) * (p - avgPressure)
                sizeVar += (s - avgSize) * (s - avgSize)
            }
        }
        pressureVar /= samplesCount
        sizeVar /= samplesCount

        val positionDelta = sqrt(
            (endEvent.x - startEvent.x) * (endEvent.x - startEvent.x) +
            (endEvent.y - startEvent.y) * (endEvent.y - startEvent.y)
        )

        return BiometricProfile(
            avgPressure = avgPressure,
            pressureVariance = pressureVar,
            avgSize = avgSize,
            sizeVariance = sizeVar,
            avgVelocity = avgVelocity,
            duration = duration,
            positionDelta = positionDelta
        )
    }

    fun profileToString(profile: BiometricProfile): String {
        return "${profile.avgPressure},${profile.pressureVariance},${profile.avgSize},${profile.sizeVariance},${profile.avgVelocity},${profile.duration},${profile.positionDelta}"
    }

    fun stringToProfile(str: String): BiometricProfile? {
        val parts = str.split(",")
        if (parts.size != 7) return null
        return try {
            BiometricProfile(
                parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat(),
                parts[3].toFloat(), parts[4].toFloat(), parts[5].toLong(), parts[6].toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
}
