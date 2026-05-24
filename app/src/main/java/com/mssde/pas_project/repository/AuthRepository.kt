package com.mssde.pas_project.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, pass: String): Result<FirebaseUser?> =
        runCatching {
            Log.d("AuthRepo", "Solicitando login a Firebase...")
            val user = auth.signInWithEmailAndPassword(email, pass).await().user
            Log.d("AuthRepo", "Login completado: ${user?.email}")
            user
        }

    suspend fun register(email: String, pass: String): Result<FirebaseUser?> =
        runCatching {
            Log.d("AuthRepo", "Solicitando registro a Firebase para: $email")
            val user = auth.createUserWithEmailAndPassword(email, pass).await().user
            Log.d("AuthRepo", "Registro completado: ${user?.email}")
            user
        }.onFailure {
            Log.e("AuthRepo", "Fallo en la tarea de Firebase", it)
        }

    fun currentUser(): FirebaseUser? = auth.currentUser
    fun logout(): Unit = auth.signOut()
}
