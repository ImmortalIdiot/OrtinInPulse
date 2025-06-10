package com.ortin.inpulse.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasurementRepository(context: Context) {
    companion object {
        private val MEASUREMENTS_KEY = stringPreferencesKey("measurements")
    }

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("ortin_in_pulse_prefs") }
    )

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    suspend fun getAllMeasurements(): List<MeasurementResult> {
        val preferences = dataStore.data.first()
        val json = preferences[MEASUREMENTS_KEY] ?: return emptyList()

        return try {
            val type = object : TypeToken<List<MeasurementResult>>() {}.type
            val measurements = gson.fromJson<List<MeasurementResult>>(json, type)
            measurements.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveMeasurement(heartRate: Int, confidence: Float): MeasurementResult {
        val newMeasurement = MeasurementResult(
            heartRate = heartRate,
            confidence = confidence
        )

        val currentMeasurements = getAllMeasurements().toMutableList()
        currentMeasurements.add(newMeasurement)

        val json = gson.toJson(currentMeasurements)

        dataStore.edit { preferences ->
            preferences[MEASUREMENTS_KEY] = json
        }

        return newMeasurement
    }

    suspend fun deleteMeasurement(id: String) {
        val currentMeasurements = getAllMeasurements().toMutableList()
        val updatedMeasurements = currentMeasurements.filter { it.id != id }

        if (updatedMeasurements.size < currentMeasurements.size) {
            val json = gson.toJson(updatedMeasurements)
            dataStore.edit { preferences ->
                preferences[MEASUREMENTS_KEY] = json
            }
        }
    }

    suspend fun clearAllMeasurements() {
        dataStore.edit { preferences ->
            preferences.remove(MEASUREMENTS_KEY)
        }
    }

    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>,
        JsonDeserializer<LocalDateTime> {

        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(formatter.format(src))
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDateTime {
            return LocalDateTime.parse(json.asString, formatter)
        }
    }
}
