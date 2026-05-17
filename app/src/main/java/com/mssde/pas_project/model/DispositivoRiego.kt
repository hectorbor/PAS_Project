package com.mssde.pas_project.model

import com.google.android.gms.maps.model.LatLng

data class DispositivoRiego(
    val nombre: String,
    val ubicacion: LatLng,
    val humedad: Double,
    val temperatura: Double,
    val ph: Double,
    var activo: Boolean
)