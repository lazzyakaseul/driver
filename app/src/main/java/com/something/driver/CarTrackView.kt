package com.something.driver

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animationHelper: AnimationHelper
    private val pathDispatcher = PathDispatcher()
    private var roadColor: Int by Delegates.notNull()
    private var handleTouches = true
    private var cacheX = 0f
    private var cacheY = 0f
    private var cacheAngle = 90f
    private var cacheOldValue = 0
    private var cacheSurfRotation = 0

    init {
        animationHelper = AnimationHelper(TRIP_DURATION, { handleTouches = false }, { invalidate() })
            .apply {
                setOnFinishListener {
                    handleTouches = true
                    pathDispatcher.carX = endPoint[0]
                    pathDispatcher.carY = endPoint[1]
                }
            }
        attrs?.apply {
            val typedArray = context.obtainStyledAttributes(this, R.styleable.CarTrackView, 0, 0)
            typedArray.getResourceId(R.styleable.CarTrackView_roadColor, 0)
                .apply {
                    roadColor = ContextCompat.getColor(context, if (this > 0) this else R.color.colorAccent)
                }

            typedArray.recycle()
        }

        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    if (handleTouches) {
                        pathDispatcher.countPoints(event.x, event.y, animationHelper.receiveAngle().toFloat()).apply {
                            animationHelper.animate(this)
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
                setStartPositions(first, second, getNewAngle(cacheAngle))
            }
        } else if (cacheX == 0f && cacheY == 0f) {
            val startX =
                measuredWidth / 2f + car.height / 2//cos(Math.toRadians(cacheAngle.toDouble())).toFloat() * car.width / 2
            val startY =
                measuredHeight / 2f - car.width / 2//sin(Math.toRadians(cacheAngle.toDouble())).toFloat() * car.height / 2
            setStartPositions(startX, startY, cacheAngle)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawColor(roadColor)
            canvas.drawBitmap(car, animationHelper.carMatrix, paint)
        }
        super.onDraw(canvas)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState())
        bundle.putFloat(POINT_X, pathDispatcher.carX)
        bundle.putFloat(POINT_Y, pathDispatcher.carY)
        val angle = -(animationHelper.receiveAngle() * 180 / Math.PI).toFloat()
        bundle.putFloat(ANGLE, angle)
        bundle.putInt(SURF_ROTATION, cacheSurfRotation)

        val oldValue = if (getScreenRotation().let { it == ROTATION_90 || it == ROTATION_270 }) {
            measuredWidth
        } else {
            measuredHeight
        }
        bundle.putInt(OLD_VALUE, oldValue)
        return bundle
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


    private fun setStartPositions(startX: Float, startY: Float, startAngle: Float) {
        animationHelper.setStartPosition(startX, startY, startAngle)
        pathDispatcher.apply {
            carX = startX
            carY = startY
        }
    }

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