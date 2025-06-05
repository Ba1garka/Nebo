package com.example.nebo.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


import android.graphics.Path
import android.graphics.Shader
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Параметры кисти
    data class BrushSettings(
        var color: Int = Color.BLACK,
        var size: Float = 10f,
        var alpha: Int = 255,
        var strokeCap: Paint.Cap = Paint.Cap.ROUND,
        var texture: Bitmap? = null,
        var blurRadius: Float = 0f
    )

    private val paths = mutableListOf<Path>()
    private val paints = mutableListOf<Paint>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null
    private var onDrawListener: (Canvas) -> Unit = {}

    // История действий для отмены/повтора
    private val undoStack = mutableListOf<Pair<Path, Paint>>()
    private val redoStack = mutableListOf<Pair<Path, Paint>>()

    // Текущие настройки кисти
    private val brushSettings = BrushSettings()

    fun setBrushColor(color: Int) {
        brushSettings.color = color
        updateCurrentPaint()
    }

    fun setBrushSize(size: Float) {
        brushSettings.size = size
        updateCurrentPaint()
    }

    fun setBrushAlpha(alpha: Int) {
        brushSettings.alpha = alpha.coerceIn(0, 255)
        updateCurrentPaint()
    }

    fun setBrushStrokeCap(cap: Paint.Cap) {
        brushSettings.strokeCap = cap
        updateCurrentPaint()
    }

    fun setBrushTexture(bitmap: Bitmap?) {
        brushSettings.texture = bitmap
        updateCurrentPaint()
    }

    fun setBrushBlur(radius: Float) {
        brushSettings.blurRadius = radius
        updateCurrentPaint()
    }

    private fun updateCurrentPaint() {
        currentPaint = createPaintFromSettings()
    }

    private fun createPaintFromSettings(): Paint {
        return Paint().apply {
            color = brushSettings.color
            strokeWidth = brushSettings.size
            this.alpha = brushSettings.alpha
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = brushSettings.strokeCap
            isAntiAlias = true
            isDither = true

            brushSettings.texture?.let { texture ->
                shader = BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }

            if (brushSettings.blurRadius > 0) {
                maskFilter = BlurMaskFilter(brushSettings.blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    fun undo() {
        if (paths.isNotEmpty() && paints.isNotEmpty()) {
            redoStack.add(Pair(paths.removeAt(paths.size - 1), paints.removeAt(paints.size - 1)))
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val (path, paint) = redoStack.removeAt(redoStack.size - 1)
            paths.add(path)
            paints.add(paint)
            invalidate()
        }
    }

    fun clearCanvas() {
        paths.clear()
        paints.clear()
        undoStack.clear()
        redoStack.clear()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                redoStack.clear()
                currentPath = Path()
                currentPath?.moveTo(x, y)
                currentPath?.let { path ->
                    paths.add(path)
                    currentPaint?.let { paint ->
                        paints.add(paint)
                        undoStack.add(Pair(Path(path), Paint(paint)))
                    }
                }
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
