package com.sigcpa.monitortemperatura

// ============================================================
// MODELO: Representa una lectura del sensor
// ============================================================
data class LecturaSensor(
    val temperatura: Double?,
    val humedad: Double?,
    val esError: Boolean = false
)