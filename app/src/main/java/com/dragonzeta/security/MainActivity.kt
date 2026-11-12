package com.dragonzeta.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import android.text.InputType
import android.animation.ObjectAnimator
import android.animation.ValueAnimator

class MainActivity : Activity() {

    private val bg = Color.rgb(3, 8, 20)
    private val panelColor = Color.rgb(8, 19, 38)
    private val cyan = Color.rgb(0, 220, 255)
    private val green = Color.rgb(0, 235, 145)
    private val red = Color.rgb(255, 55, 75)
    private val amber = Color.rgb(255, 180, 40)
    private val blueButton = Color.rgb(0, 105, 220)

    private val accessUser = "ZEPROXX"
    private val accessPassword = "16071997"

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private var loggedIn = false
    private var pendingSOSNumber = ""

    private var whatsappScanTimer: android.os.CountDownTimer? = null
    private var whatsappScanAnimator: ObjectAnimator? = null

    private var vpnSimulationTimer: android.os.CountDownTimer? = null
    private var vpnOverlayView: View? = null

    private var overlayTimer: android.os.CountDownTimer? = null
    private var overlayFrame: FrameLayout? = null

    companion object {
        private const val REQUEST_CALL_PERMISSION = 100
        private const val REQUEST_NOTIFICATION_SOS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        window.decorView.systemUiVisibility = 0
        showLogin()
    }

