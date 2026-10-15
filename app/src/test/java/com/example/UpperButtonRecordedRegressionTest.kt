package com.example

import com.example.util.ContactRestartDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class UpperButtonRecordedRegressionTest {
    private fun pair(start: Long, gap: Long, x: Float, y: Float, endX: Float, endY: Float, endDevice: Int = 30): ContactRestartDetector.Edge {
        val detector = ContactRestartDetector()
        detector.observeHover(ContactRestartDetector.Sample(10, start, x, y, 0f, 30), true)
        return detector.observeHover(ContactRestartDetector.Sample(9, start + gap, endX, endY, 0f, endDevice), true)
    }

    @Test fun movingRecorded46msRestartIsAccepted() {
        assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE,
            pair(122353650L, 46, 444.68262f, 845.2881f, 454.7351f, 977.6367f))
    }

    @Test fun bothRecorded52msRestartsAreAccepted() {
        assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE,
            pair(122381384L, 52, 403.9453f, 965.7715f, 387.46582f, 973.8281f))
        assertEquals(ContactRestartDetector.Edge.PRESS_CANDIDATE,
            pair(122385560L, 52, 323.06396f, 1057.9102f, 315.58228f, 1094.0918f))
    }

    @Test fun recorded84msGapIsNotGuessedToBeAPress() {
        assertEquals(ContactRestartDetector.Edge.NONE,
            pair(122449256L, 84, 406.38428f, 1243.0664f, 428.49976f, 1288.2568f))
    }

    @Test fun eventsFromDifferentDevicesCannotFormAPress() {
        assertEquals(ContactRestartDetector.Edge.NONE, pair(100, 46, 0f, 0f, 1f, 1f, 31))
    }
}
