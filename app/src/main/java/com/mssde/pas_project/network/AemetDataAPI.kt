package com.mssde.pas_project.network

import com.mssde.pas_project.model.Prediccion
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface AemetDataApi {

    @GET
    suspend fun getWeatherData(
        @Url url: String
    ): Response<List<Prediccion>>
}