package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestionpedidos.PedidosViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(
    viewModel: PedidosViewModel,
    onVolver: () -> Unit
) {
    val formato = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val totalGeneral = viewModel.comprasEnviadas.sumOf { compra ->
        compra.detallesCompra.sumOf { it.cantidadCompra * it.precioUnitario }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Compras") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen General", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total de compras enviadas: ${viewModel.comprasEnviadas.size}")
                    Text("Total gastado: ${formato.format(totalGeneral)}", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.comprasEnviadas.reversed()) { compra ->
                    val total = compra.detallesCompra.sumOf { it.cantidadCompra * it.precioUnitario }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Compra #${compra.idCompra}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text(compra.proveedor)
                                    Text(compra.fechaCompra, fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Enviada", tint = Color(0xFF4CAF50))
                                    Text(formato.format(total), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            compra.detallesCompra.forEach { detalle ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
        }
    }
}
