package com.finalproject.vamara

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import java.text.NumberFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvSaldo: TextView
    private lateinit var tvIngresos: TextView
    private lateinit var tvGastos: TextView
    private lateinit var tvMovimientos: TextView

    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val usuario = auth.currentUser

        if (usuario == null || !usuario.isEmailVerified) {
            auth.signOut()
            irAlLogin()
            return
        }

        setContentView(R.layout.activity_dashboard)

        tvSaldo = findViewById(R.id.tvSaldo)
        tvIngresos = findViewById(R.id.tvIngresos)
        tvGastos = findViewById(R.id.tvGastos)
        tvMovimientos = findViewById(R.id.tvListaMovimientos)

        val nombre = usuario.displayName ?: "Usuario"

        findViewById<TextView>(R.id.tvSaludo).text =
            "Hola, $nombre"

        // Abrir formulario
        findViewById<Button>(
            R.id.btnNuevoMovimiento
        ).setOnClickListener {

            startActivity(
                Intent(this, MovimientoActivity::class.java)
            )
        }

        // Cerrar sesión
        findViewById<Button>(
            R.id.btnCerrarSesion
        ).setOnClickListener {

            auth.signOut()
            irAlLogin()
        }
    }

    override fun onStart() {
        super.onStart()

        val usuario = auth.currentUser ?: return

        tvMovimientos.text = "Cargando movimientos..."

        listener = db.collection("usuarios")
            .document(usuario.uid)
            .collection("movimientos")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    tvMovimientos.text =
                        "Error al cargar: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    actualizarDashboard(snapshot)
                }
            }
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    private fun actualizarDashboard(
        snapshot: QuerySnapshot
    ) {

        var ingresos = 0L
        var gastos = 0L

        val movimientos = snapshot.documents
            .sortedByDescending {
                it.getTimestamp("fecha")?.seconds ?: 0L
            }

        val historial = StringBuilder()

        for (documento in movimientos) {

            val tipo = documento.getString("tipo") ?: ""
            val categoria = documento.getString("categoria") ?: "Otros"
            val descripcion = documento.getString("descripcion") ?: ""
            val monto = documento.getLong("monto") ?: 0L

            if (tipo == "Ingreso") {
                ingresos += monto
            } else if (tipo == "Gasto") {
                gastos += monto
            }
        }

        val saldo = ingresos - gastos

        tvSaldo.text = formatearDinero(saldo)
        tvIngresos.text = formatearDinero(ingresos)
        tvGastos.text = formatearDinero(gastos)

        if (movimientos.isEmpty()) {
            tvMovimientos.text =
                "Aún no tienes movimientos."
            return
        }

        for (documento in movimientos.take(5)) {

            val tipo = documento.getString("tipo") ?: ""
            val categoria = documento.getString("categoria") ?: "Otros"
            val descripcion = documento.getString("descripcion") ?: ""
            val monto = documento.getLong("monto") ?: 0L

            val simbolo = if (tipo == "Ingreso") "+" else "-"

            historial.append("$categoria\n")
            historial.append("$simbolo${formatearDinero(monto)}")

            if (descripcion.isNotEmpty()) {
                historial.append(" · $descripcion")
            }

            historial.append("\n\n")
        }

        tvMovimientos.text = historial.toString().trim()
    }

    private fun formatearDinero(valor: Long): String {
        val formato = NumberFormat.getNumberInstance(
            Locale("es", "CO")
        )

        formato.maximumFractionDigits = 0

        return "$" + formato.format(valor) + " COP"
    }

    private fun irAlLogin() {
        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
