package com.something.driver

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface.*
import android.view.View
import android.view.WindowManager
import kotlin.properties.Delegates


class CarTrackView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val car = BitmapFactory.decodeResource(resources, R.drawable.ic_taxi)
    private val offsetX = car.width / 2f
    private val offsetY = car.height / 2f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val carMatrix = Matrix()
    private val animationHelper: AnimationHelper
    private val pathDispatcher = PathDispatcher()
    private var roadColor: Int by Delegates.notNull()
    private var handleTouches = true

    private var cacheAngle: Float by Delegates.notNull()
    private var cacheX = 0f
    private var cacheY = 0f
    private var cacheOldValue = 0
    private var cacheSurfRotation = 0

    init {
        animationHelper =
            AnimationHelper(TRIP_DURATION, { handleTouches = false }, { handleTouches = true }, { invalidate() })
        attrs?.apply {
            context.obtainStyledAttributes(this, R.styleable.CarTrackView, 0, 0).apply {
                getResourceId(R.styleable.CarTrackView_roadColor, 0)
                    .apply {
                        roadColor = ContextCompat.getColor(context, if (this > 0) this else R.color.colorAccent)
                    }
                getFloat(R.styleable.CarTrackView_carStartAngle, 0f)
                    .apply {
                        cacheAngle = this
                    }
                recycle()
            }
        }

        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    if (handleTouches) {
                        animationHelper.getPosition().apply {
                            pathDispatcher.countPoints(x, y, event.x, event.y, animationHelper.receiveAngle().toFloat())
                                .apply {
                                    animationHelper.animate(this)
                                }
                        }
                    }
                }
            }
            true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (cacheX > 0 && cacheY > 0 && cacheSurfRotation != getScreenRotation()) {
            getNewCoordinates(cacheX, cacheY).apply {
                val angle = -(cacheAngle * 180 / Math.PI).toFloat()
                setStartPositions(first, second, getNewAngle(angle))
            }
        } else if (cacheX == 0f && cacheY == 0f) {
            val startX = measuredWidth / 2f + offsetY
            val startY = measuredHeight / 2f - offsetX
            setStartPositions(startX, startY, cacheAngle)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawColor(roadColor)
            carMatrix.reset()
            carMatrix.postRotate(animationHelper.getAngleInDegrees(), offsetX, offsetY)
            animationHelper.getPosition()
                .apply {
                    carMatrix.postTranslate(this.x - offsetX, this.y - offsetY)
                }
            canvas.drawBitmap(car, carMatrix, paint)
        }
        super.onDraw(canvas)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply {
            putParcelable(SUPER_STATE, super.onSaveInstanceState())
            animationHelper.getPosition()
                .apply {
                    putFloat(POINT_X, x)
                    putFloat(POINT_Y, y)
                }
            putFloat(ANGLE, animationHelper.receiveAngle().toFloat())
            putInt(SURF_ROTATION, cacheSurfRotation)
            val oldValue = if (getScreenRotation().let { it == ROTATION_90 || it == ROTATION_270 }) {
                measuredWidth
            } else {
                measuredHeight
            }
            putInt(OLD_VALUE, oldValue)

        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            cacheX = state.getFloat(POINT_X)
            cacheY = state.getFloat(POINT_Y)
            cacheAngle = state.getFloat(ANGLE)
            cacheOldValue = state.getInt(OLD_VALUE)
            cacheSurfRotation = state.getInt(SURF_ROTATION)
            super.onRestoreInstanceState(state.getParcelable<Parcelable>(SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun getNewCoordinates(px: Float, py: Float): Pair<Float, Float> =
        when (getScreenRotation()) {
            ROTATION_0 ->
                if (cacheSurfRotation == ROTATION_90)
                    measuredWidth - py * measuredWidth / cacheOldValue to px
                else
                    py to measuredHeight - px * cacheOldValue / measuredHeight
            ROTATION_90 -> py to measuredHeight - px * measuredHeight / cacheOldValue
            ROTATION_180 -> py * measuredWidth / cacheOldValue to px
            else -> measuredWidth - py to px * measuredHeight / cacheOldValue
        }

    private fun getNewAngle(angle: Float): Float {
        val rotation = getScreenRotation()
        val newAngle = when (rotation) {
            ROTATION_90 -> angle - 90f
            ROTATION_270 -> angle + 90f
            else -> if (cacheSurfRotation == ROTATION_90) angle + 90f else angle - 90f
        }
        cacheSurfRotation = rotation
        return newAngle
    }

    private fun getScreenRotation() =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

    private fun setStartPositions(startX: Float, startY: Float, startAngle: Float) =
        animationHelper.setStartPosition(startX, startY, Math.toRadians(startAngle.toDouble()).toFloat())

    private companion object {

        private const val SUPER_STATE = "superState"
        private const val POINT_X = "point_x"
        private const val POINT_Y = "point_y"
        private const val ANGLE = "angle"
        private const val SURF_ROTATION = "surf_rotation"
        private const val OLD_VALUE = "old_value"
        private const val TRIP_DURATION = 4000L

    }

}