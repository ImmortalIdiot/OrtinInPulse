package com.ortin.inpulse.data

import java.time.LocalDateTime
import java.util.UUID

data class MeasurementResult(
    val id: String = UUID.randomUUID().toString(),
    val heartRate: Int,
    val confidence: Float,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
