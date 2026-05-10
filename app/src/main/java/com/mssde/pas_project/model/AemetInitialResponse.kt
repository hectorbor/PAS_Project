
package com.mssde.pas_project.model

data class AemetInitialResponse(
    val descripcion: String?,
    val estado: Int?,
    val datos: String?,    // <-- URL con los datos reales
    val metadatos: String?
)