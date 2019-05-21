package com.something.driver

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class CarTrackView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val car = BitmapFactory.decodeResource(resources, R.drawable.ic_taxi)
    private var carX = 0f
    private var carY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animationHelper = AnimationHelper(carX, carY, TRIP_DURATION)
    private val carMatrix = Matrix()
    private var handleTouches = true

    init {
        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    if (handleTouches) {
                        val values = FloatArray(9)
                        carMatrix.getValues(values)
                        val angle = Math.atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble())
                        val x2 = carX + cos(angle).toFloat() * POINT_TWO_DISTANCE
                        val y2 = carY - sin(angle).toFloat() * POINT_TWO_DISTANCE
                        val x4 = event.x
                        val y4 = event.y
                        val angle2 = -Math.atan2(y4.toDouble() - carY, x4.toDouble() - carX)
                        val angleInDegrees = angle * 180 / Math.PI
                        val angleTwoInDegrees = angle2 * 180 / Math.PI
                        val angleDegRound = if (angleInDegrees < 0) angleInDegrees + 360 else angleInDegrees
                        val angleDegTwoRound = if (angleTwoInDegrees < 0) angleTwoInDegrees + 360 else angleTwoInDegrees

                        val diff = Math.abs(angleDegRound - angleDegTwoRound)
                        val resAngle = Math.toRadians(
                            if (diff <= 180) {
                                angleInDegrees + 90
                            } else {
                                angleTwoInDegrees - 90
                            }
                        )

                        val ax = x2 - carX
                        val ay = y2 - carY
                        val bx = x4 - carX
                        val by = y4 - carY
                        val a = sqrt(ax.pow(2) + ay.pow(2))
                        val b = sqrt(bx.pow(2) + by.pow(2))
                        val multiply = ax * bx + ay * by
                        val arccos = Math.acos(multiply / (a * b.toDouble())) * 180 / Math.PI

                        val x3 = carX + cos(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()
                        val y3 = carY - sin(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()

                        animationHelper.animate(x2, y2, x3, y3, x4, y4)
                            .addListener(object : Animator.AnimatorListener {
                                override fun onAnimationRepeat(animation: Animator?) {}
                                override fun onAnimationCancel(animation: Animator?) {}

                                override fun onAnimationStart(animation: Animator?) {
                                    handleTouches = false
                                }

                                override fun onAnimationEnd(animation: Animator?) {
                                    handleTouches = true
                                }
                            })
                    }
                }
            }
            true
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawColor(ContextCompat.getColor(context, R.color.colorAccent))
            canvas.drawBitmap(car, carMatrix, paint)
        }
        super.onDraw(canvas)
    }

    inner class AnimationHelper(
        private var x: Float,
        private var y: Float,
        private val duration: Long
    ) {
        private val animator = ValueAnimator()
        private val path = Path()
        private val pathMeasure = PathMeasure()
        private val points = FloatArray(2) { 0f }

        init {
            animator.apply {
                duration = this@AnimationHelper.duration
                this.interpolator = AccelerateDecelerateInterpolator()
                setFloatValues(0f, 1f)
                addUpdateListener {
                    carMatrix.reset()
                    pathMeasure.getMatrix(
                        pathMeasure.length * it.animatedFraction,
                        carMatrix,
                        PathMeasure.POSITION_MATRIX_FLAG + PathMeasure.TANGENT_MATRIX_FLAG
                    )
                    pathMeasure.getPosTan(pathMeasure.length * it.animatedFraction, points, null)

                    carX = points[0]
                    carY = points[1]
                    invalidate()
                }
            }
        }

        fun animate(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Animator {
            this.x = carX
            this.y = carY
            path.reset()
            path.moveTo(x, y)
            path.cubicTo(x1, y1, x2, y2, x3, y3)
            pathMeasure.setPath(path, false)
            animator.start()
            return animator
        }

    }

    companion object {

        private const val TRIP_DURATION = 4000L
        private const val POINT_TWO_DISTANCE = 200
        private const val POINT_THREE_DISTANCE = 4

    }

}