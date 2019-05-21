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
    private val animationHelper = AnimationHelper(TRIP_DURATION, { handleTouches = false }, { handleTouches = true })
    private val carMatrix = Matrix()
    private var roadColor: Int? = null
    private var handleTouches = true

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CarTrackView, 0, 0)
            typedArray.getResourceId(R.styleable.CarTrackView_roadColor, 0)
                .apply {
                    roadColor = ContextCompat.getColor(context, this)
                }

            typedArray.recycle()
        }

        setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    if (handleTouches) {
                        carX = animationHelper.endPoint[0]
                        carY = animationHelper.endPoint[1]
                        val values = FloatArray(9)
                        carMatrix.getValues(values)
                        val angle = Math.atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble())
                        countPoints(carX, carY, event.x, event.y, angle.toFloat()).apply {
                            animationHelper.animate(this)
                        }
                    }
                }
            }
            true
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            roadColor?.apply {
                drawColor(this)
            } ?: ContextCompat.getColor(context, R.color.colorAccent)
            canvas.drawBitmap(car, carMatrix, paint)
        }
        super.onDraw(canvas)
    }

    private fun countPoints(px1: Float, py1: Float, px2: Float, py2: Float, angle: Float): BezierCurve {

        val x2 = px1 + cos(angle) * POINT_TWO_DISTANCE
        val y2 = py1 - sin(angle) * POINT_TWO_DISTANCE
        val x4 = px2
        val y4 = py2
        val angle2 = -Math.atan2(y4.toDouble() - py1, x4.toDouble() - px1)
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

        val ax = x2 - px1
        val ay = y2 - py1
        val bx = x4 - px1
        val by = y4 - py1
        val a = sqrt(ax.pow(2) + ay.pow(2))
        val b = sqrt(bx.pow(2) + by.pow(2))
        val multiply = ax * bx + ay * by
        val arccos = Math.acos(multiply / (a * b).toDouble()) * 180 / Math.PI

        val x3 = px1 + cos(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()
        val y3 = py1 - sin(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()

        return BezierCurve(px1, py1, x2, y2, x3, y3, x4, y4)
    }

    private inner class AnimationHelper(duration: Long, doOnStart: () -> Unit, doOnFinish: () -> Unit) {
        val endPoint = FloatArray(2) { 0f }
        private val animator = ValueAnimator()
        private val path = Path()
        private val pathMeasure = PathMeasure()

        init {
            animator.apply {
                this.duration = duration
                this.interpolator = AccelerateDecelerateInterpolator()
                setFloatValues(0f, 1f)
                addUpdateListener {
                    carMatrix.reset()
                    pathMeasure.getMatrix(
                        pathMeasure.length * it.animatedFraction,
                        carMatrix,
                        PathMeasure.POSITION_MATRIX_FLAG + PathMeasure.TANGENT_MATRIX_FLAG
                    )
                    pathMeasure.getPosTan(pathMeasure.length * it.animatedFraction, endPoint, null)

                    invalidate()
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {
                        doOnStart()
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        doOnFinish()
                    }
                })
            }
        }

        fun animate(curve: BezierCurve) {
            path.reset()
            curve.apply {
                path.moveTo(x1, y1)
                path.cubicTo(x2, y2, x3, y3, x4, y4)
            }
            pathMeasure.setPath(path, false)
            animator.start()
        }

    }

    private data class BezierCurve(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x3: Float,
        val y3: Float,
        val x4: Float,
        val y4: Float
    )

    private companion object {

        private const val TRIP_DURATION = 4000L
        private const val POINT_TWO_DISTANCE = 200
        private const val POINT_THREE_DISTANCE = 4

    }

}