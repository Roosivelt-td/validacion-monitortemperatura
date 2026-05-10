package com.sigcpa.monitortemperatura

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var layoutFondo: ConstraintLayout
    private lateinit var txtTemperatura: TextView
    private lateinit var txtHumedad: TextView
    private lateinit var txtAlerta: TextView
    private lateinit var txtEstado: TextView
    private lateinit var btnConectar: Button

    // Bluetooth
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var conectado = false

    companion object {
        private const val REQUEST_BLUETOOTH = 1
        private val UUID_HC05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Conectar UI
        layoutFondo = findViewById(R.id.mainLayout)
        txtTemperatura = findViewById(R.id.txtTemperatura)
        txtHumedad = findViewById(R.id.txtHumedad)
        txtAlerta = findViewById(R.id.txtMensajeAlerta)
        txtEstado = findViewById(R.id.txtEstado)
        txtEstado.setTextColor(0xFFFF4444.toInt()) // Rojo por defecto
        btnConectar = findViewById(R.id.btnConectar)

        // Inicializar Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // Pedir permisos
        verificarPermisos()

        // Botón conectar
        btnConectar.setOnClickListener {
            conectarBluetooth()
        }

        // Intentar conectar automáticamente
        conectarBluetooth()
    }

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

    @SuppressLint("MissingPermission")
    private fun conectarBluetooth() {
        if (conectado) {
            Toast.makeText(this, "Ya está conectado", Toast.LENGTH_SHORT).show()
            return
        }

        txtEstado.text = "Bluetooth conectando..."

        val dispositivosVinculados: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        var dispositivoHC05: BluetoothDevice? = null

        for (dispositivo in dispositivosVinculados) {
            if (dispositivo.name.contains("HC-05", ignoreCase = true) ||
                dispositivo.name.contains("HC-06", ignoreCase = true)) {
                dispositivoHC05 = dispositivo
                break
            }
        }

        if (dispositivoHC05 == null) {
            txtEstado.text = "Bluetooth: HC-05 no encontrado"
            Toast.makeText(this, "Vincula el HC-05 en Ajustes > Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        try {
            bluetoothSocket = dispositivoHC05.createRfcommSocketToServiceRecord(UUID_HC05)
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            conectado = true
            txtEstado.text = "Bluetooth conectado"
            txtEstado.setTextColor(0xFF00FF00.toInt()) // Verde
            btnConectar.text = "Reconectar HC-05"
            Toast.makeText(this, "Conectado al HC-05", Toast.LENGTH_SHORT).show()
            iniciarLectura()

        } catch (e: IOException) {
            txtEstado.text = "Bluetooth desconectado"
            txtEstado.setTextColor(0xFFFF4444.toInt()) // Rojo
            Toast.makeText(this, "Error: Asegúrate de que el HC-05 esté conectado en Ajustes", Toast.LENGTH_LONG).show()
        }
    }

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
                            procesarDatoRecibido(datoRecibido)
                        }
                    }
                }
            } catch (e: IOException) {
                conectado = false
                runOnUiThread {
                    txtEstado.text = "Bluetooth desconectado"
                    txtEstado.setTextColor(0xFFFF4444.toInt()) // Rojo
                    btnConectar.text = "Conectar HC-05"
                    mostrarError()
                }
            }
        }
    }

    private fun procesarDatoRecibido(dato: String) {
        val datoLimpio = dato.trim()
        if (datoLimpio.isEmpty()) {
            mostrarError()
            return
        }

        // Suponiendo formato "temp,hum" o solo "temp"
        val partes = datoLimpio.split(",", ";", " ")
        val tempString = partes.getOrNull(0)
        val humString = partes.getOrNull(1)

        val temperatura = tempString?.toDoubleOrNull()
        val humedad = humString?.toDoubleOrNull()

        if (temperatura != null) {
            if (temperatura >= 35.0) {
                mostrarAlerta(temperatura)
            } else {
                mostrarNormal(temperatura)
            }
        } else {
            mostrarError()
        }

        if (humedad != null) {
            txtHumedad.text = "${humedad} %"
        }
    }

    private fun mostrarNormal(temp: Double) {
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTemp)
            ?.setCardBackgroundColor(0xFF2D2D44.toInt())
        txtTemperatura.text = "${temp} °C"
        txtAlerta.text = ""
        txtAlerta.visibility = android.view.View.GONE
    }

    private fun mostrarAlerta(temp: Double) {
        // Podríamos cambiar el color de la card de temperatura a rojo
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardTemp)
            ?.setCardBackgroundColor(0xFFFF4444.toInt())
        txtTemperatura.text = "${temp} °C"
        txtAlerta.text = "¡ALERTA: CALOR EXTREMO!"
        txtAlerta.visibility = android.view.View.VISIBLE
    }

    private fun mostrarError() {
        txtTemperatura.text = "Error"
        txtHumedad.text = "Error"
    }

    override fun onDestroy() {
        super.onDestroy()
        conectado = false
        try {
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Ignorar
        }
    }
}