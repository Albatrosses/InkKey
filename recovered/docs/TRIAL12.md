# Trial 12

Preserves the effective Trial 11 keyboard, captured K7/K8, toolbar docking and fullscreen fixes. Prior CI patch steps were materialized into editable source so the workflow no longer overwrites reviewed Kotlin.

Express identity: K1 B, K2 E, K3 Left Alt, K4 Space, K5 Ctrl+S, K6 Ctrl+Z, K7 scan 78 / keypad add, K8 scan 74 / keypad subtract. Learned key+scan pairs require both identifiers; ambiguous duplicate learned bindings are not guessed. Main-app native barrel key release handling is restored.

Shape recognition uses equal-distance sampling, handles polygon starts on an edge, preserves tilted rectangle/ellipse orientation, supports two-point explicitly selected line/arrow tools, and rejects open arcs and clearly curved lines. Uncertain and invalid strokes retain their original points.

Collapsed toolbar uses a 14 dp borderless water-droplet lens inside a 36 dp transparent touch area. No bezel, rectangular backing, chevron or click ripple. Docking, vertical drag and page magnification remain available. Physical input and appearance still require device testing.
