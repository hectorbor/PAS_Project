package com.mssde.pas_project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mssde.pas_project.model.OpenMeteoResponse
import com.mssde.pas_project.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _weather = MutableLiveData<OpenMeteoResponse>()
    val weather: LiveData<OpenMeteoResponse> = _weather

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchWeather(municipio: String = "28079") {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getWeather(municipio)
            }
            result.fold(
                onSuccess = { _weather.value = it },
                onFailure = { _error.value = it.message }
            )
        }
    }
}