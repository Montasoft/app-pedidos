package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.Compra
import com.example.gestionpedidos.PedidosViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaComprasPendientes(
    viewModel: PedidosViewModel,
    onEliminarCompra: (Compra) -> Unit,
    onVolver: () -> Unit
) {
    // ✅ Observar StateFlow en lugar de acceso directo
    val compras by viewModel.comprasFlow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val formato = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    var compraAEliminar by remember { mutableStateOf<Compra?>(null) }
    var mostrarDialogoEnviar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compras Pendientes (${compras.size})") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // ✅ Botón para enviar todas las compras
                    if (compras.isNotEmpty()) {
                        IconButton(onClick = { mostrarDialogoEnviar = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar todas",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // ✅ Manejar estado de carga
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // ✅  Manejar lista vacía
                compras.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay compras pendientes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Las compras confirmadas aparecerán aquí",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ✅ Mostrar lista de compras
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = compras,
                            key = { compra -> compra.idCompra }
                        ) { compra ->
                            CompraCard(
                                compra = compra,
                                formato = formato,
                                onEliminar = { compraAEliminar = compra }
                            )
                        }

                        // Espacio adicional al final
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // ✅ CORRECCIÓN 5: Diálogo de confirmación para eliminar
        if (compraAEliminar != null) {
            AlertDialog(
                onDismissRequest = { compraAEliminar = null },
                title = { Text("Eliminar compra") },
                text = {
                    Text("¿Estás seguro de eliminar la compra #${compraAEliminar!!.idCompra} de ${compraAEliminar!!.proveedor}?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onEliminarCompra(compraAEliminar!!)
                            compraAEliminar = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { compraAEliminar = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // ✅ NUEVO: Diálogo para enviar todas las compras
        if (mostrarDialogoEnviar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoEnviar = false },
                title = { Text("Enviar compras") },
                text = {
                    Text("¿Deseas enviar todas las ${compras.size} compras pendientes al servidor?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // TODO: Implementar función de envío masivo
                            // viewModel.enviarTodasLasCompras()
                            mostrarDialogoEnviar = false
                        }
                    ) {
                        Text("Enviar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoEnviar = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// ✅ NUEVO: Componente reutilizable para las cards de compra
@Composable
private fun CompraCard(
    compra: Compra,
    formato: NumberFormat,
    onEliminar: () -> Unit
) {
    val total = compra.detallesCompra.sumOf { it.cantidadCompra * it.precioUnitario }
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado de la compra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compra #${compra.idCompra}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Proveedor: ${compra.proveedor}",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Fecha: ${compra.fechaCompra}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${compra.detallesCompra.size} productos",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${formato.format(total)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onEliminar) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Botón para expandir/colapsar detalles
                if (compra.detallesCompra.isNotEmpty()) {
                    TextButton(
                        onClick = { expandido = !expandido },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (expandido) "Ocultar detalles ▲" else "Ver detalles ▼",
                            fontSize = 13.sp
                        )
                    }
                }

                // Detalles expandibles
                if (expandido) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Productos:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    compra.detallesCompra.forEach { detalle ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detalle.productoNombre,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (detalle.codigoDeBarras.isNotEmpty()) {
                                    Text(
                                        text = "Código: ${detalle.codigoDeBarras}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "${detalle.cantidadCompra} × ${formato.format(detalle.precioUnitario)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Subtotal de esta compra
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formato.format(total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ✅ PREVIEW para desarrollo
/*
@Preview(showBackground = true)
@Composable
fun PreviewCompraCard() {
    MaterialTheme {
        CompraCard(
            compra = Compra(
                idCompra = 1,
                fechaCompra = "06/12/2025",
                proveedor = "Proveedor XYZ",
                estado = "pendiente",
                detallesCompra = mutableListOf(
                    DetalleCompra(
                        productoId = 1,
                        productoNombre = "Producto A",
                        codigoDeBarras = "123456",
                        cantidadCompra = 5.0,
                        precioUnitario = 10000.0
                    )
                )
            ),
            formato = NumberFormat.getCurrencyInstance(Locale("es", "CO")),
            onEliminar = {}
        )
    }
}
*/