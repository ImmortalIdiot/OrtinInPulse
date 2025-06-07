package com.ortin.inpulse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ortin.inpulse.data.MeasurementRepository
import com.ortin.inpulse.data.MeasurementResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class Screen {
        MAIN,
        HISTORY,
        GRAPHICS
    }

    private val repository = MeasurementRepository(application)

    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    val currentScreen = _currentScreen.asStateFlow()

    private val _measurements = MutableStateFlow<List<MeasurementResult>>(emptyList())
    val measurements = _measurements.asStateFlow()

    private val _tempHeartRates = mutableListOf<Int>()
    private val _tempConfidences = mutableListOf<Float>()

    private val _currentAverageHeartRate = MutableStateFlow(0)
    val currentAverageHeartRate = _currentAverageHeartRate.asStateFlow()

    private val _currentAverageConfidence = MutableStateFlow(0f)

    init {
        loadMeasurements()
    }

    fun changeScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            _measurements.value = repository.getAllMeasurements()
        }
    }

    fun clearTemporaryMeasurements() {
        _tempHeartRates.clear()
        _tempConfidences.clear()
        _currentAverageHeartRate.value = 0
        _currentAverageConfidence.value = 0f
    }

    fun addIntermediateMeasurement(heartRate: Int, confidence: Float) {
        _tempHeartRates.add(heartRate)
        _tempConfidences.add(confidence)

        updateCurrentAverage()
    }

    private fun updateCurrentAverage() {
        if (_tempHeartRates.isNotEmpty()) {
            _currentAverageHeartRate.value = _tempHeartRates.average().toInt()
            _currentAverageConfidence.value = _tempConfidences.average().toFloat()
        }
    }

    fun finalizeMeasurement() {
        if (_tempHeartRates.isNotEmpty()) {
            val avgHeartRate = _tempHeartRates.average().toInt()
            val avgConfidence = _tempConfidences.average().toFloat()

            _currentAverageHeartRate.value = avgHeartRate
            _currentAverageConfidence.value = avgConfidence

            viewModelScope.launch {
                repository.saveMeasurement(avgHeartRate, avgConfidence)
                loadMeasurements()

                _tempHeartRates.clear()
                _tempConfidences.clear()
            }
        }
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch {
            repository.deleteMeasurement(id)
            loadMeasurements()
        }
    }

    fun clearAllMeasurements() {
        viewModelScope.launch {
            repository.clearAllMeasurements()
            loadMeasurements()
        }
    }
}
