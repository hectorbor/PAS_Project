package com.mssde.pas_project.model

//import com.google.android.gms.maps.model.LatLng

data class DispositivoRiego(
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val humedad: Double = 0.0,
    val temperatura: Double = 0.0,
    val ph: Double = 0.0,
    var activo: Boolean = false
)