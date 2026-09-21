package com.miyu.companion.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.miyu.companion.MainActivity
import com.miyu.companion.R
import com.miyu.companion.data.FloatingPosition
import com.miyu.companion.data.PreferencesRepository
import com.miyu.companion.data.ToyType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service for Miyu floating companion.
 * 
 * SECURITY REQUIREMENTS:
 * - Shows persistent notification with stop button
 * - Respects battery saver
 * - Does NOT record audio in background without explicit permission
 * - Does NOT access camera automatically
 * - Does NOT send other apps' data anywhere
 * - Does NOT read other apps' content
 */
class FloatingCompanionService : Service() {

    companion object {
        const val CHANNEL_ID = "miyu_floating_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.miyu.companion.ACTION_STOP"
        const val ACTION_HIDE = "com.miyu.companion.ACTION_HIDE"
        const val ACTION_SHOW_CONTROLS = "com.miyu.companion.ACTION_SHOW_CONTROLS"
    }

    private var bubbleView: FloatingBubbleView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var prefs: PreferencesRepository
    private var currentToy: ToyType? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HIDE -> {
                bubbleView?.hide()
                // Save hidden state but keep service running? Or stop?
                // For UX, we hide bubble but keep notification to re-show
                return START_STICKY
            }
            ACTION_SHOW_CONTROLS -> {
                val controlIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("show_controls", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(controlIntent)
                return START_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            val position = prefs.floatingPosition.first()
            val state = prefs.miyuState.first()
            currentToy = state.toy?.let { key -> ToyType.values().find { it.key == key } }

            withContext(Dispatchers.Main) {
                bubbleView = FloatingBubbleView(
                    context = this@FloatingCompanionService,
                    position = position,
                    currentToy = currentToy,
                    onPositionChanged = { newPos ->
                        serviceScope.launch { prefs.savePosition(newPos) }
                    },
                    onClick = {
                        // Open control panel
                        val controlIntent = Intent(this@FloatingCompanionService, MainActivity::class.java).apply {
                            putExtra("show_controls", true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(controlIntent)
                    },
                    onClose = {
                        stopSelf()
                    }
                )
                bubbleView?.show()
            }

            // Observe position changes
            launch {
                prefs.floatingPosition.collect { pos ->
                    withContext(Dispatchers.Main) {
                        bubbleView?.updatePosition(pos)
                    }
                }
            }

            // Observe toy changes
            launch {
                prefs.miyuState.collect { state ->
                    val toy = state.toy?.let { key -> ToyType.values().find { it.key == key } }
                    if (toy != currentToy) {
                        currentToy = toy
                        withContext(Dispatchers.Main) {
                            bubbleView?.updateToy(toy)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        bubbleView?.hide()
        bubbleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Miyu Floating Companion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Miyu as floating bubble. Tap to control."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, FloatingCompanionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = Intent(this, FloatingCompanionService::class.java).apply {
            action = ACTION_HIDE
        }
        val hidePending = PendingIntent.getService(
            this, 1, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("show_controls", true)
        }
        val openPending = PendingIntent.getActivity(
            this, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Try to get launcher icon, fallback to system icon if not present
        val iconRes = try {
            this.resources.getIdentifier("ic_launcher", "mipmap", this.packageName).takeIf { it != 0 }
                ?: android.R.drawable.ic_dialog_info
        } catch (_: Exception) {
            android.R.drawable.ic_dialog_info
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Miyu is floating ✨")
            .setContentText("Tap to open controls. Drag bubble to move.")
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hide", hidePending)
            .addAction(android.R.drawable.ic_menu_preferences, "Controls", openPending)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
