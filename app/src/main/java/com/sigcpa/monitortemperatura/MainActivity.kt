package com.sigcpa.monitortemperatura

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.material.card.MaterialCardView
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // ============================================================
    // VIEWMODEL (MVVM)
    // ============================================================
    private val viewModel: MainViewModel by viewModels()

    // ============================================================
    // UI
    // ============================================================
    private lateinit var cardTemp: MaterialCardView
    private lateinit var txtTemperatura: TextView
    private lateinit var txtIconoClima: TextView
    private lateinit var txtHumedad: TextView
    private lateinit var txtAlerta: TextView
    private lateinit var txtEstado: TextView
    private lateinit var btnConectar: Button

    // ============================================================
    // BLUETOOTH
    // ============================================================
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var conectado = false

    // Activity Result Launcher para QR
    private val qrScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val qrText = data?.getStringExtra(QrScannerActivity.EXTRA_QR_RESULT) ?: ""
            if (qrText.isNotEmpty()) {
                Toast.makeText(this, "Escaneo completado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 1
        private val UUID_HC05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // ============================================================
    // ONCREATE
    // ============================================================
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Conectar UI
        cardTemp = findViewById(R.id.cardTemp)
        txtTemperatura = findViewById(R.id.txtTemperatura)
        txtIconoClima = findViewById(R.id.txtIconoClima)
        txtHumedad = findViewById(R.id.txtHumedad)
        txtAlerta = findViewById(R.id.txtMensajeAlerta)
        txtEstado = findViewById(R.id.txtEstado)
        btnConectar = findViewById(R.id.btnConectar)

        // Observar cambios del ViewModel
        observarViewModel()

        // Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        verificarPermisos()

        btnConectar.setOnClickListener { conectarBluetooth() }
        
        // Botón historial
        val btnHistorial: Button = findViewById(R.id.btnHistorial)
        btnHistorial.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }

        // Botón escanear QR
        val btnEscanearQR: Button = findViewById(R.id.btnEscanearQR)
        btnEscanearQR.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        // Botón generar QR
        val btnGenerarQR: Button = findViewById(R.id.btnGenerarQR)
        btnGenerarQR.setOnClickListener {
            val intent = Intent(this, GenerarQrActivity::class.java)
            startActivity(intent)
        }

        conectarBluetooth()
    }

    // ============================================================
    // OBSERVAR VIEWMODEL (La UI reacciona a cambios de datos)
    // ============================================================
    private fun observarViewModel() {
        // Observar temperatura
        viewModel.temperaturaTexto.observe(this, Observer { texto ->
            txtTemperatura.text = texto
        })

        // Observar icono de clima
        viewModel.iconoClima.observe(this, Observer { icono ->
            txtIconoClima.text = icono
        })

        // Observar color de texto
        viewModel.colorTexto.observe(this, Observer { color ->
            txtTemperatura.setTextColor(color)
        })

        // Observar humedad
        viewModel.humedadTexto.observe(this, Observer { texto ->
            txtHumedad.text = texto
        })

        // Observar mensaje de alerta
        viewModel.mensajeAlerta.observe(this, Observer { texto ->
            txtAlerta.text = texto
        })

        // Observar visibilidad de alerta
        viewModel.alertaVisible.observe(this, Observer { visible ->
            txtAlerta.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        })

        // Observar estado (cambia colores de la card)
        viewModel.estado.observe(this, Observer { estado ->
            when (estado) {
                MainViewModel.EstadoApp.NORMAL -> {
                    cardTemp.setCardBackgroundColor(0xFF2D2D44.toInt()) // Color normal
                }
                MainViewModel.EstadoApp.ALERTA -> {
                    cardTemp.setCardBackgroundColor(0xFFFF4444.toInt()) // Rojo
                }
                MainViewModel.EstadoApp.ERROR -> {
                    cardTemp.setCardBackgroundColor(0xFFFFAA00.toInt()) // Amarillo
                }
            }
        })
    }

    // ============================================================
    // PERMISOS
    // ============================================================
    private fun verificarPermisos() {
        val permisos = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val faltantes = permisos.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, faltantes.toTypedArray(), REQUEST_BLUETOOTH)
        }
    }

    // ============================================================
    // CONEXIÓN BLUETOOTH
    // ============================================================
    @SuppressLint("MissingPermission")
    private fun conectarBluetooth() {
        if (conectado) {
            Toast.makeText(this, "Ya está conectado", Toast.LENGTH_SHORT).show()
            return
        }

        txtEstado.text = "Bluetooth conectando..."

        val dispositivosVinculados: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var dispositivoHC05: BluetoothDevice? = null

        if (dispositivosVinculados != null) {
            for (d in dispositivosVinculados) {
                if (d.name.contains("HC-05", ignoreCase = true) ||
                    d.name.contains("HC-06", ignoreCase = true)) {
                    dispositivoHC05 = d
                    break
                }
            }
        }

        if (dispositivoHC05 == null) {
            txtEstado.text = "Bluetooth: HC-05 no encontrado"
            Toast.makeText(this, "Vincula el HC-05 en Ajustes", Toast.LENGTH_LONG).show()
            return
        }

        try {
            bluetoothSocket = dispositivoHC05.createRfcommSocketToServiceRecord(UUID_HC05)
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            conectado = true
            txtEstado.text = "Bluetooth conectado"
            txtEstado.setTextColor(0xFF00FF00.toInt())
            btnConectar.text = "Reconectar HC-05"
            Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show()
            iniciarLectura()
        } catch (e: IOException) {
            txtEstado.text = "Bluetooth desconectado"
            txtEstado.setTextColor(0xFFFF4444.toInt())
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show()
        }
    }

    // ============================================================
    // LECTURA BLUETOOTH (pasa datos al ViewModel)
    // ============================================================
    private fun iniciarLectura() {
        thread {
            val buffer = ByteArray(1024)
            var bytesLeidos: Int
            try {
                while (conectado) {
                    bytesLeidos = inputStream?.read(buffer) ?: -1
                    if (bytesLeidos > 0) {
                        val datoRecibido = String(buffer, 0, bytesLeidos)
                        runOnUiThread {
                            // Pasamos el dato al ViewModel (él decide qué hacer)
                            viewModel.procesarDatoRecibido(datoRecibido)
                        }
                    }
                }
            } catch (e: IOException) {
                conectado = false
                runOnUiThread {
                    txtEstado.text = "Bluetooth desconectado"
                    txtEstado.setTextColor(0xFFFF4444.toInt())
                    btnConectar.text = "Conectar HC-05"
                }
            }
        }
    }

    // ============================================================
    // MÉTODO PARA PRUEBAS
    // ============================================================
    fun simularRecepcionDato(dato: String) {
        viewModel.procesarDatoRecibido(dato)
    }

    // ============================================================
    // CERRAR CONEXIÓN
    // ============================================================
    override fun onDestroy() {
        super.onDestroy()
        conectado = false
        try {
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) { }
    }
}