    override fun onResume() {
        super.onResume()
        if (loggedIn) showHome()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun panel(
        color: Int,
        translucent: Boolean = false
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(12, 30, 55),
                Color.rgb(5, 13, 29)
            )
        ).apply {
            setStroke(dp(1), color)
            cornerRadius = dp(17).toFloat()
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
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(6), dp(8), dp(6))
            letterSpacing = 0.035f
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
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.04f
            background = GradientDrawable().apply {
                colors = intArrayOf(
                    color,
                    Color.rgb(
                        (Color.red(color) * 0.72f).toInt(),
                        (Color.green(color) * 0.72f).toInt(),
                        (Color.blue(color) * 0.72f).toInt()
                    )
                )
                cornerRadius = dp(13).toFloat()
                setStroke(dp(1), Color.argb(100, 255, 255, 255))
            }
            setOnClickListener { action() }
        }
    }

    private fun addButton(parent: LinearLayout, child: View) {
        parent.addView(
            child,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(49)
            ).apply {
                setMargins(dp(4), dp(6), dp(4), dp(6))
            }
        )
    }

    // CONTENEDOR: DEJA ESPACIO SUPERIOR PARA LA BARRA DEL TELÉFONO
    private fun createRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(17),
                dp(28),
                dp(17),
                dp(22)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    // ICONOS TECNOLÓGICOS DIBUJADOS
    private inner class CyberIconView(
        private val iconType: String,
        private val iconColor: Int
    ) : View(this) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.4).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = iconColor
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val s = dp(62).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val left = cx - s / 2f
            val top = cy - s / 2f

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(28, Color.red(iconColor),
                Color.green(iconColor), Color.blue(iconColor))
            canvas.drawCircle(cx, cy, s * 0.49f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = iconColor
            canvas.drawCircle(cx, cy, s * 0.49f, paint)

            paint.strokeWidth = dp(2.6).toFloat()

            when (iconType) {
                "mic" -> {
                    val r = RectF(
                        cx - s * 0.12f,
                        cy - s * 0.30f,
                        cx + s * 0.12f,
                        cy + s * 0.12f
                    )
                    canvas.drawRoundRect(r, s * 0.12f, s * 0.12f, paint)

                    val p = Path().apply {
                        moveTo(cx - s * 0.25f, cy - s * 0.02f)
                        lineTo(cx - s * 0.25f, cy + s * 0.08f)
                        quadTo(cx, cy + s * 0.40f, cx + s * 0.25f, cy + s * 0.08f)
                        lineTo(cx + s * 0.25f, cy - s * 0.02f)
                        moveTo(cx, cy + s * 0.28f)
                        lineTo(cx, cy + s * 0.40f)
                        moveTo(cx - s * 0.14f, cy + s * 0.40f)
                        lineTo(cx + s * 0.14f, cy + s * 0.40f)
                    }
                    canvas.drawPath(p, paint)
                }

                "pin" -> {
                    val p = Path().apply {
                        moveTo(cx, cy + s * 0.40f)
                        cubicTo(
                            cx - s * 0.10f, cy + s * 0.20f,
                            cx - s * 0.31f, cy - s * 0.04f,
                            cx - s * 0.31f, cy - s * 0.18f
                        )
                        arcTo(
                            RectF(
                                cx - s * 0.31f,
                                cy - s * 0.43f,
                                cx + s * 0.31f,
                                cy + s * 0.18f
                            ),
                            180f,
                            180f,
                            false
                        )
                        cubicTo(
                            cx + s * 0.31f, cy - s * 0.04f,
                            cx + s * 0.10f, cy + s * 0.20f,
                            cx, cy + s * 0.40f
                        )
                    }
                    canvas.drawPath(p, paint)
                    canvas.drawCircle(cx, cy - s * 0.18f, s * 0.09f, paint)
                }

                "shield" -> {
                    val p = Path().apply {
                        moveTo(cx, cy - s * 0.37f)
                        lineTo(cx + s * 0.29f, cy - s * 0.24f)
                        lineTo(cx + s * 0.25f, cy + s * 0.12f)
                        quadTo(cx + s * 0.17f, cy + s * 0.31f, cx, cy + s * 0.40f)
                        quadTo(cx - s * 0.17f, cy + s * 0.31f, cx - s * 0.25f, cy + s * 0.12f)
                        lineTo(cx - s * 0.29f, cy - s * 0.24f)
                        close()
                    }
                    canvas.drawPath(p, paint)

                    val check = Path().apply {
                        moveTo(cx - s * 0.13f, cy)
                        lineTo(cx - s * 0.02f, cy + s * 0.12f)
                        lineTo(cx + s * 0.16f, cy - s * 0.12f)
                    }
                    canvas.drawPath(check, paint)
                }

                "whatsapp" -> {
                    canvas.drawCircle(cx, cy - s * 0.04f, s * 0.27f, paint)

                    val phone = Path().apply {
                        moveTo(cx - s * 0.13f, cy - s * 0.18f)
                        quadTo(cx - s * 0.20f, cy - s * 0.10f, cx - s * 0.08f, cy + s * 0.04f)
                        quadTo(cx + s * 0.02f, cy + s * 0.17f, cx + s * 0.15f, cy + s * 0.10f)
                    }
                    canvas.drawPath(phone, paint)

                    val tail = Path().apply {
                        moveTo(cx - s * 0.18f, cy + s * 0.18f)
                        lineTo(cx - s * 0.31f, cy + s * 0.32f)
                        lineTo(cx - s * 0.05f, cy + s * 0.25f)
                    }
                    canvas.drawPath(tail, paint)
                }

                "sos" -> {
                    canvas.drawCircle(cx, cy, s * 0.29f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, s * 0.09f, paint)
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(cx, cy - s * 0.48f, cx, cy - s * 0.36f, paint)
                    canvas.drawLine(cx, cy + s * 0.36f, cx, cy + s * 0.48f, paint)
                    canvas.drawLine(cx - s * 0.48f, cy, cx - s * 0.36f, cy, paint)
                    canvas.drawLine(cx + s * 0.36f, cy, cx + s * 0.48f, cy, paint)
                }
            }
        }
    }

    private fun showScreen(
        root: LinearLayout,
        watermark: Boolean = false
    ) {
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(8))
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        scroll.setBackgroundColor(Color.TRANSPARENT)

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val frame = FrameLayout(this)
        frame.setBackgroundColor(bg)

        if (watermark) {
            val logo = ImageView(this).apply {
                setImageResource(R.drawable.dragon_zeta_logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 0.10f
                setPadding(dp(25), dp(50), dp(25), dp(50))
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            frame.addView(
                logo,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        frame.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        overlayFrame = frame
        setContentView(frame)
    }

    private fun createCard(
        root: LinearLayout,
        title: String,
        color: Int
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = panel(color, true)
            elevation = dp(3).toFloat()
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(7), 0, dp(7))
            }
        )

        val heading = label(title, 15f, color, true)
        heading.letterSpacing = 0.07f
        card.addView(heading)

        return card
    }

    // ENCABEZADO CON LOGO DESTACADO
    private fun addHeader(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(8))
        }

        val logoFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(5, 28, 48))
                setStroke(dp(1), cyan)
            }
            elevation = dp(8).toFloat()
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.dragon_zeta_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }

        logoFrame.addView(
            logo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        header.addView(
            logoFrame,
            LinearLayout.LayoutParams(dp(68), dp(68)).apply {
                setMargins(dp(3), 0, dp(10), 0)
            }
        )

        val nameColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nameColumn.addView(
            label("DRAGÓN ZETA", 20f, cyan, true)
        )
        nameColumn.addView(
            label("CYBER SECURITY SYSTEM", 9f, Color.LTGRAY, true)
        )

        header.addView(
            nameColumn,
            LinearLayout.LayoutParams(0, dp(65), 1f)
        )

        val paid = label("PAGADO", 11f, Color.BLACK, true)
        paid.background = GradientDrawable().apply {
            setColor(green)
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.WHITE)
        }
        header.addView(paid)

        root.addView(header)

        val line = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.TRANSPARENT,
                    cyan,
                    green,
                    Color.TRANSPARENT
                )
            )
        }

        root.addView(
            line,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(2)
            ).apply {
                setMargins(dp(5), dp(3), dp(5), dp(9))
            }
        )
    }

    // LOGIN FUTURISTA
    private fun showLogin() {
        val root = createRoot()

        root.addView(
            label("SISTEMA DE ACCESO", 12f, green, true)
        )

        val logoFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(5, 25, 45))
                setStroke(dp(2), cyan)
            }
            elevation = dp(10).toFloat()
        }

        val image = ImageView(this).apply {
            setImageResource(R.drawable.dragon_zeta_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        logoFrame.addView(
            image,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            logoFrame,
            LinearLayout.LayoutParams(dp(142), dp(142)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(12), 0, dp(12))
            }
        )

        root.addView(label("DRAGÓN ZETA", 26f, cyan, true))
        root.addView(label("SEGURIDAD PERSONAL", 12f, green, true))
        root.addView(
            label("PROTECCIÓN • CONTROL • SEGURIDAD", 9f, Color.LTGRAY)
        )

        val card = createCard(root, "ACCESO DE USUARIO", cyan)

        val username = EditText(this).apply {
            hint = "Usuario"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

        card.addView(
            username,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                setMargins(0, dp(7), 0, dp(7))
            }
        )

        card.addView(
            password,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                setMargins(0, dp(7), 0, dp(7))
            }
        )

        addButton(card, makeButton("INGRESAR AL SISTEMA", green) {
            if (username.text.toString().trim() == accessUser &&
                password.text.toString() == accessPassword
            ) {
                loggedIn = true
                showHome()
            } else {
                toast("Usuario o contraseña incorrectos.")
            }
        })

        root.addView(
            label("SISTEMA DE PROTECCIÓN", 10f, Color.GRAY)
        )

        showScreen(root)
    }

    // PANEL PRINCIPAL
    private fun showHome() {
        if (!loggedIn) {
            showLogin()
            return
        }

        val root = createRoot()
        addHeader(root)

        root.addView(
            label("PANEL DE CONTROL", 21f, Color.WHITE, true)
        )
        root.addView(
            label("CENTRO DE SEGURIDAD PERSONAL", 10f, cyan, true)
        )

        val profile = createCard(root, "PERFIL DE USUARIO", cyan)
        profile.addView(label("ZEPROXX", 22f, Color.WHITE, true))
        profile.addView(label("USUARIO AUTORIZADO", 12f, green, true))
        profile.addView(
            label("ESTADO: SISTEMA DISPONIBLE", 11f, Color.LTGRAY)
        )

        val statusCard = createCard(root, "PROTECCIÓN DEL SISTEMA", green)
        statusCard.addView(CyberIconView("shield", green),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )
        statusCard.addView(
            label("DRAGÓN ZETA ESTÁ LISTO", 15f, Color.WHITE, true)
        )
        statusCard.addView(
            label("Controla tus opciones desde este panel", 11f, Color.LTGRAY)
        )

        addSOSCard(root)
        addOption(root, "ANTI-CHUPONEO", "spy")
        addOption(root, "ANTI-RASTREO", "track")
        addWhatsAppProtection(root)

        addButton(root, makeButton("CERRAR SESIÓN", amber) {
            logout()
        })

        root.addView(
            label("DRAGÓN ZETA • VERSIÓN 1.0", 10f, Color.GRAY)
        )

        showScreen(root, watermark = true)
    }

    // SISTEMA SOS
    private fun addSOSCard(root: LinearLayout) {
        val active = prefs.getBoolean("sos_configured", false)
        val number = prefs.getString("sos_phone", "") ?: ""

        val card = createCard(
            root,
            "BOTÓN SOS",
            if (active) green else red
        )

        card.addView(
            CyberIconView("sos", if (active) green else red),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        if (active) {
            card.addView(label("SOS ACTIVADO", 20f, green, true))
            card.addView(
                label("Número de emergencia: $number", 12f, Color.WHITE)
            )
            card.addView(
                label(
                    "Dos pulsaciones de VOLUMEN ARRIBA activan la alerta.",
                    11f,
                    Color.LTGRAY
                )
            )

            addButton(card, makeButton("PROBAR SOS AHORA", red) {
                if (number.isNotBlank()) {
                    SosEmergency.start(this, number)
                } else {
                    toast("Configura primero tu número SOS.")
                }
            })

            addButton(card, makeButton("CAMBIAR NÚMERO", cyan) {
                prefs.edit()
                    .putBoolean("sos_configured", false)
                    .apply()
                showHome()
            })

            addButton(
                card,
                makeButton("CONFIGURAR ACTIVACIÓN POR VOLUMEN", green) {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    } catch (e: Exception) {
                        toast("No se pudieron abrir los ajustes.")
                    }
                }
            )

        } else {
            card.addView(
                label("Configura tu número de emergencia", 13f, Color.WHITE)
            )

            val phone = EditText(this).apply {
                hint = "+51 999 999 999"
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                inputType = InputType.TYPE_CLASS_PHONE
                isSingleLine = true
                setText(number)
                background = panel(cyan)
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }

            card.addView(
                phone,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
                )
            )

            addButton(card, makeButton("GUARDAR Y ACTIVAR SOS", green) {
                val newNumber = phone.text.toString().trim()
                if (newNumber.isBlank()) {
                    toast("Escribe un número.")
                } else {
                    activateSOS(newNumber)
                }
            })

            card.addView(
                label(
                    "Después de activar, podrás usar dos pulsaciones de volumen arriba.",
                    10f,
                    Color.LTGRAY
                )
            )
        }
    }

    private fun activateSOS(number: String) {
        pendingSOSNumber = number

        prefs.edit()
            .putString("sos_phone", number)
            .apply()

        if (checkSelfPermission(Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PERMISSION
            )
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_SOS
            )
            return
        }

        finishSOSActivation(number)
    }

    private fun finishSOSActivation(number: String) {
        prefs.edit()
            .putString("sos_phone", number)
            .putBoolean("sos_configured", true)
            .apply()

        showHome()
        toast("SOS ACTIVADO CORRECTAMENTE")
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

        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_CALL_PERMISSION -> {
                if (granted) {
                    activateSOS(pendingSOSNumber)
                } else {
                    toast("Necesitas permitir las llamadas para usar SOS.")
                }
            }

            REQUEST_NOTIFICATION_SOS -> {
                if (granted) {
                    finishSOSActivation(pendingSOSNumber)
                } else {
                    toast("Permite las notificaciones para poder cancelar el SOS.")
                }
            }
        }
    }

    // ANTI-CHUPONEO Y ANTI-RASTREO
    private fun addOption(
        root: LinearLayout,
        title: String,
        key: String
    ) {
        val active = prefs.getBoolean("${key}_active", false)
        val isSpy = key == "spy"
        val optionColor = if (isSpy) green else cyan

        val card = createCard(
            root,
            title,
            if (active) green else optionColor
        )

        card.addView(
            CyberIconView(
                if (isSpy) "mic" else "pin",
                if (active) green else optionColor
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
            )
        )

        if (isSpy) {
            card.addView(
                label(
                    if (active) "FUNCIÓN ACTIVADA"
                    else "Sistema de análisis de seguridad de red",
                    11f,
                    if (active) green else Color.LTGRAY,
                    active
                )
            )

            addButton(
                card,
                makeButton(
                    if (active) "ANTI-CHUPONEO ACTIVADO"
                    else "ACTIVAR ANTI-CHUPONEO",
                    if (active) green else blueButton
                ) {
                    if (prefs.getBoolean("spy_active", false)) {
                        prefs.edit()
                            .putBoolean("spy_active", false)
                            .apply()
                        showHome()
                        toast("ANTI-CHUPONEO DESACTIVADO.")
                    } else {
                        showAntiChuponeoOverlay()
                    }
                }
            )
        } else {
            card.addView(
                label(
                    if (active) "VPN ACTUALIZADA"
                    else "Actualiza la presentación de tu conexión",
                    11f,
                    if (active) green else Color.LTGRAY,
                    active
                )
            )

            addButton(
                card,
                makeButton(
                    if (active) "DESACTIVAR VPN ACTUALIZADA"
                    else "ACTUALIZAR VPN",
                    if (active) green else blueButton
                ) {
                    if (vpnSimulationTimer != null) return@makeButton

                    if (prefs.getBoolean("track_active", false)) {
                        prefs.edit()
                            .putBoolean("track_active", false)
                            .apply()
                        showHome()
                        toast("VPN desconectada.")
                    } else {
                        showVpnSimulationOverlay()
                    }
                }
            )
        }
    }

    // VENTANA DE ACTUALIZACIÓN VPN
    private fun showVpnSimulationOverlay() {
        val frame = overlayFrame ?: return
        if (vpnSimulationTimer != null) return

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(190, 0, 5, 16))
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            background = panel(cyan)
            elevation = dp(18).toFloat()
        }

        card.addView(label("DRAGÓN ZETA", 22f, cyan, true))
        card.addView(label("VPN ACTUALIZADA", 14f, green, true))
        card.addView(label("◈", 54f, cyan, true))

        val lock = card.getChildAt(2)

        val status = label("INICIANDO CONEXIÓN...", 16f, Color.WHITE, true)
        card.addView(status)

        val server = label("Servidor: seleccionando ubicación", 12f, Color.LTGRAY)
        card.addView(server)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            this.progress = 0
            progressTintList = ColorStateList.valueOf(cyan)
            progressBackgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        card.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply { setMargins(dp(4), dp(15), dp(4), dp(12)) }
        )

        val ipPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = panel(cyan)
        }

        val oldIp = label("IP ANTERIOR: —", 12f, Color.LTGRAY)
        val newIp = label("IP ACTUALIZADA: conectando...", 12f, cyan, true)

        ipPanel.addView(oldIp)
        ipPanel.addView(newIp)
        card.addView(ipPanel)

        val log = TextView(this).apply {
            text = "Preparando túnel cifrado..."
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
            gravity = Gravity.START
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }

        card.addView(log)

        val close = makeButton("CANCELAR", red) {
            closeVpnSimulationOverlay(false)
        }

        card.addView(
            close,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { setMargins(0, dp(10), 0, 0) }
        )

        overlay.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                setMargins(dp(22), dp(18), dp(22), dp(18))
            }
        )

        frame.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        vpnOverlayView = overlay

        val messages = arrayOf(
            "Buscando servidor disponible...",
            "Negociando protocolo WireGuard...",
            "Creando túnel seguro...",
            "Verificando ruta de conexión...",
            "Asignando nueva dirección IP...",
            "Finalizando conexión..."
        )

        vpnSimulationTimer = object : android.os.CountDownTimer(8000, 250) {

            override fun onTick(millisUntilFinished: Long) {
                val elapsed = 8000L - millisUntilFinished
                val percent = (elapsed * 100 / 8000).toInt()

                progress.progress = percent

                status.text = when {
                    percent < 20 -> "LOCALIZANDO SERVIDOR..."
                    percent < 45 -> "NEGOCIANDO CONEXIÓN..."
                    percent < 70 -> "CREANDO TÚNEL CIFRADO..."
                    else -> "ASIGNANDO NUEVA RUTA..."
                }

                server.text = when {
                    percent < 35 -> "Servidor: buscando mejor ubicación"
                    percent < 70 -> "Servidor: Ámsterdam, Países Bajos"
                    else -> "Servidor: Ámsterdam • conectado"
                }

                val index = (percent / 18).coerceAtMost(messages.lastIndex)
                log.text = messages.take(index + 1)
                    .joinToString("\n") { "› $it" }

                lock.rotation = if (percent % 20 < 10) -8f else 8f
            }

            override fun onFinish() {
                vpnSimulationTimer = null
                progress.progress = 100
                status.text = "VPN ACTUALIZADA"
                status.setTextColor(green)
                server.text = "Servidor: Ámsterdam, Países Bajos"
                oldIp.text = "IP ANTERIOR: 198.51.100.24"
                newIp.text = "IP ACTUALIZADA\nNUEVA IP: 203.0.113.58"
                newIp.setTextColor(green)
                log.text = "TÚNEL: LISTO\nRUTA: ESTABLECIDA\nESTADO: CONECTADO"
                lock.rotation = 0f
                lock.setTextColor(green)

                close.text = "FINALIZAR"
                close.background = GradientDrawable().apply {
                    setColor(green)
                    cornerRadius = dp(12).toFloat()
                }

                close.setOnClickListener {
                    closeVpnSimulationOverlay(true)
                }
            }
        }.start()
    }

    private fun closeVpnSimulationOverlay(activated: Boolean) {
        vpnSimulationTimer?.cancel()
        vpnSimulationTimer = null

        vpnOverlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        vpnOverlayView = null

        if (activated) {
            prefs.edit().putBoolean("track_active", true).apply()
            showHome()
            toast("VPN ACTUALIZADA")
        }
    }

    // ANTI-CHUPONEO
    private fun showAntiChuponeoOverlay() {
        val frame = overlayFrame ?: return
        if (overlayTimer != null) return

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(25), dp(20), dp(25))
            setBackgroundColor(Color.argb(190, 1, 7, 15))
        }

        overlay.addView(label("DRAGÓN ZETA", 24f, cyan, true))
        overlay.addView(
            label("CYBER SECURITY TERMINAL", 13f, green, true)
        )
        overlay.addView(
            label("NETWORK ANALYSIS • SYSTEM SCAN", 10f, Color.LTGRAY)
        )

        val codeView = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
            gravity = Gravity.START
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setLineSpacing(dp(4).toFloat(), 1f)
            background = panel(green)
        }

        overlay.addView(
            codeView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, dp(18), 0, dp(15))
            }
        )

        val status = label("INICIANDO ANÁLISIS...", 17f, cyan, true)
        overlay.addView(status)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            this.progress = 0
            progressTintList = ColorStateList.valueOf(green)
            progressBackgroundTintList =
                ColorStateList.valueOf(Color.DKGRAY)
        }

        overlay.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(12)
            ).apply {
                setMargins(dp(10), dp(12), dp(10), dp(15))
            }
        )

        val closeButton = makeButton("RED ASEGURADA", green) {
            closeAntiChuponeoOverlay()
        }

        closeButton.visibility = View.GONE
        addButton(overlay, closeButton)

        frame.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val codeLines = arrayOf(
            "root@dragonzeta:~$ ./network_scan.sh",
            "",
            "[INIT] Loading security modules...",
            "[ OK ] Firewall module initialized",
            "[SCAN] Inspecting network interfaces...",
            "[INFO] Checking connection integrity",
            "[SCAN] Reviewing access permissions...",
            "[ OK ] Secure protocols verified",
            "[INFO] Analyzing network traffic...",
            "[SCAN] Running security diagnostics...",
            "[ OK ] Verification completed",
            "",
            "01001001 01001110 01000110 01001111",
            "0x4E4554 0x534543 0x555245",
            "TCP_CHECK: PASS",
            "FIREWALL_STATUS: ACTIVE",
            "NETWORK_INTEGRITY: VERIFIED",
            "",
            "root@dragonzeta:~$ scan_complete"
        )

        codeView.text = codeLines.take(3).joinToString("\n")

        overlayTimer = object : android.os.CountDownTimer(7000, 500) {

            override fun onTick(millisUntilFinished: Long) {
                val elapsed = 7000L - millisUntilFinished
                val percent = (elapsed * 100 / 7000).toInt()

                progress.progress = percent

                val visibleLines = (3 + elapsed / 300)
                    .toInt()
                    .coerceAtMost(codeLines.size)

                codeView.text = codeLines
                    .take(visibleLines)
                    .joinToString("\n")

                status.text = when {
                    percent < 25 -> "INICIANDO SISTEMA..."
                    percent < 50 -> "ANALIZANDO CONEXIÓN..."
                    percent < 75 -> "VERIFICANDO SEGURIDAD..."
                    else -> "FINALIZANDO DIAGNÓSTICO..."
                }
            }

            override fun onFinish() {
                overlayTimer = null
                progress.progress = 100
                codeView.text = codeLines.joinToString("\n")

                prefs.edit()
                    .putBoolean("spy_active", true)
                    .apply()

                status.text = "RED SEGURA\nFUNCIÓN ACTIVADA"
                status.setTextColor(green)
                closeButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeAntiChuponeoOverlay() {
        overlayTimer?.cancel()
        overlayTimer = null

        val frame = overlayFrame
        val overlay = frame?.getChildAt(frame.childCount - 1)

        if (overlay != null && overlay !== frame.getChildAt(0)) {
            frame.removeView(overlay)
        }

        showHome()
        toast("ANTI-CHUPONEO ACTIVADO.")
    }

    // ANTI-HACK WSP
    private fun addWhatsAppProtection(root: LinearLayout) {
        val card = createCard(root, "ANTI-HACK WSP", green)

        card.addView(
            CyberIconView("whatsapp", green),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(78)
            )
        )

        card.addView(
            label(
                "Sistema visual de protección de WhatsApp",
                12f,
                Color.LTGRAY
            )
        )

        val active = prefs.getBoolean(
            "whatsapp_protection_active",
            false
        )

        val shield = label(
            "◈",
            40f,
            if (active) green else cyan,
            true
        )

        card.addView(shield)

        val status = label(
            if (active) "REVISIÓN VISUAL COMPLETADA"
            else "SISTEMA EN ESPERA",
            16f,
            if (active) green else cyan,
            true
        )

        card.addView(status)

        val detail = label(
            if (active) "Revisión visual finalizada"
            else "Presiona el botón para comenzar",
            11f,
            Color.LTGRAY
        )

        card.addView(detail)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            this.progress = if (active) 100 else 0
            progressTintList = ColorStateList.valueOf(
                if (active) green else cyan
            )
            progressBackgroundTintList =
                ColorStateList.valueOf(Color.rgb(35, 48, 65))
        }

        card.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply {
                setMargins(dp(12), dp(10), dp(12), dp(10))
            }
        )

        val button = makeButton(
            if (active) "ANTI-HACK WSP ACTIVADO"
            else "ACTIVAR ANTI-HACK WSP",
            if (active) green else blueButton
        ) {}

        addButton(card, button)

        button.setOnClickListener {
            if (whatsappScanTimer != null) return@setOnClickListener

            val currentlyActive = prefs.getBoolean(
                "whatsapp_protection_active",
                false
            )

            if (currentlyActive) {
                prefs.edit()
                    .putBoolean("whatsapp_protection_active", false)
                    .apply()

                shield.animate().cancel()
                shield.rotation = 0f
                shield.setTextColor(cyan)

                status.text = "SISTEMA EN ESPERA"
                status.setTextColor(cyan)
                detail.text = "Presiona el botón para comenzar"
                progress.progress = 0
                progress.progressTintList =
                    ColorStateList.valueOf(cyan)

                button.text = "ACTIVAR ANTI-HACK WSP"
                button.background = GradientDrawable().apply {
                    setColor(blueButton)
                    cornerRadius = dp(12).toFloat()
                }

                toast("ANTI-HACK WSP desactivado.")
            } else {
                button.isEnabled = false
                button.text = "REVISANDO CUENTA..."
                status.text = "REVISANDO CUENTA..."
                status.setTextColor(cyan)
                detail.text = "Realizando revisión visual..."
                shield.setTextColor(cyan)

                progress.progress = 0
                progress.progressTintList =
                    ColorStateList.valueOf(cyan)

                whatsappScanAnimator = ObjectAnimator.ofFloat(
                    shield,
                    View.ROTATION,
                    0f,
                    360f
                ).apply {
                    duration = 1200
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    start()
                }

                whatsappScanTimer =
                    object : android.os.CountDownTimer(5000, 100) {

                        override fun onTick(millisUntilFinished: Long) {
                            val percent =
                                ((5000L - millisUntilFinished) * 100 / 5000)
                                    .toInt()

                            progress.progress = percent
                            status.text = "REVISANDO CUENTA..."
                            detail.text = "Revisión visual... $percent%"
                        }

                        override fun onFinish() {
                            whatsappScanTimer = null
                            whatsappScanAnimator?.cancel()
                            whatsappScanAnimator = null

                            shield.rotation = 0f
                            shield.setTextColor(green)
                            progress.progress = 100
                            progress.progressTintList =
                                ColorStateList.valueOf(green)

                            status.text = "REVISIÓN VISUAL COMPLETADA"
                            status.setTextColor(green)
                            detail.text = "PROCESO FINALIZADO"

                            prefs.edit()
                                .putBoolean(
                                    "whatsapp_protection_active",
                                    true
                                )
                                .apply()

                            button.text = "ANTI-HACK WSP ACTIVADO"
                            button.isEnabled = true
                            button.background = GradientDrawable().apply {
                                setColor(green)
                                cornerRadius = dp(12).toFloat()
                            }

                            toast("REVISIÓN VISUAL COMPLETADA")
                        }
                    }.start()
            }
        }
    }

    private fun logout() {
        vpnSimulationTimer?.cancel()
        vpnSimulationTimer = null

        vpnOverlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        vpnOverlayView = null

        overlayTimer?.cancel()
        overlayTimer = null

        whatsappScanTimer?.cancel()
        whatsappScanTimer = null

        whatsappScanAnimator?.cancel()
        whatsappScanAnimator = null

        loggedIn = false
        showLogin()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        vpnSimulationTimer?.cancel()
        vpnSimulationTimer = null
        vpnOverlayView = null

        overlayTimer?.cancel()
        overlayTimer = null

        whatsappScanTimer?.cancel()
        whatsappScanTimer = null

        whatsappScanAnimator?.cancel()
        whatsappScanAnimator = null

        super.onDestroy()
    }
}
