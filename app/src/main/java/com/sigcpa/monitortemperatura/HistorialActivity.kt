package com.sigcpa.monitortemperatura

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: LecturaAdapter
    private lateinit var btnBorrar: Button
    private lateinit var btnVolver: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        recycler = findViewById(R.id.recyclerHistorial)
        btnBorrar = findViewById(R.id.btnBorrarHistorial)
        btnVolver = findViewById(R.id.btnVolver)

        // Configurar RecyclerView
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = LecturaAdapter(emptyList())
        recycler.adapter = adapter

        // Observar datos de Room en tiempo real
        val database = AppDatabase.obtenerInstancia(this)
        database.lecturaDao().obtenerTodas().observe(this) { lecturas ->
            adapter.actualizarLista(lecturas)
        }

        // Botón borrar
        btnBorrar.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                database.lecturaDao().borrarTodo()
                // No hace falta actualizarLista aquí, el observer lo hará solo
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistorialActivity, "Historial borrado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Botón volver
        btnVolver.setOnClickListener {
            finish() // Cierra esta pantalla y vuelve a MainActivity
        }
    }

    // Ya no necesitamos cargarHistorial ni onResume porque LiveData es automático
}
