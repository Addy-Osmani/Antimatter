package dev.saifmukhtar.antimatter

import android.app.Application
import dev.saifmukhtar.antimatter.network.BridgeWebSocket

class AntimatterApp : Application() {
    
    lateinit var webSocket: BridgeWebSocket
        private set

    override fun onCreate() {
        super.onCreate()
        webSocket = BridgeWebSocket(this)
    }
}
