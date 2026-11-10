private fun addOption(
    root: LinearLayout,
    title: String,
    key: String
) {
    var paid = prefs.getBoolean("${key}_paid", false)
    var active = prefs.getBoolean("${key}_active", false)

    if (!paid) {
        active = false
        prefs.edit().putBoolean("${key}_active", false).apply()
    }

    val card = createCard(root, title, cyan)

    val status = label("", 16f, Color.WHITE, true)
    card.addView(status)

    var refreshCard: (() -> Unit)? = null

    val paymentButton = makeButton("", amber) {
        paid = !paid
        if (!paid) active = false

        prefs.edit()
            .putBoolean("${key}_paid", paid)
            .putBoolean("${key}_active", active)
            .apply()

        refreshCard?.invoke()
    }

    card.addView(paymentButton)

    val activationButton = makeButton("", green) {
        if (!paid) {
            toast("Primero debes pagar esta opción.")
        } else {
            active = !active

            prefs.edit()
                .putBoolean("${key}_active", active)
                .apply()

            refreshCard?.invoke()
        }
    }

    card.addView(activationButton)

    refreshCard = {
        status.text = when {
            !paid -> "🔴 OPCIÓN NO PAGADA"
            active -> "🟢 SEGURIDAD MÁXIMA"
            else -> "🔴 SEGURIDAD DESACTIVADA"
        }

        status.setTextColor(
            if (paid && active) green else red
        )

        paymentButton.text =
            if (paid) "MARCAR COMO NO PAGADA"
            else "MARCAR COMO PAGADA"

        activationButton.text =
            if (active) "DESACTIVAR"
            else "ACTIVAR"

        activationButton.isEnabled = paid
    }

    refreshCard?.invoke()
}
