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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

class MainActivity : Activity() {

    private val bg = Color.rgb(4, 10, 23)
    private val cyan = Color.rgb(44, 216, 255)
    private val green = Color.rgb(30, 190, 90)
    private val red = Color.rgb(220, 55, 55)
    private val amber = Color.rgb(220, 145, 45)

    private val adminUid = "c1ps7o6YDidnHyFK7Cv3jnq2NFM2"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val prefs by lazy {
        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
    }

    private var timer: CountDownTimer? = null
    private var pendingNumber = ""
    private var licenseValid = false
    private var isAdmin = false

    private lateinit var countdownText: TextView
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (auth.currentUser == null) {
            showLogin()
        } else {
            checkAccess()
        }
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
            if (bold) setTypeface(typeface, Typeface.BOLD)
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
            setOnClickListener { action() }
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

    private fun createRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(bg)
        }
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

    private fun showLogin() {
        val root = createRoot()

        root.addView(label("🐉 DRAGÓN ZETA", 30f, cyan, true))
        root.addView(label("SEGURIDAD PERSONAL", 14f, Color.WHITE, true))
        root.addView(label("INICIAR SESIÓN", 21f, cyan, true))

        val email = EditText(this).apply {
            hint = "Correo electrónico"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val password = EditText(this).apply {
            hint = "Contraseña"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            background = panel(cyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        root.addView(email)
        root.addView(password)

        addButton(root, makeButton("INICIAR SESIÓN", green) {
            val e = email.text.toString().trim()
            val p = password.text.toString()

            if (e.isBlank() || p.isBlank()) {
                toast("Completa correo y contraseña.")
            } else {
                auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener { checkAccess() }
                    .addOnFailureListener {
                        toast("No se pudo iniciar sesión. Revisa tus datos.")
                    }
            }
        })

        addButton(root, makeButton("CREAR CUENTA", cyan) {
            val e = email.text.toString().trim()
            val p = password.text.toString()

            if (e.isBlank() || p.length < 6) {
                toast("Escribe un correo y una contraseña de 6 caracteres o más.")
            } else {
                auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener {
                        toast("Cuenta creada correctamente.")
                        checkAccess()
                    }
                    .addOnFailureListener {
                        toast("No se pudo crear la cuenta: ${it.message}")
                    }
            }
        })

        root.addView(
            label(
                "Crea tu cuenta o ingresa con tu correo y contraseña.",
                13f,
                Color.LTGRAY
            )
        )

        showScreen(root)
    }

    private fun checkAccess() {
        val user = auth.currentUser

        if (user == null) {
            showLogin()
            return
        }

        if (user.uid == adminUid) {
            db.collection("admins").document(user.uid).get()
                .addOnSuccessListener { document ->
                    isAdmin = document.exists()

                    if (isAdmin) {
                        licenseValid = true
                        showHome()
                    } else {
                        toast("La cuenta no tiene autorización de administrador.")
                        logout()
                    }
                }
                .addOnFailureListener {
                    toast("No se pudo verificar el administrador.")
                    showLogin()
                }

            return
        }

        db.collection("licenses").document(user.uid).get()
            .addOnSuccessListener { document ->
                val active = document.getBoolean("active") ?: false
                val expiry = document.getTimestamp("expiresAt")

                licenseValid = active &&
                    expiry != null &&
                    expiry.toDate().after(Date())

                showHome()
            }
            .addOnFailureListener {
                licenseValid = false
                toast("No se pudo consultar la licencia. Revisa tu conexión.")
                showHome()
            }
    }

    private fun showHome() {
        val root = createRoot()

        root.addView(label("🐉 DRAGÓN ZETA", 30f, cyan, true))
        root.addView(label("SEGURIDAD PERSONAL", 14f, Color.WHITE, true))

        val user = auth.currentUser
        root.addView(
            label(user?.email ?: "", 12f, Color.LTGRAY)
        )

        if (isAdmin) {
            root.addView(label("🛡️ ADMINISTRADOR", 16f, green, true))

            addButton(root, makeButton("ABRIR PANEL ADMINISTRADOR", green) {
                showAdminPanel()
            })
        } else {
            root.addView(
                label(
                    if (licenseValid) "🟢 LICENCIA ACTIVA"
                    else "🔴 LICENCIA NO ACTIVA",
                    17f,
                    if (licenseValid) green else red,
                    true
                )
            )

            if (!licenseValid) {
                showPaymentOptions(root)
            } else {
                showLicenseInfo(root)
            }
        }

        // SOS

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

            if (!isAdmin && !licenseValid) {
                toast("Necesitas una licencia activa.")
            } else if (number.isBlank()) {
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

    private fun showLicenseInfo(root: LinearLayout) {
        val user = auth.currentUser ?: return

        db.collection("licenses").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val expiry = doc.getTimestamp("expiresAt")
                if (expiry != null) {
                    val date = android.text.format.DateFormat
                        .format("dd/MM/yyyy", expiry.toDate())
                    root.addView(
                        label("Vence el: $date", 14f, green, true)
                    )
                }
            }
    }

    private fun showPaymentOptions(root: LinearLayout) {
        val card = createCard(root, "💳 ACTIVAR LICENCIA", amber)

        card.addView(
            label("Una sola licencia para todo el sistema", 14f, Color.WHITE)
        )
        card.addView(
            label("YAPE: 991292264", 20f, cyan, true)
        )

        card.addView(label("Mensual: S/ 55", 16f, Color.WHITE))
        addButton(card, makeButton("SOLICITAR PLAN MENSUAL", amber) {
            createPaymentRequest("monthly")
        })

        card.addView(label("Semestral: S/ 200", 16f, Color.WHITE))
        addButton(card, makeButton("SOLICITAR PLAN SEMESTRAL", amber) {
            createPaymentRequest("semiannual")
        })

        card.addView(label("Anual: S/ 400", 16f, Color.WHITE))
        addButton(card, makeButton("SOLICITAR PLAN ANUAL", amber) {
            createPaymentRequest("annual")
        })

        card.addView(
            label(
                "Realiza el pago por Yape y solicita la activación. " +
                    "El administrador verificará el pago antes de aprobarlo.",
                13f,
                Color.LTGRAY
            )
        )
    }

    private fun createPaymentRequest(plan: String) {
        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "plan" to plan,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("paymentRequests").document()
            .set(data)
            .addOnSuccessListener {
                toast("Solicitud enviada. Espera la verificación del administrador.")
            }
            .addOnFailureListener {
                toast("No se pudo enviar la solicitud: ${it.message}")
            }
    }

    private fun addOption(
        root: LinearLayout,
        title: String,
        key: String
    ) {
        val card = createCard(root, title, cyan)

        val status = label("", 16f, Color.WHITE, true)
        card.addView(status)

        var active = prefs.getBoolean("${key}_active", false)

        fun refresh() {
            if (!isAdmin && !licenseValid) active = false

            status.text = when {
                !isAdmin && !licenseValid -> "🔴 LICENCIA NO ACTIVA"
                active -> "🟢 FUNCIÓN ACTIVADA"
                else -> "🔴 FUNCIÓN DESACTIVADA"
            }

            status.setTextColor(
                if (active && (licenseValid || isAdmin)) green else red
            )
        }

        addButton(card, makeButton("ACTIVAR / DESACTIVAR", green) {
            if (!isAdmin && !licenseValid) {
                toast("Necesitas una licencia activa.")
            } else {
                active = !active
                prefs.edit().putBoolean("${key}_active", active).apply()
                refresh()
            }
        })

        refresh()
    }

    private fun showAdminPanel() {
        if (!isAdmin) {
            toast("Acceso exclusivo del administrador.")
            return
        }

        val root = createRoot()
        root.addView(label("🛡️ PANEL ADMINISTRADOR", 24f, cyan, true))

        addButton(root, makeButton("VOLVER", amber) {
            showHome()
        })

        root.addView(
            label("SOLICITUDES DE PAGO", 18f, green, true)
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(list)

        db.collection("paymentRequests").get()
            .addOnSuccessListener { result ->
                list.removeAllViews()

                if (result.isEmpty) {
                    list.addView(
                        label("No hay solicitudes todavía.", 14f, Color.WHITE)
                    )
                }

                for (doc in result.documents) {
                    val uid = doc.getString("uid") ?: continue
                    val email = doc.getString("email") ?: "Sin correo"
                    val plan = doc.getString("plan") ?: ""
                    val status = doc.getString("status") ?: ""

                    val card = createCard(
                        list,
                        "SOLICITUD DE CLIENTE",
                        cyan
                    )

                    card.addView(label(email, 14f, Color.WHITE))
                    card.addView(label("UID: $uid", 10f, Color.LTGRAY))
                    card.addView(label("Plan: ${planName(plan)}", 14f, amber))
                    card.addView(label("Estado: $status", 14f, Color.WHITE))

                    if (status == "pending") {
                        addButton(card, makeButton("APROBAR PAGO Y ACTIVAR", green) {
                            approveRequest(doc.id, uid, plan)
                        })
                    }
                }
            }
            .addOnFailureListener {
                toast("No se pudieron cargar las solicitudes.")
            }

        showScreen(root)
    }

    private fun planName(plan: String): String {
        return when (plan) {
            "monthly" -> "Mensual - S/ 55"
            "semiannual" -> "Semestral - S/ 200"
            "annual" -> "Anual - S/ 400"
            else -> "Desconocido"
        }
    }

    private fun approveRequest(
        requestId: String,
        uid: String,
        plan: String
    ) {
        if (!isAdmin) return

        val months = when (plan) {
            "monthly" -> 1
            "semiannual" -> 6
            "annual" -> 12
            else -> {
                toast("Plan desconocido.")
                return
            }
        }

        db.collection("licenses").document(uid).get()
            .addOnSuccessListener { current ->
                val oldExpiry = current.getTimestamp("expiresAt")?.toDate()
                val now = Date()

                val baseDate = if (
                    oldExpiry != null && oldExpiry.after(now)
                ) oldExpiry else now

                val calendar = Calendar.getInstance()
                calendar.time = baseDate
                calendar.add(Calendar.MONTH, months)

                val newExpiry = calendar.time

                val license = hashMapOf(
                    "active" to true,
                    "plan" to plan,
                    "expiresAt" to Timestamp(newExpiry),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                db.collection("licenses").document(uid)
                    .set(license)
                    .addOnSuccessListener {
                        db.collection("paymentRequests")
                            .document(requestId)
                            .update("status", "approved")
                            .addOnSuccessListener {
                                toast("Licencia activada correctamente.")
                                showAdminPanel()
                            }
                            .addOnFailureListener {
                                toast("Licencia activada, pero no se actualizó la solicitud.")
                                showAdminPanel()
                            }
                    }
                    .addOnFailureListener {
                        toast("No se pudo activar la licencia: ${it.message}")
                    }
            }
            .addOnFailureListener {
                toast("No se pudo consultar la licencia anterior.")
            }
    }

    private fun logout() {
        timer?.cancel()
        auth.signOut()
        licenseValid = false
        isAdmin = false
        showLogin()
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
