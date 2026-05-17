package com.mssde.pas_project.model

data class OpenMeteoResponse(
    val daily: DailyData?
)

data class DailyData(
    val time: List<String>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val precipitation_probability_max: List<Int>?,
    val weathercode: List<Int>?
)