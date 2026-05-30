package com.mssde.pas_project.viewmodel

import androidx.lifecycle.*
import com.mssde.pas_project.model.DispositivoRiego
import com.mssde.pas_project.repository.RiegoRepository

class RiegoViewModel : ViewModel() {
    private val repository = RiegoRepository()
    val listaRiegos = repository.getRiegosFlow().asLiveData()

    fun actualizarEnFirebase(dispositivo: DispositivoRiego) {
        repository.updateRiego(dispositivo)
    }

    fun eliminarDeFirebase(nombre: String) {
        repository.deleteRiego(nombre)
    }
}