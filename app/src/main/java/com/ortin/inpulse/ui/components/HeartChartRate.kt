package com.ortin.inpulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.ortin.inpulse.data.MeasurementResult
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HeartRateChart(
    measurements: List<MeasurementResult>,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val dayFormatter = DateTimeFormatter.ofPattern("dd.MM")

    val minHeartRate = measurements.minOfOrNull { it.heartRate }?.toFloat() ?: 0f
    val maxHeartRate = measurements.maxOfOrNull { it.heartRate }?.toFloat() ?: 100f
    val yRange = (maxHeartRate - minHeartRate).coerceAtLeast(10f) * 1.1f

    val adjustedMinHeartRate = (minHeartRate - yRange * 0.05f).coerceAtLeast(0f)
    val adjustedMaxHeartRate = maxHeartRate + yRange * 0.05f

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryColorInt = android.graphics.Color.valueOf(
        primaryColor.red, primaryColor.green, primaryColor.blue
    ).toArgb()
    val primaryColorWithAlpha = primaryColor.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val chartHeight = height * 0.7f
        val topPadding = height * 0.05f

        val itemWidth = if (measurements.size > 1) {
            width / (measurements.size - 1)
        } else {
            width
        }

        val gridLineCount = 5
        val gridStepSize = chartHeight / gridLineCount
        val valueStep = (adjustedMaxHeartRate - adjustedMinHeartRate) / gridLineCount

        for (i in 0..gridLineCount) {
            val y = topPadding + chartHeight - (i * gridStepSize)

            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )

            val value = adjustedMinHeartRate + (i * valueStep)
            drawContext.canvas.nativeCanvas.drawText(
                "${value.toInt()}",
                8f,
                y - 8f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )
        }

        if (measurements.size == 1) {
            val heartRate = measurements[0].heartRate.toFloat()
            val x = width / 2
            val y = topPadding + chartHeight -
                    ((heartRate - adjustedMinHeartRate) / (adjustedMaxHeartRate - adjustedMinHeartRate) * chartHeight)

            drawCircle(
                color = primaryColor,
                radius = 8f,
                center = Offset(x, y)
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${heartRate.toInt()}",
                x - 15f,
                y - 12f,
                android.graphics.Paint().apply {
                    color = primaryColorInt
                    textSize = 30f
                }
            )

            val day = dayFormatter.format(measurements[0].timestamp)
            drawContext.canvas.nativeCanvas.drawText(
                day,
                x - 25f,
                height - 50f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                    isFakeBoldText = true
                }
            )

            val time = timeFormatter.format(measurements[0].timestamp)
            drawContext.canvas.nativeCanvas.drawText(
                time,
                x - 25f,
                height - 15f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )

            return@Canvas
        }

        if (measurements.size > 1) {
            val linePath = Path()
            val fillPath = Path()
            val firstHeartRate = measurements[0].heartRate.toFloat()
            val startX = 0f
            val startY = topPadding + chartHeight -
                    ((firstHeartRate - adjustedMinHeartRate) / (adjustedMaxHeartRate - adjustedMinHeartRate) * chartHeight)

            linePath.moveTo(startX, startY)
            fillPath.moveTo(startX, topPadding + chartHeight)
            fillPath.lineTo(startX, startY)

            val pointCoordinates = mutableListOf<Pair<Float, Float>>()
            pointCoordinates.add(Pair(startX, startY))

            for (i in 1 until measurements.size) {
                val heartRate = measurements[i].heartRate.toFloat()
                val x = i * itemWidth
                val y = topPadding + chartHeight -
                        ((heartRate - adjustedMinHeartRate) / (adjustedMaxHeartRate - adjustedMinHeartRate) * chartHeight)

                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)

                pointCoordinates.add(Pair(x, y))
            }

            fillPath.lineTo(width, topPadding + chartHeight)
            fillPath.lineTo(startX, topPadding + chartHeight)

            drawPath(
                path = fillPath,
                color = primaryColorWithAlpha
            )

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round
                )
            )

            for (i in pointCoordinates.indices) {
                val (x, y) = pointCoordinates[i]
                val heartRate = measurements[i].heartRate

                drawCircle(
                    color = primaryColor,
                    radius = 6f,
                    center = Offset(x, y)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "$heartRate",
                    x - 15f,
                    y - 12f,
                    android.graphics.Paint().apply {
                        color = primaryColorInt
                        textSize = 30f
                    }
                )

                val shouldShowLabel = when (measurements.size) {
                    in 2..4 -> true
                    in 5..7 -> i % 2 == 0 || i == pointCoordinates.size - 1
                    else -> i % 3 == 0 || i == pointCoordinates.size - 1
                }

                if (shouldShowLabel) {
                    val day = dayFormatter.format(measurements[i].timestamp)
                    drawContext.canvas.nativeCanvas.drawText(
                        day,
                        x - 25f,
                        height - 50f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 30f
                            isFakeBoldText = true
                        }
                    )

                    val time = timeFormatter.format(measurements[i].timestamp)
                    drawContext.canvas.nativeCanvas.drawText(
                        time,
                        x - 25f,
                        height - 15f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 30f
                        }
                    )
                }
            }
        }
    }
}
