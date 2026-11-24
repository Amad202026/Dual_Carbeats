package com.kelompok6.dualcarbeats

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Lightweight gradients rather than blurred bitmaps or per-frame allocations. */
class NeonHaloDrawable(private val tint: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onBoundsChange(bounds: Rect) {
        paint.shader = RadialGradient(0f, 0f, 1f,
            intArrayOf((tint and 0x00ffffff) or 0x78000000,
                (tint and 0x00ffffff) or 0x38000000, Color.TRANSPARENT),
            floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
    }
    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val save = canvas.save()
        canvas.translate(bounds.exactCenterX(), bounds.exactCenterY())
        canvas.scale(bounds.width() / 2f, bounds.height() / 2f)
        canvas.drawCircle(0f, 0f, 1f, paint)
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Suppress("DEPRECATION") override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Animate pixels from road.png itself. No replacement road art or target guides. */
class ScrollingRoadDrawable(private val source: Bitmap, private val density: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var scroll = 0f
    private data class Stripe(val region: Rect, val cleanRoad: Rect, val markings: Bitmap)
    private val stripes = listOf(.25f, .75f).map { center ->
        val left = ((center - .018f) * source.width).toInt()
        val right = ((center + .018f) * source.width).toInt()
        val width = right - left
        val pixels = IntArray(width * source.height)
        source.getPixels(pixels, 0, width, left, 0, width, source.height)
        // Keep only the existing cyan dash pixels; adjacent road pixels fill their old locations.
        for (i in pixels.indices) {
            val color = pixels[i]
            if (!(Color.red(color) < 65 && Color.green(color) > 120 && Color.blue(color) > 140)) {
                pixels[i] = Color.TRANSPARENT
            }
        }
        Stripe(Rect(left, 0, right, source.height),
            Rect(left - width - 2, 0, left - 2, source.height),
            Bitmap.createBitmap(pixels, width, source.height, Bitmap.Config.ARGB_8888))
    }
    private val destination = RectF()
    fun advance(timeMs: Long) {
        val height = bounds.height().toFloat()
        scroll = if (height > 0) ((timeMs.coerceAtLeast(0) * .12 * density) % height).toFloat() else 0f
        invalidateSelf()
    }
    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val save = canvas.save()
        canvas.clipRect(bounds)
        canvas.drawBitmap(source, null, bounds, paint)
        val scaleX = bounds.width().toFloat() / source.width
        val height = bounds.height().toFloat()
        for (stripe in stripes) {
            val left = bounds.left + stripe.region.left * scaleX
            val right = bounds.left + stripe.region.right * scaleX
            destination.set(left, bounds.top.toFloat(), right, bounds.bottom.toFloat())
            canvas.drawBitmap(source, stripe.cleanRoad, destination, paint)
            destination.offset(0f, scroll)
            canvas.drawBitmap(stripe.markings, null, destination, paint)
            destination.offset(0f, -height)
            canvas.drawBitmap(stripe.markings, null, destination, paint)
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Suppress("DEPRECATION") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
