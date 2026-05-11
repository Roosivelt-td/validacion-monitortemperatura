package com.sigcpa.monitortemperatura

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerarQrActivity : AppCompatActivity() {

    private lateinit var etEntrada: EditText
    private lateinit var imgQR: ImageView
    private lateinit var btnGenerar: Button
    private lateinit var btnVolver: Button
    private lateinit var txtInfoQR: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generar_qr)

        etEntrada = findViewById(R.id.etCargaId)
        imgQR = findViewById(R.id.imgQR)
        btnGenerar = findViewById(R.id.btnGenerarQR)
        btnVolver = findViewById(R.id.btnVolverGenerar)
        txtInfoQR = findViewById(R.id.txtInfoQR)

        // Cambiar el hint para la simulación
        etEntrada.hint = "Ingresa temperatura (Simulación)"

        // REVISAR SI VIENE DEL HISTORIAL (DATOS REALES)
        val datosQRReal = intent.getStringExtra("datos_qr")
        val temperaturaReal = intent.getDoubleExtra("temperatura", 0.0)
        val humedadReal = intent.getDoubleExtra("humedad", 0.0)
        val horaReal = intent.getStringExtra("hora")

        if (datosQRReal != null) {
            // Caso 2: Datos Reales del Historial
            etEntrada.visibility = View.GONE
            btnGenerar.visibility = View.GONE
            txtInfoQR.visibility = View.VISIBLE
            
            txtInfoQR.text = "Lectura Real: ${temperaturaReal}°C | ${humedadReal}% | $horaReal"
            generarQR(datosQRReal)
            Toast.makeText(this, "QR generado con datos reales", Toast.LENGTH_SHORT).show()
        }

        btnGenerar.setOnClickListener {
            // Caso 1: Simulación desde el botón del Main
            val entrada = etEntrada.text.toString().trim()
            if (entrada.isEmpty()) {
                Toast.makeText(this, "Ingresa un valor para simular", Toast.LENGTH_SHORT).show()
            } else {
                val temp = entrada.toDoubleOrNull()
                if (temp == null) {
                    Toast.makeText(this, "Ingresa un número válido", Toast.LENGTH_SHORT).show()
                } else {
                    // Simulación de humedad y hora
                    val humedadSim = (40..80).random() + (0..99).random() / 100.0
                    val horaSim = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    
                    // Formato descriptivo y legible por humanos
                    val textoQRSimulado = "Simulación Sensor\nTemp: ${temp}°C\nHum: ${"%.1f".format(humedadSim)}%\nHora: $horaSim"
                    
                    generarQR(textoQRSimulado)
                    
                    Toast.makeText(
                        this,
                        "SIMULACIÓN: ${temp}°C, ${"%.1f".format(humedadSim)}%, $horaSim",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun generarQR(texto: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            imgQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
