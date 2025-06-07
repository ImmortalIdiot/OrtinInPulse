package com.ortin.inpulse.ui.screen

import android.Manifest
import androidx.camera.core.Camera
import androidx.camera.core.TorchState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ortin.inpulse.MainViewModel
import com.ortin.inpulse.R
import com.ortin.inpulse.domain.PulseDetectorInterface
import com.ortin.inpulse.domain.TFLitePulseDetectorAdapter
import com.ortin.inpulse.ml.PulseDetector
import com.ortin.inpulse.ui.components.CameraPreview
import com.ortin.inpulse.ui.components.HeartCanvas
import com.ortin.inpulse.ui.components.SizeSpacer
import com.ortin.inpulse.ui.components.VerticalScreenSpacer
import com.ortin.inpulse.ui.components.createHeartPath
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HeartScreen(navigationVM: MainViewModel) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showCamera by remember { mutableStateOf(false) }
    var isMeasuring by remember { mutableStateOf(false) }
    var pulseDetector by remember { mutableStateOf<PulseDetectorInterface?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var isInitializing by remember { mutableStateOf(true) }

    var timeLeft by remember { mutableIntStateOf(0) }
    var showLastPulse by remember { mutableStateOf(false) }

    val imageAnalysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val currentIntervalRates = remember { mutableListOf<Float>() }
    val currentIntervalConfidences = remember { mutableListOf<Float>() }

    val measurementDurationSeconds = 30
    val saveInterval = 3

    val avgPulse by navigationVM.currentAverageHeartRate.collectAsState()

    LaunchedEffect(showLastPulse) {
        if (showLastPulse) {
            delay(5000)
            showLastPulse = false
        }
    }

    LaunchedEffect(Unit) {
        isInitializing = true
        try {
            val detector = PulseDetector(context)
            if (!detector.isModelInitialized()) {
                modelError = "Ошибка: Не удалось загрузить модель обнаружения пульса"
                pulseDetector = null
            } else {
                pulseDetector = TFLitePulseDetectorAdapter(detector)
                modelError = null
            }
        } catch (e: Exception) {
            modelError = "Ошибка: ${e.message}"
            pulseDetector = null
        } finally {
            isInitializing = false
        }
    }

    LaunchedEffect(pulseDetector) {
        pulseDetector?.onPulseDetected = { rate, confidence ->
            if (isMeasuring) {
                currentIntervalRates.add(rate)
                currentIntervalConfidences.add(confidence)
            }
        }
    }

    LaunchedEffect(isMeasuring) {
        if (isMeasuring && pulseDetector != null) {
            navigationVM.clearTemporaryMeasurements()
            currentIntervalRates.clear()
            currentIntervalConfidences.clear()
            timeLeft = measurementDurationSeconds
            showLastPulse = false

            for (i in measurementDurationSeconds downTo 0) {
                timeLeft = i
                delay(1000)
                if (!isMeasuring) break

                if (i % saveInterval == 0 && currentIntervalRates.isNotEmpty()) {
                    val avgRate = currentIntervalRates.average().toFloat()
                    val avgConf = currentIntervalConfidences.average().toFloat()
                    navigationVM.addIntermediateMeasurement(avgRate.toInt(), avgConf)
                    currentIntervalRates.clear()
                    currentIntervalConfidences.clear()
                }

                if (i == 0) {
                    navigationVM.finalizeMeasurement()
                    delay(100)
                    showLastPulse = navigationVM.currentAverageHeartRate.value > 0
                    isMeasuring = false
                }
            }
        }
    }

    LaunchedEffect(showCamera, camera) {
        if (showCamera && camera?.cameraInfo?.hasFlashUnit() == true) {
            try {
                camera?.cameraControl?.enableTorch(true)
            } catch (_: Exception) {
                /* do nothing */
            }
        } else {
            try {
                if (camera?.cameraInfo?.torchState?.value == TorchState.ON) {
                    camera?.cameraControl?.enableTorch(false)
                }
            } catch (_: Exception) {
                /* do nothing */
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (camera?.cameraInfo?.torchState?.value == TorchState.ON) {
                    camera?.cameraControl?.enableTorch(false)
                }
            } catch (_: Exception) {
                /* do nothing */
            }

            pulseDetector?.close()
            imageAnalysisExecutor.shutdown()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (modelError != null) {
                Text(
                    text = modelError!!,
                    color = Color.Red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (isInitializing) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text(
                    text = stringResource(R.string.initialization),
                    fontSize = 16.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                val heartPath = remember { Path() }

                if (showCamera && cameraPermissionState.status.isGranted) {
                    key(showCamera) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val centerX = size.width / 2
                                    val centerY = size.height / 2

                                    heartPath.reset()
                                    createHeartPath(path = heartPath, centerX = centerX, centerY = centerY, size = size.width * 0.4f)

                                    onDrawWithContent {
                                        clipPath(heartPath) {
                                            this@onDrawWithContent.drawContent()
                                        }
                                    }
                                }
                        ) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                onCameraReady = { camera = it },
                                imageAnalysisExecutor = imageAnalysisExecutor,
                                analyzer = { if (isMeasuring) pulseDetector?.processFrame(it) }
                            )
                        }
                    }
                }

                HeartCanvas(
                    modifier = Modifier.fillMaxSize(),
                    showFill = !showCamera
                )
            }

            VerticalScreenSpacer()

            if (showCamera && isMeasuring) {
                val currentAvgPulse =
                    currentIntervalRates.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.pulse) + " $currentAvgPulse " + stringResource(R.string.pulse_scale),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    VerticalScreenSpacer(8.dp)
                    Text(
                        text = stringResource(R.string.time_to_end) + " $timeLeft " + stringResource(R.string.seconds),
                        fontSize = 18.sp
                    )
                }
            }

            if (showCamera && !isMeasuring && showLastPulse && avgPulse > 0) {
                Text(
                    text = stringResource(R.string.pulse) + " $avgPulse " + stringResource(R.string.pulse_scale),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            VerticalScreenSpacer()

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            if (showCamera) {
                                try {
                                    camera?.cameraControl?.enableTorch(false)
                                } catch (_: Exception) {
                                }
                                showCamera = false
                                isMeasuring = false
                                showLastPulse = false
                                camera = null
                                navigationVM.clearTemporaryMeasurements()
                            } else {
                                showCamera = true
                            }
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    enabled = !isInitializing && pulseDetector != null
                ) {
                    Text(
                        text = if (showCamera) {
                            stringResource(R.string.stop)
                        } else {
                            stringResource(R.string.measure)
                        }
                    )
                }

                if (showCamera) {
                    SizeSpacer()
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { isMeasuring = !isMeasuring },
                        enabled = pulseDetector != null && !isInitializing
                    ) {
                        Text(
                            text = if (isMeasuring) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.start)
                            }
                        )
                    }
                }
            }

            VerticalScreenSpacer()

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { navigationVM.changeScreen(MainViewModel.Screen.HISTORY) }
                ) {
                    Text(text = stringResource(R.string.story))
                }

                SizeSpacer()

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { navigationVM.changeScreen(MainViewModel.Screen.GRAPHICS) }
                ) {
                    Text(text = stringResource(R.string.graph))
                }
            }
        }
    }
}
