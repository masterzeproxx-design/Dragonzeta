
package com.dragonzeta.security

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object SosEmergency {

    private const val CHANNEL_ID = "dragon_zeta_sos"
    private const val NOTIFICATION_ID = 777

    private val handler = Handler(Looper.getMainLooper())
    private var pendingCall: Runnable? = null

    fun start(context: Context, number: String) {

        if (pendingCall != null) return

        createChannel(context)

        val cancelIntent = Intent(
            context,
            SosCancelReceiver::class.java
        ).apply {
            action = "com.dragonzeta.security.CANCEL_SOS"
        }

        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            778,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }.setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🐉 DRAGÓN ZETA • SOS")
            .setContentText("Emergencia activada. Llamando en 5 segundos.")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "CANCELAR SOS",
                cancelPendingIntent
            )
            .build()

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify(NOTIFICATION_ID, notification)

        Toast.makeText(
            context,
            "SOS ACTIVADO. Tienes 5 segundos para cancelar.",
            Toast.LENGTH_LONG
        ).show()

        pendingCall = Runnable {

            pendingCall = null
            manager.cancel(NOTIFICATION_ID)

            if (context.checkSelfPermission(
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val callIntent = Intent(
                        Intent.ACTION_CALL,
                        Uri.parse("tel:$number")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    context.startActivity(callIntent)

                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "No se pudo realizar la llamada.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Abre Dragon Zeta y concede permiso para llamar.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        handler.postDelayed(pendingCall!!, 5000)
    }

    fun cancel(context: Context) {

        pendingCall?.let {
            handler.removeCallbacks(it)
        }

        pendingCall = null

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.cancel(NOTIFICATION_ID)

        Toast.makeText(
            context,
            "EMERGENCIA CANCELADA",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas SOS Dragón Zeta",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}

class SosCancelReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == "com.dragonzeta.security.CANCEL_SOS") {
            SosEmergency.cancel(context)
        }
    }
}
