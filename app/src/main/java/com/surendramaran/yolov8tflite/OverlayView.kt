package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // Default box color; can be overridden later
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Check the orientation of the canvas and set width and height accordingly
        val canvasWidth = width
        val canvasHeight = height

        results.forEach { box ->
            // Adjust coordinates based on current orientation
            val left = box.x1 * canvasWidth
            val top = box.y1 * canvasHeight
            val right = box.x2 * canvasWidth
            val bottom = box.y2 * canvasHeight

            // Set the box color based on expiry status
            boxPaint.color = box.color // Use the color from the BoundingBox

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)
            val drawableText = box.clsName

            // Prepare text background
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )
            // Draw text on the bounding box
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    fun setResults(boundingBoxesModel1: List<BoundingBox>) {
        results = boundingBoxesModel1 // Update the results
        invalidate() // Request redraw
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
