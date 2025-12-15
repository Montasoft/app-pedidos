package com.example.gestionpedidos.utils

import java.text.NumberFormat
import java.util.Locale

fun Double.aPesos(): String {
    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    formato.maximumFractionDigits = 0
    return "$ ${formato.format(this)}"
}
