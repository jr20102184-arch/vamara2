package com.finalproject.vamara

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("es")

        val correo = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Regresar a bienvenida
        findViewById<TextView>(R.id.tvVolver)
            .setOnClickListener {
                finish()
            }

        // Abrir registro
        findViewById<TextView>(R.id.tvIrRegistro)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this,
                        RegisterActivity::class.java
                    )
                )
            }

        btnLogin.setOnClickListener {

            val emailTexto = correo.text.toString().trim()
            val clave = password.text.toString()

            if (!Patterns.EMAIL_ADDRESS
                    .matcher(emailTexto).matches()) {
                correo.error = "Ingresa un correo válido"
                correo.requestFocus()
                return@setOnClickListener
            }

            if (clave.isEmpty()) {
                password.error = "Ingresa tu contraseña"
                password.requestFocus()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Verificando..."

            auth.signInWithEmailAndPassword(
                emailTexto,
                clave
            ).addOnCompleteListener(this) { tarea ->

                if (!tarea.isSuccessful) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"

                    Toast.makeText(
                        this,
                        "No se pudo iniciar sesión. Revisa tus datos e inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                val usuario = auth.currentUser

                if (usuario == null) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"

                    Toast.makeText(
                        this,
                        "No se pudo obtener la cuenta",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                // Actualizar el estado de verificación
                usuario.reload().addOnCompleteListener { recarga ->

                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"

                    if (!recarga.isSuccessful) {
                        Toast.makeText(
                            this,
                            "No se pudo verificar tu cuenta. Revisa tu conexión.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    if (!usuario.isEmailVerified) {
                        Toast.makeText(
                            this,
                            "Verifica tu correo antes de continuar",
                            Toast.LENGTH_LONG
                        ).show()

                        auth.signOut()
                        return@addOnCompleteListener
                    }

                    // Login exitoso
                    Toast.makeText(
                        this,
                        "¡Bienvenido a VAMARA, ${usuario.displayName ?: "usuario"}!",
                        Toast.LENGTH_LONG
                    ).show()


                    val intent = Intent(
                        this,
                        DashboardActivity::class.java
                    )

                    startActivity(intent)
                    finish()

                }
            }
        }
    }
}
