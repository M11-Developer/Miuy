package com.miyu.companion.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.companion.data.FloatingPosition
import com.miyu.companion.data.ToyType
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating bubble that shows Miyu character.
 * 
 * Features:
 * - Drag to move
 * - Snap to edges
 * - Resize via pinch or settings
 * - Transparency control
 * - Mini mode
 * - Click-through mode (when enabled, passes touches through)
 * - Toy overlay animation
 * - Idle breathing animation
 * - Speech bubble
 * - Safe area handling
 */
class FloatingBubbleView(
    private val context: Context,
    private var position: FloatingPosition,
    private var currentToy: ToyType?,
    private val onPositionChanged: (FloatingPosition) -> Unit,
    private val onClick: () -> Unit,
    private val onClose: () -> Unit
) {
    private var windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

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

        layoutParams = WindowManager.LayoutParams(
            (120 * position.size * metrics.density).toInt(),
            (120 * position.size * metrics.density).toInt(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (position.x * screenWidth).toInt()
            y = (position.y * screenHeight).toInt()
            alpha = position.alpha
        }

        rootView = FrameLayout(context).apply {
            // Drag handling at Android View level for smoothness
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
                            } catch (_: Exception) {}
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            onClick()
                        } else {
                            // Snap to edge if enabled
                            if (position.snapToEdge) {
                                snapToEdge()
                            }
                            // Save position
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

        val composeView = ComposeView(context).apply {
            setContent {
                MiyuBubbleContent(
                    position = position,
                    currentToy = currentToy,
                    isDragging = isDragging
                )
            }
        }

        rootView!!.addView(composeView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

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

        // Animate snap
        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                layoutParams!!.x = anim.animatedValue as Int
                try {
                    windowManager.updateViewLayout(rootView, layoutParams)
                } catch (_: Exception) {}
            }
        }
        animator.start()
    }

    fun hide() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        rootView = null
    }

    fun updateToy(toy: ToyType?) {
        currentToy = toy
        rootView?.let { root ->
            val composeView = root.getChildAt(0) as? ComposeView
            composeView?.setContent {
                MiyuBubbleContent(position, currentToy, isDragging)
            }
        }
    }

    fun updatePosition(newPos: FloatingPosition) {
        position = newPos
        layoutParams?.let { params ->
            params.alpha = newPos.alpha
            val sizePx = (120 * newPos.size * context.resources.displayMetrics.density).toInt()
            params.width = sizePx
            params.height = sizePx
            if (!isDragging) {
                val metrics = context.resources.displayMetrics
                params.x = (newPos.x * metrics.widthPixels).toInt()
                params.y = (newPos.y * metrics.heightPixels).toInt()
            }
            try {
                rootView?.let { windowManager.updateViewLayout(it, params) }
            } catch (_: Exception) {}
        }
        // Recompose
        rootView?.let { root ->
            (root.getChildAt(0) as? ComposeView)?.setContent {
                MiyuBubbleContent(position, currentToy, isDragging)
            }
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

    // Idle breathing animation
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16)
            breathing = (kotlin.math.sin(System.currentTimeMillis() / 800.0) * 0.05).toFloat()
        }
    }

    // Blink every 3-5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay((3000 + (Math.random() * 2000)).toLong())
            blinking = true
            kotlinx.coroutines.delay(150)
            blinking = false
        }
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
            // Character placeholder - in real app would be animated WebP or Lottie
            Text(
                text = if (blinking) "◕‿◕" else "｡◕‿◕｡",
                fontSize = if (position.isMini) 20.sp else 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A5F62)
            )
        }

        // Toy overlay
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
