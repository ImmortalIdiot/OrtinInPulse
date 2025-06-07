package com.ortin.inpulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun HeartCanvas(modifier: Modifier = Modifier, showFill: Boolean = true) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        val drawPath = Path()
        createHeartPath(drawPath, centerX, centerY, size.width * 0.4f)

        drawPath(
            path = drawPath,
            color = Color.Red,
            style = Stroke(width = 5f)
        )

        if (showFill) {
            drawPath(
                path = drawPath,
                color = Color.Red.copy(alpha = 0.2f)
            )
        }
    }
}

fun createHeartPath(path: Path, centerX: Float, centerY: Float, size: Float) {
    path.moveTo(x = centerX, y = centerY + size)
    val controlPointDistance = size * 1.5f

    path.cubicTo(
        x1 = centerX - controlPointDistance,
        y1 = centerY,
        x2 = centerX - size,
        y2 = centerY - size,
        x3 = centerX,
        y3 = centerY - size / 2
    )

    path.cubicTo(
        x1 = centerX + size,
        y1 = centerY - size,
        x2 = centerX + controlPointDistance,
        y2 = centerY,
        x3 = centerX,
        y3 = centerY + size
    )
}
