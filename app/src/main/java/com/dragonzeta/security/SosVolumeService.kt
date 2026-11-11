package com.dragonzeta.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class SosVolumeService : AccessibilityService() {

    private var upPressCount = 0
    private var downPressCount = 0

    private var lastUpPress = 0L
    private var lastDownPress = 0L

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

        val isVolumeUp =
            event.keyCode == KeyEvent.KEYCODE_VOLUME_UP

        val isVolumeDown =
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

        if (!isVolumeUp && !isVolumeDown) {
            return false
        }

        // Solo contamos la pulsación inicial.
        if (event.action != KeyEvent.ACTION_DOWN ||
            event.repeatCount > 0
        ) {
            return false
        }

        val now = SystemClock.elapsedRealtime()

        // DOS PULSACIONES DE VOLUMEN ARRIBA = SOS
        if (isVolumeUp) {

            downPressCount = 0
            lastDownPress = 0L

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
        }

        // TRES PULSACIONES DE VOLUMEN ABAJO = GPS
        if (isVolumeDown) {

            upPressCount = 0
            lastUpPress = 0L

            if (now - lastDownPress > pressInterval) {
                downPressCount = 0
            }

            downPressCount++
            lastDownPress = now

            if (downPressCount >= 3) {

                downPressCount = 0
                lastDownPress = 0L

                if (now - lastActivation > 1500L) {
                    lastActivation = now
                    SosTrackingService.startTracking(this)
                }
            }
        }

        // IMPORTANTE: permite que Android cambie el volumen.
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesita procesar eventos de pantalla.
    }

    override fun onInterrupt() {
        // El servicio fue interrumpido.
    }
}
