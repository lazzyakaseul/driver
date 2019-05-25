package com.something.driver

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.view.animation.AccelerateDecelerateInterpolator


class AnimationHelper(duration: Long, doOnStart: () -> Unit, doOnUpdate: () -> Unit) {
    val endPoint = FloatArray(2) { 0f }
    val carMatrix = Matrix()
    private var doOnFinish: (() -> Unit)? = null
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

    fun setStartPosition(startX: Float, startY: Float, startAngle: Float) {
        carMatrix.reset()
        carMatrix.setTranslate(startX, startY)
        carMatrix.preRotate(startAngle)
    }

    fun setOnFinishListener(doOnFinish: () -> Unit) {
        this.doOnFinish = doOnFinish
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
        val values = FloatArray(9)
        carMatrix.getValues(values)
        return Math.atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble())
    }

}