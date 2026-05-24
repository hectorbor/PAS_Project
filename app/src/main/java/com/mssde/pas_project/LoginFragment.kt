package com.mssde.pas_project

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mssde.pas_project.R
import com.mssde.pas_project.databinding.FragmentLoginBinding
import com.mssde.pas_project.viewmodel.AuthViewModel

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val viewModel: AuthViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        
        Log.i("LOGIN_TEST", "Fragmento cargado correctamente")

        viewModel.authState.observe(viewLifecycleOwner) { result ->
            Log.i("LOGIN_TEST", "Llegó respuesta de Firebase")
            result.onSuccess {
                Toast.makeText(context, "Éxito", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_login_to_first)
            }
            result.onFailure {
                Log.e("LOGIN_TEST", "Error en Firebase: ${it.message}")
                Toast.makeText(context, "Error Firebase: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            
            // Log forzado para ver en Logcat sin filtros
            println("!!! BOTON PULSADO: Intentando registrar $email")
            Toast.makeText(context, "Click detectado", Toast.LENGTH_SHORT).show()
            
            if (validateInput(email, pass)) {
                if (pass.length < 6) {
                    binding.tilPassword.error = "Mínimo 6 caracteres"
                } else {
                    Log.i("LOGIN_TEST", "Llamando a viewModel.register")
                    viewModel.register(email, pass)
                }
            }
        }
    }

    private fun validateInput(email: String, pass: String): Boolean {
        var isValid = true
        if (email.isEmpty()) { binding.tilEmail.error = "Vacío"; isValid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.error = "Inválido"; isValid = false }
        else { binding.tilEmail.error = null }

        if (pass.isEmpty()) { binding.tilPassword.error = "Vacío"; isValid = false }
        else { binding.tilPassword.error = null }
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
