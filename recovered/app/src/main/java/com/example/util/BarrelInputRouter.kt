package com.example.util

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.example.model.TabletConfig
import com.example.model.TabletAction

/** Shared by the workspace and IME; reads the tool synchronously for each event. */
class BarrelInputRouter(config: () -> TabletConfig, execute: (TabletAction) -> Unit) {
    val tools = BarrelToolController({ upper ->
        val c = config()
        if (upper != c.swapBarrelButtons) c.upperBarrelAction else c.lowerBarrelAction
    }, execute)
    private val detector = ContactRestartDetector()
    private val handler = Handler(Looper.getMainLooper())
    private val leaveRange = Runnable { clearInferred() }

    private fun eligible(event: MotionEvent): Boolean {
        val dev = event.device ?: return false
        return dev.vendorId == 0x28BD && dev.productId == 0x0948 && event.pointerCount == 1 &&
            event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && event.buttonState == 0 && event.actionButton == 0
    }

    private fun edge(edge: ContactRestartDetector.Edge, time: Long) {
        if (edge == ContactRestartDetector.Edge.NONE) return
        tools.setPressed(BarrelToolController.Input.INFERRED_UPPER,
            edge == ContactRestartDetector.Edge.PRESS_CANDIDATE)
        UpperTrialTrace.record("INFERRED_EDGE=$edge time=$time override=${tools.tool.value}")
    }

    fun touch(event: MotionEvent) {
        val action = event.actionMasked
        if (action in intArrayOf(9, 7, 10)) return
        if (action == MotionEvent.ACTION_DOWN) handler.removeCallbacks(leaveRange)
        if (action == MotionEvent.ACTION_CANCEL) { reset(); return }
        physical(event)
        if (!eligible(event)) { clearInferred(); return }
        UpperTrialTrace.motion(event, "TOUCH")
        edge(detector.observe(sample(event), true), event.eventTime)
    }

    fun hover(event: MotionEvent): Boolean {
        val hasPhysical = physical(event)
        if (event.pointerCount == 0) return hasPhysical
        val eligible = eligible(event)
        edge(detector.observeHover(sample(event), eligible), event.eventTime)
        if (eligible && event.actionMasked == MotionEvent.ACTION_HOVER_EXIT) {
            handler.removeCallbacks(leaveRange)
            handler.postDelayed(leaveRange, 200L)
        } else if (event.actionMasked == MotionEvent.ACTION_HOVER_ENTER) {
            handler.removeCallbacks(leaveRange)
        }
        return hasPhysical
    }

    private fun physical(event: MotionEvent): Boolean {
        val target = StylusHardwareDetector.classifyMotionEvent(event)
        val down = event.actionMasked !in intArrayOf(MotionEvent.ACTION_BUTTON_RELEASE, MotionEvent.ACTION_CANCEL)
        tools.setPressed(BarrelToolController.Input.LOWER, down && target == StylusHardwareDetector.BarrelTarget.LOWER_BARREL)
        tools.setPressed(BarrelToolController.Input.UPPER, down && target == StylusHardwareDetector.BarrelTarget.UPPER_BARREL)
        return target != StylusHardwareDetector.BarrelTarget.NONE
    }

    private fun sample(e: MotionEvent) = ContactRestartDetector.Sample(e.actionMasked, e.eventTime, e.x, e.y, e.pressure, e.deviceId)
    private fun clearInferred() {
        tools.setPressed(BarrelToolController.Input.INFERRED_UPPER, false)
        detector.reset()
    }
    fun reset() { handler.removeCallbacks(leaveRange); detector.reset(); tools.clear() }
}
