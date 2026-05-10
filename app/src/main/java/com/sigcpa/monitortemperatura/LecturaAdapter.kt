package com.sigcpa.monitortemperatura

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// ADAPTADOR: Convierte datos de Room en filas visuales
// ============================================================
class LecturaAdapter(
    private var lecturas: List<LecturaEntity>
) : RecyclerView.Adapter<LecturaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTemp: TextView = view.findViewById(R.id.txtTempHistorial)
        val txtIcono: TextView = view.findViewById(R.id.txtIconoHistorial)
        val txtHum: TextView = view.findViewById(R.id.txtHumHistorial)
        val txtHora: TextView = view.findViewById(R.id.txtHoraHistorial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lectura, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lectura = lecturas[position]
        val temp = lectura.temperatura

        // Mostrar temperatura
        holder.txtTemp.text = "$temp °C"
        
        // Color e Icono según rangos
        val (color, icono) = when {
            temp < 10.0 -> Pair(0xFF0088FF.toInt(), "🥶") // Azul
            temp < 20.0 -> Pair(0xFF00CCFF.toInt(), "😎") // Celeste
            temp < 35.0 -> Pair(0xFF00FF00.toInt(), "🌤️") // Verde
            temp <= 40.0 -> Pair(0xFFFF8800.toInt(), "🔥") // Naranja
            else -> Pair(0xFFFF0000.toInt(), "🚒")       // Rojo
        }
        
        holder.txtTemp.setTextColor(color)
        holder.txtIcono.text = icono

        // Mostrar humedad
        holder.txtHum.text = "${lectura.humedad} %"

        // Mostrar hora formateada (HH:mm:ss)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        holder.txtHora.text = sdf.format(Date(lectura.timestamp))
    }

    override fun getItemCount(): Int = lecturas.size

    // Actualizar la lista cuando lleguen nuevos datos
    fun actualizarLista(nuevaLista: List<LecturaEntity>) {
        lecturas = nuevaLista
        notifyDataSetChanged()
    }
}