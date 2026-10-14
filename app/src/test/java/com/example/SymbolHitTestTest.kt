package com.example

import com.example.model.TextAnnotation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SymbolHitTestTest {

    @Test
    fun testOmegaSymbolTightHitBounds_WritingNearbyDoesNotSelect() {
        // In the physics formula: ω = 2πf
        // Symbol ω is placed at x = 100f, y = 200f with font size 28f
        val omega = TextAnnotation(
            text = "ω",
            x = 100f,
            y = 200f,
            fontSize = 28f
        )

        // 1. Tapping directly inside the omega symbol glyph
        assertTrue("Touching directly on omega should select it", omega.hitTest(108f, 190f, padding = 8f))

        // 2. Writing the '=' sign at x = 145f, y = 195f (nearby handwriting)
        assertFalse(
            "Writing '=' sign nearby at x=145 must NOT accidentally select omega",
            omega.hitTest(145f, 195f, padding = 8f)
        )

        // 3. Writing above or below the symbol
        assertFalse(
            "Writing above omega must not select it",
            omega.hitTest(108f, 150f, padding = 8f)
        )
        assertFalse(
            "Writing below omega must not select it",
            omega.hitTest(108f, 230f, padding = 8f)
        )
    }

    @Test
    fun testSymbolHitBoundsScaleWithFontSize() {
        val smallSym = TextAnnotation(text = "π", x = 50f, y = 100f, fontSize = 16f)
        val largeSym = TextAnnotation(text = "π", x = 50f, y = 100f, fontSize = 48f)

        val smallBounds = smallSym.getRenderedBounds()
        val largeBounds = largeSym.getRenderedBounds()

        assertTrue("Large font bounds should be wider than small font bounds", largeBounds.width() > smallBounds.width())
        assertTrue("Large font bounds height should be taller than small font bounds", largeBounds.height() > smallBounds.height())
    }

    @Test
    fun testRotatedSymbolHitTesting() {
        val rotatedSym = TextAnnotation(
            text = "F",
            x = 200f,
            y = 200f,
            fontSize = 32f,
            rotation = 90f // Rotated 90 degrees clockwise
        )

        // Touching directly on rotated glyph
        assertTrue("Touching rotated glyph should register a hit", rotatedSym.hitTest(205f, 210f, padding = 8f))

        // Far away touch
        assertFalse("Touching far from rotated glyph must not hit", rotatedSym.hitTest(250f, 250f, padding = 8f))
    }
}
