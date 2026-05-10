package com.mssde.pas_project.model

data class Prediccion(
    val origen: Origen?,
    val elaborado: String?,
    val prediccion: PrediccionData?,
    val nombre: String?,
    val provincia: String?,
    val id: String?
)