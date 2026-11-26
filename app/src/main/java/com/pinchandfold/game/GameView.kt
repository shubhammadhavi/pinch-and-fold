package com.pinchandfold.game

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * SurfaceView hosting the 60fps game loop and multi-touch input.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val audio = SoundAndHaptics(context)
    private val engine = GameEngine(audio)
    private val renderer = GameRenderer()

    @Volatile
    private var running = false
    private var gameThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        gameThread = Thread(this, "PinchAndFoldLoop").apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(engine) {
            engine.resize(width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            gameThread?.join(500)
        } catch (_: InterruptedException) {
        }
        gameThread = null
    }

    override fun run() {
        var lastTimeNs = System.nanoTime()
        while (running) {
            val nowNs = System.nanoTime()
            val dt = (nowNs - lastTimeNs) / 1_000_000_000f
            lastTimeNs = nowNs

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(engine) {
                        engine.update(dt)
                        renderer.render(canvas, engine)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            // Cap around ~60fps without busy-spinning
            val frameMs = (System.nanoTime() - nowNs) / 1_000_000L
            if (frameMs < 16L) {
                try {
                    Thread.sleep(16L - frameMs)
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        synchronized(engine) {
            return engine.handleTouchEvent(event)
        }
    }

    fun release() {
        running = false
        audio.release()
    }
}
