package com.sigcpa.monitortemperatura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.obtenerInstancia(application)
    private val lecturaDao = database.lecturaDao()

    private val _temperaturaTexto = MutableLiveData<String>()
    val temperaturaTexto: LiveData<String> = _temperaturaTexto

    private val _humedadTexto = MutableLiveData<String>()
    val humedadTexto: LiveData<String> = _humedadTexto

    private val _mensajeAlerta = MutableLiveData<String>()
    val mensajeAlerta: LiveData<String> = _mensajeAlerta

    private val _alertaVisible = MutableLiveData<Boolean>()
    val alertaVisible: LiveData<Boolean> = _alertaVisible

    private val _estado = MutableLiveData<EstadoApp>()
    val estado: LiveData<EstadoApp> = _estado

    private val _iconoClima = MutableLiveData<String>()
    val iconoClima: LiveData<String> = _iconoClima

    private val _colorTexto = MutableLiveData<Int>()
    val colorTexto: LiveData<Int> = _colorTexto

    val ultimasLecturas: LiveData<List<LecturaEntity>> = lecturaDao.obtenerUltimas10()

    enum class EstadoApp {
        NORMAL, ALERTA, ERROR
    }

    private var enPausaAlerta = false

    fun procesarDatoRecibido(dato: String) {
        if (enPausaAlerta) return

        val datoLimpio = dato.trim()
        if (datoLimpio.isEmpty()) {
            mostrarError()
            return
        }

        val partes = datoLimpio.split(",", ";", " ")
        val tempString = partes.getOrNull(0)
        val humString = partes.getOrNull(1)

        val temperatura = tempString?.toDoubleOrNull()
        val humedad = humString?.toDoubleOrNull()

        if (temperatura == null) {
            mostrarError()
            return
        }

        if (humedad != null) {
            _humedadTexto.value = "${humedad} %"
        }

        // Guardar en BD (en hilo de fondo porque el DAO de Java no es suspend)
        guardarEnBaseDeDatos(temperatura, humedad ?: 0.0)

        // Definir color e icono según rangos
        val (color, icono) = obtenerColorEIcono(temperatura)
        _colorTexto.value = color
        _iconoClima.value = icono

        when {
            temperatura >= 35.0 -> {
                mostrarAlerta(temperatura)
                iniciarPausaAlerta()
            }
            else -> mostrarNormal(temperatura)
        }
    }

    private fun obtenerColorEIcono(temp: Double): Pair<Int, String> {
        return when {
            temp < 10.0 -> Pair(0xFF0088FF.toInt(), "🥶") // Azul
            temp < 20.0 -> Pair(0xFF00CCFF.toInt(), "😎") // Celeste
            temp < 35.0 -> Pair(0xFF00FF00.toInt(), "🌤️") // Verde
            temp <= 40.0 -> Pair(0xFFFF8800.toInt(), "🔥") // Naranja
            else -> Pair(0xFFFF0000.toInt(), "🚒")       // Rojo
        }
    }

    private fun guardarEnBaseDeDatos(temperatura: Double, humedad: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val lectura = LecturaEntity(temperatura, humedad)
            lecturaDao.insertar(lectura)
        }
    }

    fun borrarHistorial() {
        viewModelScope.launch(Dispatchers.IO) {
            lecturaDao.borrarTodo()
        }
    }

    private fun iniciarPausaAlerta() {
        enPausaAlerta = true
        viewModelScope.launch {
            delay(2000)
            enPausaAlerta = false
        }
    }

    private fun mostrarNormal(temp: Double) {
        _temperaturaTexto.value = "${temp} °C"
        _mensajeAlerta.value = ""
        _alertaVisible.value = false
        _estado.value = EstadoApp.NORMAL
    }

    private fun mostrarAlerta(temp: Double) {
        _temperaturaTexto.value = "${temp} °C"
        _mensajeAlerta.value = "¡ALERTA: CALOR EXTREMO!"
        _alertaVisible.value = true
        _estado.value = EstadoApp.ALERTA
    }

    private fun mostrarError() {
        _temperaturaTexto.value = "Error"
        _humedadTexto.value = "Error"
        _mensajeAlerta.value = ""
        _alertaVisible.value = false
        _estado.value = EstadoApp.ERROR
    }

    init {
        _temperaturaTexto.value = "-- °C"
        _humedadTexto.value = "-- %"
        _mensajeAlerta.value = ""
        _alertaVisible.value = false
        _estado.value = EstadoApp.NORMAL
    }
}
