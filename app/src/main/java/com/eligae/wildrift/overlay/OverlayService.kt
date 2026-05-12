package com.eligae.wildrift.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.eligae.wildrift.overlay.prefs.OverlayPrefs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: OverlayPrefs
    private var overlayView: View? = null
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var dragStartX = 0
    private var dragStartY = 0

    private val loadingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            (overlayView as? OverlayView)?.reloadAll()
        }
    }

    private val previewReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val alpha = intent.getFloatExtra(EXTRA_PREVIEW_ALPHA, 0.8f)
            val accent = intent.getBooleanExtra(EXTRA_PREVIEW_ACCENT, false)
            (overlayView as? OverlayView)?.applyBg(alpha, accent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = OverlayPrefs(this)
        createNotificationChannel()

        val filter = IntentFilter(ScreenCaptureService.ACTION_LOADING_DETECTED)
        val previewFilter = IntentFilter(ACTION_PREVIEW_BG)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(loadingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(previewReceiver, previewFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(loadingReceiver, filter)
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(previewReceiver, previewFilter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(loadingReceiver) } catch (_: Throwable) {}
        try { unregisterReceiver(previewReceiver) } catch (_: Throwable) {}
        hideOverlay()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (overlayView != null) return
        val view = OverlayView(
            this,
            prefs,
            onDragStart = {
                dragStartX = overlayParams.x
                dragStartY = overlayParams.y
            },
            onDrag = { dx, dy ->
                overlayParams.x = dragStartX + dx
                overlayParams.y = dragStartY + dy
                overlayView?.let { windowManager.updateViewLayout(it, overlayParams) }
            },
            onDragEnd = {
                prefs.overlayX = overlayParams.x
                prefs.overlayY = overlayParams.y
            },
        )
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.overlayX
            y = prefs.overlayY
        }
        windowManager.addView(view, overlayParams)
        overlayView = view
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1
        const val ACTION_PREVIEW_BG = "com.eligae.wildrift.overlay.PREVIEW_BG"
        const val EXTRA_PREVIEW_ALPHA = "alpha"
        const val EXTRA_PREVIEW_ACCENT = "accent"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }

        fun previewBg(context: Context, alpha: Float, accent: Boolean) {
            if (!isRunning) return
            val intent = Intent(ACTION_PREVIEW_BG).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_PREVIEW_ALPHA, alpha)
                putExtra(EXTRA_PREVIEW_ACCENT, accent)
            }
            context.sendBroadcast(intent)
        }
    }
}
