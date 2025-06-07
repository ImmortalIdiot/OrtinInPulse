package com.ortin.inpulse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ortin.inpulse.MainViewModel
import com.ortin.inpulse.R
import com.ortin.inpulse.ui.components.HeartRateChart
import com.ortin.inpulse.ui.components.VerticalScreenSpacer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsScreen(navigationVM: MainViewModel) {
    val allMeasurements by navigationVM.measurements.collectAsState()
    val lastMeasurements = remember(allMeasurements) { allMeasurements.take(5) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.pulse_graph)) },
                navigationIcon = {
                    IconButton(onClick = { navigationVM.changeScreen(MainViewModel.Screen.MAIN) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.last_measures),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (lastMeasurements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data),
                        color = Color.Gray
                    )
                }
            } else {
                HeartRateChart(
                    measurements = lastMeasurements,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                )
                
                VerticalScreenSpacer(32.dp)
                
                Text(
                    text = stringResource(R.string.info),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val minHeartRate = lastMeasurements.minOfOrNull { it.heartRate } ?: 0
                val maxHeartRate = lastMeasurements.maxOfOrNull { it.heartRate } ?: 0
                val avgHeartRate = lastMeasurements.takeIf { it.isNotEmpty() }
                    ?.map { it.heartRate }
                    ?.average()
                    ?.toInt() ?: 0
                
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.min_pulse),
                        color = Color.Gray,
                        modifier = Modifier.width(180.dp)
                    )
                    Text(
                        text = minHeartRate.toString() + " " + stringResource(R.string.pulse_scale),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.max_pulse),
                        color = Color.Gray,
                        modifier = Modifier.width(180.dp)
                    )
                    Text(
                        text = maxHeartRate.toString() + " " + stringResource(R.string.pulse_scale),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.avg_pulse),
                        color = Color.Gray,
                        modifier = Modifier.width(180.dp)
                    )
                    Text(
                        text = avgHeartRate.toString() + " " + stringResource(R.string.pulse_scale),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
