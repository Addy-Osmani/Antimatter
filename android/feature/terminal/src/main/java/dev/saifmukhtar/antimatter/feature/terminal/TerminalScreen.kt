package dev.saifmukhtar.antimatter.feature.terminal

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    var terminalViewRef by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<TerminalView?>(null) }

    LaunchedEffect(terminalViewRef) {
        if (terminalViewRef != null) {
            viewModel.redrawEvent.collect {
                terminalViewRef?.onScreenUpdated()
            }
        }
    }

    val ctrlDownState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val altDownState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Extra Keys Row
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).background(androidx.compose.ui.graphics.Color.DarkGray)
            ) {
                val buttonModifier = Modifier.weight(1f)
                val activeColor = androidx.compose.ui.graphics.Color.LightGray
                val defaultColor = androidx.compose.ui.graphics.Color.Transparent
                val textStyle = androidx.compose.ui.text.TextStyle(color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                val btnPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                
                TextButton(onClick = { ctrlDownState.value = !ctrlDownState.value }, modifier = buttonModifier, contentPadding = btnPadding, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(containerColor = if (ctrlDownState.value) activeColor else defaultColor)) { Text("CTRL", style = textStyle) }
                TextButton(onClick = { altDownState.value = !altDownState.value }, modifier = buttonModifier, contentPadding = btnPadding, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(containerColor = if (altDownState.value) activeColor else defaultColor)) { Text("ALT", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(111, android.view.KeyEvent(0, 111)); terminalViewRef?.onKeyUp(111, android.view.KeyEvent(1, 111)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("ESC", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(61, android.view.KeyEvent(0, 61)); terminalViewRef?.onKeyUp(61, android.view.KeyEvent(1, 61)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("TAB", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(19, android.view.KeyEvent(0, 19)); terminalViewRef?.onKeyUp(19, android.view.KeyEvent(1, 19)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("↑", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(20, android.view.KeyEvent(0, 20)); terminalViewRef?.onKeyUp(20, android.view.KeyEvent(1, 20)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("↓", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(21, android.view.KeyEvent(0, 21)); terminalViewRef?.onKeyUp(21, android.view.KeyEvent(1, 21)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("←", style = textStyle) }
                TextButton(onClick = { terminalViewRef?.onKeyDown(22, android.view.KeyEvent(0, 22)); terminalViewRef?.onKeyUp(22, android.view.KeyEvent(1, 22)) }, modifier = buttonModifier, contentPadding = btnPadding) { Text("→", style = textStyle) }
            }

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { ctx ->
                    val terminalView = TerminalView(ctx, null)
                    terminalViewRef = terminalView
                    terminalView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    terminalView.setBackgroundColor(android.graphics.Color.BLACK)
                    terminalView.isFocusable = true
                    terminalView.isFocusableInTouchMode = true
                    
                    var currentTextSize = 28f
                    terminalView.setTextSize(currentTextSize.toInt()) // Set standard font size
                    
                    // Attach the session manually initialized
                    terminalView.attachSession(viewModel.terminalSession)
                    
                    // Client to handle resizing
                    terminalView.setTerminalViewClient(object : TerminalViewClient {
                        override fun onScale(scale: Float): Float {
                            currentTextSize *= scale
                            currentTextSize = currentTextSize.coerceIn(8f, 100f)
                            terminalView.setTextSize(currentTextSize.toInt())
                            return 1.0f
                        }
                        override fun onSingleTapUp(e: android.view.MotionEvent?) {
                            terminalView.requestFocus()
                            val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(terminalView, 0)
                        }
                        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                        override fun shouldEnforceCharBasedInput(): Boolean = false
                        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                        override fun isTerminalViewSelected(): Boolean = true
                        override fun copyModeChanged(copyMode: Boolean) {}
                        override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent?, session: com.termux.terminal.TerminalSession?): Boolean = false
                        override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent?): Boolean = false
                        override fun onLongPress(event: android.view.MotionEvent?): Boolean = false
                        override fun readControlKey(): Boolean {
                            val down = ctrlDownState.value
                            ctrlDownState.value = false
                            return down
                        }
                        override fun readAltKey(): Boolean {
                            val down = altDownState.value
                            altDownState.value = false
                            return down
                        }
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
