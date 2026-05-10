package com.mssde.pas_project.network

import com.mssde.pas_project.model.AemetInitialResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AemetApi {

    // Primera llamada: obtiene la URL con los datos
    @GET("opendata/api/prediccion/especifica/municipio/diaria/{municipio}")
    suspend fun getWeatherUrl(
        @Path("municipio") municipio: String,
        @Query("api_key") apiKey: String
    ): Response<AemetInitialResponse>
}

