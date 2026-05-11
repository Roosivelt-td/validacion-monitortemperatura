package com.sigcpa.monitortemperatura

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var layoutFondo: ConstraintLayout
    private lateinit var panelResultados: LinearLayout
    private lateinit var txtIcono: TextView
    private lateinit var txtEstado: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtHum: TextView
    private lateinit var txtHora: TextView
    private lateinit var btnVolver: Button
    private lateinit var btnNuevo: Button
    
    private var qrDetectado = false

    companion object {
        private const val REQUEST_CAMERA = 100
        const val EXTRA_QR_RESULT = "qr_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        // Inicializar UI
        previewView = findViewById(R.id.previewView)
        layoutFondo = findViewById(R.id.layoutScanner)
        panelResultados = findViewById(R.id.panelResultados)
        txtIcono = findViewById(R.id.txtResultadoIcono)
        txtEstado = findViewById(R.id.txtResultadoEstado)
        txtTemp = findViewById(R.id.txtResultadoTemp)
        txtHum = findViewById(R.id.txtResultadoHum)
        txtHora = findViewById(R.id.txtResultadoHora)
        btnVolver = findViewById(R.id.btnCerrarEscaner)
        btnNuevo = findViewById(R.id.btnEscanearNuevo)

        btnVolver.setOnClickListener { finish() }

        // LÓGICA PARA ESCANEAR OTRO QR (RESET)
        btnNuevo.setOnClickListener {
            reiniciarEscaneo()
        }

        if (tienePermisoCamara()) {
            iniciarCamara()
        } else {
            pedirPermisoCamara()
        }
    }

    private fun reiniciarEscaneo() {
        qrDetectado = false
        btnNuevo.visibility = View.GONE
        
        // Resetear UI a estado inicial
        txtIcono.text = "🔍"
        txtEstado.text = "ESPERANDO QR..."
        txtEstado.setTextColor(Color.WHITE)
        txtTemp.text = "-- °C"
        txtHum.text = "Humedad: --%"
        txtHora.text = "Hora: --:--:--"
        
        panelResultados.setBackgroundColor(Color.parseColor("#1a1a2e"))
        layoutFondo.setBackgroundColor(Color.BLACK)
        
        Toast.makeText(this, "Escáner reactivado", Toast.LENGTH_SHORT).show()
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun pedirPermisoCamara() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                if (!qrDetectado) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        val scanner = BarcodeScanning.getClient()
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val texto = barcode.displayValue ?: ""
                                    if (texto.isNotEmpty()) {
                                        qrDetectado = true
                                        runOnUiThread { procesarTextoQR(texto) }
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else { imageProxy.close() }
                } else { imageProxy.close() }
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun procesarTextoQR(texto: String) {
        // Extraer números usando Regex
        val regexNumeros = "([+-]?\\d+\\.?\\d*)".toRegex()
        val coincidencias = regexNumeros.findAll(texto).toList()
        val tempC = coincidencias.getOrNull(0)?.value?.toDoubleOrNull()
        val humedad = coincidencias.getOrNull(1)?.value?.toDoubleOrNull()
        
        val regexHora = "(\\d{2}:\\d{2}:\\d{2})".toRegex()
        val horaMatch = regexHora.find(texto)?.value ?: "--:--:--"

        if (tempC == null) {
            txtTemp.text = "QR Desconocido"
            txtEstado.text = "FORMATO NO RECONOCIDO"
            txtIcono.text = "❓"
            panelResultados.setBackgroundColor(Color.parseColor("#333333"))
            btnNuevo.visibility = View.VISIBLE
            return
        }

        val tempF = (tempC * 9.0 / 5.0) + 32.0
        txtTemp.text = "${tempC}°C / ${"%.1f".format(tempF)}°F"
        txtHum.text = "Humedad: ${humedad ?: "--"}%"
        txtHora.text = "Hora de Lectura: $horaMatch"

        // Mostrar botón para nuevo escaneo
        btnNuevo.visibility = View.VISIBLE

        // DISEÑO DINÁMICO SEGÚN EL CLIMA
        when {
            tempC >= 35.0 -> { // CALOR
                txtIcono.text = "🔥"
                txtEstado.text = "ALERTA: CALOR EXTREMO"
                txtEstado.setTextColor(Color.WHITE)
                panelResultados.setBackgroundColor(Color.parseColor("#CC0000")) 
                layoutFondo.setBackgroundColor(Color.parseColor("#FF4444"))
            }
            tempC >= 0.0 -> { // NORMAL
                txtIcono.text = "🌤️"
                txtEstado.text = "ESTADO: CLIMA SEGURO"
                txtEstado.setTextColor(Color.WHITE)
                panelResultados.setBackgroundColor(Color.parseColor("#008800"))
                layoutFondo.setBackgroundColor(Color.parseColor("#44FF44"))
            }
            else -> { // FRÍO
                txtIcono.text = "🥶"
                txtEstado.text = "ALERTA: CONGELACIÓN"
                txtEstado.setTextColor(Color.BLACK)
                panelResultados.setBackgroundColor(Color.parseColor("#FFCC00"))
                layoutFondo.setBackgroundColor(Color.parseColor("#FFFF44"))
            }
        }
        
        Toast.makeText(this, "Datos certificados correctamente", Toast.LENGTH_SHORT).show()
    }
}
