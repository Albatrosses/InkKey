package com.example.util

/** Experimental, device-specific inference from paired pressure-bearing contact restarts. */
class ContactRestartDetector {
    data class Sample(val action: Int, val time: Long, val x: Float, val y: Float, val pressure: Float, val deviceId: Int)
    enum class Edge { NONE, PRESS_CANDIDATE, RELEASE_CANDIDATE }
    private var pendingUp: Sample? = null

    fun reset() { pendingUp = null }

    fun observe(sample: Sample, eligible: Boolean): Edge {
        if (!eligible || !sample.pressure.isFinite() || !sample.x.isFinite() || !sample.y.isFinite()) {
            reset(); return Edge.NONE
        }
        when (sample.action) {
            3 -> reset() // ACTION_CANCEL
            1 -> pendingUp = sample.takeIf { it.pressure > 0.02f } // Normal releases were 0.009949338.
            0 -> {
                val previous = pendingUp
                reset()
                if (previous == null || previous.deviceId != sample.deviceId) return Edge.NONE
                val gap = sample.time - previous.time
                val distance = kotlin.math.hypot(sample.x - previous.x, sample.y - previous.y)
                // During a restart the pen can keep moving along the stroke.
                val movementAllowance = (12f + gap * 4f).coerceAtMost(160f)
                if (gap !in 1L..100L || distance > movementAllowance || sample.pressure <= 0.02f) return Edge.NONE
                val ratio = sample.pressure / previous.pressure
                if (ratio >= 1.7f) return Edge.PRESS_CANDIDATE
                if (ratio <= 0.65f) return Edge.RELEASE_CANDIDATE
            }
        }
        return Edge.NONE
    }
}

/** Transient state; repeated press candidates never toggle the selected tool. */
class HeldBarrelState {
    var pressed: Boolean = false
        private set
    fun update(edge: ContactRestartDetector.Edge) {
        when (edge) {
            ContactRestartDetector.Edge.PRESS_CANDIDATE -> pressed = true
            ContactRestartDetector.Edge.RELEASE_CANDIDATE -> pressed = false
            ContactRestartDetector.Edge.NONE -> Unit
        }
    }
    fun clear() { pressed = false }
}
