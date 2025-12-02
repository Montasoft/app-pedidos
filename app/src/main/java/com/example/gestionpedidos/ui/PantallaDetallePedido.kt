package com.example.gestionpedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.Compra
import com.example.gestionpedidos.DetalleCompra
import com.example.gestionpedidos.DetallePedido
import com.example.gestionpedidos.Pedido
import com.example.gestionpedidos.PedidosViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetallePedido(
    pedido: Pedido,
    compraActual: Compra,
    onDetalleSeleccionado: (DetallePedido) -> Unit,
    onEditarDetalle: (DetallePedido) -> Unit,
    onEliminarDetalle: (DetallePedido) -> Unit,
    onVolver: () -> Unit,
    onFinalizarCompra: (Compra) -> Unit
) {
    val formato = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val total = remember { derivedStateOf { compraActual.detallesCompra.sumOf { it.cantidadCompra * it.precioUnitario } } }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }
    val detalles = pedido.detallesPedido

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedido #${pedido.idPedido}") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Proveedor: ${compraActual.proveedor}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Fecha: ${compraActual.fechaCompra}")
                    Text("Total: ${formato.format(total.value)}", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { mostrarDialogoConfirmacion = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = compraActual.detallesCompra.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar Compra")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(detalles) { detalle ->
                val detalleCompra = compraActual.detallesCompra.find { it.productoId == detalle.productoId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !detalle.confirmado) {
                            if (!detalle.confirmado) onDetalleSeleccionado(detalle)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(detalle.productoNombre, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Código: ${detalle.codigoDeBarras}", fontSize = 12.sp, color = Color.Gray)

                            if (detalle.confirmado && detalleCompra != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cantidad: ${detalleCompra.cantidadCompra}", fontSize = 14.sp, color = Color(0xFF2E7D32))
                                Text("Precio: ${formato.format(detalleCompra.precioUnitario)}", fontSize = 14.sp, color = Color(0xFF2E7D32))
                                Text("Subtotal: ${formato.format(detalleCompra.cantidadCompra * detalleCompra.precioUnitario)}",
                                    fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color(0xFF2E7D32))
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cantidad pedida: ${detalle.cantidadPedida}", fontSize = 14.sp)
                                Text("Precio esperado: ${formato.format(detalle.precioEsperado)}", fontSize = 14.sp)
                            }
                        }

                        if (detalle.confirmado) {
                            Row {
                                IconButton(onClick = { onEditarDetalle(detalle) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onEliminarDetalle(detalle) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F))
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Agregar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (mostrarDialogoConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoConfirmacion = false },
                title = { Text("Confirmar compra") },
                text = { Text("¿Deseas finalizar esta compra por un total de ${NumberFormat.getCurrencyInstance(Locale("es","CO")).format(total.value)}?") },
                confirmButton = {
                    Button(onClick = {
                        mostrarDialogoConfirmacion = false
                        onFinalizarCompra(compraActual)
                    }) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoConfirmacion = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
