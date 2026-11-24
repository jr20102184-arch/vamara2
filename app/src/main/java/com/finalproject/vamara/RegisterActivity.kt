package com.finalproject.vamara
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("es")

        val nombre = findViewById<EditText>(R.id.etNombreRegistro)
        val correo = findViewById<EditText>(R.id.etCorreoRegistro)
        val password = findViewById<EditText>(R.id.etPasswordRegistro)
        val confirmar = findViewById<EditText>(R.id.etConfirmarRegistro)
        val btnCrear = findViewById<Button>(R.id.btnCrearCuenta)

        findViewById<android.widget.TextView>(
            R.id.tvVolverRegistro
        ).setOnClickListener {
            finish()
        }

        btnCrear.setOnClickListener {

            val nombreTexto = nombre.text.toString().trim()
            val correoTexto = correo.text.toString().trim()
            val clave = password.text.toString()
            val confirmacion = confirmar.text.toString()

            if (nombreTexto.isBlank()) {
                nombre.error = "Escribe tu nombre"
                nombre.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correoTexto).matches()) {
                correo.error = "Correo electrónico inválido"
                correo.requestFocus()
                return@setOnClickListener
            }

            if (clave.length < 8) {
                password.error = "Usa al menos 8 caracteres"
                password.requestFocus()
                return@setOnClickListener
            }

            if (clave != confirmacion) {
                confirmar.error = "Las contraseñas no coinciden"
                confirmar.requestFocus()
                return@setOnClickListener
            }

            btnCrear.isEnabled = false
            btnCrear.text = "Creando cuenta..."

            auth.createUserWithEmailAndPassword(correoTexto, clave)
                .addOnCompleteListener(this) { tarea ->

                    if (!tarea.isSuccessful) {
                        btnCrear.isEnabled = true
                        btnCrear.text = "Crear cuenta"

                        val mensaje = when (tarea.exception) {
                            is FirebaseAuthUserCollisionException ->
                                "Este correo ya está registrado"
                            else ->
                                tarea.exception?.localizedMessage
                                    ?: "No se pudo crear la cuenta"
                        }

                        Toast.makeText(
                            this,
                            mensaje,
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    val usuario = tarea.result?.user

                    if (usuario == null) {
                        btnCrear.isEnabled = true
                        btnCrear.text = "Crear cuenta"

                        Toast.makeText(
                            this,
                            "No se pudo obtener el usuario",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    val perfil = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombreTexto)
                        .build()

                    usuario.updateProfile(perfil)
                        .addOnCompleteListener { tareaPerfil ->

                            usuario.sendEmailVerification()
                                .addOnCompleteListener { tareaCorreo ->

                                    btnCrear.isEnabled = true
                                    btnCrear.text = "Crear cuenta"

                                    auth.signOut()

                                    val mensaje = when {
                                        !tareaCorreo.isSuccessful ->
                                            "Cuenta creada, pero falló el envío del correo de verificación"

                                        !tareaPerfil.isSuccessful ->
                                            "Cuenta creada y correo enviado, pero no se guardó el nombre"

                                        else ->
                                            "Cuenta creada. Revisa tu correo para verificarla"
                                    }

                                    Toast.makeText(
                                        this,
                                        mensaje,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    if (tareaCorreo.isSuccessful) {
                                        finish()
                                    }
                                }
                        }
                }
        }
    }
}
