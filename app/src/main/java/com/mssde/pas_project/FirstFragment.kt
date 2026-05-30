package com.mssde.pas_project

import android.os.Bundle
import android.util.Log
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
import com.mssde.pas_project.ml.RiegoPredictor
import com.mssde.pas_project.model.DispositivoRiego
import com.mssde.pas_project.repository.WeatherRepository
import com.mssde.pas_project.viewmodel.RiegoViewModel
import com.mssde.pas_project.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirstFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()
    private var googleMap: GoogleMap? = null

    private lateinit var riegoPredictor: RiegoPredictor
    private val repository = WeatherRepository()

    private val riegoViewModel: RiegoViewModel by viewModels()

    // Lista que se sincronizará con Firebase
    private var dispositivosFirebase: List<DispositivoRiego> = emptyList()

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

        // Observamos los cambios en tiempo real desde Firebase
        riegoViewModel.listaRiegos.observe(viewLifecycleOwner) { lista ->
            Log.d("Firebase", "Datos recibidos: ${lista.size} dispositivos")
            dispositivosFirebase = lista
            actualizarMapa()
            
            // Si la base de datos está vacía, creamos los puntos iniciales
            inicializarDatosSiEsNecesario(lista)
        }

        riegoPredictor = RiegoPredictor(requireContext())
    }

    private fun inicializarDatosSiEsNecesario(lista: List<DispositivoRiego>) {
        if (lista.isEmpty()) {
            Log.d("Firebase", "Base de datos vacía, creando puntos iniciales...")
            val puntosIniciales = listOf(
                DispositivoRiego("Sector Norte", 40.4168, -3.7038, 45.0, 22.0, 6.5, false),
                DispositivoRiego("Sector Sur", 40.4180, -3.7050, 30.0, 24.0, 6.8, false),
                DispositivoRiego("Invernadero A", 40.4150, -3.7020, 60.0, 20.0, 6.2, true),
                DispositivoRiego("Zona Huerto", 40.4160, -3.7070, 15.0, 26.0, 7.0, false)
            )
            puntosIniciales.forEach { riegoViewModel.actualizarEnFirebase(it) }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.setOnMarkerClickListener { marker ->
            // Buscamos el dispositivo por el título del marcador
            val dispositivo = dispositivosFirebase.find { it.nombre == marker.title }
            dispositivo?.let { mostrarBottomSheet(it) }
            true
        }

        val centro = LatLng(40.4168, -3.7038)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 13f))
        
        actualizarMapa()
    }

    private fun actualizarMapa() {
        val map = googleMap ?: return
        map.clear()
        
        dispositivosFirebase.forEach { dispositivo ->
            val color = if (dispositivo.activo)
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_RED

            map.addMarker(
                MarkerOptions()
                    .position(LatLng(dispositivo.latitud, dispositivo.longitud))
                    .title(dispositivo.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )
        }
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

        actualizarEstadoUI(tvEstado, btnActivar, dispositivo)

        btnActivar.setOnClickListener {
            // Cambiamos el estado y lo mandamos a Firebase
            dispositivo.activo = !dispositivo.activo
            riegoViewModel.actualizarEnFirebase(dispositivo)
            
            // Actualizamos la UI local del diálogo
            actualizarEstadoUI(tvEstado, btnActivar, dispositivo)
        }

        view.findViewById<Button>(R.id.btnMeteo).setOnClickListener {
            tvMeteo.text = "Cargando datos meteorológicos..."
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.getWeatherByCoords(
                        dispositivo.latitud,
                        dispositivo.longitud
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

        val btnRiego = view.findViewById<Button>(R.id.btnRiego)
        btnRiego.setOnClickListener {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud)
                }
                result.fold(
                    onSuccess = { data ->
                        val tempMax = data.daily?.temperature_2m_max?.getOrNull(0)?.toFloat() ?: 25f
                        val tempMin = data.daily?.temperature_2m_min?.getOrNull(0)?.toFloat() ?: 12f
                        val lluvia = data.daily?.precipitation_probability_max?.getOrNull(0)?.toFloat() ?: 50f

                        val tempMedia = (tempMax + tempMin) / 2f
                        val eto = 0.0023f * (tempMedia + 17.8f) * Math.sqrt(Math.abs((tempMax - tempMin).toDouble())).toFloat() * 10f

                        val mes = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                        val estacion = when (mes) {
                            11, 0, 1 -> 0f   // invierno
                            2, 3, 4 -> 1f    // primavera
                            5, 6, 7 -> 2f    // verano
                            else -> 3f       // otoño
                        }

                        val (debeRegar, probabilidad) = riegoPredictor.predecir(
                            humedadSuelo = dispositivo.humedad.toFloat(),
                            tempSuelo = dispositivo.temperatura.toFloat(),
                            ph = dispositivo.ph.toFloat(),
                            tempMax = tempMax,
                            tempMin = tempMin,
                            probLluvia = lluvia,
                            lluvia24h = 0f,
                            humedadAire = 60f,
                            viento = 10f,
                            diasSinRiego = 2f,
                            estacion = estacion,
                            eto = eto
                        )

                        val porcentaje = (probabilidad * 100).toInt()
                        tvMeteo.text = if (debeRegar) 
                            "✅ SE RECOMIENDA REGAR ($porcentaje%)" 
                        else 
                            "❌ NO SE RECOMIENDA REGAR (${100-porcentaje}%)"
                    },
                    onFailure = { tvMeteo.text = "Error de predicción" }
                )
            }
        }

        dialog.show()
    }

    private fun actualizarEstadoUI(tvEstado: TextView, btnActivar: Button, dispositivo: DispositivoRiego) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        riegoPredictor.close()
        _binding = null
    }
}
