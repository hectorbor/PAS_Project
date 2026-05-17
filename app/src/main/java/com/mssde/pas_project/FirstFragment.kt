package com.mssde.pas_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mssde.pas_project.databinding.FragmentFirstBinding
import com.mssde.pas_project.model.DispositivoRiego
import com.mssde.pas_project.repository.WeatherRepository
import com.mssde.pas_project.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirstFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val repository = WeatherRepository()

    private val dispositivos = listOf(
        DispositivoRiego("Dispositivo Riego 1", LatLng(40.4168, -3.7038), 65.0, 22.5, 6.8, true),
        DispositivoRiego("Dispositivo Riego 2", LatLng(40.4200, -3.7100), 45.0, 21.0, 7.1, false),
        DispositivoRiego("Dispositivo Riego 3", LatLng(40.4150, -3.6980), 78.0, 23.0, 6.5, true),
        DispositivoRiego("Dispositivo Riego 4", LatLng(40.4220, -3.6950), 55.0, 20.5, 7.0, false)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        dispositivos.forEach { dispositivo ->
            val color = if (dispositivo.activo)
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_RED

            googleMap?.addMarker(
                MarkerOptions()
                    .position(dispositivo.ubicacion)
                    .title(dispositivo.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )
        }

        googleMap?.setOnMarkerClickListener { marker ->
            val dispositivo = dispositivos.find { it.nombre == marker.title }
            dispositivo?.let { mostrarBottomSheet(it) }
            true
        }

        val centro = LatLng(40.4168, -3.7038)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 13f))
    }

    private fun mostrarBottomSheet(dispositivo: DispositivoRiego) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dispositivo, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvNombre).text = dispositivo.nombre
        view.findViewById<TextView>(R.id.tvHumedad).text = "${dispositivo.humedad}%"
        view.findViewById<TextView>(R.id.tvTemperatura).text = "${dispositivo.temperatura}°C"
        view.findViewById<TextView>(R.id.tvPh).text = "${dispositivo.ph}"

        val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
        val btnActivar = view.findViewById<Button>(R.id.btnActivar)
        val tvMeteo = view.findViewById<TextView>(R.id.tvMeteo)

        actualizarEstado(tvEstado, btnActivar, dispositivo)

        btnActivar.setOnClickListener {
            dispositivo.activo = !dispositivo.activo
            actualizarEstado(tvEstado, btnActivar, dispositivo)
            actualizarColorMarcador(dispositivo)
        }

        view.findViewById<Button>(R.id.btnMeteo).setOnClickListener {
            tvMeteo.text = "Cargando datos meteorológicos..."
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.getWeatherByCoords(
                        dispositivo.ubicacion.latitude,
                        dispositivo.ubicacion.longitude
                    )
                }
                result.fold(
                    onSuccess = { data ->
                        val daily = data.daily
                        val max = daily?.temperature_2m_max?.getOrNull(0)
                        val min = daily?.temperature_2m_min?.getOrNull(0)
                        val lluvia = daily?.precipitation_probability_max?.getOrNull(0)
                        tvMeteo.text = "Máx: $max°C  Mín: $min°C  Lluvia: $lluvia%"
                    },
                    onFailure = {
                        tvMeteo.text = "Error al obtener datos"
                    }
                )
            }
        }

        dialog.show()
    }

    private fun actualizarEstado(tvEstado: TextView, btnActivar: Button, dispositivo: DispositivoRiego) {
        if (dispositivo.activo) {
            tvEstado.text = "Activo"
            tvEstado.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            btnActivar.text = "Desactivar"
        } else {
            tvEstado.text = "Inactivo"
            tvEstado.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            btnActivar.text = "Activar"
        }
    }

    private fun actualizarColorMarcador(dispositivo: DispositivoRiego) {
        googleMap?.clear()
        dispositivos.forEach { d ->
            val color = if (d.activo)
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_RED
            googleMap?.addMarker(
                MarkerOptions()
                    .position(d.ubicacion)
                    .title(d.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )
        }
        googleMap?.setOnMarkerClickListener { marker ->
            val d = dispositivos.find { it.nombre == marker.title }
            d?.let { mostrarBottomSheet(it) }
            true
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}