package com.ortin.inpulse.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ortin.inpulse.MainViewModel
import com.ortin.inpulse.data.MeasurementResult
import com.ortin.inpulse.ui.components.VerticalScreenSpacer
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navigationVM: MainViewModel) {
    val measurements by navigationVM.measurements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История измерений") },
                navigationIcon = {
                    IconButton(onClick = { navigationVM.changeScreen(MainViewModel.Screen.MAIN) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (measurements.isNotEmpty()) {
                        IconButton(onClick = { navigationVM.clearAllMeasurements() }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Очистить все"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (measurements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет сохраненных измерений",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(measurements) { measurement ->
                        MeasurementItem(
                            measurement = measurement,
                            onDelete = { navigationVM.deleteMeasurement(measurement.id) }
                        )
                        VerticalScreenSpacer(8.dp)
                    }
                    item { VerticalScreenSpacer() }
                }
            }
        }
    }
}

@Composable
fun MeasurementItem(
    measurement: MeasurementResult,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${measurement.heartRate}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "уд/мин",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = Color.Gray
                    )
                }
            }

            VerticalScreenSpacer(4.dp)

            Text(
                text = "Измерено: ${formatter.format(measurement.timestamp)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
