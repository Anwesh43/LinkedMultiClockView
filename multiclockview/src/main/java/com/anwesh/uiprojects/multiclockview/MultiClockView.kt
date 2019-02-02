package com.anwesh.uiprojects.multiclockview

/**
 * Created by anweshmishra on 02/02/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

val nodes : Int = 5
val clocks : Int = 3
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#512DA8")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Paint.setStyle(w : Float, h : Float) {
    strokeWidth = Math.min(w, h) / strokeFactor
    strokeCap = Paint.Cap.ROUND
    color = foreColor
    style = Paint.Style.STROKE
}

fun Canvas.drawHand(size : Float, deg : Float, paint : Paint) {
    save()
    rotate(deg)
    drawLine(0f, 0f, 0f, -size, paint)
    restore()
}

fun Canvas.drawHands(size : Float, deg : Float, paint : Paint) {
    val sw : Float = paint.strokeWidth
    val hDegFactor : Int = 12
    val mSizeFactor : Int = 2
    paint.strokeWidth = 2 * sw
    drawHand(size, deg / hDegFactor, paint)
    paint.strokeWidth = sw
    drawHand(size * mSizeFactor, deg, paint)
}

fun Canvas.drawMCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.setStyle(w, h)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    save()
    translate(w/2, gap * (i + 1))
    for (j in 0..(clocks - 1)) {
        save()
        translate(2 * size *  (j - 1) * sc1, 0f)
        drawCircle(0f, 0f, size, paint)
        drawHands(size / 3, 360f * sc2.divideScale(j, clocks), paint)
        restore()
    }
    restore()
}
class MultiClockView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean  {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, 1, clocks)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }
}