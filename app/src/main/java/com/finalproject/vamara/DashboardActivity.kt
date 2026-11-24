package com.finalproject.vamara

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
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
    private lateinit var tvPorcentajeAhorro: TextView
    private lateinit var tvEstadoFinanciero: TextView
    private lateinit var progresoAhorro: ProgressBar

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

        findViewById<Button>(R.id.btnMisMetas)
            .setOnClickListener {
                startActivity(
                    Intent(this, AhorroActivity::class.java)
                )
            }


        // Referencias a los elementos del dashboard
        tvSaldo = findViewById(R.id.tvSaldo)
        tvIngresos = findViewById(R.id.tvIngresos)
        tvGastos = findViewById(R.id.tvGastos)
        tvMovimientos = findViewById(R.id.tvListaMovimientos)

        // Elementos del análisis financiero
        tvPorcentajeAhorro = findViewById(R.id.tvPorcentajeAhorro)
        tvEstadoFinanciero = findViewById(R.id.tvEstadoFinanciero)
        progresoAhorro = findViewById(R.id.progresoAhorro)

        val nombre = usuario.displayName ?: "Usuario"

        findViewById<TextView>(R.id.tvSaludo).text =
            "Hola, $nombre"

        // Abrir formulario de movimientos
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

        // Escuchar cambios en Firestore en tiempo real
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

        val movimientos = snapshot.documents.sortedByDescending {
            it.getTimestamp("fecha")?.seconds ?: 0L
        }

        val historial = StringBuilder()

        // Calcular ingresos y gastos
        for (documento in movimientos) {

            val tipo = documento.getString("tipo") ?: ""
            val monto = documento.getLong("monto") ?: 0L

            if (tipo == "Ingreso") {
                ingresos += monto
            } else if (tipo == "Gasto") {
                gastos += monto
            }
        }

        // CÁLCULO 1: SALDO DISPONIBLE
        val saldo = ingresos - gastos

        tvSaldo.text = formatearDinero(saldo)
        tvIngresos.text = formatearDinero(ingresos)
        tvGastos.text = formatearDinero(gastos)

        // CÁLCULO 2: PORCENTAJE DE AHORRO
        val porcentajeAhorro = if (ingresos > 0L) {
            saldo.toDouble() / ingresos.toDouble() * 100.0
        } else {
            0.0
        }

        val porcentajeTexto = String.format(
            Locale("es", "CO"),
            "%.1f%%",
            porcentajeAhorro
        )

        tvPorcentajeAhorro.text = porcentajeTexto

        // La barra siempre debe estar entre 0 y 100
        progresoAhorro.progress = porcentajeAhorro
            .coerceIn(0.0, 100.0)
            .toInt()

        // CÁLCULO 3: ESTADO FINANCIERO
        val estado = when {
            ingresos == 0L && gastos == 0L ->
                "Registra tus movimientos para comenzar."

            saldo < 0L ->
                "Atención: estás gastando más de lo que recibes."

            ingresos == 0L ->
                "Tienes gastos, pero aún no registras ingresos."

            saldo == 0L ->
                "Estás utilizando el 100% de tus ingresos."

            porcentajeAhorro >= 30.0 ->
                "¡Excelente! Estás conservando una buena parte de tus ingresos."

            else ->
                "Vas por buen camino. Intenta aumentar tu ahorro."
        }

        tvEstadoFinanciero.text = estado

        // Mostrar últimos movimientos
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
