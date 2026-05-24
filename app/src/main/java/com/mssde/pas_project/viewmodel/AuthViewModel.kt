package com.mssde.pas_project.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.mssde.pas_project.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    val authState = MutableLiveData<Result<FirebaseUser?>>()

    fun login(email: String, pass: String) = viewModelScope.launch {
        Log.d("AuthViewModel", "Intentando login para: $email")
        val result = repo.login(email, pass)
        Log.d("AuthViewModel", "Resultado login: ${result.isSuccess}")
        authState.value = result
    }

    fun register(email: String, pass: String) = viewModelScope.launch {
        Log.d("AuthViewModel", "Intentando registro para: $email")
        val result = repo.register(email, pass)
        Log.d("AuthViewModel", "Resultado registro: ${result.isSuccess}")
        authState.value = result
    }
}
