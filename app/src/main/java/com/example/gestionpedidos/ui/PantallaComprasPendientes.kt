package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val formato = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    var compraAEliminar by remember { mutableStateOf<Compra?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compras Pendientes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.compras) { compra ->
                val total = compra.detallesCompra.sumOf { it.cantidadCompra * it.precioUnitario }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Compra #${compra.idCompra}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Proveedor: ${compra.proveedor}")
                                Text("Fecha: ${compra.fechaCompra}", fontSize = 12.sp, color = Color.Gray)
                                Text("Productos: ${compra.detallesCompra.size}")
                                Text("Total: ${formato.format(total)}", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }

                            IconButton(onClick = { compraAEliminar = compra }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFD32F2F))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("Productos:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                        compra.detallesCompra.forEach { detalle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(detalle.productoNombre, fontSize = 14.sp)
                                Text("${detalle.cantidadCompra} × ${formato.format(detalle.precioUnitario)}", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        if (compraAEliminar != null) {
            AlertDialog(
                onDismissRequest = { compraAEliminar = null },
                title = { Text("Eliminar compra") },
                text = { Text("¿Estás seguro de eliminar esta compra?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onEliminarCompra(compraAEliminar!!)
                            compraAEliminar = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { compraAEliminar = null }) { Text("Cancelar") }
                }
            )
        }
    }
}
