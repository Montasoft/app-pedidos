package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestionpedidos.DetallePedido
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioProducto(
    detalle: DetallePedido,
    onConfirmar: (Double, Double) -> Unit,
    onVolver: () -> Unit
) {
    var cantidad by remember { mutableStateOf(detalle.cantidadPedida.toString()) }
    var precioUnitario by remember { mutableStateOf(detalle.precioEsperado.toString()) }
    var precioTotal by remember { mutableStateOf("") }
    var ultimoCampoEditado by remember { mutableStateOf("") }
    val formato = remember { NumberFormat.getCurrencyInstance(Locale("es","CO")) }

    LaunchedEffect(cantidad, precioUnitario, precioTotal, ultimoCampoEditado) {
        try {
            when (ultimoCampoEditado) {
                "cantidad", "precioUnitario" -> {
                    val cant = cantidad.toDoubleOrNull() ?: 0.0
                    val precio = precioUnitario.toDoubleOrNull() ?: 0.0
                    precioTotal = (cant * precio).toString()
                }
                "precioTotal" -> {
                    val cant = cantidad.toDoubleOrNull() ?: 0.0
                    val total = precioTotal.toDoubleOrNull() ?: 0.0
                    if (cant > 0) precioUnitario = (total / cant).toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Producto") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(detalle.productoNombre, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Código: ${detalle.codigoDeBarras}")
                }
            }

            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it; ultimoCampoEditado = "cantidad" },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Pedida: ${detalle.cantidadPedida}") }
            )

            OutlinedTextField(
                value = precioUnitario,
                onValueChange = { precioUnitario = it; ultimoCampoEditado = "precioUnitario" },
                label = { Text("Precio Unitario (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Esperado: ${formato.format(detalle.precioEsperado)}") }
            )

            OutlinedTextField(
                value = precioTotal,
                onValueChange = { precioTotal = it; ultimoCampoEditado = "precioTotal" },
                label = { Text("Precio Total (COP)") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    val cantVal = cantidad.toDoubleOrNull() ?: 0.0
                    val precioVal = precioUnitario.toDoubleOrNull() ?: 0.0
                    val totalVal = cantVal * precioVal
                    Text("Total a pagar: ${formato.format(totalVal)}", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val cant = cantidad.toDoubleOrNull() ?: 0.0
                    val precio = precioUnitario.toDoubleOrNull() ?: 0.0
                    if (cant > 0 && precio > 0) onConfirmar(cant, precio)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cantidad.toDoubleOrNull() != null &&
                        precioUnitario.toDoubleOrNull() != null &&
                        cantidad.toDoubleOrNull()!! > 0 &&
                        precioUnitario.toDoubleOrNull()!! > 0
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Producto")
            }
        }
    }
}
