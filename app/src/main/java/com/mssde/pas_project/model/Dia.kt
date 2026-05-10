package com.mssde.pas_project.model

data class Dia(
    val fecha: String?,
    val temperatura: Temperatura?,
    val estadoCielo: List<EstadoCielo>?
)