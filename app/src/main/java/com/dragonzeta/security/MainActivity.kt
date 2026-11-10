package com.dragonzeta.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {

    private val bg = Color.rgb(4, 10, 23)
    private val cyan = Color.rgb(44, 216, 255)
    private val green = Color.rgb(30, 190, 90)
    private val red = Color.rgb(220, 55, 55)

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var countdown = 5
    private var pendingNumber = ""
    private var counting = false

    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    private val countdownTask = object : Runnable {
        override fun run() {
            if (!counting) return

            countdown--
            countdownText.text = "Llamada en $countdown segundos"

            if (countdown <= 0) {
                counting = false
                countdownText.text = "Iniciando llamada..."
                callEmergency()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun panel(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.rgb(10, 25, 45))
            cornerRadius = dp(18).toFloat()
            setStroke(dp(2), color)
        }
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(5), dp(7), dp(5), dp(7))
    }

    private fun button(
        text: String,
        color: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = panel(color)
            setOnClickListener { action() }
        }
    }

    private fun base(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(bg)
        }
    }

    private fun addOption(
        root: LinearLayout,
        title: String,
        description: String,
        key: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(cyan)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        card.addView(label(title, 20f, cyan, true))
        card.addView(label(description, 13f, Color.LTGRAY))

        var paid = prefs.getBoolean("${key}_paid", false)
        var active = prefs.getBoolean("${key}_active", false)

        if (!paid) active = false

        val status = label("", 15f, Color.WHITE, true)

        fun updateStatus() {
            status.text = if (active && paid) {
                "🟢 SEGURIDAD MÁXIMA"
            } else if (!paid) {
                "🔴 OPCIÓN NO PAGADA"
            } else {
                "🔴 DESACTIVADO"
            }
            status.setTextColor(
                if (active && paid) green else red
            )
        }

        updateStatus()
        card.addView(status)

        val paymentButton = button("", amberColor()) {
            paid = !paid
            if (!paid) active = false

            prefs.edit()
                .putBoolean("${key}_paid", paid)
                .putBoolean("${key}_active", active)
                .apply()

            paymentButtonText()
        }

        fun refresh() {
            paymentButton.text =
                if (paid) "Marcar como NO pagada"
                else "Marcar como PAGADA"

            updateStatus()
        }

        fun paymentButtonText() {
            refresh()
        }

        card.addView(paymentButton)

        val activationButton = button("", green) {
            if (!paid) {
                toast("Esta opción todavía no está pagada.")
            } else {
                active = !active
                prefs.edit()
                    .putBoolean("${key}_active", active)
                    .apply()
                updateStatus()
                activationButton.text =
                    if (active) "DESACTIVAR" else "ACTIVAR"
            }
        }

        activationButton.text =
            if (active) "DESACTIVAR" else "ACTIVAR"

        card.addView(activationButton)
        refresh()

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, dp(8), 0, dp(8))
        root.addView(card, lp)
    }

    private fun amberColor() = Color.rgb(220, 145, 45)

    private fun showHome() {
        val root = base()

        root.addView(
            label("🐉 DRAGÓN ZETA", 30f, cyan, true)
        )
        root.addView(
            label("SISTEMA DE SEGURIDAD PERSONAL", 13f, Color.WHITE, true)
        )

        // SOS
        val sosCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(red)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        sosCard.addView(
            label("🆘 SOS – EMERGENCIA", 21f, red, true)
        )

        sosCard.addView(
            label("Número de emergencia", 14f, Color.WHITE)
        )

        val phone = EditText(this).apply {
            hint = "+51 999 999 999"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            setText(prefs.getString("sos_phone", ""))
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        sosCard.addView(
            phone,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val save = button("GUARDAR NÚMERO", cyan) {
            val number = phone.text.toString().trim()
            if (number.isBlank()) {
                toast("Escribe un número.")
            } else {
                prefs.edit().putString("sos_phone", number).apply()
                toast("Número guardado.")
            }
        }
        sosCard.addView(save)

        countdownText = label(
            "SOS preparado",
            20f,
            Color.WHITE,
            true
        )
        sosCard.addView(countdownText)

        val sosPaid = prefs.getBoolean("sos_paid", false)

        val paySOS = button(
            if (sosPaid) "Marcar SOS como NO pagado"
            else "Marcar SOS como PAGADO",
            amberColor()
        ) {
            val newValue = !prefs.getBoolean("sos_paid", false)
            prefs.edit()
                .putBoolean("sos_paid", newValue)
                .apply()
            showHome()
        }
        sosCard.addView(paySOS)

        val startSOS = button("ACTIVAR SOS", red) {
            val number = phone.text.toString().trim()
            if (!prefs.getBoolean("sos_paid", false)) {
                toast("SOS no está pagado.")
            } else if (number.isBlank()) {
                toast("Primero guarda un número.")
            } else if (!counting) {
                prefs.edit().putString("sos_phone", number).apply()
                startCountdown(number)
            }
        }
        sosCard.addView(startSOS)

        cancelButton = button("CANCELAR", red) {
            cancelCountdown()
        }
        cancelButton.visibility = View.GONE
        sosCard.addView(cancelButton)

        root.addView(sosCard)

        addOption(
            root,
            "🎙️ ANTI-CHUPONEO",
            "Control de activación de esta función",
            "spy"
        )

        addOption(
            root,
            "📍 ANTI-RASTREO",
            "Control de activación de esta función",
            "track"
        )

        root.addView(
            label("DRAGÓN ZETA • VERSIÓN 1.0", 11f, Color.GRAY)
        )

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun startCountdown(number: String) {
        pendingNumber = number
        countdown = 5
        counting = true

        countdownText.text = "Llamada en 5 segundos"
        cancelButton.visibility = View.VISIBLE

        handler.removeCallbacks(countdownTask)
        handler.postDelayed(countdownTask, 1000)
    }

    private fun cancelCountdown() {
        counting = false
        handler.removeCallbacks(countdownTask)
        countdownText.text = "Llamada cancelada"
        cancelButton.visibility = View.GONE
        toast("Emergencia cancelada.")
    }

    private fun callEmergency() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val intent = Intent(
                    Intent.ACTION_CALL,
                    Uri.fromParts("tel", pendingNumber, null)
                )
                startActivity(intent)
            } catch (e: Exception) {
                toast("No se pudo iniciar la llamada.")
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                100
            )
        }
        cancelButton.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                callEmergency()
            } else {
                toast("Permiso de llamada denegado.")
                countdownText.text = "Llamada no realizada"
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        counting = false
        handler.removeCallbacks(countdownTask)
        super.onDestroy()
    }
}
}
