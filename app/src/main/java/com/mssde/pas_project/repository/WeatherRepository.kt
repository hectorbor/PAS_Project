package com.mssde.pas_project.repository

import com.mssde.pas_project.model.Prediccion
import com.mssde.pas_project.network.RetrofitClient

class WeatherRepository {

    private val API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoLmJvcnJlZ3Vlcm9AYWx1bW5vcy51cG0uZXMiLCJqdGkiOiIxMzkxNDgzOC1hOTJiLTRkMzktODY4YS03MTNkZGJmMzhmYjUiLCJpc3MiOiJBRU1FVCIsImlhdCI6MTc3ODM0MTc1OSwidXNlcklkIjoiMTM5MTQ4MzgtYTkyYi00ZDM5LTg2OGEtNzEzZGRiZjM4ZmI1Iiwicm9sZSI6IiJ9.Js-8DuWVmY48fgAE6QxRpg0fC3pqBZCEySvmQn3qVSE"

    suspend fun getWeather(municipio: String): Result<List<Prediccion>> {
        return try {
            // Paso 1: obtener la URL con los datos
            val initialResponse = RetrofitClient.aemetApi
                .getWeatherUrl(municipio, API_KEY)

            if (!initialResponse.isSuccessful) {
                return Result.failure(Exception("Error: ${initialResponse.code()}"))
            }

            val dataUrl = initialResponse.body()?.datos
                ?: return Result.failure(Exception("No se recibió URL de datos"))

// Añade esta línea para ver la URL en el Logcat
            android.util.Log.d("AEMET", "URL de datos: $dataUrl")

            // Paso 2: obtener los datos reales desde esa URL
            val dataResponse = RetrofitClient.aemetDataApi
                .getWeatherData(dataUrl)

            if (dataResponse.isSuccessful) {
                Result.success(dataResponse.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al obtener datos: ${dataResponse.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}