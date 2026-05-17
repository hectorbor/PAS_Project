package com.mssde.pas_project.repository

import com.google.gson.Gson
import com.mssde.pas_project.model.OpenMeteoResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WeatherRepository {

    private fun getHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getWeather(municipio: String): Result<OpenMeteoResponse> {
        // Coordenadas de Madrid por defecto
        // Puedes añadir más municipios aquí si lo necesitas
        val (lat, lon) = when (municipio) {
            "28079" -> Pair(40.4168, -3.7038) // Madrid
            "08019" -> Pair(41.3874, 2.1686)  // Barcelona
            "46250" -> Pair(39.4699, -0.3763) // Valencia
            "41091" -> Pair(37.3891, -5.9845) // Sevilla
            else -> Pair(40.4168, -3.7038)
        }

        return try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat" +
                    "&longitude=$lon" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode" +
                    "&timezone=Europe/Madrid" +
                    "&forecast_days=7"

            android.util.Log.d("WEATHER", "URL: $url")

            val client = getHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            android.util.Log.d("WEATHER", "Respuesta: ${body?.take(200)}")

            if (response.isSuccessful && body != null) {
                val data = Gson().fromJson(body, OpenMeteoResponse::class.java)
                Result.success(data)
            } else {
                Result.failure(Exception("Error: ${response.code}"))
            }

        } catch (e: Exception) {
            android.util.Log.e("WEATHER", "Error: ${e.message}")
            Result.failure(e)
        }
    }
}