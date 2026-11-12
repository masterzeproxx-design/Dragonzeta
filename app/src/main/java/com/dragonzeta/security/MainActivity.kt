package com.dragonzeta.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.*
import android.text.InputType
import android.animation.ObjectAnimator
import android.animation.ValueAnimator

class MainActivity : Activity() {

    private val bg = Color.rgb(2, 7, 23)
    private val panelColor = Color.rgb(5, 17, 39)
    private val cyan = Color.rgb(0, 205, 255)
    private val green = Color.rgb(0, 240, 90)
    private val red = Color.rgb(255, 35, 65)
    private val amber = Color.rgb(255, 145, 0)
    private val blue = Color.rgb(0, 95, 255)

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
    private var antiOverlayView: View? = null

    companion object {
        private const val REQUEST_CALL_PERMISSION = 100
        private const val REQUEST_NOTIFICATION_SOS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener visibles la hora, batería, señal y navegación
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

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
        return GradientDrawable().apply {
            setColor(
                if (translucent) Color.argb(170, 3, 13, 34)
                else panelColor
            )
            setStroke(dp(2), color)
            cornerRadius = dp(11).toFloat()
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
                typeface = Typeface.create(
                    "sans-serif-condensed",
                    Typeface.BOLD_ITALIC
                )
            }
            setPadding(dp(4), dp(3), dp(4), dp(3))
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
            textSize = 12f
            typeface = Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD_ITALIC
            )
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            background = GradientDrawable().apply {
                setColor(color)
                setStroke(dp(1), Color.WHITE)
                cornerRadius = dp(11).toFloat()
            }
            elevation = dp(3).toFloat()
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }
    }

    private fun addButton(parent: LinearLayout, child: View) {
        parent.addView(
            child,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(43)
            ).apply {
                setMargins(dp(3), dp(4), dp(3), dp(4))
            }
        )
    }

    private fun addButtonRow(
        parent: LinearLayout,
        buttons: List<View>
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        buttons.forEach { button ->
            row.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(43),
                    1f
                ).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
            )
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun createRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(11), dp(4), dp(11), dp(12))
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    // PANTALLA Y MARCA DE AGUA
    private fun showScreen(
        root: LinearLayout,
        watermark: Boolean = false
    ) {
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
        }

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val frame = FrameLayout(this).apply {
            setBackgroundColor(bg)
        }

        if (watermark) {
            val logo = ImageView(this).apply {
                setImageResource(R.drawable.dragon_zeta_logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 0.25f
                setPadding(dp(12), dp(80), dp(12), dp(80))
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

    // TARJETAS COMPACTAS
    private fun createCard(
        root: LinearLayout,
        title: String,
        color: Int
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = panel(color, true)
            elevation = dp(2).toFloat()
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
        )

        val heading = label(title, 15f, color, true)
        heading.gravity = Gravity.CENTER_VERTICAL
        card.addView(heading)

        return card
    }

    // ENCABEZADO
    private fun addHeader(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(1), dp(2), dp(5))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.dragon_zeta_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(48), dp(43))
        )

        header.addView(
            label("DRAGÓN ZETA", 19f, cyan, true),
            LinearLayout.LayoutParams(0, dp(43), 1f)
        )

        val paid = label("PAGADO", 13f, Color.BLACK, true)
        paid.background = GradientDrawable().apply {
            setColor(green)
            setStroke(dp(1), Color.WHITE)
            cornerRadius = dp(25).toFloat()
        }

        header.addView(
            paid,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
            )
        )

        root.addView(header)

        root.addView(
            View(this).apply { setBackgroundColor(cyan) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(2)
            )
        )
    }

    // INICIO DE SESIÓN
    private fun showLogin() {
        val root = createRoot()

        root.addView(label("🐉 DRAGÓN ZETA", 25f, cyan, true))
        root.addView(label("CENTRO DE SEGURIDAD PERSONAL", 12f, green, true))

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
                    dp(155)
                )
            )
        }

        val card = createCard(root, "ACCESO DE USUARIO", cyan)

        val username = EditText(this).apply {
            hint = "Usuario"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }

        card.addView(username)
        card.addView(password)

        addButton(card, makeButton("INGRESAR", green) {
            if (username.text.toString().trim() == accessUser &&
                password.text.toString() == accessPassword
            ) {
                loggedIn = true
                showHome()
            } else {
                toast("Usuario o contraseña incorrectos.")
            }
        })

        root.addView(label("SISTEMA DE PROTECCIÓN", 10f, Color.LTGRAY))
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

        root.addView(label("PANEL DE CONTROL", 23f, Color.WHITE, true))
        root.addView(
            label("CENTRO DE SEGURIDAD PERSONAL", 12f, cyan, true)
        )

        // PERFIL
        val profile = createCard(root, "👤 PERFIL DE USUARIO", cyan)

        val profileRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = label("●", 39f, cyan, true)
        profileRow.addView(
            avatar,
            LinearLayout.LayoutParams(dp(65), dp(60))
        )

        val profileInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        profileInfo.addView(label("ZEPROXX", 21f, Color.WHITE, true))
        profileInfo.addView(
            label("USUARIO AUTORIZADO", 13f, green, true)
        )

        profileRow.addView(
            profileInfo,
            LinearLayout.LayoutParams(0, dp(60), 1f)
        )

        profile.addView(profileRow)
        profile.addView(
            label("ESTADO: SISTEMA DISPONIBLE", 13f, Color.WHITE, true)
        )

        // PROTECCIÓN
        val protection = createCard(root, "🛡️ PROTECCIÓN", green)
        protection.addView(
            label("DRAGÓN ZETA ESTÁ LISTO", 18f, green, true)
        )
        protection.addView(
            label(
                "Controla tus opciones desde este panel",
                12f,
                Color.WHITE
            )
        )

        addSOSCard(root)
        addOption(root, "🎙️ ANTI-CHUPONEO", "spy")
        addOption(root, "🌐 ANTI-RASTREO", "track")
        addWhatsAppProtection(root)

        addButton(root, makeButton("➜  CERRAR SESIÓN", amber) {
            logout()
        })

        root.addView(
            label("🐉 DRAGÓN ZETA • VERSIÓN 1.0", 11f, cyan, true)
        )

        showScreen(root, watermark = true)
    }

    // SISTEMA SOS
    private fun addSOSCard(root: LinearLayout) {
        val active = prefs.getBoolean("sos_configured", false)
        val number = prefs.getString("sos_phone", "") ?: ""

        val card = createCard(root, "🆘 BOTÓN SOS", red)

        if (active) {
            card.addView(label("✓ SOS ACTIVADO", 17f, green, true))
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

            addButtonRow(
                card,
                listOf(
                    makeButton("PROBAR SOS", red) {
                        if (number.isNotBlank()) {
                            SosEmergency.start(this, number)
                        } else {
                            toast("Configura primero tu número SOS.")
                        }
                    },
                    makeButton("CAMBIAR NÚMERO", blue) {
                        prefs.edit()
                            .putBoolean("sos_configured", false)
                            .apply()
                        showHome()
                    }
                )
            )

            addButton(card, makeButton("CONFIGURAR ACTIVACIÓN POR VOLUMEN", green) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                } catch (e: Exception) {
                    toast("No se pudieron abrir los ajustes.")
                }
            })

        } else {
            card.addView(
                label("Configura tu número de emergencia", 12f, Color.WHITE)
            )

            val phone = EditText(this).apply {
                hint = "+51 999 999 999"
                setHintTextColor(Color.LTGRAY)
                setTextColor(Color.WHITE)
                textSize = 17f
                inputType = InputType.TYPE_CLASS_PHONE
                isSingleLine = true
                setText(number)
                background = panel(cyan)
                setPadding(dp(10), dp(5), dp(10), dp(5))
            }

            card.addView(
                phone,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(44)
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
        toast("✓ SOS ACTIVADO CORRECTAMENTE")
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
        val color = if (active) green else cyan

        val card = createCard(root, title, color)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (key == "spy") {
            info.addView(
                label(
                    if (active) "✓ RED SEGURA • FUNCIÓN ACTIVADA"
                    else "Sistema de análisis visual de red",
                    12f,
                    if (active) green else Color.WHITE,
                    true
                )
            )

            val action = makeButton(
                if (active) "DESACTIVAR" else "ACTIVAR",
                if (active) green else blue
            ) {
                if (prefs.getBoolean("spy_active", false)) {
                    prefs.edit().putBoolean("spy_active", false).apply()
                    showHome()
                    toast("ANTI-CHUPONEO DESACTIVADO.")
                } else {
                    showAntiChuponeoOverlay()
                }
            }

            row.addView(info, LinearLayout.LayoutParams(0, dp(47), 1f))
            row.addView(
                action,
                LinearLayout.LayoutParams(dp(112), dp(40))
            )

        } else {
            info.addView(
                label(
                    if (active) "✓ VPN ACTUALIZADA"
                    else "Cambio visual de servidor VPN",
                    12f,
                    if (active) green else Color.WHITE,
                    true
                )
            )

            val action = makeButton(
                if (active) "DESCONECTAR" else "CONECTAR VPN",
                blue
            ) {
                if (vpnSimulationTimer != null) return@makeButton

                if (prefs.getBoolean("track_active", false)) {
                    prefs.edit().putBoolean("track_active", false).apply()
                    showHome()
                    toast("VPN desconectada.")
                } else {
                    showVpnSimulationOverlay()
                }
            }

            row.addView(info, LinearLayout.LayoutParams(0, dp(47), 1f))
            row.addView(
                action,
                LinearLayout.LayoutParams(dp(120), dp(40))
            )
        }

        card.addView(row)
    }

    // VENTANA DE SIMULACIÓN VPN
    private fun showVpnSimulationOverlay() {
        val frame = overlayFrame ?: return
        if (vpnSimulationTimer != null) return

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(215, 0, 4, 16))
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = panel(cyan)
            elevation = dp(15).toFloat()
        }

        card.addView(label("🐉 DRAGÓN ZETA", 22f, cyan, true))
        card.addView(label("SIMULACIÓN DE CONEXIÓN VPN", 13f, green, true))
        card.addView(label("◈", 48f, cyan, true))

        val status = label("INICIANDO CONEXIÓN...", 14f, Color.WHITE, true)
        card.addView(status)

        val server = label("Servidor: seleccionando ubicación", 11f, Color.LTGRAY)
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
                dp(9)
            ).apply {
                setMargins(dp(4), dp(12), dp(4), dp(10))
            }
        )

        val log = label("Preparando simulación...", 10f, green)
        card.addView(log)

        val close = makeButton("CANCELAR", red) {
            closeVpnSimulationOverlay(false)
        }
        addButton(card, close)

        overlay.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                setMargins(dp(20), dp(15), dp(20), dp(15))
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

        vpnSimulationTimer = object : android.os.CountDownTimer(8000, 250) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = 8000L - millisUntilFinished
                val percent = (elapsed * 100 / 8000).toInt()
                progress.progress = percent

                status.text = when {
                    percent < 20 -> "LOCALIZANDO SERVIDOR..."
                    percent < 45 -> "NEGOCIANDO CONEXIÓN..."
                    percent < 70 -> "CREANDO TÚNEL..."
                    else -> "FINALIZANDO..."
                }

                server.text = when {
                    percent < 35 -> "Servidor: buscando ubicación"
                    else -> "Servidor: Ámsterdam, Países Bajos"
                }

                log.text = "PROCESO VISUAL: $percent%"
            }

            override fun onFinish() {
                vpnSimulationTimer = null
                progress.progress = 100
                status.text = "✓ SIMULACIÓN COMPLETADA"
                status.setTextColor(green)
                server.text = "Servidor simulado: Ámsterdam"
                log.text = "PRESENTACIÓN FINALIZADA"

                close.text = "FINALIZAR"
                close.background = GradientDrawable().apply {
                    setColor(green)
                    cornerRadius = dp(11).toFloat()
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
            toast("SIMULACIÓN VPN COMPLETADA")
        }
    }

    // SIMULACIÓN ANTI-CHUPONEO
    private fun showAntiChuponeoOverlay() {
        val frame = overlayFrame ?: return
        if (overlayTimer != null) return

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(Color.argb(235, 1, 7, 19))
        }

        overlay.addView(label("🐉 DRAGÓN ZETA", 23f, cyan, true))
        overlay.addView(label("CYBER SECURITY TERMINAL", 13f, green, true))
        overlay.addView(label("ANÁLISIS VISUAL DEL SISTEMA", 10f, Color.LTGRAY))

        val codeView = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
            gravity = Gravity.START
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setLineSpacing(dp(3).toFloat(), 1f)
            background = panel(green, true)
        }

        overlay.addView(
            codeView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(0, dp(12), 0, dp(12))
            }
        )

        val status = label("INICIANDO ANÁLISIS...", 15f, cyan, true)
        overlay.addView(status)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            this.progress = 0
            progressTintList = ColorStateList.valueOf(green)
            progressBackgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        overlay.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply {
                setMargins(dp(8), dp(10), dp(8), dp(12))
            }
        )

        val closeButton = makeButton("FINALIZAR", green) {
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
        antiOverlayView = overlay

        val codeLines = arrayOf(
            "root@dragonzeta:~$ ./visual_scan.sh",
            "",
            "[INIT] Iniciando presentación...",
            "[ OK ] Módulo visual preparado",
            "[SCAN] Revisando estado visual...",
            "[INFO] Comprobando interfaz...",
            "[ OK ] Secuencia completada",
            "FIREWALL_STATUS: DEMOSTRACIÓN",
            "NETWORK_STATUS: SIMULACIÓN",
            "",
            "root@dragonzeta:~$ scan_complete"
        )

        codeView.text = codeLines.take(2).joinToString("\n")

        overlayTimer = object : android.os.CountDownTimer(7000, 500) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = 7000L - millisUntilFinished
                val percent = (elapsed * 100 / 7000).toInt()
                progress.progress = percent

                val visibleLines = (2 + elapsed / 400)
                    .toInt()
                    .coerceAtMost(codeLines.size)

                codeView.text = codeLines
                    .take(visibleLines)
                    .joinToString("\n")

                status.text = when {
                    percent < 25 -> "INICIANDO SISTEMA..."
                    percent < 50 -> "REVISANDO INTERFAZ..."
                    percent < 75 -> "PROCESANDO..."
                    else -> "FINALIZANDO..."
                }
            }

            override fun onFinish() {
                overlayTimer = null
                progress.progress = 100
                codeView.text = codeLines.joinToString("\n")
                prefs.edit().putBoolean("spy_active", true).apply()

                status.text = "✓ PROCESO VISUAL COMPLETADO"
                status.setTextColor(green)
                closeButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun closeAntiChuponeoOverlay() {
        overlayTimer?.cancel()
        overlayTimer = null

        antiOverlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        antiOverlayView = null

        showHome()
        toast("ANTI-CHUPONEO ACTIVADO.")
    }

    // ANTI-HACK WSP
    private fun addWhatsAppProtection(root: LinearLayout) {
        val active = prefs.getBoolean(
            "whatsapp_protection_active",
            false
        )

        val card = createCard(root, "🛡️ ANTI-HACK WSP", green)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = label("◉", 40f, green, true)
        row.addView(
            icon,
            LinearLayout.LayoutParams(dp(60), dp(60))
        )

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        info.addView(
            label(
                "Sistema visual de protección de WhatsApp",
                11f,
                Color.WHITE
            )
        )

        val status = label(
            if (active) "✓ REVISIÓN VISUAL COMPLETADA"
            else "SISTEMA EN ESPERA",
            12f,
            if (active) green else cyan,
            true
        )
        info.addView(status)

        row.addView(
            info,
            LinearLayout.LayoutParams(0, dp(60), 1f)
        )
        card.addView(row)

        val detail = label(
            if (active) "Proceso visual finalizado"
            else "Presiona para comenzar",
            10f,
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
                ColorStateList.valueOf(Color.DKGRAY)
        }

        card.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(8)
            ).apply {
                setMargins(dp(6), dp(7), dp(6), dp(7))
            }
        )

        val button = makeButton(
            if (active) "ANTI-HACK WSP ACTIVADO"
            else "ACTIVAR ANTI-HACK WSP",
            green
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

                icon.animate().cancel()
                icon.rotation = 0f
                status.text = "SISTEMA EN ESPERA"
                status.setTextColor(cyan)
                detail.text = "Presiona para comenzar"
                progress.progress = 0
                progress.progressTintList =
                    ColorStateList.valueOf(cyan)

                button.text = "ACTIVAR ANTI-HACK WSP"
                toast("ANTI-HACK WSP desactivado.")
            } else {
                button.isEnabled = false
                button.text = "REVISANDO..."
                status.text = "REVISIÓN VISUAL EN CURSO..."
                status.setTextColor(cyan)
                detail.text = "Procesando... 0%"

                whatsappScanAnimator = ObjectAnimator.ofFloat(
                    icon,
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
                            detail.text = "Revisión visual... $percent%"
                        }

                        override fun onFinish() {
                            whatsappScanTimer = null
                            whatsappScanAnimator?.cancel()
                            whatsappScanAnimator = null

                            icon.rotation = 0f
                            status.text = "✓ REVISIÓN VISUAL COMPLETADA"
                            status.setTextColor(green)
                            detail.text = "PROCESO FINALIZADO"
                            progress.progress = 100
                            progress.progressTintList =
                                ColorStateList.valueOf(green)

                            prefs.edit()
                                .putBoolean(
                                    "whatsapp_protection_active",
                                    true
                                )
                                .apply()

                            button.text = "ANTI-HACK WSP ACTIVADO"
                            button.isEnabled = true
                            toast("REVISIÓN VISUAL COMPLETADA")
                        }
                    }.start()
            }
        }
    }

    // CERRAR SESIÓN
    private fun logout() {
        vpnSimulationTimer?.cancel()
        vpnSimulationTimer = null

        vpnOverlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        vpnOverlayView = null

        overlayTimer?.cancel()
        overlayTimer = null

        antiOverlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        antiOverlayView = null

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
        antiOverlayView = null

        whatsappScanTimer?.cancel()
        whatsappScanTimer = null

        whatsappScanAnimator?.cancel()
        whatsappScanAnimator = null

        super.onDestroy()
    }
}
