package com.mssde.pas_project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.mssde.pas_project.databinding.FragmentFirstBinding
import com.mssde.pas_project.ml.RiegoPredictor
import com.mssde.pas_project.model.DispositivoRiego
import com.mssde.pas_project.repository.WeatherRepository
import com.mssde.pas_project.viewmodel.RiegoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class FirstFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
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

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.FirstFragment, true)
                .build()
            try {
                findNavController().navigate(R.id.loginFragment, null, navOptions)
            } catch (e: Exception) {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMarkerClickListener { marker ->
            val dispositivo = dispositivosFirebase.find { it.nombre == marker.title }
            dispositivo?.let { mostrarBottomSheet(it) }
            true
        }

        googleMap?.setOnMapLongClickListener { latLng ->
            mostrarDialogoNuevoDispositivo(latLng)
        }

        val centro = LatLng(40.4168, -3.7038)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 13f))
        actualizarMapaYCapas()
    }

    private fun mostrarDialogoNuevoDispositivo(latLng: LatLng) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.app_name))
        val input = EditText(requireContext())
        input.setHint(R.string.app_name) 
        builder.setView(input)
        builder.setPositiveButton("Añadir") { _, _ ->
            val nombre = input.text.toString()
            if (nombre.isNotEmpty()) {
                val nuevo = DispositivoRiego(nombre, latLng.latitude, latLng.longitude, 50.0, 20.0, 7.0, false)
                riegoViewModel.actualizarEnFirebase(nuevo)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun getBitmapFromVector(context: Context, vectorResId: Int, color: Int): BitmapDescriptor? {
        val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
        drawable.setTint(color)
        val bitmap = drawable.toBitmap()
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun actualizarMapaYCapas() {
        val map = googleMap ?: return
        map.clear()
        val tempPoints = mutableListOf<WeightedLatLng>()
        val rainPoints = mutableListOf<WeightedLatLng>()

        dispositivosFirebase.forEach { dispositivo ->
            val pos = LatLng(dispositivo.latitud, dispositivo.longitud)
            
            // COLORACIÓN: Azul Primario para activo, Gris #9E9E9E para inactivo
            val color = if (dispositivo.activo) {
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            } else {
                ContextCompat.getColor(requireContext(), R.color.inactive_gray)
            }

            val customIcon = getBitmapFromVector(requireContext(), R.drawable.ic_dispositivo_riego, color)
                ?: BitmapDescriptorFactory.defaultMarker()

            map.addMarker(MarkerOptions().position(pos).title(dispositivo.nombre).icon(customIcon))
            tempPoints.add(WeightedLatLng(pos, dispositivo.temperatura))
            cargarDatosLluviaParaHeatmap(dispositivo, rainPoints)
        }

        if (tempPoints.isNotEmpty()) {
            val gradient = Gradient(intArrayOf(Color.YELLOW, Color.RED), floatArrayOf(0.2f, 1.0f))
            val provider = HeatmapTileProvider.Builder().weightedData(tempPoints).radius(50).gradient(gradient).opacity(0.6).build()
            heatOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        }
    }

    private fun cargarDatosLluviaParaHeatmap(dispositivo: DispositivoRiego, rainList: MutableList<WeightedLatLng>) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud) }
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
            val provider = HeatmapTileProvider.Builder().weightedData(rainPoints).radius(45).gradient(gradient).opacity(0.5).build()
            rainOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        }
    }

    private fun mostrarBottomSheet(dispositivo: DispositivoRiego) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dispositivo, null)
        dialog.setContentView(view)

        val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
        val tvHumedad = view.findViewById<TextView>(R.id.tvHumedad)
        val tvTemperatura = view.findViewById<TextView>(R.id.tvTemperatura)
        val tvPh = view.findViewById<TextView>(R.id.tvPh)
        val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
        val btnActivar = view.findViewById<Button>(R.id.btnActivar)
        val tvMeteo = view.findViewById<TextView>(R.id.tvMeteo)
        val btnMeteo = view.findViewById<Button>(R.id.btnMeteo)
        val btnRiego = view.findViewById<Button>(R.id.btnRiego)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminar)

        tvNombre?.text = dispositivo.nombre
        tvHumedad?.text = getString(R.string.humedad_format, dispositivo.humedad)
        tvTemperatura?.text = getString(R.string.temp_format, dispositivo.temperatura)
        tvPh?.text = getString(R.string.ph_format, dispositivo.ph)

        actualizarEstadoUI(tvEstado, btnActivar, dispositivo)

        btnActivar?.setOnClickListener {
            dispositivo.activo = !dispositivo.activo
            riegoViewModel.actualizarEnFirebase(dispositivo)
            actualizarEstadoUI(tvEstado, btnActivar, dispositivo)
        }

        btnEliminar?.setOnClickListener {
            riegoViewModel.eliminarDeFirebase(dispositivo.nombre)
            dialog.dismiss()
        }

        btnMeteo?.setOnClickListener {
            tvMeteo?.setText(R.string.meteo_cargando)
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud) }
                result.fold(
                    onSuccess = { data ->
                        val max = data.daily?.temperature_2m_max?.getOrNull(0) ?: 0.0
                        val lluvia = data.daily?.precipitation_probability_max?.getOrNull(0) ?: 0
                        tvMeteo?.text = getString(R.string.meteo_formato, max, lluvia)
                    },
                    onFailure = { tvMeteo?.setText(R.string.meteo_error) }
                )
            }
        }

        btnRiego?.setOnClickListener {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { repository.getWeatherByCoords(dispositivo.latitud, dispositivo.longitud) }
                result.onSuccess { data ->
                    val tempMax = data.daily?.temperature_2m_max?.getOrNull(0)?.toFloat() ?: 25f
                    val tempMin = data.daily?.temperature_2m_min?.getOrNull(0)?.toFloat() ?: 12f
                    val lluvia = data.daily?.precipitation_probability_max?.getOrNull(0)?.toFloat() ?: 50f
                    val tempMedia = (tempMax + tempMin) / 2f
                    val eto = 0.0023f * (tempMedia + 17.8f) * sqrt(abs((tempMax - tempMin).toDouble())).toFloat() * 10f
                    val (debeRegar, prob) = riegoPredictor.predecir(dispositivo.humedad.toFloat(), dispositivo.temperatura.toFloat(), dispositivo.ph.toFloat(), tempMax, tempMin, lluvia, 0f, 60f, 10f, 2f, 1f, eto)
                    val porc = (prob * 100).toInt()
                    tvMeteo?.text = if (debeRegar) getString(R.string.riego_si, porc) else getString(R.string.riego_no, 100 - porc)
                }
            }
        }

        dialog.show()
    }

    private fun actualizarEstadoUI(tvEstado: TextView?, btnActivar: Button?, dispositivo: DispositivoRiego) {
        if (dispositivo.activo) {
            tvEstado?.setText(R.string.estado_activo)
            tvEstado?.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan))
            btnActivar?.setText(R.string.btn_detener)
        } else {
            tvEstado?.setText(R.string.estado_inactivo)
            tvEstado?.setTextColor(ContextCompat.getColor(requireContext(), R.color.inactive_gray))
            btnActivar?.setText(R.string.btn_iniciar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        riegoPredictor.close()
        _binding = null
    }
}
