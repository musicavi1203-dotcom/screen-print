package com.example.screenprint

import kotlin.math.abs

object SecurityMatcher {

    fun calculateMatchScore(stored: BiometricProfile, current: BiometricProfile): Int {
        var score = 100.0

        // 1. לחץ (משקל גבוה)
        val pressureDiff = abs(stored.avgPressure - current.avgPressure)
        if (pressureDiff > 0.3f) return 0 
        score -= (pressureDiff * 100)

        // 2. שטח מגע (משקל גבוה)
        val sizeDiff = abs(stored.avgSize - current.avgSize)
        if (sizeDiff > 0.4f) return 0 
        score -= (sizeDiff * 60)

        // 3. מהירות (משקל בינוני)
        val velocityDiff = abs(stored.avgVelocity - current.avgVelocity)
        score -= (velocityDiff * 2).coerceAtMost(15.0)

        // 4. משך זמן (משקל בינוני)
        val durationDiff = abs(stored.duration - current.duration)
        if (durationDiff > 600) return 0
        score -= (durationDiff / 40.0)

        // 5. שונות בלחץ (משקל נמוך)
        val pressureVarDiff = abs(stored.pressureVariance - current.pressureVariance)
        score -= (pressureVarDiff * 20)

        return score.toInt().coerceIn(0, 100)
    }

    // מחזיר את הציון הגבוה ביותר מבין כל הדגימות השמורות
    fun getBestMatchScore(storedProfiles: List<BiometricProfile>, current: BiometricProfile): Int {
        if (storedProfiles.isEmpty()) return 0
        return storedProfiles.maxOfOrNull { calculateMatchScore(it, current) } ?: 0
    }
}
