package com.finalproject.vamara

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal

class MovimientoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movimiento)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val spTipo = findViewById<Spinner>(R.id.spTipo)
        val spCategoria = findViewById<Spinner>(R.id.spCategoria)
        val etMonto = findViewById<EditText>(R.id.etMonto)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val btnGuardar = findViewById<Button>(
            R.id.btnGuardarMovimiento
        )

        findViewById<TextView>(
            R.id.tvVolverMovimiento
        ).setOnClickListener {
            finish()
        }

        configurarSpinner(
            spTipo,
            listOf("Ingreso", "Gasto")
        )

        configurarSpinner(
            spCategoria,
            listOf(
                "Alimentación",
                "Transporte",
                "Servicios",
                "Entretenimiento",
                "Salario",
                "Ventas",
                "Otros"
            )
        )

        btnGuardar.setOnClickListener {

            val usuario = auth.currentUser

            if (usuario == null) {
                mostrarMensaje("Inicia sesión nuevamente")
                return@setOnClickListener
            }

            val montoTexto = etMonto.text.toString()
                .trim()
                .replace(",", ".")

            val monto = try {
                BigDecimal(montoTexto).longValueExact()
            } catch (e: Exception) {
                null
            }

            if (monto == null || monto <= 0) {
                etMonto.error =
                    "Ingresa un monto entero mayor que cero"
                etMonto.requestFocus()
                return@setOnClickListener
            }

            val tipo = spTipo.selectedItem.toString()
            val categoria = spCategoria.selectedItem.toString()
            val descripcion = etDescripcion.text.toString().trim()

            val movimiento = hashMapOf(
                "tipo" to tipo,
                "monto" to monto,
                "categoria" to categoria,
                "descripcion" to descripcion,
                "fecha" to FieldValue.serverTimestamp()
            )

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            db.collection("usuarios")
                .document(usuario.uid)
                .collection("movimientos")
                .add(movimiento)
                .addOnSuccessListener {

                    mostrarMensaje(
                        "Movimiento guardado correctamente"
                    )

                    finish()
                }
                .addOnFailureListener { error ->

                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar movimiento"

                    mostrarMensaje(
                        "Error al guardar: ${error.localizedMessage}"
                    )
                }
        }
    }

    private fun configurarSpinner(
        spinner: Spinner,
        opciones: List<String>
    ) {
        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adaptador
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
