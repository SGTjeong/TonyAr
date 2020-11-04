package com.example.tonyar.ARCore

import android.util.Log
import android.view.Choreographer
import com.google.ar.core.Frame
import java.util.concurrent.TimeUnit

class TonyFrameCallback(
    private val arCore : TonyArCore,
    private val doFrame : (frame : Frame) -> Unit
) : Choreographer.FrameCallback {
    companion object {
        private const val maxFramesPerSecond: Long = 60
    }

    private val choreographer: Choreographer = Choreographer.getInstance()
    private var lastTick: Long = 0

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)

        val nanoTime = System.nanoTime()
        val tick = nanoTime / (TimeUnit.SECONDS.toNanos(1) / maxFramesPerSecond)

        lastTick = tick

        if (// only render if we have an ar frame
            arCore.timeStamp != 0L &&
            arCore.filament.uiHelper.isReadyToRender &&
            // This means you are sending frames too quickly to the GPU
            arCore.filament.renderer.beginFrame(arCore.filament.swapChain!!, frameTimeNanos)
        ) {
            arCore.filament.timeStamp= arCore.timeStamp
            arCore.filament.renderer.render(arCore.filament.view)
            arCore.filament.renderer.endFrame()
        }

        val frame = arCore.session.update()

        // During startup the camera system may not produce actual images immediately. In
        // this common case, a frame with timestamp = 0 will be returned.
        if (frame.timestamp != 0L &&
            frame.timestamp != arCore.timeStamp
        ) {
            arCore.timeStamp = frame.timestamp
            arCore.update(frame, arCore.filament)
            doFrame(frame)
        }
    }

    fun start() {
        choreographer.postFrameCallback(this)
    }

    fun stop() {
        choreographer.removeFrameCallback(this)
    }
}