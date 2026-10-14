from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Trial 11 anchor count for {path}: expected 1, found {count}\nANCHOR:\n{old}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


# User-facing express-key feedback should show only the physical K number and
# action. Raw keyboard chords/letters are implementation details and are kept
# out of the normal popup.
repo = "app/src/main/java/com/example/ime/data/ImeDocumentRepository.kt"
replace_once(
    repo,
    '''                    executeTabletAction(action)
                    val chord = if (isXpPen && customIndex == null) {
                        com.example.util.XpPenExpressKeyResolver.chordLabel(keyIndex)
                    } else {
                        "Learned"
                    }
                    _tabletFeedbackMessage.value = "K$keyIndex ($chord): ${action.displayName}"
''',
    '''                    executeTabletAction(action)
                    _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
''',
)

vm = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
replace_once(
    vm,
    '''                executeTabletAction(action)
                val chord = if (isXpPen && customIndex == null) {
                    com.example.util.XpPenExpressKeyResolver.chordLabel(keyIndex)
                } else {
                    "Learned"
                }
                _tabletFeedbackMessage.value = "K$keyIndex ($chord): ${action.displayName}"
''',
    '''                executeTabletAction(action)
                _tabletFeedbackMessage.value = "K$keyIndex: ${action.displayName}"
''',
)

for path in (repo, vm):
    text = Path(path).read_text(encoding="utf-8")
    if "($chord)" in text:
        raise SystemExit(f"Trial 11 popup cleanup failed in {path}")

print("Trial 11 popup cleanup complete")
