package com.android.daviddev.ecoscancmem.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.android.daviddev.ecoscancmem.R

class ScanOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // — Paints
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_primary)
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.SQUARE
    }
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_primary)
        alpha = 180
        strokeWidth = 3f
    }
    private val objectBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_primary)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val ocrBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF9F27")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_primary)
        style = Paint.Style.FILL
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }

    // — Estado
    var objectBoxes: List<Pair<RectF, String>> = emptyList()
        set(value) { field = value; invalidate() }

    var ocrBoxes: List<RectF> = emptyList()
        set(value) { field = value; invalidate() }

    private var scanLineY = 0f
    private var scanLineDir = 1f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { invalidate() }
        start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawViewfinder(canvas)
        drawScanLine(canvas)
        drawObjectBoxes(canvas)
        drawOcrBoxes(canvas)
    }

    private fun drawViewfinder(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val half = minOf(width, height) * 0.35f
        val left = cx - half; val top = cy - half
        val right = cx + half; val bottom = cy + half
        val arm = half * 0.28f

        // Semi-escurecimento fora do viewfinder
        val dimPaint = Paint().apply {
            color = Color.parseColor("#66000000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), top, dimPaint)
        canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, top, left, bottom, dimPaint)
        canvas.drawRect(right, top, width.toFloat(), bottom, dimPaint)

        // Cantos do viewfinder
        // Canto superior esquerdo
        canvas.drawLine(left, top, left + arm, top, cornerPaint) // horizontal
        canvas.drawLine(left, top, left, top + arm, cornerPaint) // vertical

        // Canto superior direito
        canvas.drawLine(right, top, right - arm, top, cornerPaint)
        canvas.drawLine(right, top, right, top + arm, cornerPaint)

        // Canto inferior esquerdo
        canvas.drawLine(left, bottom, left + arm, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left, bottom - arm, cornerPaint)

        // Canto inferior direito
        canvas.drawLine(right, bottom, right - arm, bottom, cornerPaint)
        canvas.drawLine(right, bottom, right, bottom - arm, cornerPaint)
    }

    private fun drawScanLine(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val half = minOf(width, height) * 0.35f
        val progress = (animator.animatedValue as Float)
        val y = (cy - half + 8f) + progress * (half * 2 - 16f)
        canvas.drawLine(cx - half + 8f, y, cx + half - 8f, y, scanLinePaint)
    }

    private fun drawObjectBoxes(canvas: Canvas) {
        objectBoxes.forEach { (box, label) ->
            canvas.drawRoundRect(box, 12f, 12f, objectBoxPaint)
            val bgRect = RectF(box.left, box.top - 36f, box.left + labelTextPaint.measureText(label) + 16f, box.top)
            canvas.drawRoundRect(bgRect, 6f, 6f, labelBgPaint)
            canvas.drawText(label, box.left + 8f, box.top - 10f, labelTextPaint)
        }
    }

    private fun drawOcrBoxes(canvas: Canvas) {
        ocrBoxes.forEach { box ->
            canvas.drawRoundRect(box, 6f, 6f, ocrBoxPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}