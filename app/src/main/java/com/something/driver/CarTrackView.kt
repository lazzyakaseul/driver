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
import android.view.View
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
                        val values = FloatArray(9)
                        animationHelper.carMatrix.getValues(values)
                        val angle = Math.atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble())
                        pathDispatcher.countPoints(event.x, event.y, angle.toFloat()).apply {
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
        val startX = measuredWidth / 2f - car.width / 2
        val startY = measuredHeight / 2f - car.height / 2
        if (cacheX > 0 && cacheY > 0) {
            setStartPositions(cacheX, cacheY)
        } else {
            setStartPositions(startX, startY)
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
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            cacheX = state.getFloat(POINT_X)
            cacheY = state.getFloat(POINT_Y)
            super.onRestoreInstanceState(state.getParcelable<Parcelable>(SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun setStartPositions(startX: Float, startY: Float) {
        animationHelper.setStartPosition(startX, startY)
        pathDispatcher.apply {
            carX = startY
            carY = startY
        }
    }

    private companion object {

        private const val SUPER_STATE = "superState"
        private const val POINT_X = "point_x"
        private const val POINT_Y = "point_y"
        private const val TRIP_DURATION = 4000L

    }

}