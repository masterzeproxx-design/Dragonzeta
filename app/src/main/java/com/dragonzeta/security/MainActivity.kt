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
import android.os.CountDownTimer
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
    private val amber = Color.rgb(220, 145, 45)

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private var timer: CountDownTimer? = null
    private var pendingNumber = ""

    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

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
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(dp(5), dp(7), dp(5), dp(7))
        }
    }

    private fun makeButton(
        text: String,
        color: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = panel(color)
            setOnClickListener {
                action()
            }
        }
    }

    private fun addButton(
        parent: LinearLayout,
        child: View
    ) {
        parent.addView(
            child,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                setMargins(0, dp(5), 0, dp(5))
            }
        )
    }

    private fun createCard(
        root: LinearLayout,
        title: String,
        color: Int
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = panel(color)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        card.addView(label(title, 20f, color, true))

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(9), 0, dp(9))
            }
        )

        return card
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(bg)
        }

        root.addView(
            label("🐉 DRAGÓN ZETA", 30f, cyan, true)
        )

        root.addView(
            label("SEGURIDAD PERSONAL", 14f, Color.WHITE, true)
        )

        // SOS EMERGENCIA

        val sosCard = createCard(
            root,
            "🆘 SOS – EMERGENCIA",
            red
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
                dp(54)
            )
        )

        addButton(
            sosCard,
            makeButton("GUARDAR NÚMERO", cyan) {
                val number = phone.text.toString().trim()

                if (number.isBlank()) {
                    toast("Escribe un número.")
                } else {
                    prefs.edit()
                        .putString("sos_phone", number)
                        .apply()
                    toast("Número guardado.")
                }
            }
        )

        val sosPaid = prefs.getBoolean("sos_paid", false)

        sosCard.addView(
            label(
                if (sosPaid) "🟢 SOS PAGADO"
                else "🔴 SOS NO PAGADO",
                15f,
                if (sosPaid) green else red,
                true
            )
        )

        addButton(
            sosCard,
            makeButton(
                if (sosPaid) "MARCAR COMO NO PAGADO"
                else "MARCAR COMO PAGADO",
                amber
            ) {
                prefs.edit()
                    .putBoolean("sos_paid", !sosPaid)
                    .apply()
                showHome()
            }
        )

        countdownText = label(
            "SOS PREPARADO",
            20f,
            Color.WHITE,
            true
        )
        sosCard.addView(countdownText)

        addButton(
            sosCard,
            makeButton("ACTIVAR SOS", red) {
                val number = phone.text.toString().trim()

                if (!prefs.getBoolean("sos_paid", false)) {
                    toast("La opción SOS no está pagada.")
                } else if (number.isBlank()) {
                    toast("Primero escribe un número.")
                } else {
                    prefs.edit()
                        .putString("sos_phone", number)
                        .apply()
                    prepareSOS(number)
                }
            }
        )

        cancelButton = makeButton("CANCELAR EMERGENCIA", red) {
            cancelSOS()
        }

        cancelButton.visibility = View.GONE
        addButton(sosCard, cancelButton)

        // ANTI-CHUPONEO

        addOption(
            root,
            "🎙️ ANTI-CHUPONEO",
            "spy"
        )

        // ANTI-RASTREO

        addOption(
            root,
            "📍 ANTI-RASTREO",
            "track"
        )

        root.addView(
            label("DRAGÓN ZETA • VERSIÓN 1.0", 11f, Color.GRAY)
        )

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun addOption(
        root: LinearLayout,
        title: String,
        key: String
    ) {
        var paid = prefs.getBoolean("${key}_paid", false)
        var active = prefs.getBoolean("${key}_active", false)

        if (!paid) {
            active = false
            prefs.edit()
                .putBoolean("${key}_active", false)
                .apply()
        }

        val card = createCard(root, title, cyan)

        val status = label("", 16f, Color.WHITE, true)
        card.addView(status)

        var refreshCard: (() -> Unit)? = null

        val paymentButton = makeButton("", amber) {
            paid = !paid

            if (!paid) {
                active = false
            }

            prefs.edit()
                .putBoolean("${key}_paid", paid)
                .putBoolean("${key}_active", active)
                .apply()

            refreshCard?.invoke()
        }

        card.addView(paymentButton)

        val activationButton = makeButton("", green) {
            if (!paid) {
                toast("Primero debes pagar esta opción.")
            } else {
                active = !active

                prefs.edit()
                    .putBoolean("${key}_active", active)
                    .apply()

                refreshCard?.invoke()
            }
        }

        card.addView(activationButton)

        refreshCard = {
            status.text = when {
                !paid -> "🔴 OPCIÓN NO PAGADA"
                active -> "🟢 SEGURIDAD MÁXIMA"
                else -> "🔴 SEGURIDAD DESACTIVADA"
            }

            status.setTextColor(
                if (paid && active) green else red
            )

            paymentButton.text =
                if (paid) "MARCAR COMO NO PAGADA"
                else "MARCAR COMO PAGADA"

            activationButton.text =
                if (active) "DESACTIVAR"
                else "ACTIVAR"

            activationButton.isEnabled = paid
        }

        refreshCard?.invoke()
    }

    private fun prepareSOS(number: String) {
        pendingNumber = number

        if (
            checkSelfPermission(Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCountdown()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                100
            )
        }
    }

    private fun startCountdown() {
        timer?.cancel()

        countdownText.text = "LLAMADA EN 5 SEGUNDOS"
        cancelButton.visibility = View.VISIBLE

        timer = object : CountDownTimer(5000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds =
                    (millisUntilFinished + 999) / 1000

                countdownText.text =
                    "LLAMADA EN $seconds SEGUNDOS"
            }

            override fun onFinish() {
                countdownText.text = "INICIANDO LLAMADA"
                cancelButton.visibility = View.GONE
                callEmergency()
            }
        }.start()
    }

    private fun cancelSOS() {
        timer?.cancel()
        timer = null

        countdownText.text = "EMERGENCIA CANCELADA"
        cancelButton.visibility = View.GONE

        toast("Llamada cancelada.")
    }

    private fun callEmergency() {
        if (pendingNumber.isBlank()) {
            toast("No hay número de emergencia.")
            return
        }

        if (
            checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toast("Falta el permiso para llamar.")
            return
        }

        try {
            val intent = Intent(
                Intent.ACTION_CALL,
                Uri.parse("tel:${Uri.encode(pendingNumber)}")
            )
            startActivity(intent)
        } catch (e: Exception) {
            toast("No se pudo iniciar la llamada.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startCountdown()
            } else {
                toast("Permiso de llamada denegado.")
                countdownText.text = "SOS NO DISPONIBLE"
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
