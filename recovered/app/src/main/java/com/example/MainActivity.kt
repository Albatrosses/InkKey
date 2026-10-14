package com.example
 
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.canvas.CanvasScreen
import com.example.ui.home.HomeScreen
import com.example.ui.ime.ImeSetupScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                mainViewModel = viewModel

                LaunchedEffect(Unit) {
                    handleIntent(intent, viewModel)
                }

                val currentScreen by viewModel.currentScreen.collectAsState()
                val isFullScreen by viewModel.isFullScreen.collectAsState()

                // Genuine Full Screen Mode: hide system bars dynamically
                LaunchedEffect(isFullScreen) {
                    try {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        if (isFullScreen) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.systemBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.systemBars())
                        }
                    } catch (ignored: Exception) {
                    }
                }

                Surface(modifier = Modifier.fillMaxSize().onPreviewKeyEvent { composeEvent ->
                    val event = composeEvent.nativeKeyEvent
                    val xpPen = event.device?.name?.contains("ugtablet deco mini7 v2", true) == true
                    if (xpPen && event.keyCode == KeyEvent.KEYCODE_SPACE) {
                        viewModel.handleHardwareKeyEvent(event.keyCode, event.scanCode, event)
                        true
                    } else false
                }) {
                    when (currentScreen) {
                        ScreenState.IME_SETUP -> {
                            ImeSetupScreen(
                                onOpenFullscreenWorkspace = { viewModel.navigateTo(ScreenState.HOME) }
                            )
                        }
                        ScreenState.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenCanvas = { viewModel.navigateTo(ScreenState.CANVAS) },
                                onOpenSettings = { viewModel.navigateTo(ScreenState.SETTINGS) }
                            )
                        }
                        ScreenState.CANVAS -> {
                            CanvasScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateTo(ScreenState.HOME) }
                            )
                        }
                        ScreenState.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
        window.decorView.post {
            (window.decorView as? android.view.ViewGroup)?.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            window.decorView.isFocusableInTouchMode = true
            window.decorView.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        if (mainViewModel?.handleHardwareKeyEvent(keyCode, event.scanCode, event) == true) true else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (mainViewModel?.handleHardwareKeyEvent(keyCode, event.scanCode, event) == true) true else super.onKeyUp(keyCode, event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel?.let { handleIntent(intent, it) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && mainViewModel?.tabletConfig?.value?.customKeyCodes?.values?.contains(KeyEvent.KEYCODE_BACK) != true) return super.dispatchKeyEvent(event)
        val vm = mainViewModel
        if (vm != null && vm.handleHardwareKeyEvent(event.keyCode, event.scanCode, event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    val vm = mainViewModel
    if (vm != null && vm.handleTouchMotionEvent(event)) {
        return true
    }
    return super.dispatchTouchEvent(event)
}

override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        com.example.util.UpperTrialTrace.motion(event, "GENERIC")
        val vm = mainViewModel
        if (vm != null && vm.handleGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onPause() {
        mainViewModel?.resetUpperTrialInput()
        super.onPause()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        mainViewModel?.trimMemory()
    }

    private fun handleIntent(intent: Intent?, viewModel: MainViewModel) {
        if (intent == null) return
        val isExplicitSettings = intent.getBooleanExtra("OPEN_SETTINGS", false) ||
                intent.action == "android.intent.action.APPLICATION_PREFERENCES" ||
                intent.hasExtra("settings") ||
                intent.categories?.contains("android.intent.category.PREFERENCE") == true ||
                intent.categories?.contains("android.intent.category.NOTIFICATION_PREFERENCES") == true
        if (isExplicitSettings) {
            viewModel.navigateTo(ScreenState.SETTINGS)
        }
    }
}
