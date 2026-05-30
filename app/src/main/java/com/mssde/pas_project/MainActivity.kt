package com.mssde.pas_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.mssde.pas_project.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Verificar sesión de Firebase
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            navController.navigate(R.id.loginFragment)
        }
    }
    
    // Hemos eliminado setupActionBarWithNavController y los métodos de menú 
    // para que no aparezca la barra superior del sistema.
}
