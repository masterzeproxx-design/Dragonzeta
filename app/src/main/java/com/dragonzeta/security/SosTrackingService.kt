package com.dragonzeta.security

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class SosTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.dragonzeta.security.START_TRACKING"
        const val ACTION_STOP = "com.dragonzeta.security.STOP_TRACKING"

        private const val BASE_URL =
            "https://dragon-zeta-sos.lidiajimenez-199716.workers.dev"

        private const val CHANNEL_ID = "dragon_zeta_tracking"
        private const val NOTIFICATION_ID = 779
        private const val MAX_DURATION = 60 * 60 * 1000L

        fun startTracking(context: Context) {
            val intent = Intent(context, SosTrackingService::class.java).apply {
                action = ACTION_START
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Abre DRAGÓN ZETA e inténtalo de nuevo.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, SosTrackingService::class.java).apply {
                action = ACTION_STOP
            }

            try {
                context.startService(intent)
            } catch (_: Exception) {
                context.stopService(
                    Intent(context, SosTrackingService::class.java)
                )
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private var trackingStarted = false

    private var sessionId: String? = null
    private var writeToken: String? = null

    private val stopTimer = Runnable {
        finishTracking("SE CUMPLIÓ 1 HORA")
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finishTracking("SEGUIMIENTO CANCELADO")
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (!trackingStarted) {
                    beginTracking()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun beginTracking() {
        if (!hasLocationPermission()) {
            Toast.makeText(
                this,
                "Concede permiso de ubicación desde DRAGÓN ZETA.",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
                )
            }
        } catch (_: Exception) {
            stopSelf()
            return
        }

        trackingStarted = true

        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
            .edit()
            .putBoolean("tracking_active", true)
            .putString("tracking_share_url", "")
            .apply()

        handler.postDelayed(stopTimer, MAX_DURATION)

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                sendLocation(location)
            }

            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {
            }

            override fun onProviderEnabled(provider: String) {
            }

            override fun onProviderDisabled(provider: String) {
            }
        }

        locationListener = listener

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: SecurityException) {
            finishTracking("PERMISO DE UBICACIÓN NO DISPONIBLE")
            return
        }

        Toast.makeText(
            this,
            "SEGUIMIENTO GPS ACTIVADO",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun sendLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        executor.execute {
            try {
                if (sessionId == null || writeToken == null) {
                    createServerSession()
                }

                val id = sessionId ?: return@execute
                val token = writeToken ?: return@execute

                val data = JSONObject().apply {
                    put("id", id)
                    put("writeToken", token)
                    put("latitude", latitude)
                    put("longitude", longitude)
                }

                postRequest("$BASE_URL/api/update", data)
            } catch (_: Exception) {
                // Se volverá a intentar con la siguiente ubicación.
            }
        }
    }

    private fun createServerSession() {
        val response = postRequest(
            "$BASE_URL/api/start",
            JSONObject()
        )

        val id = response.getString("id")
        val token = response.getString("writeToken")
        val shareUrl = response.getString("shareUrl")

        sessionId = id
        writeToken = token

        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
            .edit()
            .putString("tracking_share_url", shareUrl)
            .putBoolean("tracking_active", true)
            .apply()

        handler.post {
            updateNotification("Ubicación activa. Enlace listo para compartir.")

            Toast.makeText(
                this,
                "UBICACIÓN GPS LISTA PARA COMPARTIR",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun postRequest(
        address: String,
        data: JSONObject
    ): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.outputStream.use { output ->
                output.write(data.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            if (responseCode !in 200..299) {
                throw Exception("Error del servidor: $responseCode")
            }

            return if (responseText.isBlank()) {
                JSONObject()
            } else {
                JSONObject(responseText)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun finishTracking(message: String) {
        if (!trackingStarted) {
            stopSelf()
            return
        }

        trackingStarted = false
        handler.removeCallbacks(stopTimer)

        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {
            }
        }
        locationListener = null

        getSharedPreferences("dragon_zeta", MODE_PRIVATE)
            .edit()
            .putBoolean("tracking_active", false)
            .apply()

        val id = sessionId
        val token = writeToken

        if (id != null && token != null) {
            executor.execute {
                try {
                    val data = JSONObject().apply {
                        put("id", id)
                        put("writeToken", token)
                    }
                    postRequest("$BASE_URL/api/stop", data)
                } catch (_: Exception) {
                }
            }
        }

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.cancel(NOTIFICATION_ID)

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()

        executor.shutdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        message: String = "Seguimiento de ubicación activado."
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java)

        val openPendingIntent = PendingIntent.getActivity(
            this,
            780,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(
            this,
            SosTrackingStopReceiver::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            781,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("DRAGÓN ZETA • GPS")
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_delete,
                "DETENER",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(message)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento GPS DRAGÓN ZETA",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopTimer)

        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {
            }
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class SosTrackingStopReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        SosTrackingService.stopTracking(context)
    }
}
