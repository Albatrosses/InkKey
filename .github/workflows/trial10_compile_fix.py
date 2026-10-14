from pathlib import Path

p = Path("app/src/main/java/com/example/ui/components/GlassLensHandle.kt")
text = p.read_text(encoding="utf-8")
old_import = "import androidx.compose.foundation.layout.matchParentSize\n"
old_call = "Canvas(Modifier.matchParentSize())"
if old_import not in text or old_call not in text:
    raise SystemExit("Trial 10 compile-fix anchors not found")
text = text.replace(old_import, "import androidx.compose.foundation.layout.fillMaxSize\n", 1)
text = text.replace(old_call, "Canvas(Modifier.fillMaxSize())", 1)
p.write_text(text, encoding="utf-8")
print("Trial 10 compile fix applied: matchParentSize -> fillMaxSize")
