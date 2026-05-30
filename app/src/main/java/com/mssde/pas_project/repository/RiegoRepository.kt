package com.mssde.pas_project.repository

import com.google.firebase.database.*
import com.mssde.pas_project.model.DispositivoRiego
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RiegoRepository {
    // Especificamos la URL de la región correcta (europe-west1)
    private val database = FirebaseDatabase.getInstance("https://smart-mssde-irrigation-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference("dispositivos")

    // Esta es la función que guarda el estado exacto en Firebase
    fun updateRiego(dispositivo: DispositivoRiego) {
        // Usamos el nombre como ID o un ID único si lo tienes
        database.child(dispositivo.nombre.replace(" ", "_")).setValue(dispositivo)
    }

    // Escucha cambios en tiempo real
    fun getRiegosFlow(): Flow<List<DispositivoRiego>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(DispositivoRiego::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }
}