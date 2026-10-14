import os

# 1. Fix MainActivity.kt (Catch XP-Pen zero-pressure upper barrel touch events)
main_activity_path = "app/src/main/java/com/example/MainActivity.kt"
if os.path.exists(main_activity_path):
    with open(main_activity_path, "r", encoding="utf-8") as f:
        code = f.read()

    old_dispatch = """    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val vm = mainViewModel
        if (vm != null && vm.handleGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }"""

    new_dispatch = """    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val vm = mainViewModel
        val isStylusZeroPressure = event.pointerCount > 0 &&
                event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
                event.pressure <= 0.001f &&
                (event.actionMasked == MotionEvent.ACTION_DOWN ||
                 event.actionMasked == MotionEvent.ACTION_MOVE ||
                 event.actionMasked == MotionEvent.ACTION_UP)
        if (vm != null && (event.buttonState != 0 || event.actionButton != 0 ||
                (event.pointerCount > 0 && event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) ||
                isStylusZeroPressure)) {
            vm.handleGenericMotionEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val vm = mainViewModel
        if (vm != null && vm.handleGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }"""

    if "override fun dispatchTouchEvent" not in code:
        code = code.replace(old_dispatch, new_dispatch)
        with open(main_activity_path, "w", encoding="utf-8") as f:
            f.write(code)
        print("Patched MainActivity.kt")

