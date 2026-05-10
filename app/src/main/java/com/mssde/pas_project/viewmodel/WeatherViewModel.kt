package com.mssde.pas_project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mssde.pas_project.model.Prediccion
import com.mssde.pas_project.repository.WeatherRepository
import kotlinx.coroutines.launch
class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _weather = MutableLiveData<List<Prediccion>>()
    val weather: LiveData<List<Prediccion>> = _weather

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchWeather(municipio: String = "28079") { // 28079 = Madrid
        viewModelScope.launch {
            val result = repository.getWeather(municipio)
            result.fold(
                onSuccess = { _weather.value = it },
                onFailure = { _error.value = it.message }
            )
        }
    }
}