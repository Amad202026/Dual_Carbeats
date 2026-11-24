package com.kelompok6.dualcarbeats

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

open class BaseActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        if (this is GameActivity) MenuAudio.enterGame() else MenuAudio.onMenuStarted()
    }
    override fun onResume() {
        super.onResume()
        if (this !is GameActivity) MenuAudio.resume(applicationContext)
    }
    override fun onStop() {
        if (this !is GameActivity) MenuAudio.onMenuStopped()
        super.onStop()
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        // Play on press so navigation does not immediately cut off the feedback.
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            fun clickableAt(v: android.view.View): Boolean {
                val rect = android.graphics.Rect()
                if (!v.isShown || !v.getGlobalVisibleRect(rect) || !rect.contains(event.rawX.toInt(), event.rawY.toInt())) return false
                if (v is ViewGroup) for (i in v.childCount - 1 downTo 0) if (clickableAt(v.getChildAt(i))) return true
                return v.isClickable && v.isEnabled
            }
            if (this !is GameActivity && clickableAt(window.decorView)) MenuAudio.button()
        }
        return super.dispatchTouchEvent(event)
    }
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        hideSystemBars()

        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        // Gameplay's road fills the window; only its HUD receives safe-area padding.
        // Other screens continue to use their root's original safe spacing.
        val insetTarget = root.findViewById<android.view.View>(R.id.gameHud) ?: root
        val density = resources.displayMetrics.density
        val left = maxOf(insetTarget.paddingLeft, (16 * density).toInt())
        val top = maxOf(insetTarget.paddingTop, (12 * density).toInt())
        val right = maxOf(insetTarget.paddingRight, (16 * density).toInt())
        val bottom = maxOf(insetTarget.paddingBottom, (28 * density).toInt())
        insetTarget.setPadding(left, top, right, bottom)
        ViewCompat.setOnApplyWindowInsetsListener(insetTarget) { view, insets ->
            val safe = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(insetTarget)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
