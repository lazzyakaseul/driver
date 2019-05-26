package com.something.driver

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin


class AnimationHelper(duration: Long, doOnStart: () -> Unit, doOnFinish: () -> Unit, doOnUpdate: () -> Unit) {
    private val positionPoint = FloatArray(2) { 0f }
    private val tan = FloatArray(2) { 0f }
    private val animator = ValueAnimator()
    private val path = Path()
    private val pathMeasure = PathMeasure()

    init {
        animator.apply {
            this.duration = duration
            this.interpolator = AccelerateDecelerateInterpolator()
            setFloatValues(0f, 1f)
            addUpdateListener {
                pathMeasure.getPosTan(pathMeasure.length * it.animatedFraction, positionPoint, tan)
                doOnUpdate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {
                    doOnStart()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    doOnFinish?.invoke()
                }
            })
        }
    }

    fun getPosition() = PointF(positionPoint[0], positionPoint[1])

    fun getAngleInDegrees() = (Math.atan2(tan[1].toDouble(), tan[0].toDouble()) * 180.0 / Math.PI).toFloat()

    fun setStartPosition(startX: Float, startY: Float, startAngleInRadians: Float) {
        positionPoint[0] = startX
        positionPoint[1] = startY
        tan[0] = cos(startAngleInRadians)
        tan[1] = sin(startAngleInRadians)
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

    fun receiveAngle(): Double {
        return -Math.atan2(tan[1].toDouble(), tan[0].toDouble())
    }

}