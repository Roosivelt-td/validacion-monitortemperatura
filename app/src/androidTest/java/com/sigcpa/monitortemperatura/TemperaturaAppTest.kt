package com.sigcpa.monitortemperatura

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemperaturaAppTest {

    // ============================================================
    // OTORGAR PERMISOS AUTOMÁTICAMENTE DURANTE LAS PRUEBAS
    // ============================================================
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // ============================================================
    // REGLA DE ACTIVITY
    // ============================================================
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // ============================================================
    // TC-01: Temperatura normal (22°C)
    // ============================================================
    @Test
    fun testTemperaturaNormal_MuestraCorrectamente() {
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("22")
        }

        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("22.0 °C")))

        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // ============================================================
    // TC-02: Calor extremo (35°C) - Valor límite
    // ============================================================
    @Test
    fun testCalorExtremo_MuestraRojoYAlerta() {
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("35")
        }

        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("35.0 °C")))

        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(isDisplayed()))

        onView(withId(R.id.txtMensajeAlerta))
            .check(matches(withText("¡ALERTA: CALOR EXTREMO!")))
    }

    // ============================================================
    // TC-03: Entrada vacía → Error
    // ============================================================
    @Test
    fun testEntradaVacia_MuestraError() {
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("")
        }

        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("Error")))
    }

    // ============================================================
    // TC-04: Entrada corrupta → Error
    // ============================================================
    @Test
    fun testEntradaCorrupta_MuestraError() {
        activityRule.scenario.onActivity { activity ->
            activity.simularRecepcionDato("err")
        }

        onView(withId(R.id.txtTemperatura))
            .check(matches(withText("Error")))
    }
}