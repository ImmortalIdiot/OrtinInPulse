package com.ortin.inpulse.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasurementRepository(context: Context) {
    companion object {
        private const val PREFS_NAME = "ortin_in_pulse_prefs"
        private const val MEASUREMENTS_KEY = "measurements"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    fun getAllMeasurements(): List<MeasurementResult> {
        val json = sharedPreferences.getString(MEASUREMENTS_KEY, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<MeasurementResult>>() {}.type
            val measurements = gson.fromJson<List<MeasurementResult>>(json, type)
            measurements.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveMeasurement(heartRate: Int, confidence: Float): MeasurementResult {
        val newMeasurement = MeasurementResult(
            heartRate = heartRate,
            confidence = confidence
        )
        
        val currentMeasurements = getAllMeasurements().toMutableList()
        currentMeasurements.add(newMeasurement)
        
        val json = gson.toJson(currentMeasurements)
        sharedPreferences.edit().putString(MEASUREMENTS_KEY, json).apply()
        
        return newMeasurement
    }
    
    fun deleteMeasurement(id: String) {
        val currentMeasurements = getAllMeasurements().toMutableList()
        val updatedMeasurements = currentMeasurements.filter { it.id != id }
        
        if (updatedMeasurements.size < currentMeasurements.size) {
            val json = gson.toJson(updatedMeasurements)
            sharedPreferences.edit().putString(MEASUREMENTS_KEY, json).apply()
        }
    }
    
    fun clearAllMeasurements() {
        sharedPreferences.edit().remove(MEASUREMENTS_KEY).apply()
    }
    
    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>,
        JsonDeserializer<LocalDateTime> {
            
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: java.lang.reflect.Type,
            context: com.google.gson.JsonSerializationContext
        ): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(formatter.format(src))
        }

        override fun deserialize(
            json: com.google.gson.JsonElement,
            typeOfT: java.lang.reflect.Type,
            context: com.google.gson.JsonDeserializationContext
        ): LocalDateTime {
            return LocalDateTime.parse(json.asString, formatter)
        }
    }
}
