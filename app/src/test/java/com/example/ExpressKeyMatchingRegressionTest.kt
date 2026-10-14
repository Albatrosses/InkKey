package com.example

import android.view.KeyEvent
import com.example.util.XpPenExpressKeyResolver as Keys
import org.junit.Assert.*
import org.junit.Test

class ExpressKeyMatchingRegressionTest {
    @Test fun allEightCapturedKeysHaveTheirPhysicalIndices() {
        assertEquals(1,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_B,false,false,false))
        assertEquals(2,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_E,false,false,false))
        assertEquals(3,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_ALT_LEFT,false,true,false))
        assertEquals(4,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_SPACE,false,false,false))
        assertEquals(5,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_S,true,false,false))
        assertEquals(6,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_Z,true,false,false))
        assertEquals(7,Keys.resolveCapturedScanCode(78))
        assertEquals(8,Keys.resolveCapturedScanCode(74))
    }
    @Test fun keypadIdentityAlsoWorksWithoutAnExposedScanCode() {
        assertEquals(7,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_NUMPAD_ADD,false,false,false))
        assertEquals(8,Keys.resolveKnownShortcut(KeyEvent.KEYCODE_NUMPAD_SUBTRACT,true,false,false))
    }
    @Test fun modifiedLettersAndPrefixPacketsAreNotOtherExpressKeys() {
        assertNull(Keys.resolveKnownShortcut(KeyEvent.KEYCODE_B,true,false,false))
        assertNull(Keys.resolveKnownShortcut(KeyEvent.KEYCODE_E,false,true,false))
        assertNull(Keys.resolveKnownShortcut(KeyEvent.KEYCODE_S,false,false,false))
        assertNull(Keys.resolveKnownShortcut(KeyEvent.KEYCODE_CTRL_LEFT,true,false,false))
        assertNull(Keys.resolveCapturedScanCode(29))
    }
    @Test fun aLearnedPairMustMatchBothIdentifiers() {
        val keys=mapOf(5 to KeyEvent.KEYCODE_S);val scans=mapOf(5 to 31)
        assertEquals(5,Keys.resolveCustomBinding(KeyEvent.KEYCODE_S,31,keys,scans))
        assertNull(Keys.resolveCustomBinding(KeyEvent.KEYCODE_S,44,keys,scans))
        assertNull(Keys.resolveCustomBinding(KeyEvent.KEYCODE_B,31,keys,scans))
    }
    @Test fun singleIdentifierBindingsStillWork() {
        assertEquals(7,Keys.resolveCustomBinding(0,78,emptyMap(),mapOf(7 to 78)))
        assertEquals(1,Keys.resolveCustomBinding(KeyEvent.KEYCODE_B,0,mapOf(1 to KeyEvent.KEYCODE_B),emptyMap()))
    }
    @Test fun conflictingLearnedBindingsAreNotGuessed() {
        assertNull(Keys.resolveCustomBinding(KeyEvent.KEYCODE_B,48,mapOf(1 to KeyEvent.KEYCODE_B,2 to KeyEvent.KEYCODE_B),emptyMap()))
    }
}
