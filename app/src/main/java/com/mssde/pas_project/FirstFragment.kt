package com.mssde.pas_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mssde.pas_project.databinding.FragmentFirstBinding
import com.mssde.pas_project.viewmodel.WeatherViewModel
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al pulsar el botón, llama a AEMET
        binding.buttonFirst.setOnClickListener {
            binding.textViewResult.text = "Cargando..."
            viewModel.fetchWeather("28079")
        }

        // Observa los datos cuando lleguen
        viewModel.weather.observe(viewLifecycleOwner) { data ->
            val daily = data.daily

            val fecha = daily?.time?.getOrNull(0)
            val max = daily?.temperature_2m_max?.getOrNull(0)
            val min = daily?.temperature_2m_min?.getOrNull(0)
            val lluvia = daily?.precipitation_probability_max?.getOrNull(0)
            val codigo = daily?.weathercode?.getOrNull(0)

            binding.textViewResult.text = """
            Fecha: $fecha
            Temperatura máx: $max°C
            Temperatura mín: $min°C
            Prob. lluvia: $lluvia%
            Estado: ${weatherCodeToText(codigo)}
            """.trimIndent()
        }

        // Observa los errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.textViewResult.text = "Error: $error"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun weatherCodeToText(code: Int?): String {
        return when (code) {
            0 -> "Despejado"
            1, 2, 3 -> "Parcialmente nublado"
            45, 48 -> "Niebla"
            51, 53, 55 -> "Llovizna"
            61, 63, 65 -> "Lluvia"
            71, 73, 75 -> "Nieve"
            80, 81, 82 -> "Chubascos"
            95 -> "Tormenta"
            else -> "Desconocido"
        }
    }
}