package com.example.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.ime.ui.KeyboardCanvasView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class InkKeyInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(AndroidUiDispatcher.CurrentThread + serviceJob)
    private var recomposer: Recomposer? = null
    private var currentComposeView: ComposeView? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val r = Recomposer(AndroidUiDispatcher.CurrentThread)
        recomposer = r
        serviceScope.launch {
            r.runRecomposeAndApplyChanges()
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        // Clean up any previously existing view to prevent accumulating compositions
        currentComposeView?.disposeComposition()

        val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
        val inputRepo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val composeView = ComposeView(themedContext)
        val rootView = object : android.widget.FrameLayout(themedContext) {
            override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                inputRepo.handleTouchMotionEvent(event)
                return super.dispatchTouchEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
                val handled = inputRepo.handleGenericMotionEvent(event)
                return if (handled) true else super.dispatchGenericMotionEvent(event)
            }

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return if (com.example.util.InputOwnership.routeImeHardwareKey(isInputViewShown) { inputRepo.handleHardwareKeyEvent(event) }) true else super.dispatchKeyEvent(event)
            }

            override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
                return if (com.example.util.InputOwnership.routeImeHardwareKey(isInputViewShown) { inputRepo.handleHardwareKeyEvent(event) }) true else super.dispatchKeyEventPreIme(event)
            }
        }
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setParentCompositionContext(recomposer)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        composeView.setContent {
            MyApplicationTheme {
                KeyboardCanvasView(
                    onCommitText = { text ->
                        currentInputConnection?.commitText(text, 1)
                    },
                    onCommitImage = { file, _ ->
                        commitHandwrittenImage(file)
                    },
                    onSendEditorAction = {
                        handleEditorAction()
                    },
                    onDeleteLastChar = {
                        val ic = currentInputConnection
                        val selectedText = ic?.getSelectedText(0)
                        if (selectedText.isNullOrEmpty()) {
                            ic?.deleteSurroundingText(1, 0)
                        } else {
                            ic.commitText("", 1)
                        }
                    },
                    onSwitchKeyboard = {
                        switchInputMethod()
                    }
                )
            }
        }
        currentComposeView = composeView
        rootView.addView(
            composeView,
            android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return rootView
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Note: Android reuses the InputView returned by onCreateInputView across show/hide cycles.
        // Disposing composition here breaks subsequent keyboard sessions.
        // ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed and onDestroy() handle proper disposal.
        try {
            val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
            repo.clearTransientState()
        } catch (ignored: Exception) {
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        com.example.ime.data.ImeDocumentRepository.getInstance(this).reloadTabletConfig()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        // Reset any live transient stylus state when switching apps
        try {
            val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
            repo.clearTransientState()
        } catch (ignored: Exception) {
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
            repo.trimMemory()
        } catch (ignored: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        currentComposeView?.disposeComposition()
        currentComposeView = null
        store.clear()
        serviceScope.cancel()
        recomposer?.cancel()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val ownsInput = com.example.util.InputOwnership.imeOwnsHardwareInput(isInputViewShown)
        val handled = com.example.util.InputOwnership.routeImeHardwareKey(ownsInput) { repo.handleHardwareKeyEvent(event) }
        if (handled) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event)
        val repo = com.example.ime.data.ImeDocumentRepository.getInstance(this)
        val ownsInput = com.example.util.InputOwnership.imeOwnsHardwareInput(isInputViewShown)
        val handled = com.example.util.InputOwnership.routeImeHardwareKey(ownsInput) { repo.handleHardwareKeyEvent(event) }
        if (handled) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (!isInputViewShown) return super.onGenericMotionEvent(event)
        if (com.example.ime.data.ImeDocumentRepository.getInstance(this).handleGenericMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    private fun commitHandwrittenImage(file: File) {
        val ic = currentInputConnection
        val editorInfo = currentInputEditorInfo

        try {
            val authority = "${packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(this, authority, file)

            // Grant temporary read permission to the active foreground application package
            val targetPackage = editorInfo?.packageName
            if (!targetPackage.isNullOrEmpty()) {
                try {
                    grantUriPermission(
                        targetPackage,
                        contentUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (ignored: Exception) {
                }
            }

            // 1. Always copy to Android system clipboard so the user can immediately paste in ANY application
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(contentResolver, "InkKey Handwritten Answer", contentUri)
            clipboard.setPrimaryClip(clip)

            // 2. Check if receiving app supports direct image insertion (WhatsApp, Telegram, Notes, Keep, etc.)
            if (ic != null && editorInfo != null) {
                val supportedMimes = EditorInfoCompat.getContentMimeTypes(editorInfo)
                val hasImageSupport = supportedMimes.any {
                    it.equals("image/png", ignoreCase = true) ||
                            it.equals("image/jpeg", ignoreCase = true) ||
                            it.equals("image/*", ignoreCase = true)
                }

                if (hasImageSupport) {
                    val inputContentInfo = InputContentInfoCompat(
                        contentUri,
                        ClipDescription("InkKey Handwritten Answer", arrayOf("image/png")),
                        null
                    )

                    var flags = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                    }

                    val success = InputConnectionCompat.commitContent(ic, editorInfo, inputContentInfo, flags, null)
                    if (success) {
                        Toast.makeText(this, "Handwritten answer inserted directly!", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }

            // 3. Graceful Fallback for text-only fields: inform user clearly that it's on the clipboard ready to paste
            Toast.makeText(
                this,
                "Answer copied to clipboard! Tap Paste in your app to attach.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Insertion notice: ${e.localizedMessage}. Copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleEditorAction() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo

        val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            // Send newline / enter key event
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun switchInputMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (switchToPreviousInputMethod()) return
            } catch (e: Exception) {
                // fall through
            }
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
