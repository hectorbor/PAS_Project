package com.mssde.pas_project

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
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

    private var heatOverlay: TileOverlay? = null
    private var rainOverlay: TileOverlay? = null

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

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        riegoViewModel.listaRiegos.observe(viewLifecycleOwner) { lista ->
            dispositivosFirebase = lista
            actualizarMapaYCapas()
        }

        riegoPredictor = RiegoPredictor(requireContext())
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap?.setOnMarkerClickListener { marker ->
            val dispositivo = dispositivosFirebase.find { it.nombre == marker.title }
            dispositivo?.let { mostrarBottomSheet(it) }
            true
        }

        // FUNCIONALIDAD: Poner pines con pulsación larga
        googleMap?.setOnMapLongClickListener { latLng ->
            mostrarDialogoNuevoDispositivo(latLng)
        }

        val centro = LatLng(40.4168, -3.7038)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 13f))
        actualizarMapaYCapas()
    }

    private fun mostrarDialogoNuevoDispositivo(latLng: LatLng) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Añadir nuevo dispositivo")
        
        val input = EditText(requireContext())
        input.hint = "Nombre del dispositivo"
        builder.setView(input)

        builder.setPositiveButton("Añadir") { _, _ ->
            val nombre = input.text.toString()
            if (nombre.isNotEmpty()) {
                val nuevo = DispositivoRiego(nombre, latLng.latitude, latLng.longitude, 50.0, 20.0, 7.0, false)
                riegoViewModel.actualizarEnFirebase(nuevo)
                Toast.makeText(requireContext(), "Dispositivo añadido", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun actualizarMapaYCapas() {
        val map = googleMap ?: return
        map.clear()
        
        val tempPoints = mutableListOf<WeightedLatLng>()
        val rainPoints = mutableListOf<WeightedLatLng>()

        dispositivosFirebase.forEach { dispositivo ->
            val pos = LatLng(dispositivo.latitud, dispositivo.longitud)
            
            val color = if (dispositivo.activo) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
            map.addMarker(MarkerOptions().position(pos).title(dispositivo.nombre).icon(BitmapDescriptorFactory.defaultMarker(color)))

            tempPoints.add(WeightedLatLng(pos, dispositivo.temperatura))
            cargarDatosLluviaParaHeatmap(dispositivo, rainPoints)
        }

        if (tempPoints.isNotEmpty()) {
            val gradient = Gradient(intArrayOf(Color.YELLOW, Color.RED), floatArrayOf(0.2f, 1.0f))
            val provider = HeatmapTileProvider.Builder()
                .weightedData(tempPoints)
                .radius(50)
                .gradient(gradient)
                .opacity(0.6)
                .build()
            heatOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        }
    }

    private fun cargarDatosLluviaParaHeatmap(dispositivo: DispositivoRiego, rainList: MutableList<WeightedLatLng>) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud)
            }
            result.onSuccess { data ->
                val probLluvia = data.daily?.precipitation_probability_max?.getOrNull(0)?.toDouble() ?: 0.0
                if (probLluvia > 0) {
                    val pos = LatLng(dispositivo.latitud, dispositivo.longitud)
                    rainList.add(WeightedLatLng(pos, probLluvia))
                    actualizarCapaLluvia(rainList)
                }
            }
        }
    }

    private fun actualizarCapaLluvia(rainPoints: List<WeightedLatLng>) {
        val map = googleMap ?: return
        rainOverlay?.remove()
        
        if (rainPoints.isNotEmpty()) {
            val gradient = Gradient(intArrayOf(Color.CYAN, Color.BLUE), floatArrayOf(0.2f, 1.0f))
            val provider = HeatmapTileProvider.Builder()
                .weightedData(rainPoints)
                .radius(45)
                .gradient(gradient)
                .opacity(0.5)
                .build()
            rainOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
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
            dispositivo.activo = !dispositivo.activo
            riegoViewModel.actualizarEnFirebase(dispositivo)
            actualizarEstadoUI(tvEstado, btnActivar, dispositivo)
        }

        // FUNCIONALIDAD: Quitar pines
        view.findViewById<Button>(R.id.btnEliminar).setOnClickListener {
            riegoViewModel.eliminarDeFirebase(dispositivo.nombre)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Dispositivo eliminado", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnMeteo).setOnClickListener {
            tvMeteo.text = "Consultando clima..."
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud)
                }
                result.fold(
                    onSuccess = { data ->
                        val max = data.daily?.temperature_2m_max?.getOrNull(0)
                        val lluvia = data.daily?.precipitation_probability_max?.getOrNull(0)
                        tvMeteo.text = "Máx: $max°C | Lluvia: $lluvia%"
                    },
                    onFailure = { tvMeteo.text = "Error al obtener datos" }
                )
            }
        }

        view.findViewById<Button>(R.id.btnRiego).setOnClickListener {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud)
                }
                result.onSuccess { data ->
                    val tempMax = data.daily?.temperature_2m_max?.getOrNull(0)?.toFloat() ?: 25f
                    val tempMin = data.daily?.temperature_2m_min?.getOrNull(0)?.toFloat() ?: 12f
                    val lluvia = data.daily?.precipitation_probability_max?.getOrNull(0)?.toFloat() ?: 50f
                    val tempMedia = (tempMax + tempMin) / 2f
                    val eto = 0.0023f * (tempMedia + 17.8f) * Math.sqrt(Math.abs((tempMax - tempMin).toDouble())).toFloat() * 10f

                    val (debeRegar, prob) = riegoPredictor.predecir(
                        humedadSuelo = dispositivo.humedad.toFloat(),
                        tempSuelo = dispositivo.temperatura.toFloat(),
                        ph = dispositivo.ph.toFloat(),
                        tempMax, tempMin, lluvia, 0f, 60f, 10f, 2f, 1f, eto
                    )
                    val porc = (prob * 100).toInt()
                    tvMeteo.text = if (debeRegar) "✅ REGAR ($porc%)" else "❌ NO REGAR (${100-porc}%)"
                }
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
