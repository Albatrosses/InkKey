from pathlib import Path

p = Path("app/src/main/java/com/example/ime/InkKeyInputMethodService.kt")
text = p.read_text(encoding="utf-8")
old = '''        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val inputRepo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val composeView = object : ComposeView(themedContext) {
            override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_TOUCH")
                inputRepo.handleTouchMotionEvent(event)
                return super.dispatchTouchEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_GENERIC")
                val handled = inputRepo.handleGenericMotionEvent(event)
                return if (handled) true else super.dispatchGenericMotionEvent(event)
            }
        }
'''
new = '''        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val inputRepo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val composeView = ComposeView(themedContext)
        val rootView = object : android.widget.FrameLayout(themedContext) {
            override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_TOUCH")
                inputRepo.handleTouchMotionEvent(event)
                return super.dispatchTouchEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                com.example.util.UpperTrialTrace.motion(event, "IME_VIEW_GENERIC")
                val handled = inputRepo.handleGenericMotionEvent(event)
                return if (handled) true else super.dispatchGenericMotionEvent(event)
            }
        }
'''
if text.count(old) != 1:
    raise SystemExit(f"expected one invalid ComposeView host block, found {text.count(old)}")
text = text.replace(old, new, 1)
old_return = '''        currentComposeView = composeView
        return composeView
'''
new_return = '''        currentComposeView = composeView
        rootView.addView(
            composeView,
            android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return rootView
'''
if text.count(old_return) != 1:
    raise SystemExit(f"expected one IME return block, found {text.count(old_return)}")
text = text.replace(old_return, new_return, 1)
p.write_text(text, encoding="utf-8")
print("Trial 7 root host fixed: FrameLayout intercepts before final ComposeView")
