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
import android.util.Log

val nodes : Int = 1
val clocks : Int = 3
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#512DA8")
val backColor : Int = Color.parseColor("#BDBDBD")
val rotDeg : Float = 360f
val clockLengthFactor : Float = 5.5f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap
fun Int.div2() : Int = this / 2
fun Int.gapForI(n : Int, gap : Float) : Float = (this - n.div2()) * gap
fun Int.skipMid(n : Int) : Int =  this  - (this + 1) / (n.div2() + 1)
fun Float.divideScaleSkipMid(i : Int, n : Int) : Float =  this.divideScale(i.skipMid(n), n - 1)

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
    val xGap : Float = w / (clocks + 1)
    save()
    translate(w/2, gap * (i + 1))
    for (j in 0..(clocks - 1)) {
        Log.d("${j} skip mid", "${j.skipMid(clocks)}")
        save()
        translate(j.gapForI(clocks, xGap * sc1.divideScaleSkipMid(j, clocks)),0f)
        drawCircle(0f, 0f, xGap / 2, paint)
        drawHands(xGap / clockLengthFactor, (rotDeg / clocks) * (j + 1) * sc2.divideScale(j, clocks), paint)
        restore()
    }
    restore()
}
class MultiClockView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean  {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, clocks - 1, clocks)
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

    class MCNode(var i : Int, val state : State = State()) {

        private var next : MCNode? = null
        private var prev : MCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = MCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawMCNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : MCNode {
            var curr : MCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class MultiClock(var i : Int) {

        private val root : MCNode = MCNode(0)
        private var curr : MCNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : MultiClockView) {

        private val animator : Animator = Animator(view)
        private val mc : MultiClock = MultiClock(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            mc.draw(canvas, paint)
            animator.animate {
                mc.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            mc.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : MultiClockView {
            val view : MultiClockView = MultiClockView(activity)
            activity.setContentView(view)
            return view
        }
    }
}