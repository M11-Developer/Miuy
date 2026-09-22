package com.miyu.companion.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.miyu.companion.data.FloatingPosition
import com.miyu.companion.data.ToyType
import kotlin.math.abs

/**
 * Floating bubble that shows the Miyu character.
 *
 * Features:
 * - Drag to move, snap to edges (animated)
 * - Resize through the control panel (size 0.5x .. 1.5x)
 * - Transparency control
 * - Mini mode
 * - Click-through mode (FLAG_NOT_TOUCHABLE: touches reach the app underneath)
 * - Toy overlay animation (Play Lab toy rides along with the bubble)
 * - Idle breathing animation, blinking, speech bubble
 * - Stop-motion switch (freezes every animation for calmer play / reduced motion)
 * - Safe area + notch aware placement, portrait & landscape aware
 *
 * Privacy: the bubble draws the Miyu character only. It never reads other apps, never
 * captures keystrokes, never inspects screen content and never performs automatic clicks.
 */
class FloatingBubbleView(
    private val context: Context,
    private var position: FloatingPosition,
    private var currentToy: ToyType?,
    private val onPositionChanged: (FloatingPosition) -> Unit,
    private val onClick: () -> Unit,
    private val onClose: () -> Unit
) {
    private var windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: BubbleLifecycleOwner? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    /** Insets (status bar / notch / gesture bar) so the bubble never sits under system UI. */
    private fun safeInsets(): IntArray {
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = if (id > 0) res.getDimensionPixelSize(id) else 0
        val navId = res.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBar = if (navId > 0) res.getDimensionPixelSize(navId) else 0
        return intArrayOf(statusBar, navBar)
    }

    fun show() {
        if (rootView != null) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val (statusBar, navBar) = Pair(safeInsets()[0], safeInsets()[1])

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (position.clickThrough) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        layoutParams = WindowManager.LayoutParams(
            (120 * position.size * metrics.density).toInt(),
            (120 * position.size * metrics.density).toInt(),
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (position.x * screenWidth).toInt().coerceIn(0, maxOf(0, screenWidth - width))
            y = ((position.y * screenHeight).toInt() + statusBar).coerceIn(statusBar, maxOf(statusBar, screenHeight - navBar - height))
            alpha = position.alpha
        }

        val owner = BubbleLifecycleOwner()
        lifecycleOwner = owner

        rootView = FrameLayout(context).apply {
            setOnTouchListener { _, event ->
                if (position.clickThrough) return@setOnTouchListener false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams!!.x
                        initialY = layoutParams!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (!isDragging && (abs(dx) > 10 || abs(dy) > 10)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            layoutParams!!.x = initialX + dx.toInt()
                            layoutParams!!.y = initialY + dy.toInt()
                            try {
                                windowManager.updateViewLayout(this, layoutParams)
                            } catch (_: Exception) {
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            onClick()
                        } else {
                            if (position.snapToEdge) {
                                snapToEdge()
                            }
                            val metrics = context.resources.displayMetrics
                            val newPos = position.copy(
                                x = layoutParams!!.x.toFloat() / metrics.widthPixels,
                                y = layoutParams!!.y.toFloat() / metrics.heightPixels
                            )
                            onPositionChanged(newPos)
                        }
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
        }

        // Compose needs a ViewTreeLifecycleOwner + SavedStateRegistryOwner on the overlay
        // window, otherwise ComposeView.setContent() throws (a WindowManager view has no
        // Activity lifecycle of its own).
        val container = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MiyuBubbleContent(
                    position = position,
                    currentToy = currentToy,
                    isDragging = isDragging
                )
            }
        }
        composeView = container
        rootView!!.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        try {
            windowManager.addView(rootView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun snapToEdge() {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val currentX = layoutParams!!.x
        val threshold = screenWidth * 0.3

        val targetX = when {
            currentX < threshold -> 0
            currentX > screenWidth - threshold -> screenWidth - (layoutParams!!.width)
            else -> currentX
        }

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                layoutParams!!.x = anim.animatedValue as Int
                try {
                    rootView?.let { windowManager.updateViewLayout(it, layoutParams) }
                } catch (_: Exception) {
                }
            }
        }
        animator.start()
    }

    fun hide() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        rootView = null
        composeView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    fun updateToy(toy: ToyType?) {
        currentToy = toy
        refreshContent()
    }

    fun updatePosition(newPos: FloatingPosition) {
        position = newPos
        layoutParams?.let { params ->
            params.alpha = newPos.alpha
            val sizePx = (120 * newPos.size * context.resources.displayMetrics.density).toInt()
            params.width = sizePx
            params.height = sizePx
            // Click-through: pass every touch to the app underneath until it is switched off.
            params.flags = if (newPos.clickThrough) {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            if (!isDragging) {
                val metrics = context.resources.displayMetrics
                params.x = (newPos.x * metrics.widthPixels).toInt()
                params.y = (newPos.y * metrics.heightPixels).toInt()
            }
            try {
                rootView?.let { windowManager.updateViewLayout(it, params) }
            } catch (_: Exception) {
            }
        }
        refreshContent()
    }

    private fun refreshContent() {
        composeView?.setContent {
            MiyuBubbleContent(
                position = position,
                currentToy = currentToy,
                isDragging = isDragging
            )
        }
    }
}

@Composable
fun MiyuBubbleContent(
    position: FloatingPosition,
    currentToy: ToyType?,
    isDragging: Boolean
) {
    var breathing by remember { mutableStateOf(0f) }
    var blinking by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }

    // Idle breathing animation. "Stop motion" freezes it instantly (accessibility + calm play).
    LaunchedEffect(position.motionPaused) {
        if (position.motionPaused) {
            breathing = 0f
            blinking = false
            speaking = false
            return@LaunchedEffect
        }
        while (true) {
            kotlinx.coroutines.delay(16)
            breathing = (kotlin.math.sin(System.currentTimeMillis() / 800.0) * 0.05).toFloat()
        }
    }

    // Blink every 3-5 seconds (stopped when motion is paused).
    LaunchedEffect(position.motionPaused) {
        if (position.motionPaused) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay((3000 + (Math.random() * 2000)).toLong())
            blinking = true
            kotlinx.coroutines.delay(150)
            blinking = false
        }
    }

    // Toy reaction behaves like a tiny speech moment: bubble "talks" when a toy is grabbed.
    LaunchedEffect(currentToy) {
        if (currentToy == null || position.motionPaused) return@LaunchedEffect
        speaking = true
        kotlinx.coroutines.delay(900)
        speaking = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(position.alpha)
            .scale(1f + breathing * 0.1f),
        contentAlignment = Alignment.Center
    ) {
        // Main bubble
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (position.isMini) Color(0xFFF8E8E8) else Color(0xFFFFF5F5)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Character face. The production character art is intentionally not bundled in the
            // public repository; this vector/emoji face keeps the bubble readable and tiny.
            Text(
                text = if (blinking) "◕‿◕" else "｡◕‿◕｡",
                fontSize = if (position.isMini) 20.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A5F62)
            )
        }

        // Toy overlay: appears only when the child asked Miyu to hold something.
        if (currentToy != null && !position.isMini) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentToy.emoji,
                    fontSize = 24.sp
                )
            }
        }

        // Speech bubble (no audio is recorded or stored to produce this).
        if (speaking && !position.isMini) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Miyu ماسكة ${currentToy?.labelAr ?: ""}!",
                    fontSize = 10.sp,
                    color = Color(0xFF8A5F62)
                )
            }
        }

        // Drag handle visual when dragging
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.1f))
            )
        }
    }
}
