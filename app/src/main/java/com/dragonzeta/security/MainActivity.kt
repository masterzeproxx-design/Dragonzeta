private fun validateFirebaseAccess(email: String, uid: String) {

    val connectivityManager =
        getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager

    val activeNetwork = connectivityManager?.activeNetwork
    val capabilities =
        connectivityManager?.getNetworkCapabilities(activeNetwork)

    val hasInternet =
        capabilities?.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true

    val isValidated =
        capabilities?.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
        ) == true

    val firebaseUser = auth.currentUser

    if (firebaseUser == null) {
        rejectFirebaseAccess(
            "DIAGNÓSTICO FIREBASE\n\n" +
            "ERROR: No existe una sesión autenticada.\n" +
            "UID solicitado: $uid\n" +
            "Internet Android: $hasInternet\n" +
            "Internet validado: $isValidated"
        )
        return
    }

    if (firebaseUser.uid != uid) {
        rejectFirebaseAccess(
            "ERROR: UID DIFERENTE\n\n" +
            "UID recibido: $uid\n" +
            "UID Firebase: ${firebaseUser.uid}"
        )
        return
    }

    db.enableNetwork()
        .addOnSuccessListener {

            db.collection("usuarios")
                .document(uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener { userDoc ->

                    if (!userDoc.exists()) {
                        rejectFirebaseAccess(
                            "No existe el perfil para UID: $uid"
                        )
                        return@addOnSuccessListener
                    }

                    val active = userDoc.getBoolean("activo") == true
                    val role = userDoc.getString("rol")?.trim() ?: "USUARIO"
                    val profileEmail = userDoc.getString("correo")?.trim() ?: ""

                    if (!active) {
                        rejectFirebaseAccess("Tu cuenta está desactivada.")
                        return@addOnSuccessListener
                    }

                    if (email.equals(adminEmail, ignoreCase = true)) {

                        if (!role.equals("ADMIN", ignoreCase = true)) {
                            rejectFirebaseAccess(
                                "El rol encontrado es: $role"
                            )
                            return@addOnSuccessListener
                        }

                        if (!profileEmail.equals(email, ignoreCase = true)) {
                            rejectFirebaseAccess(
                                "El correo del perfil no coincide: $profileEmail"
                            )
                            return@addOnSuccessListener
                        }

                        currentEmail = email
                        currentRole = "ADMIN"
                        licenseExpiration = "ACCESO PERMANENTE"
                        licenseDaysRemaining = -1

                        loggedIn = true
                        loginInProgress = false

                        showHome()
                        toast("BIENVENIDO ADMINISTRADOR")
                        return@addOnSuccessListener
                    }

                    db.collection("licenses")
                        .document(uid)
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .addOnSuccessListener { licenseDoc ->

                            if (!licenseDoc.exists()) {
                                rejectFirebaseAccess(
                                    "No tienes una licencia registrada."
                                )
                                return@addOnSuccessListener
                            }

                            val licenseActive =
                                licenseDoc.getBoolean("activo") == true

                            val expiration =
                                licenseDoc.getTimestamp("expiresAt")

                            if (!licenseActive || expiration == null) {
                                rejectFirebaseAccess(
                                    "Tu licencia no está activa o no tiene vencimiento."
                                )
                                return@addOnSuccessListener
                            }

                            val expirationDate = expiration.toDate()
                            val remaining =
                                expirationDate.time - System.currentTimeMillis()

                            if (remaining <= 0L) {
                                rejectFirebaseAccess(
                                    "Tu licencia de 365 días ha vencido."
                                )
                                return@addOnSuccessListener
                            }

                            licenseDaysRemaining =
                                ((remaining + 86_399_999L) / 86_400_000L).toInt()

                            licenseExpiration = java.text.SimpleDateFormat(
                                "dd/MM/yyyy",
                                java.util.Locale.getDefault()
                            ).format(expirationDate)

                            currentEmail = email
                            currentRole = role

                            loggedIn = true
                            loginInProgress = false

                            showHome()

                            toast(
                                "LICENCIA ACTIVA: $licenseDaysRemaining días restantes."
                            )
                        }
                        .addOnFailureListener { error ->

                            val code =
                                (error as? com.google.firebase.firestore.FirebaseFirestoreException)
                                    ?.code?.name ?: "SIN_CODIGO_FIRESTORE"

                            rejectFirebaseAccess(
                                "ERROR AL LEER LICENCIA\n\n" +
                                "Tipo: ${error.javaClass.name}\n" +
                                "Código: $code\n" +
                                "Mensaje: ${error.message}\n" +
                                "Internet Android: $hasInternet\n" +
                                "Internet validado: $isValidated\n" +
                                "UID: $uid"
                            )
                        }
                }
                .addOnFailureListener { error ->

                    val code =
                        (error as? com.google.firebase.firestore.FirebaseFirestoreException)
                            ?.code?.name ?: "SIN_CODIGO_FIRESTORE"

                    rejectFirebaseAccess(
                        "ERROR FIRESTORE AL LEER PERFIL\n\n" +
                        "Tipo: ${error.javaClass.name}\n" +
                        "Código: $code\n" +
                        "Mensaje: ${error.message}\n\n" +
                        "Internet Android: $hasInternet\n" +
                        "Internet validado: $isValidated\n" +
                        "Firebase Auth UID: ${firebaseUser.uid}\n" +
                        "UID solicitado: $uid\n" +
                        "Correo: $email"
                    )
                }
        }
        .addOnFailureListener { error ->

            rejectFirebaseAccess(
                "NO SE PUDO ACTIVAR LA CONEXIÓN FIREBASE\n\n" +
                "Tipo: ${error.javaClass.name}\n" +
                "Mensaje: ${error.message}\n" +
                "Internet Android: $hasInternet\n" +
                "Internet validado: $isValidated\n" +
                "UID: $uid"
            )
        }
}
