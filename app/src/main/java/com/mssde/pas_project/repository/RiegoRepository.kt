package com.mssde.pas_project.repository

import android.util.Log
import com.google.firebase.database.*
import com.mssde.pas_project.model.DispositivoRiego
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RiegoRepository {
    private val database = FirebaseDatabase.getInstance("https://smart-mssde-irrigation-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference("dispositivos")

    fun updateRiego(dispositivo: DispositivoRiego) {
        database.child(dispositivo.nombre.replace(" ", "_")).setValue(dispositivo)
    }

    fun deleteRiego(nombre: String) {
        database.child(nombre.replace(" ", "_")).removeValue()
    }

    fun getRiegosFlow(): Flow<List<DispositivoRiego>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(DispositivoRiego::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                // CAMBIO CLAVE: No lanzamos excepción al cerrar sesión, solo avisamos al log
                Log.w("Firebase", "Conexión cerrada o falta de permisos: ${error.message}")
            }
        }
        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }
}