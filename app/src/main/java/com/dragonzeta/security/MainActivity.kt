package com.dragonzeta.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
    private var pendingSOSNumber = ""

    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    private var whatsappScanTimer: android.os.CountDownTimer? = null
    private var whatsappScanAnimator: ObjectAnimator? = null

    private var overlayTimer: android.os.CountDownTimer? = null
    private var overlayFrame: FrameLayout? = null

    companion object {
        private const val REQUEST_CALL_PERMISSION = 100
        private const val REQUEST_NOTIFICATION_SOS = 101
        private const val REQUEST_LOCATION_PERMISSION = 200
        private const val REQUEST_TRACKING_NOTIFICATION = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    override fun onResume() {
        super.onResume()

        if (loggedIn) {
            showHome()
        }
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
            if (bold) typeface = Typeface.DEFAULT_BOLD
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

        val frame = FrameLayout(this)
        frame.setBackgroundColor(bg)

        frame.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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

        header.addView(
            label("🐉 DRAGÓN ZETA", 23f, cyan, true),
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

        root.addView(label("🐉 DRAGÓN ZETA", 27f, cyan, true))
        root.addView(label("SEGURIDAD PERSONAL", 13f, green, true))

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
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
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

        root.addView(label("SISTEMA DE PROTECCIÓN", 11f, Color.GRAY))
        showScreen(root)
    }

    private fun showHome() {
        if (!loggedIn) {
            showLogin()
            return
        }

        val root = createRoot()
        addHeader(root)

        root.addView(label("PANEL DE CONTROL", 22f, Color.WHITE, true))
        root.addView(
            label("CENTRO DE SEGURIDAD PERSONAL", 11f, cyan, true)
        )

        val profile = createCard(root, "👤 PERFIL DE USUARIO", cyan)
        profile.addView(label("ZEPROXX", 23f, Color.WHITE, true))
        profile.addView(label("USUARIO AUTORIZADO", 13f, green, true))
        profile.addView(
            label("ESTADO: SISTEMA DISPONIBLE", 12f, Color.LTGRAY)
        )

        val statusCard = createCard(root, "🛡️ PROTECCIÓN", green)
        statusCard.addView(
            label("DRAGÓN ZETA ESTÁ LISTO", 16f, Color.WHITE, true)
        )
        statusCard.addView(
            label(
                "Controla tus opciones desde este panel",
                12f,
                Color.LTGRAY
            )
        )

        addSOSCard(root)

        // CONTACTO DE EMERGENCIA WHATSAPP
        addWhatsAppEmergencyCard(root)

        // SEGUIMIENTO GPS
        addTrackingCard(root)

        addOption(root, "🎙️ ANTI-CHUPONEO", "spy")
        addOption(root, "📍 ANTI-RASTREO", "track")
        addWhatsAppProtection(root)

        addButton(root, makeButton("CERRAR SESIÓN", amber) {
            logout()
        })

        root.addView(
            label("🐉 DRAGÓN ZETA • VERSIÓN 1.0", 11f, Color.GRAY)
        )

        showScreen(root)
    }

    // SISTEMA SOS
    private fun addSOSCard(root: LinearLayout) {
        val active = prefs.getBoolean("sos_configured", false)
        val number = prefs.getString("sos_phone", "") ?: ""

        val card = createCard(
            root,
            "🆘 BOTÓN SOS",
            if (active) green else red
        )

        if (active) {
            card.addView(
                label("✓ SOS ACTIVADO", 22f, green, true)
            )

            card.addView(
                label(
                    "Número de emergencia: $number",
                    13f,
                    Color.WHITE
                )
            )

            card.addView(
                label(
                    "Dos pulsaciones de VOLUMEN ARRIBA activan la alerta.",
                    12f,
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
                label("Configura tu número de emergencia", 14f, Color.WHITE)
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
                    dp(54)
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
                    "Después de activar, podrás usar dos pulsaciones " +
                        "de volumen arriba.",
                    11f,
                    Color.LTGRAY
                )
            )
        }
    }

    // CONTACTO DE EMERGENCIA WHATSAPP
    private fun addWhatsAppEmergencyCard(root: LinearLayout) {

        val number = prefs.getString(
            "whatsapp_emergency_phone",
            ""
        ) ?: ""

        val card = createCard(
            root,
            "📲 CONTACTO DE EMERGENCIA WHATSAPP",
            green
        )

        card.addView(
            label(
                "Número que recibirá las alertas de emergencia",
                13f,
                Color.WHITE
            )
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
                dp(54)
            )
        )

        addButton(
            card,
            makeButton("GUARDAR CONTACTO DE EMERGENCIA", green) {

                val newNumber = phone.text.toString().trim()

                if (newNumber.isBlank()) {
                    toast("Escribe el número de WhatsApp.")
                } else {
                    prefs.edit()
                        .putString("whatsapp_emergency_phone", newNumber)
                        .apply()

                    toast("✓ CONTACTO DE EMERGENCIA GUARDADO")
                    showHome()
                }
            }
        )

        card.addView(
            label(
                if (number.isNotBlank())
                    "✓ CONTACTO CONFIGURADO"
                else
                    "AÚN NO HAY UN CONTACTO CONFIGURADO",
                12f,
                if (number.isNotBlank()) green else amber,
                true
            )
        )
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

    // PERMISOS DEL SISTEMA SOS Y GPS
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

            REQUEST_LOCATION_PERMISSION -> {
                if (granted) {
                    requestTrackingNotificationPermission()
                } else {
                    toast("Debes permitir la ubicación precisa para usar el GPS.")
                }
            }

            REQUEST_TRACKING_NOTIFICATION -> {
                if (granted) {
                    startGpsTracking()
                } else {
                    toast("Permite las notificaciones para iniciar el seguimiento.")
                }
            }
        }
    }

    // SISTEMA DE SEGUIMIENTO GPS
    private fun addTrackingCard(root: LinearLayout) {
        val active = prefs.getBoolean("tracking_active", false)
        val shareUrl = prefs.getString("tracking_share_url", "") ?: ""

        val card = createCard(
            root,
            "📍 SEGUIMIENTO GPS",
            if (active) green else cyan
        )

        card.addView(
            label(
                if (active) "✓ SEGUIMIENTO EN CURSO"
                else "UBICACIÓN EN ESPERA",
                19f,
                if (active) green else cyan,
                true
            )
        )

        card.addView(
            label(
                "Máximo 1 hora. Puedes detenerlo antes cuando quieras.",
                12f,
                Color.LTGRAY
            )
        )

        card.addView(
            label(
                "Tres pulsaciones de VOLUMEN ABAJO inician el seguimiento " +
                    "cuando los permisos ya están concedidos.",
                11f,
                Color.LTGRAY
            )
        )

        addButton(
            card,
            makeButton(
                if (active) "SEGUIMIENTO ACTIVADO"
                else "ACTIVAR SEGUIMIENTO GPS",
                green
            ) {
                requestTrackingPermissions()
            }
        )

        addButton(
            card,
            makeButton("COMPARTIR UBICACIÓN POR WHATSAPP", cyan) {
                shareTrackingLink()
            }
        )

        addButton(
            card,
            makeButton("DETENER SEGUIMIENTO GPS", red) {
                if (prefs.getBoolean("tracking_active", false)) {
                    SosTrackingService.stopTracking(this)
                    toast("Solicitud para detener el seguimiento enviada.")
                } else {
                    toast("No hay un seguimiento activo.")
                }
            }
        )

        if (shareUrl.isNotBlank()) {
            card.addView(
                label(
                    "ENLACE PRIVADO GENERADO",
                    11f,
                    green,
                    true
                )
            )
        } else {
            card.addView(
                label(
                    "El enlace aparecerá cuando el servidor reciba la primera ubicación.",
                    10f,
                    Color.GRAY
                )
            )
        }
    }

    private fun requestTrackingPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }

        requestTrackingNotificationPermission()
    }

    private fun requestTrackingNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_TRACKING_NOTIFICATION
            )
            return
        }

        startGpsTracking()
    }

    private fun startGpsTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            toast("Falta conceder el permiso de ubicación precisa.")
            return
        }

        try {
            SosTrackingService.startTracking(this)
            toast("Iniciando seguimiento GPS...")
        } catch (e: Exception) {
            toast("No se pudo iniciar el GPS. Abre la aplicación e inténtalo de nuevo.")
        }
    }

    private fun shareTrackingLink() {
        val shareUrl = prefs.getString(
            "tracking_share_url",
            ""
        ) ?: ""

        if (shareUrl.isBlank()) {
            toast("Primero activa el GPS y espera a que se genere el enlace.")
            return
        }

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Mi ubicación - DRAGÓN ZETA"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Estoy compartiendo mi ubicación en tiempo real. " +
                        "Abre este enlace para ver el mapa:\n$shareUrl"
                )
            }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Compartir ubicación con..."
                )
            )
        } catch (e: Exception) {
            toast("No se pudo abrir el menú para compartir.")
        }
    }

    // ANTI-CHUPONEO
    private fun addOption(
        root: LinearLayout,
        title: String,
        key: String
    ) {
        val active = prefs.getBoolean("${key}_active", false)

        val card = createCard(
            root,
            title,
            if (active) green else cyan
        )

        if (key == "spy") {
            card.addView(
                label(
                    if (active) "✓ RED SEGURA • FUNCIÓN ACTIVADA"
                    else "Sistema de análisis de seguridad de red",
                    12f,
                    if (active) green else Color.LTGRAY,
                    active
                )
            )

            addButton(
                card,
                makeButton(
                    if (active) "ANTI-CHUPONEO ACTIVADO"
                    else "ACTIVAR ANTI-CHUPONEO",
                    if (active) green else cyan
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
            val switch = Switch(this).apply {
                text = "Activar función"
                textSize = 14f
                setTextColor(Color.WHITE)
                isChecked = active
            }

            switch.setOnCheckedChangeListener { _, checked ->
                prefs.edit()
                    .putBoolean("${key}_active", checked)
                    .apply()

                toast(
                    if (checked) "Opción activada."
                    else "Opción desactivada."
                )
            }

            card.addView(switch)
        }
    }

    private fun showAntiChuponeoOverlay() {
        val frame = overlayFrame ?: return
        if (overlayTimer != null) return

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(25), dp(20), dp(25))
            setBackgroundColor(Color.argb(190, 1, 7, 15))
        }

        overlay.addView(label("🐉 DRAGÓN ZETA", 24f, cyan, true))
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
            background = GradientDrawable().apply {
                setColor(Color.argb(90, 0, 20, 15))
                setStroke(dp(1), Color.argb(150, 0, 230, 130))
                cornerRadius = dp(12).toFloat()
            }
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

        overlay.addView(
            label("DIAGNÓSTICO VISUAL SIMULADO", 10f, Color.LTGRAY)
        )

        frame.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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

                status.text = "✓ RED SEGURA\nFUNCIÓN ACTIVADA"
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
        val card = createCard(root, "🛡️ ANTI-HACK WSP", cyan)

        card.addView(
            label(
                "Sistema visual de protección de WhatsApp",
                13f,
                Color.LTGRAY
            )
        )

        val active = prefs.getBoolean(
            "whatsapp_protection_active",
            false
        )

        val shield = label(
            "🛡️",
            55f,
            if (active) green else cyan,
            true
        )

        card.addView(
            shield,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(95)
            )
        )

        val status = label(
            if (active) "CUENTA PROTEGIDA" else "SISTEMA EN ESPERA",
            19f,
            if (active) green else cyan,
            true
        )

        card.addView(status)

        val detail = label(
            if (active) "Protección visual activada"
            else "Presiona el botón para comenzar",
            12f,
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
                dp(12)
            ).apply {
                setMargins(dp(12), dp(12), dp(12), dp(12))
            }
        )

        val button = makeButton(
            if (active) "ANTI-HACK WSP ACTIVADO"
            else "ACTIVAR ANTI-HACK WSP",
            if (active) green else cyan
        ) {}

        addButton(card, button)

        card.addView(
            label(
                "La revisión es una simulación visual. " +
                    "No detecta ataques reales ni modifica WhatsApp.",
                10f,
                Color.GRAY
            )
        )

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

                status.text = "PROTECCIÓN DESACTIVADA"
                status.setTextColor(cyan)
                detail.text = "Sistema en espera"
                progress.progress = 0
                progress.progressTintList =
                    ColorStateList.valueOf(cyan)

                button.text = "ACTIVAR ANTI-HACK WSP"
                button.background = GradientDrawable().apply {
                    setColor(cyan)
                    cornerRadius = dp(12).toFloat()
                }

                toast("ANTI-HACK WSP desactivado.")
            } else {
                button.isEnabled = false
                button.text = "REVISANDO CUENTA..."
                status.text = "REVISANDO CUENTA..."
                status.setTextColor(cyan)
                detail.text = "Analizando estado de protección..."
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
                            detail.text =
                                "Verificando protección... $percent%"
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

                            status.text = "CUENTA PROTEGIDA"
                            status.setTextColor(green)
                            detail.text =
                                "PROTECCIÓN ACTIVADA CORRECTAMENTE"

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

                            toast("¡CUENTA PROTEGIDA!")
                        }
                    }.start()
            }
        }
    }

    private fun logout() {
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
        overlayTimer?.cancel()
        overlayTimer = null

        whatsappScanTimer?.cancel()
        whatsappScanTimer = null

        whatsappScanAnimator?.cancel()
        whatsappScanAnimator = null

        super.onDestroy()
    }
}
