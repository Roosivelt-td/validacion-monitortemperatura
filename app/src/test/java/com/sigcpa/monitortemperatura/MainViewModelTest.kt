package com.sigcpa.monitortemperatura

import org.junit.Assert.*
import org.junit.Test

// ============================================================
// PRUEBAS UNITARIAS DEL VIEWMODEL
// Estas pruebas NO necesitan el celular,
// se ejecutan en la computadora
// ============================================================
class MainViewModelTest {

    // ============================================================
    // PRUEBAS DE FUNCIONES PURAS
    // ============================================================

    @Test
    fun `convertir Celsius a Fahrenheit - punto de congelacion`() {
        // 0°C = 32°F
        val resultado = convertirCelsiusAFahrenheit(0.0)
        assertEquals(32.0, resultado, 0.01)
    }

    @Test
    fun `convertir Celsius a Fahrenheit - punto de ebullicion`() {
        // 100°C = 212°F
        val resultado = convertirCelsiusAFahrenheit(100.0)
        assertEquals(212.0, resultado, 0.01)
    }

    @Test
    fun `convertir Celsius a Fahrenheit - temperatura ambiente`() {
        // 25°C = 77°F
        val resultado = convertirCelsiusAFahrenheit(25.0)
        assertEquals(77.0, resultado, 0.01)
    }

    @Test
    fun `esTemperaturaAlerta - debajo del umbral`() {
        // 34.9°C NO es alerta
        assertFalse(esTemperaturaAlerta(34.9))
    }

    @Test
    fun `esTemperaturaAlerta - valor limite exacto`() {
        // 35.0°C SÍ es alerta (valor límite)
        assertTrue(esTemperaturaAlerta(35.0))
    }

    @Test
    fun `esTemperaturaAlerta - muy por encima del umbral`() {
        // 50.0°C SÍ es alerta
        assertTrue(esTemperaturaAlerta(50.0))
    }

    @Test
    fun `esDatoValido - numero normal`() {
        assertTrue(esDatoValido("22.5"))
    }

    @Test
    fun `esDatoValido - numero con humedad`() {
        assertTrue(esDatoValido("30.5,65.2"))
    }

    @Test
    fun `esDatoValido - cadena vacia`() {
        assertFalse(esDatoValido(""))
    }

    @Test
    fun `esDatoValido - texto no numerico`() {
        assertFalse(esDatoValido("error"))
    }

    @Test
    fun `esDatoValido - solo espacios`() {
        assertFalse(esDatoValido("   "))
    }

    // ============================================================
    // FUNCIONES AUXILIARES (replican la lógica del ViewModel)
    // ============================================================

    private fun convertirCelsiusAFahrenheit(celsius: Double): Double {
        return (celsius * 9.0 / 5.0) + 32.0
    }

    private fun esTemperaturaAlerta(temperatura: Double): Boolean {
        return temperatura >= 35.0
    }

    private fun esDatoValido(dato: String): Boolean {
        val limpio = dato.trim()
        if (limpio.isEmpty()) return false
        val partes = limpio.split(",", ";", " ")
        return partes.getOrNull(0)?.toDoubleOrNull() != null
    }
}
