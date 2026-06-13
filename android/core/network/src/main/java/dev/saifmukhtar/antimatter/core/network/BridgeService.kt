package dev.saifmukhtar.antimatter.core.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BridgeService : Service() {

    @Inject lateinit var webSocket: BridgeWebSocket

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createAlertChannel()
        
        serviceScope.launch {
            webSocket.messages.collect { message ->
                if (message is InboundMessage.SystemAlert) {
                    showSystemAlert(message.title, message.body)
                }
            }
        }

        serviceScope.launch {
            webSocket.connectionState.collect { state ->
                if (state == BridgeWebSocket.ConnectionState.DISCONNECTED) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_DISCONNECT") {
            webSocket.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }

        val disconnectIntent = PendingIntent.getService(
            this, 0,
            Intent(this, BridgeService::class.java).apply { action = "ACTION_DISCONNECT" },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "bridge_channel")
            .setContentTitle("Antimatter Bridge")
            .setContentText(if (intent == null) "Reconnecting to Antigravity..." else "Connected to Antigravity IDE")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bridge_channel",
                "Bridge Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the WebSocket connection alive in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alert_channel",
                "System Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alerts from Antigravity IDE"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showSystemAlert(title: String, body: String) {
        val notification = NotificationCompat.Builder(this, "alert_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
