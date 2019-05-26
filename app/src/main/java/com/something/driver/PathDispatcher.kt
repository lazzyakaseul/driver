package com.something.driver

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PathDispatcher {

    var carX = 0f
    var carY = 0f

    fun countPoints(px2: Float, py2: Float, angle: Float): BezierCurve {
        val x2 = carX + cos(angle) * POINT_TWO_DISTANCE
        val y2 = carY - sin(angle) * POINT_TWO_DISTANCE
        val x4 = px2
        val y4 = py2
        val angle2 = -Math.atan2(y4.toDouble() - carY, x4.toDouble() - carX)
        val angleInDegrees = angle * 180 / Math.PI
        val angleTwoInDegrees = angle2 * 180 / Math.PI
        val angleDegRound = if (angleInDegrees < 0) angleInDegrees + 360 else angleInDegrees
        val angleDegTwoRound = if (angleTwoInDegrees < 0) angleTwoInDegrees + 360 else angleTwoInDegrees

        val diff = angleDegRound - angleDegTwoRound
        val resAngle = when {
            Math.abs(diff) <= 180 && diff > 0 -> angleInDegrees - 90
            Math.abs(diff) <= 180 && diff <= 0 -> angleInDegrees + 90
            Math.abs(diff) > 180 && diff > 0 -> angleInDegrees + 90
            Math.abs(diff) > 180 && diff <= 0 -> angleInDegrees - 90
            else -> 0.0
        }.run {
            when {
                Math.abs(this) > 180 && this > 0 -> this - 360
                Math.abs(this) > 180 && this < 0 -> this + 360
                else -> this
            }
        }.run { Math.toRadians(this) }

        val ax = x2 - carX
        val ay = y2 - carY
        val bx = x4 - carX
        val by = y4 - carY
        val a = sqrt(ax.pow(2) + ay.pow(2))
        val b = sqrt(bx.pow(2) + by.pow(2))
        val multiply = ax * bx + ay * by
        val arccos = Math.acos(multiply / (a * b).toDouble()) * 180 / Math.PI

        val x3 = carX + cos(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()
        val y3 = carY - sin(resAngle).toFloat() * POINT_THREE_DISTANCE * arccos.toFloat()

        return BezierCurve(carX, carY, x2, y2, x3, y3, x4, y4)
    }

    companion object {

        private const val POINT_TWO_DISTANCE = 200
        private const val POINT_THREE_DISTANCE = 5

    }

}