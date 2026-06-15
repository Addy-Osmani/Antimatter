package dev.saifmukhtar.antimatter.feature.terminal

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val terminalView = TerminalView(ctx, null)
                    terminalView.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    terminalView.setTextSize(36) // Set standard font size
                    
                    // Attach the session manually initialized
                    terminalView.attachSession(viewModel.terminalSession)
                    
                    // Client to handle resizing
                    terminalView.setTerminalViewClient(object : TerminalViewClient {
                        override fun onScale(scale: Float): Float = 1.0f
                        override fun onSingleTapUp(e: android.view.MotionEvent?) {}
                        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                        override fun shouldEnforceCharBasedInput(): Boolean = false
                        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                        override fun isTerminalViewSelected(): Boolean = true
                        override fun copyModeChanged(copyMode: Boolean) {}
                        override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent?, session: com.termux.terminal.TerminalSession?): Boolean = false
                        override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent?): Boolean = false
                        override fun onLongPress(event: android.view.MotionEvent?): Boolean = false
                        override fun readControlKey(): Boolean = false
                        override fun readAltKey(): Boolean = false
                        override fun readShiftKey(): Boolean = false
                        override fun readFnKey(): Boolean = false
                        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: com.termux.terminal.TerminalSession?): Boolean = false
                        override fun onEmulatorSet() {
                            // First time emulator is set
                            val emulator = viewModel.terminalSession.emulator
                            if (emulator != null) {
                                viewModel.onResize(emulator.mColumns, emulator.mRows)
                            }
                        }
                        override fun logError(tag: String?, message: String?) {}
                        override fun logWarn(tag: String?, message: String?) {}
                        override fun logInfo(tag: String?, message: String?) {}
                        override fun logDebug(tag: String?, message: String?) {}
                        override fun logVerbose(tag: String?, message: String?) {}
                        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
                        override fun logStackTrace(tag: String?, e: Exception?) {}
                    })

                    terminalView
                },
                update = { view ->
                    // Every time layout updates, check emulator size
                    val emulator = viewModel.terminalSession.emulator
                    if (emulator != null && emulator.mColumns > 0 && emulator.mRows > 0) {
                        viewModel.onResize(emulator.mColumns, emulator.mRows)
                    }
                }
            )
        }
    }

    // Biometric background-locking policy: only when returning from background, not tab switching.
    // That means the terminal should remain active as long as the Activity is in the foreground.
    // We don't implement biometric logic here, it is handled at the MainActivity level.
}
