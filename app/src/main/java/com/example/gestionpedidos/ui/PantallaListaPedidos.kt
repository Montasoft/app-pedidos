package com.example.gestionpedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.DetallePedido
import com.example.gestionpedidos.Pedido
import com.example.gestionpedidos.PedidosViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaPedidos(
    viewModel: PedidosViewModel,
    onPedidoSeleccionado: (Pedido) -> Unit,
    onVolver: () -> Unit
) {
    var mostrarFiltros by remember { mutableStateOf(false) }
    val pedidosFiltrados = viewModel.pedidosFiltrados()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Pedidos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(
                            if (mostrarFiltros) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                            contentDescription = "Filtros"
                        )
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
            OutlinedTextField(
                value = viewModel.busqueda,
                onValueChange = { viewModel.busqueda = it },
                label = { Text("Buscar") },
                placeholder = { Text("Producto, proveedor o código...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            if (mostrarFiltros) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Filtros", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Proveedor:", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.filtroProveedor == null,
                                onClick = { viewModel.filtroProveedor = null },
                                label = { Text("Todos", fontSize = 12.sp) }
                            )
                            viewModel.proveedoresUnicos().forEach { proveedor ->
                                FilterChip(
                                    selected = viewModel.filtroProveedor == proveedor,
                                    onClick = { viewModel.filtroProveedor = proveedor },
                                    label = { Text(proveedor, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Estado:", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = viewModel.filtroEstado == null,
                                onClick = { viewModel.filtroEstado = null },
                                label = { Text("Todos") }
                            )
                            FilterChip(
                                selected = viewModel.filtroEstado == "abierto",
                                onClick = { viewModel.filtroEstado = "abierto" },
                                label = { Text("Abiertos") }
                            )
                            FilterChip(
                                selected = viewModel.filtroEstado == "cerrado",
                                onClick = { viewModel.filtroEstado = "cerrado" },
                                label = { Text("Cerrados") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pedidosFiltrados.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No se encontraron pedidos", color = Color.Gray)
                            }
                        }
                    }
                }

                items(pedidosFiltrados) { pedido ->
                    val confirmados = pedido.detallesPedido.count { it.confirmado }
                    val total = pedido.detallesPedido.size
                    val progreso = if (total > 0) confirmados.toFloat() / total else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPedidoSeleccionado(pedido) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pedido #${pedido.idPedido}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Text("${pedido.proveedor}", fontSize = 14.sp)
                                    Text("Fecha: ${pedido.fechaPedido}", fontSize = 12.sp, color = Color.Gray)
                                }

                                Badge(
                                    containerColor = if (pedido.estadoNombre == "cerrado")
                                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                                ) {
                                    Text(pedido.estadoNombre.uppercase())
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Productos confirmados:", fontSize = 12.sp)
                                    Text("$confirmados / $total", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = progreso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
