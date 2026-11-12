package com.dragonzeta.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class SosVolumeService : AccessibilityService() {

    private var upPressCount = 0
    private var lastUpPress = 0L

    private val pressInterval = 1000L
    private var lastActivation = 0L

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
            return false
        }

        val now = SystemClock.elapsedRealtime()

        if (now - lastUpPress > pressInterval) {
            upPressCount = 0
        }

        upPressCount++
        lastUpPress = now

        if (upPressCount >= 2) {

            upPressCount = 0
            lastUpPress = 0L

            if (now - lastActivation > 1500L) {
                lastActivation = now

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
            }
        }

        // Permite que el volumen siga funcionando normalmente.
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesita procesar eventos de pantalla.
    }

    override fun onInterrupt() {
        // El servicio fue interrumpido.
    }
}
