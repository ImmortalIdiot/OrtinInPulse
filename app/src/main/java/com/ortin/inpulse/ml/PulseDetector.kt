package com.ortin.inpulse.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.LinkedList
import java.util.Queue
import kotlin.math.sqrt

class PulseDetector(
    private val context: Context,
    private val modelName: String = "model.tflite",
) : Closeable {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false

    private val frameBuffer: Queue<Float> = LinkedList()
    private val maxFrameCount = 300

    private var heartRate = 0f
    private var confidenceScore = 0f

    private var measurementStartTimeMs = 0L
    private var isTimedMeasurement = false
    private val measurementDurationMs = 20000L
    private val handler = Handler(Looper.getMainLooper())
    private var measurementEndRunnable: Runnable? = null

    enum class MeasurementState {
        WAITING,
        MEASURING,
        COMPLETED
    }

    private var measurementState = MeasurementState.WAITING
    private var finalHeartRate = 0f
    private var finalConfidence = 0f

    var onPulseDetected: ((heartRate: Float, confidence: Float) -> Unit)? = null
    private var onMeasurementCompleted: ((finalHeartRate: Float, finalConfidence: Float) -> Unit)? =
        null

    init {
        try {
            copyModelToAccessibleFile()
            initializeModel()
        } catch (e: Exception) { /* do nothing */
        }
    }

    private fun copyModelToAccessibleFile(): Boolean {
        try {
            val assetsList = context.assets.list("")

            if (assetsList?.contains(modelName) == true) {
                val internalFile = File(context.filesDir, modelName)
                if (!internalFile.exists()) {
                    context.assets.open(modelName).use { input ->
                        FileOutputStream(internalFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else { /* do nothing */
                }

                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun initializeModel() {
        val options = Interpreter.Options().apply { numThreads = 4 }

        try {
            try {
                val internalFile = File(context.filesDir, modelName)

                if (internalFile.exists()) {
                    val modelBuffer = loadModelFromFile(internalFile)
                    interpreter = Interpreter(modelBuffer, options)
                    isInitialized = true
                } else {
                    throw IOException("Файл модели не найден во внутреннем хранилище")
                }
            } catch (e: Exception) {
                e.printStackTrace()

                try {
                    val assetsList = context.assets.list("")

                    if (assetsList?.contains(modelName) != true) {
                        throw IOException("Модель '$modelName' не найдена в ресурсах приложения")
                    }

                    val modelBuffer = loadModelFromAssets(modelName)
                    interpreter = Interpreter(modelBuffer, options)
                    isInitialized = true
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    throw IOException("Не удалось загрузить модель: ${e2.message}", e2)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
            throw RuntimeException("Ошибка инициализации модели: ${e.message}", e)
        }
    }

    private fun loadModelFromAssets(assetName: String): MappedByteBuffer {
        try {
            val fileDescriptor = context.assets.openFd(assetName)

            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength

            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            fileDescriptor.close()
            inputStream.close()

            return buffer
        } catch (e: Exception) {
            throw RuntimeException("Error loading model from assets", e)
        }
    }

    private fun loadModelFromFile(file: File): MappedByteBuffer {
        try {
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            inputStream.close()

            return buffer
        } catch (e: Exception) {
            throw RuntimeException("Error loading model from file", e)
        }
    }

    private fun stopMeasurement() {
        if (measurementEndRunnable != null) {
            handler.removeCallbacks(measurementEndRunnable!!)
            measurementEndRunnable = null
        }

        if (measurementState == MeasurementState.MEASURING) {
            completeMeasurement()
        }
    }

    private fun completeMeasurement() {
        if (measurementState == MeasurementState.MEASURING) {
            measurementState = MeasurementState.COMPLETED

            if (frameBuffer.size >= minFramesNeeded) {
                val signalData = frameBuffer.toFloatArray()
                calculatePulseFromSignal(signalData)
                finalHeartRate = heartRate
                finalConfidence = confidenceScore
            } else {
                finalHeartRate = 0f
                finalConfidence = 0f
            }

            onMeasurementCompleted?.invoke(finalHeartRate, finalConfidence)
        }
    }

    fun processFrame(bitmap: Bitmap) {
        if (!isInitialized) {
            return
        }

        try {
            if (measurementState == MeasurementState.COMPLETED) {
                return
            }

            val redChannelMean = extractRedChannelMean(bitmap)
            addFrameToBuffer(redChannelMean)

            if (frameBuffer.size >= minFramesNeeded) {
                processPulseSignal()
            }

            if (isTimedMeasurement && measurementState == MeasurementState.MEASURING) {
                val elapsedTime = System.currentTimeMillis() - measurementStartTimeMs
                if (elapsedTime >= measurementDurationMs) {
                    completeMeasurement()
                }
            }
        } catch (e: Exception) { /* do nothing */
        }
    }

    private fun extractRedChannelMean(bitmap: Bitmap): Float {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val sampleRadius = minOf(bitmap.width, bitmap.height) / 4

        var sumRed = 0f
        var pixelCount = 0

        for (x in centerX - sampleRadius until centerX + sampleRadius) {
            for (y in centerY - sampleRadius until centerY + sampleRadius) {
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = (pixel shr 16) and 0xFF
                    sumRed += red
                    pixelCount++
                }
            }
        }

        return if (pixelCount > 0) sumRed / pixelCount else 0f
    }

    private fun addFrameToBuffer(value: Float) {
        frameBuffer.add(value)

        while (frameBuffer.size > maxFrameCount) {
            frameBuffer.remove()
        }
    }

    private val minFramesNeeded = 100

    private fun processPulseSignal() {
        if (interpreter == null || frameBuffer.size < minFramesNeeded) return

        try {
            val signalData = frameBuffer.toFloatArray()
            val filtered = bandpassFilter(signalData)
            calculatePulseFromSignal(filtered)

            onPulseDetected?.invoke(heartRate, confidenceScore)

        } catch (e: Exception) { /* do nothing */
        }
    }

    private fun calculatePulseFromSignal(signal: FloatArray) {
        val normalized = normalizeSignal(signal)

        val variance = calculateVariance(normalized)
        val threshold = sqrt(variance) * 1.0f

        val peaks = mutableListOf<Int>()

        for (i in 2 until normalized.size - 2) {
            if (normalized[i] > normalized[i - 1] && normalized[i] > normalized[i + 1] &&
                normalized[i] > normalized[i - 2] && normalized[i] > normalized[i + 2] &&
                normalized[i] > threshold
            ) {
                peaks.add(i)
            }
        }

        if (peaks.size > 2) {
            val intervals = mutableListOf<Int>()
            for (i in 1 until peaks.size) {
                val interval = peaks[i] - peaks[i - 1]
                if (interval in 10..60) {
                    intervals.add(interval)
                }
            }

            if (intervals.isNotEmpty()) {
                val avgFrameInterval = intervals.average()
                val beatsPerSecond = 30.0 / avgFrameInterval
                heartRate = (beatsPerSecond * 60).toFloat()

                confidenceScore = (intervals.size / 8f)
                    .coerceAtMost(1.0f) * (1.0f - calculateVariationCoefficient(
                    intervals.map { it.toFloat() }
                        .toFloatArray())
                        .coerceIn(0f, 0.5f) / 0.5f)
            } else {
                heartRate = 70f
                confidenceScore = 0.2f
            }
        } else {
            heartRate = 70f
            confidenceScore = 0.1f
        }

        heartRate = heartRate.coerceIn(50f, 180f)
    }

    private fun bandpassFilter(signal: FloatArray): FloatArray {
        val filtered = FloatArray(signal.size)

        val windowSize = 5
        for (i in signal.indices) {
            var sum = 0f
            var count = 0
            for (j in maxOf(0, i - windowSize) until minOf(signal.size, i + windowSize + 1)) {
                sum += signal[j]
                count++
            }
            filtered[i] = sum / count
        }

        val dcFiltered = FloatArray(filtered.size)
        val alpha = 0.95f

        var y1 = 0f
        var x1 = filtered[0]

        for (i in filtered.indices) {
            y1 = alpha * (y1 + filtered[i] - x1)
            x1 = filtered[i]
            dcFiltered[i] = y1
        }

        return dcFiltered
    }

    private fun normalizeSignal(signal: FloatArray): FloatArray {
        val normalized = FloatArray(signal.size)

        var sum = 0f
        for (value in signal) {
            sum += value
        }
        val mean = sum / signal.size

        var sumOfSquares = 0f
        for (value in signal) {
            sumOfSquares += (value - mean) * (value - mean)
        }
        val stdDev = sqrt(sumOfSquares / signal.size)

        if (stdDev > 0) {
            for (i in signal.indices) {
                normalized[i] = (signal[i] - mean) / stdDev
            }
        } else {
            System.arraycopy(signal, 0, normalized, 0, signal.size)
        }

        return normalized
    }

    private fun calculateVariance(signal: FloatArray): Float {
        var sum = 0f
        for (value in signal) {
            sum += value
        }
        val mean = sum / signal.size

        var sumOfSquares = 0f
        for (value in signal) {
            sumOfSquares += (value - mean) * (value - mean)
        }

        return sumOfSquares / signal.size
    }

    private fun calculateVariationCoefficient(values: FloatArray): Float {
        var sum = 0f

        for (value in values) {
            sum += value
        }

        val mean = sum / values.size
        var sumOfSquares = 0f

        for (value in values) {
            sumOfSquares += (value - mean) * (value - mean)
        }

        val stdDev = sqrt(sumOfSquares / values.size)

        return if (mean != 0f) stdDev / mean else 0f
    }

    private fun Queue<Float>.toFloatArray(): FloatArray {
        val result = FloatArray(this.size)
        var index = 0

        for (value in this) {
            result[index++] = value
        }

        return result
    }

    fun isModelInitialized(): Boolean {
        return isInitialized && interpreter != null
    }

    override fun close() {
        stopMeasurement()
        interpreter?.close()
        gpuDelegate?.close()
        isInitialized = false
    }
}
