
package com.dragonzeta.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class SosVolumeService : AccessibilityService() {

    private var lastPressTime = 0L
    private val doublePressInterval = 700L

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        Toast.makeText(
            this,
            "DRAGÓN ZETA SOS LISTO",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {

        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return false
        }

        if (event.action != KeyEvent.ACTION_DOWN ||
            event.repeatCount > 0
        ) {
            return true
        }

        val now = System.currentTimeMillis()

        if (now - lastPressTime <= doublePressInterval &&
            lastPressTime != 0L
        ) {
            lastPressTime = 0L

            val number = getSharedPreferences(
                "dragon_zeta",
                MODE_PRIVATE
            ).getString("sos_phone", "") ?: ""

            if (number.isNotBlank()) {
                SosEmergency.start(this, number)
            } else {
                Toast.makeText(
                    this,
                    "Primero configura tu número SOS",
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {
            lastPressTime = now
        }

        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesita procesar eventos de pantalla.
    }

    override fun onInterrupt() {
        // El servicio fue interrumpido.
    }
}

