package com.dragonzeta.security

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 10, 23)
    private val cyan = Color.rgb(44, 216, 255)
    private val amber = Color.rgb(255, 166, 65)
    private val prefs by lazy { getSharedPreferences("dragon_zeta", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun panel(color: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.rgb(8, 23, 45))
            cornerRadius = radius
            setStroke((2 * resources.displayMetrics.density).toInt(), color)
        }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(20), dp(24), dp(20), dp(24))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(3, 8, 19), Color.rgb(6, 24, 48), Color.rgb(3, 8, 19)))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

    private fun addButton(parent: LinearLayout, title: String, subtitle: String, color: Int, action: () -> Unit) {
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = panel(color)
            setPadding(dp(12), dp(18), dp(12), dp(18))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        b.addView(label(title, 23f, color, true))
        b.addView(label(subtitle, 13f, Color.WHITE))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112))
        lp.setMargins(0, dp(8), 0, dp(8))
        parent.addView(b, lp)
    }

    private fun showHome() {
        val root = base()
        root.addView(label("DRAGÓN ZETA", 34f, Color.rgb(255, 205, 135), true))
        root.addView(label("SEGURIDAD PERSONAL", 15f, Color.WHITE, true))
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = panel(cyan, 28f)
            setPadding(dp(16), dp(20), dp(16), dp(20))
        }
        hero.addView(label("🛡️", 56f, cyan))
        hero.addView(label("Tu centro de seguridad", 20f, Color.WHITE, true))
        hero.addView(label("Accesos rápidos para pedir ayuda y revisar la privacidad de tu Android.", 14f, Color.LTGRAY))
        val hp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        hp.setMargins(0, dp(24), 0, dp(20))
        root.addView(hero, hp)

        addButton(root, "🆘  SOS", "Llamar o enviar un mensaje de emergencia", cyan) { showSos() }
        addButton(root, "🎙️  ANTI-CHUPONEO", "Revisar micrófono, cámara y privacidad", amber) { showAntiSpy() }
        addButton(root, "📍  ANTI-RASTREO", "Revisar permisos y acceso a ubicación", amber) { showAntiTrack() }
        root.addView(label("DRAGÓN ZETA  •  Versión 1.0", 11f, Color.GRAY))
        setContentView(root)
    }

    private fun header(root: LinearLayout, title: String, description: String) {
        root.addView(label("DRAGÓN ZETA", 25f, Color.rgb(255, 205, 135), true))
        root.addView(label(title, 21f, cyan, true))
        root.addView(label(description, 14f, Color.LTGRAY))
    }

    private fun actionButton(root: LinearLayout, text: String, color: Int = cyan, action: () -> Unit) {
        val b = Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            background = panel(color, 16f)
            isAllCaps = false
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply {
            setMargins(0, dp(8), 0, dp(8))
        })
    }

    private fun backButton(root: LinearLayout) = actionButton(root, "← Volver al inicio", Color.GRAY) { showHome() }

    private fun showSos() {
        val root = base()
        header(root, "SOS — AYUDA DE EMERGENCIA",
            "Configura un número de confianza. La app abrirá el marcador o tu aplicación de SMS; tú confirmas la llamada o el envío.")
        root.addView(label("Número de emergencia / contacto de confianza", 14f, Color.WHITE, true))
        val phone = EditText(this).apply {
            hint = "Ej.: +51 999 999 999"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            setText(prefs.getString("sos_phone", ""))
            background = panel(cyan, 12f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(phone, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        actionButton(root, "Guardar contacto") {
            prefs.edit().putString("sos_phone", phone.text.toString().trim()).apply()
            Toast.makeText(this, "Contacto guardado en este teléfono", Toast.LENGTH_SHORT).show()
        }
        actionButton(root, "LLAMAR — abrir marcador", cyan) {
            val number = phone.text.toString().trim()
            if (number.isBlank()) toast("Escribe primero un número.")
            else startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
        }
        actionButton(root, "ENVIAR SOS — preparar SMS", amber) {
            val number = phone.text.toString().trim()
            if (number.isBlank()) toast("Escribe primero un número.")
            else {
                val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
                sms.putExtra("sms_body", "SOS: necesito ayuda. Por favor, comunícate conmigo.")
                try { startActivity(sms) } catch (_: Exception) { toast("No hay una aplicación de SMS disponible.") }
            }
        }
        root.addView(label("Importante: no se realiza ninguna llamada ni se envía ningún SMS automáticamente.", 12f, Color.LTGRAY))
        backButton(root)
        setContentView(root)
    }

    private fun showAntiSpy() {
        val root = base()
        header(root, "ANTI-CHUPONEO",
            "Revisa qué aplicaciones tienen acceso al micrófono y la cámara. Android controla estos permisos desde Ajustes.")
        root.addView(label("Consejos rápidos", 18f, Color.WHITE, true))
        root.addView(label("• Retira el permiso de micrófono o cámara a apps que no lo necesiten.\n\n• Revisa el panel de privacidad para ver accesos recientes, si tu versión de Android lo ofrece.\n\n• Desinstala aplicaciones que no reconozcas y mantén Android actualizado.", 14f, Color.LTGRAY))
        actionButton(root, "Abrir panel de privacidad", cyan) { openSettings(Settings.ACTION_PRIVACY_SETTINGS) }
        actionButton(root, "Abrir ajustes de esta aplicación", amber) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        backButton(root)
        setContentView(root)
    }

    private fun showAntiTrack() {
        val root = base()
        header(root, "ANTI-RASTREO",
            "Administra el acceso a tu ubicación y revisa qué aplicaciones pueden conocerla.")
        root.addView(label("Recomendaciones", 18f, Color.WHITE, true))
        root.addView(label("• Desactiva la ubicación cuando no la necesites.\n\n• Cambia el permiso de cada app a “Solo mientras se usa” o “No permitir”, según corresponda.\n\n• Revisa los permisos de ubicación precisa y el historial/servicios de ubicación de tu dispositivo.", 14f, Color.LTGRAY))
        actionButton(root, "Abrir ajustes de ubicación", cyan) { openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) }
        actionButton(root, "Abrir panel de privacidad", amber) { openSettings(Settings.ACTION_PRIVACY_SETTINGS) }
        backButton(root)
        setContentView(root)
    }

    private fun openSettings(action: String) {
        try { startActivity(Intent(action)) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
