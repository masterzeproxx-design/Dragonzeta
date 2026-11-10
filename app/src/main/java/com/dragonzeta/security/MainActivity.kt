
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

    private val bg = Color.rgb(4, 10, 25)
    private val panelColor = Color.rgb(10, 22, 43)
    private val cyan = Color.rgb(0, 220, 255)
    private val green = Color.rgb(0, 230, 130)
    private val red = Color.rgb(255, 55, 75)
    private val amber = Color.rgb(255, 180, 40)

    private val accessUser = "ZEPROXX"
    private val accessPassword = "16071997"

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private var loggedIn = false
    private var countdown: CountDownTimer? = null
    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun panel(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(panelColor)
            setStroke(dp(1), color)
            cornerRadius = dp(15).toFloat()
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
                typeface = Typeface.DEFAULT_BOLD
            }
            setPadding(dp(8), dp(7), dp(8), dp(7))
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
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { action() }
        }
    }

    private fun addButton(parent: LinearLayout, child: View) {
        parent.addView(
            child,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                setMargins(dp(5), dp(7), dp(5), dp(7))
            }
        )
    }

    private fun createRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(25))
            setBackgroundColor(bg)
        }
    }

    private fun showScreen(root: LinearLayout) {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)
    }

    private fun createCard(
        root: LinearLayout,
        title: String,
        color: Int
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = panel(color)
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(9), 0, dp(9))
            }
        )

        card.addView(label(title, 16f, color, true))
        return card
    }

    private fun addHeader(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }

        val name = label("🐉 DRAGÓN ZETA", 23f, cyan, true)
        header.addView(
            name,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        val paid = label("PAGADO", 12f, Color.BLACK, true)
        paid.background = GradientDrawable().apply {
            setColor(green)
            cornerRadius = dp(20).toFloat()
        }
        header.addView(paid)

        root.addView(header)
    }

    private fun showLogin() {
        val root = createRoot()

        root.addView(
            label("🐉 DRAGÓN ZETA", 27f, cyan, true)
        )
        root.addView(
            label("SEGURIDAD PERSONAL", 13f, green, true)
        )

        val image = ImageView(this)
        val imageId = resources.getIdentifier(
            "dragon_zeta_login_header",
            "drawable",
            packageName
        )

        if (imageId != 0) {
            image.setImageResource(imageId)
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            root.addView(
                image,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(220)
                )
            )
        }

        val card = createCard(root, "ACCESO DE USUARIO", cyan)

        val username = EditText(this).apply {
            hint = "Usuario"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            singleLine = true
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            singleLine = true
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        card.addView(username)
        card.addView(password)

        addButton(card, makeButton("INGRESAR", green) {
            val user = username.text.toString().trim()
            val pass = password.text.toString()

            if (user == accessUser && pass == accessPassword) {
                loggedIn = true
                showHome()
            } else {
                toast("Usuario o contraseña incorrectos.")
            }
        })

        root.addView(
            label("SISTEMA DE PROTECCIÓN", 11f, Color.GRAY)
        )

        showScreen(root)
    }

    private fun showHome() {
        if (!loggedIn) {
            showLogin()
            return
        }

        val root = createRoot()

        // ENCABEZADO
        addHeader(root)

        root.addView(
            label("PANEL DE CONTROL", 22f, Color.WHITE, true)
        )
        root.addView(
            label("CENTRO DE SEGURIDAD PERSONAL", 11f, cyan, true)
        )

        // PERFIL DEL USUARIO
        val profile = createCard(root, "👤 PERFIL DE USUARIO", cyan)

        profile.addView(
            label("ZEPROXX", 23f, Color.WHITE, true)
        )
        profile.addView(
            label("USUARIO AUTORIZADO", 13f, green, true)
        )
        profile.addView(
            label("ESTADO: SISTEMA DISPONIBLE", 12f, Color.LTGRAY)
        )

        // ESTADO GENERAL
        val statusCard = createCard(root, "🛡️ PROTECCIÓN", green)
        statusCard.addView(
            label("DRAGÓN ZETA ESTÁ LISTO", 16f, Color.WHITE, true)
        )
        statusCard.addView(
            label("Controla tus opciones desde este panel", 12f, Color.LTGRAY)
        )

        // EMERGENCIA SOS
        val sosCard = createCard(root, "🆘 BOTÓN SOS", red)

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

        // FUNCIONES DE SEGURIDAD
        addOption(root, "🎙️ ANTI-CHUPONEO", "spy")
        addOption(root, "📍 ANTI-RASTREO", "track")

        // CERRAR SESIÓN
        addButton(root, makeButton("CERRAR SESIÓN", amber) {
            logout()
        })

        root.addView(
            label("🐉 DRAGÓN ZETA • VERSIÓN 1.0", 11f, Color.GRAY)
        )

        showScreen(root)
    }

    private fun addOption(
        root: LinearLayout,
        title: String,
        key: String
    ) {
        val card = createCard(root, title, cyan)

        val switch = Switch(this).apply {
            text = "Activar función"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("${key}_active", false)
        }

        switch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("${key}_active", checked).apply()
            toast(
                if (checked) "Opción activada."
                else "Opción desactivada."
            )
        }

        card.addView(switch)
    }

    private fun prepareSOS(number: String) {
        if (countdown != null) {
            toast("Ya hay una cuenta regresiva activa.")
            return
        }

        cancelButton.visibility = View.VISIBLE

        countdown = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownText.text =
                    "LLAMANDO EN ${millisUntilFinished / 1000 + 1}..."
            }

            override fun onFinish() {
                countdown = null
                cancelButton.visibility = View.GONE
                countdownText.text = "CONTACTANDO..."

                val intent = Intent(
                    Intent.ACTION_CALL,
                    Uri.parse("tel:$number")
                )

                if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        toast("No se pudo realizar la llamada.")
                    }
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.CALL_PHONE),
                        100
                    )
                }
            }
        }.start()
    }

    private fun cancelSOS() {
        countdown?.cancel()
        countdown = null
        countdownText.text = "SOS CANCELADO"
        cancelButton.visibility = View.GONE
        toast("Emergencia cancelada.")
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
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                val number = prefs.getString("sos_phone", "") ?: ""
                if (number.isNotBlank()) {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_CALL,
                                Uri.parse("tel:$number")
                            )
                        )
                    } catch (e: Exception) {
                        toast("No se pudo realizar la llamada.")
                    }
                }
            } else {
                toast("Permiso de llamada denegado.")
            }
        }
    }

    private fun logout() {
        countdown?.cancel()
        countdown = null
        loggedIn = false
        showLogin()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        countdown?.cancel()
        countdown = null
        super.onDestroy()
    }
}
