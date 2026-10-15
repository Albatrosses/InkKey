package com.example

import com.example.util.ContactRestartDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class HoverDetectorTest {
    private fun sample(action: Int, time: Long, x: Float, y: Float) =
        ContactRestartDetector.Sample(action, time, x, y, 0f, 12)

    @Test
    fun movingHoverPressFromTrial6IsAccepted() {
        val detector = ContactRestartDetector()
        assertEquals(
            ContactRestartDetector.Edge.NONE,
            detector.observeHover(sample(10, 1000L, 0f, 0f), true)
        )
        assertEquals(
            ContactRestartDetector.Edge.PRESS_CANDIDATE,
            detector.observeHover(sample(9, 1044L, 91f, 5f), true)
        )
    }

    @Test
    fun movementGuardStillRejectsLargeDropout() {
        val detector = ContactRestartDetector()
        detector.observeHover(sample(10, 2000L, 0f, 0f), true)
        assertEquals(
            ContactRestartDetector.Edge.NONE,
            detector.observeHover(sample(9, 2044L, 161f, 0f), true)
        )
    }

    @Test
    fun pressTimingWindowRemainsNarrow() {
        val detector = ContactRestartDetector()
        detector.observeHover(sample(10, 3000L, 0f, 0f), true)
        assertEquals(
            ContactRestartDetector.Edge.NONE,
            detector.observeHover(sample(9, 3041L, 2f, 0f), true)
        )
        detector.observeHover(sample(10, 4000L, 0f, 0f), true)
        assertEquals(
            ContactRestartDetector.Edge.NONE,
            detector.observeHover(sample(9, 4053L, 2f, 0f), true)
        )
    }

    @Test
    fun releaseTimingWindowIsUnchanged() {
        val detector = ContactRestartDetector()
        detector.observeHover(sample(10, 5000L, 0f, 0f), true)
        assertEquals(
            ContactRestartDetector.Edge.RELEASE_CANDIDATE,
            detector.observeHover(sample(9, 5036L, 100f, 0f), true)
        )
    }
}
