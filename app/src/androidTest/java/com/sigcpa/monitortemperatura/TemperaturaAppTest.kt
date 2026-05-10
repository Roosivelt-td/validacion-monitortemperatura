package com.sigcpa.monitortemperatura

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemperaturaAppTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // ============================================================
    // TC-01: Temperatura normal
    // ============================================================
    @Test
    fun testTemperaturaNormal_MuestraCorrectamente() {
        // Simular recepción de "22"
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("22")
        }

        // Verificar que el texto de temperatura es "22.0 °C"
        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("22.0 °C")))

        // Verificar que el mensaje de alerta NO está visible
        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // ============================================================
    // TC-02: Calor extremo (ROJO + ALERTA) - Valor límite 35°C
    // ============================================================
    @Test
    fun testCalorExtremo_MuestraRojoYAlerta() {
        // Simular recepción de "35"
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("35")
        }

        // Verificar que muestra "35.0 °C"
        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("35.0 °C")))

        // Verificar que el mensaje de alerta está visible
        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(isDisplayed()))

        // Verificar el texto exacto de la alerta
        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(withText("¡ALERTA: CALOR EXTREMO!")))
    }

    // ============================================================
    // TC-03: Entrada vacía (ERROR)
    // ============================================================
    @Test
    fun testEntradaVacia_MuestraError() {
        // Simular recepción de cadena vacía
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("")
        }

        // Verificar que el texto de temperatura muestra error
        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("Error")))
    }

    // ============================================================
    // TC-04: Entrada no numérica (ERROR)
    // ============================================================
    @Test
    fun testEntradaCorrupta_MuestraError() {
        // Simular recepción de caracteres "err"
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("err")
        }

        // Verificar que el texto de temperatura muestra error
        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("Error")))
    }
}
