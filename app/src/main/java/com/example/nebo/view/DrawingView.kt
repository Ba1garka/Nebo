package com.example.nebo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


import android.graphics.Path
import android.view.MotionEvent

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paths = mutableListOf<Path>()
    private val paints = mutableListOf<Paint>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    private var onDrawListener: (Canvas) -> Unit = {}

    fun setBrush(color: Int, size: Float) {
        currentPaint = Paint().apply {
            this.color = color
            strokeWidth = size
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    fun clearCanvas() {
        paths.clear()
        paints.clear()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath?.moveTo(x, y)
                currentPath?.let { paths.add(it) }
                currentPaint?.let { paints.add(it) }
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                currentPath = null
            }
        }

        invalidate()
        return true
    }

    fun setCustomOnDrawListener(listener: (Canvas) -> Unit) {
        onDrawListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        onDrawListener(canvas)

        paths.forEachIndexed { index, path ->
            if (index < paints.size) {
                canvas.drawPath(path, paints[index])
            }
        }
    }


}