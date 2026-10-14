package com.example.util

/** Experimental, device-specific inference from paired pressure-bearing contact restarts. */
class ContactRestartDetector {
    data class Sample(val action: Int, val time: Long, val x: Float, val y: Float, val pressure: Float, val deviceId: Int)
    enum class Edge { NONE, PRESS_CANDIDATE, RELEASE_CANDIDATE }
    private var pendingUp: Sample? = null
    private var pendingHoverExit: Sample? = null

    fun reset() { pendingUp = null; pendingHoverExit = null }

    /** Trial signature: recorded pre-held mode switches interrupt hover for 44–46 ms. */
    fun observeHover(sample: Sample, eligible: Boolean): Edge {
        if (!eligible) { pendingHoverExit = null; return Edge.NONE }
        if (sample.action == 10) pendingHoverExit = sample // ACTION_HOVER_EXIT
        if (sample.action == 9) { // ACTION_HOVER_ENTER
            val previous = pendingHoverExit
            pendingHoverExit = null
            if (previous != null && previous.deviceId == sample.deviceId &&
                previous.pressure == 0f && sample.pressure == 0f) {
                val gap = sample.time - previous.time
                val distance = kotlin.math.hypot(sample.x - previous.x, sample.y - previous.y)
                if (gap in 42L..50L && distance <= 18f) return Edge.PRESS_CANDIDATE
                // Recorded release restarts are shorter, including while the pen is moving.
                if (gap in 30L..40L && distance <= 160f) return Edge.RELEASE_CANDIDATE
            }
        }
        return Edge.NONE
    }

    fun observe(sample: Sample, eligible: Boolean): Edge {
        if (!eligible || !sample.pressure.isFinite() || !sample.x.isFinite() || !sample.y.isFinite()) {
            reset(); return Edge.NONE
        }
        when (sample.action) {
            3 -> reset() // ACTION_CANCEL
            1 -> pendingUp = sample.takeIf { it.pressure > 0.02f } // Normal releases were 0.009949338.
            0 -> {
                pendingHoverExit = null // Real touchdown must not become a hover mode switch.
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
