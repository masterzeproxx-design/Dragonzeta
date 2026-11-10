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
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(2, 6, 16)
    private val cyan = Color.rgb(0, 170, 255)
    private val green = Color.rgb(20, 220, 100)
    private val red = Color.rgb(220, 55, 55)
    private val amber = Color.rgb(220, 145, 45)

    private val accessUser = "ZEPROXX"
    private val accessPassword = "16071997"

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private var loggedIn = false
    private var timer: CountDownTimer? = null
    private var pendingNumber = ""

    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showLogin()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun panel(color: Int) = GradientDrawable().apply {
        setColor(Color.rgb(5, 15, 30))
        cornerRadius = dp(18).toFloat()
        setStroke(dp(2), color)
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

    private fun makeButton(
        text: String,
        color: Int,
        action: () -> Unit
    ) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = panel(color)
        setOnClickListener { action() }
    }

    private fun addButton(parent: LinearLayout, child: View) {
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

    private fun createRoot() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(18), dp(12), dp(18), dp(24))
        setBackgroundColor(bg)
    }

    private fun addHeader(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        header.addView(label("🐉 DRAGÓN ZETA", 27f, cyan, true))

        val paid = TextView(this).apply {
            text = "PAGADO"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = GradientDrawable().apply {
                setColor(green)
                cornerRadius = dp(6).toFloat()
            }
        }

        header.addView(paid)
        root.addView(header)
        root.addView(label("SEGURIDAD PERSONAL", 14f, Color.WHITE, true))
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

    private fun showScreen(root: LinearLayout) {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.addView(root)
        setContentView(scroll)
    }

    // PANTALLA DE ENTRADA ANONYMOUS + DRAGON
    private fun showLogin() {
        loggedIn = false
        val root = createRoot()

        val dragonImage = ImageView(this).apply {
            setImageResource(R.drawable.dragon_zeta_login_header)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        root.addView(
            dragonImage,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(225)
            )
        )

        root.addView(label("ACCESO AL SISTEMA", 20f, cyan, true))
        root.addView(
            label("IDENTIFICACIÓN DE USUARIO", 12f, Color.LTGRAY, true)
        )

        val username = EditText(this).apply {
            hint = "Usuario"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            background = panel(cyan)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            background = panel(cyan)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        root.addView(
            username,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { setMargins(0, dp(10), 0, dp(10)) }
        )

        root.addView(
            password,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply { setMargins(0, dp(8), 0, dp(14)) }
        )

        addButton(root, makeButton("INICIAR SESIÓN", green) {
            val user = username.text.toString().trim()
            val pass = password.text.toString()

            if (
                user.equals(accessUser, ignoreCase = true) &&
                pass == accessPassword
            ) {
                loggedIn = true
                toast("Acceso correcto.")
                showHome()
            } else {
                toast("Usuario o contraseña incorrectos.")
            }
        })

        root.addView(
            label("SISTEMA DE SEGURIDAD PERSONAL", 11f, Color.GRAY)
        )

        showScreen(root)
    }

    private fun showHome() {
        if (!loggedIn) {
            showLogin()
            return
        }

        val root = createRoot()
        addHeader(root)
        root.addView(label("SISTEMA ACTIVADO", 17f, green, true))

        val sosCard = createCard(root, "🆘 SOS – EMERGENCIA", red)
        sosCard.addView(label("Número de emergencia", 14f, Color.WHITE))

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

        addButton(sosCard, makeButton("GUARDAR NÚMERO", cyan) {
            val number = phone.text.toString().trim()
            if (number.isBlank()) {
                toast("Escribe un número.")
            } else {
                prefs.edit().putString("sos_phone", number).apply()
                toast("Número guardado.")
            }
        })

        countdownText = label("SOS PREPARADO", 20f, Color.WHITE, true)
        sosCard.addView(countdownText)

        addButton(sosCard, makeButton("ACTIVAR SOS", red) {
            val number = phone.text.toString().trim()
            if (number.isBlank()) {
                toast("Primero escribe un número.")
            } else {
                prefs.edit().putString("sos_phone", number).apply()
                prepareSOS(number)
            }
        })

        cancelButton = makeButton("CANCELAR EMERGENCIA", red) {
            cancelSOS()
        }
        cancelButton.visibility = View.GONE
        addButton(sosCard, cancelButton)

        addOption(root, "🎙️ ANTI-CHUPONEO", "spy")
        addOption(root, "📍 ANTI-RASTREO", "track")

        addButton(root, makeButton("CERRAR SESIÓN", amber) {
            logout()
        })

        root.addView(label("DRAGÓN ZETA • VERSIÓN 1.0", 11f, Color.GRAY))
        showScreen(root)
    }

    private fun addOption(root: LinearLayout, title: String, key: String) {
        val card = createCard(root, title, cyan)
        val status = label("", 16f, Color.WHITE, true)
        card.addView(status)

        var active = prefs.getBoolean("${key}_active", false)

        fun refresh() {
            status.text = if (active) {
                "🟢 FUNCIÓN ACTIVADA"
            } else {
                "🔴 FUNCIÓN DESACTIVADA"
            }
            status.setTextColor(if (active) green else red)
        }

        addButton(card, makeButton("ACTIVAR / DESACTIVAR", green) {
            active = !active
            prefs.edit().putBoolean("${key}_active", active).apply()
            refresh()
        })

        refresh()
    }

    private fun logout() {
        timer?.cancel()
        timer = null
        loggedIn = false
        showLogin()
    }

    private fun prepareSOS(number: String) {
        pendingNumber = number

        if (
            checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
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
                val seconds = (millisUntilFinished + 999) / 1000
                countdownText.text = "LLAMADA EN $seconds SEGUNDOS"
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
            checkSelfPermission(Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
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
            requestCode, permissions, grantResults
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
