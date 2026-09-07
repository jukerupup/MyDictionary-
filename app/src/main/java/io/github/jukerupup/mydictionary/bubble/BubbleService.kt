package io.github.jukerupup.mydictionary.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.jukerupup.mydictionary.MainActivity
import io.github.jukerupup.mydictionary.MyDictionaryApplication
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service that hosts the floating dictionary bubble.
 *
 * Extends [LifecycleService] and implements [SavedStateRegistryOwner] so a
 * [ComposeView] added to the WindowManager has the lifecycle/saved-state/view-model
 * owners that Compose requires. This mirrors the production approach used by gkd-kit/gkd.
 */
class BubbleService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubbleView == null) {
            showBubble()
        }
        return START_STICKY
    }

    private fun showBubble() {
        val container = (application as MyDictionaryApplication).container
        val audioController = container.audioControllerFactory(this)
        val controller = BubbleController(
            repository = container.dictionaryRepository,
            scope = scope,
        )

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            setContent {
                MyDictionaryTheme {
                    BubbleOverlay(
                        controller = controller,
                        audioController = audioController,
                        onDismiss = { stopSelf() },
                        onMoved = { dx, dy -> moveBubble(dx, dy) },
                        onExpandedChanged = { expanded -> setFocusable(expanded) },
                        onOpenWeb = { word -> openFullPage(word) },
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 120
        }

        windowManager.addView(view, params)
        bubbleView = view
        layoutParams = params
    }

    private fun moveBubble(deltaX: Int, deltaY: Int) {
        val params = layoutParams ?: return
        runCatching {
            params.x += deltaX
            params.y += deltaY
            bubbleView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    /**
     * When the bubble expands into the lookup card, it needs input focus so the
     * text field can show the IME. Collapsed, it must stay NOT_FOCUSABLE so it
     * does not steal touches from the app underneath.
     */
    private fun setFocusable(focusable: Boolean) {
        val params = layoutParams ?: return
        runCatching {
            if (focusable) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            bubbleView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun openFullPage(word: String) {
        val url = "https://en.wiktionary.org/wiki/" +
            java.net.URLEncoder.encode(word, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    override fun onDestroy() {
        bubbleView?.let { view ->
            runCatching {
                if (view.parent != null) windowManager.removeView(view)
            }
        }
        bubbleView = null
        layoutParams = null
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "bubble"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Dictionary bubble",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("MyDictionary bubble")
            .setContentText("Tap to open the dictionary")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop bubble",
                    stopIntent,
                ).build(),
            )
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "io.github.jukerupup.mydictionary.bubble.STOP"

        fun start(context: Context) {
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BubbleService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
