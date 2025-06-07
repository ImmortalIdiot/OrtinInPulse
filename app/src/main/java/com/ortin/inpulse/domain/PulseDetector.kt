package com.ortin.inpulse.domain

import android.graphics.Bitmap
import com.ortin.inpulse.ml.PulseDetector

interface PulseDetectorInterface : AutoCloseable {
    fun processFrame(bitmap: Bitmap)
    var onPulseDetected: ((heartRate: Float, confidence: Float) -> Unit)?
}

class TFLitePulseDetectorAdapter(private val detector: PulseDetector) :
    PulseDetectorInterface {
    override fun processFrame(bitmap: Bitmap) = detector.processFrame(bitmap)

    override var onPulseDetected: ((heartRate: Float, confidence: Float) -> Unit)?
        get() = detector.onPulseDetected
        set(value) {
            detector.onPulseDetected = value
        }

    override fun close() = detector.close()
}
