package com.example

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.symbolControlGuard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalComposeUiApi::class)
class SymbolControlTouchTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pressingEditDoesNotDeselectTheSymbolBeforeClickCompletes() {
        var selected by mutableStateOf(true)
        var edits by mutableIntStateOf(0)
        compose.setContent {
            Box(Modifier.size(320.dp)) {
                Canvas(Modifier.fillMaxSize().pointerInteropFilter { if(it.actionMasked == MotionEvent.ACTION_DOWN) selected=false;true }) {}
                if(selected) Surface(Modifier.width(200.dp).height(60.dp).symbolControlGuard().testTag("controls")) {
                    Row { TextButton(onClick={edits++}) { Text("Edit") } }
                }
            }
        }
        compose.onNodeWithText("Edit").performTouchInput { click() }
        compose.runOnIdle { assertTrue(selected);assertEquals(1,edits) }
    }

    @Test fun aGapInTheToolbarDoesNotPassThroughToTheCanvas() {
        var selected by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.size(320.dp)) {
                Canvas(Modifier.fillMaxSize().pointerInteropFilter { if(it.actionMasked == MotionEvent.ACTION_DOWN) selected=false;true }) {}
                if(selected) Surface(Modifier.width(200.dp).height(60.dp).symbolControlGuard().testTag("controls")) {
                    Row { TextButton(onClick={}) { Text("Edit") } }
                }
            }
        }
        compose.onNodeWithTag("controls").performTouchInput { click(bottomRight - Offset(8f,8f)) }
        compose.runOnIdle { assertTrue(selected) }
    }
}