# 2. Fix TabletModels.kt (Default upperBarrelAction to STROKE_ERASER instead of HAND)
tablet_models_path = "app/src/main/java/com/example/model/TabletModels.kt"
if os.path.exists(tablet_models_path):
    with open(tablet_models_path, "r", encoding="utf-8") as f:
        code = f.read()
    code = code.replace(
        "val upperBarrelAction: TabletAction = TabletAction.HAND,",
        "val upperBarrelAction: TabletAction = TabletAction.STROKE_ERASER,"
    ).replace(
        'val upperActionId = prefs.getString("upper_barrel_action", TabletAction.HAND.id)',
        'val upperActionId = prefs.getString("upper_barrel_action", TabletAction.STROKE_ERASER.id)'
    ).replace(
        "val upperAction = TabletAction.values().find { it.id == upperActionId } ?: TabletAction.HAND",
        "val upperAction = TabletAction.values().find { it.id == upperActionId } ?: TabletAction.STROKE_ERASER"
    )
    with open(tablet_models_path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Patched TabletModels.kt")

# 3. Fix HandwritingCanvas.kt (Zero-pressure Upper Barrel Eraser + Editable Symbols + Easy Symbol Selection)
canvas_path = "app/src/main/java/com/example/ui/canvas/HandwritingCanvas.kt"
if os.path.exists(canvas_path):
    with open(canvas_path, "r", encoding="utf-8") as f:
        code = f.read()

    old_barrel_block = """                // 3. Stylus barrel button detection & mapping (XP-PEN Deco Mini 7 V2 Stylus)
                val buttonState = motionEvent.buttonState
                // Lower Barrel Button (Barrel 1): BUTTON_STYLUS_PRIMARY (32) or TOOL_TYPE_ERASER
                val rawLower = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) || isHardwareEraser
                // Upper Barrel Button (Barrel 2): BUTTON_STYLUS_SECONDARY (64), BUTTON_SECONDARY (2, Right-click), BUTTON_TERTIARY (4), BUTTON_FORWARD (16), BUTTON_BACK (8)
                val rawUpper = (buttonState and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK)) != 0"""

    new_barrel_block = """                // 3. Stylus barrel button detection & mapping (XP-PEN Deco Mini 7 V2 Stylus)
                val buttonState = motionEvent.buttonState
                // XP-Pen Deco Mini 7 V2 sends ACTION_DOWN/MOVE with TOOL_TYPE_STYLUS and pressure == 0.0f when upper barrel is pressed
                val isZeroPressureUpperBarrel = (toolType == MotionEvent.TOOL_TYPE_STYLUS) &&
                        !isHardwareEraser &&
                        buttonState == 0 &&
                        motionEvent.pressure <= 0.001f &&
                        (motionEvent.actionMasked == MotionEvent.ACTION_DOWN || motionEvent.actionMasked == MotionEvent.ACTION_MOVE)

                if (isZeroPressureUpperBarrel) {
                    if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN && !prevBarrel2Pressed) {
                        prevBarrel2Pressed = true
                        val nextTool = if (currentTool == ToolType.AREA_ERASER || currentTool == ToolType.STROKE_ERASER) {
                            ToolType.PEN
                        } else {
                            if (tabletConfig.upperBarrelAction == com.example.model.TabletAction.AREA_ERASER) ToolType.AREA_ERASER else ToolType.STROKE_ERASER
                        }
                        viewModel.setTool(nextTool)
                    }
                    return@pointerInteropFilter true
                }
                if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_CANCEL) {
                    prevBarrel1Pressed = false
                    prevBarrel2Pressed = false
                }

                // Lower Barrel Button (Barrel 1): BUTTON_STYLUS_PRIMARY (32) or TOOL_TYPE_ERASER
                val rawLower = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) || isHardwareEraser
                // Upper Barrel Button (Barrel 2): BUTTON_STYLUS_SECONDARY (64), BUTTON_SECONDARY (2, Right-click), BUTTON_TERTIARY (4), BUTTON_FORWARD (16), BUTTON_BACK (8)
                val rawUpper = (buttonState and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK)) != 0"""

    code = code.replace(old_barrel_block, new_barrel_block)
    # Increase symbol hit-test padding from 8f to 22f so symbols are easy to tap and edit with a stylus
    code = code.replace("sym.hitTest(canvasX, canvasY, padding = 8f)", "sym.hitTest(canvasX, canvasY, padding = 22f)")

    with open(canvas_path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Patched HandwritingCanvas.kt")

# 4. Fix KeyboardCanvasView.kt (Zero-pressure Upper Barrel Eraser + Shape Snapping in IME Keyboard + Easy Symbol Selection)
ime_canvas_path = "app/src/main/java/com/example/ime/ui/KeyboardCanvasView.kt"
if os.path.exists(ime_canvas_path):
    with open(ime_canvas_path, "r", encoding="utf-8") as f:
        code = f.read()

    old_ime_barrel = """                            val pressure = motionEvent.pressure
                            val buttonState = motionEvent.buttonState
                            val isBarrel1 = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) || isEraserHardware
                            val isBarrel2 = (buttonState and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK)) != 0

                            val activeTool = when {
                                isBarrel1 -> ToolType.STROKE_ERASER
                                isBarrel2 -> ToolType.HAND
                                isEraserHardware -> ToolType.STROKE_ERASER
                                else -> currentTool
                            }"""

    new_ime_barrel = """                            val pressure = motionEvent.pressure
                            val buttonState = motionEvent.buttonState
                            val isZeroPressureUpper = (toolType == MotionEvent.TOOL_TYPE_STYLUS) &&
                                    !isEraserHardware &&
                                    buttonState == 0 &&
                                    pressure <= 0.001f &&
                                    (motionEvent.actionMasked == MotionEvent.ACTION_DOWN || motionEvent.actionMasked == MotionEvent.ACTION_MOVE)

                            if (isZeroPressureUpper) {
                                if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                                    val toggled = if (currentTool == ToolType.STROKE_ERASER || currentTool == ToolType.AREA_ERASER) ToolType.PEN else ToolType.STROKE_ERASER
                                    repository.setTool(toggled)
                                    notificationMessage = if (toggled == ToolType.PEN) "Switched to Pen" else "Switched to Eraser"
                                }
                                return@pointerInteropFilter true
                            }

                            val isBarrel1 = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) || isEraserHardware
                            val isBarrel2 = (buttonState and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK)) != 0

                            val activeTool = when {
                                isBarrel1 -> ToolType.STROKE_ERASER
                                isBarrel2 -> ToolType.STROKE_ERASER
                                isEraserHardware -> ToolType.STROKE_ERASER
                                else -> currentTool
                            }"""

    code = code.replace(old_ime_barrel, new_ime_barrel)
    code = code.replace("hitTestSymbol(sym, canvasX, canvasY, padding = 8f)", "hitTestSymbol(sym, canvasX, canvasY, padding = 20f)")

    with open(ime_canvas_path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Patched KeyboardCanvasView.kt")

# 5. Fix ShapeRecognizer.kt (Honor selected shape type & relax freehand shape detection tolerances)
shape_path = "app/src/main/java/com/example/util/ShapeRecognizer.kt"
if os.path.exists(shape_path):
    with open(shape_path, "r", encoding="utf-8") as f:
        code = f.read()

    # Relax circle/rectangle/line tolerances for natural tablet drawing & honor explicit shapeType
    old_start = """    fun recognizeAndSnap(stroke: DrawingStroke): DrawingStroke {
        val points = stroke.points
        if (points.size < 6) return stroke // too short to classify as shape"""

    new_start = """    fun recognizeAndSnap(stroke: DrawingStroke): DrawingStroke {
        val points = stroke.points
        if (points.size < 3) return stroke
        if (stroke.shapeType != ShapeType.NONE) {
            val s = points.first()
            val e = points.last()
            val minX = points.minOf { it.x }
            val maxX = points.maxOf { it.x }
            val minY = points.minOf { it.y }
            val maxY = points.maxOf { it.y }
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            when (stroke.shapeType) {
                ShapeType.LINE -> return stroke.copy(points = listOf(DrawingPoint(s.x, s.y), DrawingPoint(e.x, e.y)), isShape = true, shapeType = ShapeType.LINE)
                ShapeType.RECTANGLE -> return stroke.copy(points = listOf(DrawingPoint(minX, minY), DrawingPoint(maxX, minY), DrawingPoint(maxX, maxY), DrawingPoint(minX, maxY), DrawingPoint(minX, minY)), isShape = true, shapeType = ShapeType.RECTANGLE)
                ShapeType.TRIANGLE -> return stroke.copy(points = listOf(DrawingPoint(cx, minY), DrawingPoint(maxX, maxY), DrawingPoint(minX, maxY), DrawingPoint(cx, minY)), isShape = true, shapeType = ShapeType.TRIANGLE)
                ShapeType.CIRCLE -> {
                    val r = max(maxX - minX, maxY - minY) / 2f
                    val cPts = (0..48).map { idx ->
                        val ang = 2f * PI.toFloat() * idx / 48
                        DrawingPoint(cx + r * cos(ang), cy + r * sin(ang), 1f)
                    }
                    return stroke.copy(points = cPts, isShape = true, shapeType = ShapeType.CIRCLE)
                }
                ShapeType.ELLIPSE -> {
                    val a = (maxX - minX) / 2f
                    val b = (maxY - minY) / 2f
                    val ePts = (0..48).map { idx ->
                        val ang = 2f * PI.toFloat() * idx / 48
                        DrawingPoint(cx + a * cos(ang), cy + b * sin(ang), 1f)
                    }
                    return stroke.copy(points = ePts, isShape = true, shapeType = ShapeType.ELLIPSE)
                }
                else -> {}
            }
        }
        if (points.size < 6) return stroke"""

    code = code.replace(old_start, new_start)
    code = code.replace("cv < 0.13f && angularCoverage >= 315f", "cv < 0.22f && angularCoverage >= 275f")
    code = code.replace("straightnessRatio >= 0.90f", "straightnessRatio >= 0.85f")

    with open(shape_path, "w", encoding="utf-8") as f:
        f.write(code)
    print("Patched ShapeRecognizer.kt")
      
