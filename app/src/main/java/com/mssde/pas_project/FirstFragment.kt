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
        viewModel.weather.observe(viewLifecycleOwner) { predicciones ->
            val hoy = predicciones.firstOrNull()?.prediccion?.dia?.firstOrNull()
            val nombre = predicciones.firstOrNull()?.nombre
            val max = hoy?.temperatura?.maxima
            val min = hoy?.temperatura?.minima
            val cielo = hoy?.estadoCielo?.firstOrNull()?.descripcion

            binding.textViewResult.text = """
                Ciudad: $nombre
                Fecha: ${hoy?.fecha}
                Temperatura máx: $max°
                Temperatura mín: $min°
                Estado del cielo: $cielo
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
}