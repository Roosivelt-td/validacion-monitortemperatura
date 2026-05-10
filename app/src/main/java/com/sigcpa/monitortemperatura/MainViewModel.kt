package com.sigcpa.monitortemperatura

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// VIEWMODEL: Contiene toda la lógica de negocio
//            La UI (Activity) solo observa estos datos
// ============================================================
class MainViewModel : ViewModel() {

    // ---- Datos que la UI va a observar ----

    // LiveData para la temperatura formateada (ej: "32.5 °C")
    private val _temperaturaTexto = MutableLiveData<String>()
    val temperaturaTexto: LiveData<String> = _temperaturaTexto

    // LiveData para la humedad formateada (ej: "54.2 %")
    private val _humedadTexto = MutableLiveData<String>()
    val humedadTexto: LiveData<String> = _humedadTexto

    // LiveData para el mensaje de alerta (vacío si no hay alerta)
    private val _mensajeAlerta = MutableLiveData<String>()
    val mensajeAlerta: LiveData<String> = _mensajeAlerta

    // LiveData para visibilidad del mensaje de alerta
    private val _alertaVisible = MutableLiveData<Boolean>()
    val alertaVisible: LiveData<Boolean> = _alertaVisible

    // LiveData para el tipo de estado (NORMAL, ALERTA, ERROR)
    private val _estado = MutableLiveData<EstadoApp>()
    val estado: LiveData<EstadoApp> = _estado

    // ---- Estados posibles de la app ----
    enum class EstadoApp {
        NORMAL,     // 0-34°C → Verde
        ALERTA,     // ≥35°C  → Rojo
        ERROR       // Dato inválido → Amarillo
    }

    // ============================================================
    // CONTROL DE PAUSA DE ALERTA
    // ============================================================
    // Cuando se activa la alerta, se ignoran nuevos datos durante 2 segundos
    // para que el color rojo permanezca visible en la pantalla
    private var enPausaAlerta = false

    // ============================================================
    // MÉTODO PRINCIPAL: Procesa el dato crudo del Bluetooth
    // ============================================================
    fun procesarDatoRecibido(dato: String) {

        // Si estamos en pausa de alerta, ignoramos nuevos datos
        // Esto mantiene el color rojo visible por 2 segundos
        if (enPausaAlerta) return

        val datoLimpio = dato.trim()

        // Si está vacío → ERROR
        if (datoLimpio.isEmpty()) {
            mostrarError()
            return
        }

        // Intentar separar temperatura,humedad
        val partes = datoLimpio.split(",", ";", " ")
        val tempString = partes.getOrNull(0)
        val humString = partes.getOrNull(1)

        val temperatura = tempString?.toDoubleOrNull()
        val humedad = humString?.toDoubleOrNull()

        // Si la temperatura no es un número → ERROR
        if (temperatura == null) {
            mostrarError()
            return
        }

        // Si hay humedad, mostrarla
        if (humedad != null) {
            _humedadTexto.value = "${humedad} %"
        }

        // Evaluar el rango de temperatura
        when {
            temperatura >= 35.0 -> {
                mostrarAlerta(temperatura)
                // Activar pausa: durante 2 segundos se ignoran nuevos datos
                iniciarPausaAlerta()
            }
            else -> mostrarNormal(temperatura)
        }
    }

    // ============================================================
    // PAUSA DE ALERTA
    // ============================================================
    private fun iniciarPausaAlerta() {
        enPausaAlerta = true
        viewModelScope.launch {
            delay(2000) // Esperar 2000 milisegundos = 2 segundos
            enPausaAlerta = false
        }
    }

    // ============================================================
    // COMPORTAMIENTOS
    // ============================================================

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

    // ---- Valores iniciales al crear el ViewModel ----
    init {
        _temperaturaTexto.value = "-- °C"
        _humedadTexto.value = "-- %"
        _mensajeAlerta.value = ""
        _alertaVisible.value = false
        _estado.value = EstadoApp.NORMAL
    }
}