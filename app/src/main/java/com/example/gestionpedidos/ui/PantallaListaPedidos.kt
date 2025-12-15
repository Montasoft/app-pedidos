package com.example.gestionpedidos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.Pedido
import com.example.gestionpedidos.PedidosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaPedidos(
    viewModel: PedidosViewModel,
    onPedidoSeleccionado: (Pedido) -> Unit,
    onVolver: () -> Unit
) {
    // ✅ Observar StateFlows
    val pedidosFiltrados by viewModel.pedidosFiltrados().collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()
    val filtroProveedor by viewModel.filtroProveedor.collectAsState()
    val filtroEstado by viewModel.filtroEstado.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var mostrarFiltros by remember { mutableStateOf(false) }

    // ✅ Obtener proveedores únicos de forma reactiva
    val proveedoresUnicos = remember(pedidosFiltrados) {
        pedidosFiltrados.map { it.proveedor }.distinct().sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Pedidos (${pedidosFiltrados.size})") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Indicador de filtros activos
                    if (filtroProveedor != null || filtroEstado != null) {
                        Badge(
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            val count = listOfNotNull(filtroProveedor, filtroEstado).size
                            Text("$count")
                        }
                    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ✅ Barra de búsqueda
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { viewModel.setBusqueda(it) },
                    label = { Text("Buscar") },
                    placeholder = { Text("Producto, proveedor o código...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setBusqueda("") }) {
                                Icon(Icons.Default.Clear, "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true
                )

                // ✅ Panel de filtros
                if (mostrarFiltros) {
                    FiltrosCard(
                        filtroProveedor = filtroProveedor,
                        filtroEstado = filtroEstado,
                        proveedoresUnicos = proveedoresUnicos,
                        onFiltroProveedorChange = { viewModel.setFiltroProveedor(it) },
                        onFiltroEstadoChange = { viewModel.setFiltroEstado(it) },
                        onLimpiarFiltros = {
                            viewModel.setFiltroProveedor(null)
                            viewModel.setFiltroEstado(null)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ✅ Manejo de estados
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Cargando pedidos...")
                            }
                        }
                    }

                    pedidosFiltrados.isEmpty() -> {
                        EstadoVacio(
                            tieneFiltrosActivos = filtroProveedor != null ||
                                    filtroEstado != null ||
                                    busqueda.isNotEmpty(),
                            onLimpiarFiltros = {
                                viewModel.setBusqueda("")
                                viewModel.setFiltroProveedor(null)
                                viewModel.setFiltroEstado(null)
                            }
                        )
                    }

                    else -> {
                        ListaPedidos(
                            pedidos = pedidosFiltrados,
                            onPedidoClick = onPedidoSeleccionado
                        )
                    }
                }
            }
        }
    }
}

// ✅ Componente de filtros
@Composable
private fun FiltrosCard(
    filtroProveedor: String?,
    filtroEstado: String?,
    proveedoresUnicos: List<String>,
    onFiltroProveedorChange: (String?) -> Unit,
    onFiltroEstadoChange: (String?) -> Unit,
    onLimpiarFiltros: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (filtroProveedor != null || filtroEstado != null) {
                    TextButton(onClick = onLimpiarFiltros) {
                        Text("Limpiar todo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro de Proveedor
            Text(
                "Proveedor:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filtroProveedor == null,
                    onClick = { onFiltroProveedorChange(null) },
                    label = { Text("Todos", fontSize = 12.sp) }
                )
                proveedoresUnicos.take(3).forEach { proveedor ->
                    FilterChip(
                        selected = filtroProveedor == proveedor,
                        onClick = { onFiltroProveedorChange(proveedor) },
                        label = { Text(proveedor, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro de Estado
            Text(
                "Estado:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroEstado == null,
                    onClick = { onFiltroEstadoChange(null) },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtroEstado == "abierto",
                    onClick = { onFiltroEstadoChange("abierto") },
                    label = { Text("Abiertos") }
                )
                FilterChip(
                    selected = filtroEstado == "cerrado",
                    onClick = { onFiltroEstadoChange("cerrado") },
                    label = { Text("Cerrados") }
                )
            }
        }
    }
}

// ✅ Componente de estado vacío
@Composable
private fun EstadoVacio(
    tieneFiltrosActivos: Boolean,
    onLimpiarFiltros: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (tieneFiltrosActivos) Icons.Default.SearchOff else Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (tieneFiltrosActivos)
                    "No se encontraron pedidos"
                else
                    "No hay pedidos disponibles",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (tieneFiltrosActivos)
                    "Intenta con otros filtros"
                else
                    "Los pedidos aparecerán aquí",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tieneFiltrosActivos) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onLimpiarFiltros) {
                    Text("Limpiar filtros")
                }
            }
        }
    }
}

// ✅ Componente de lista
@Composable
private fun ListaPedidos(
    pedidos: List<Pedido>,
    onPedidoClick: (Pedido) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = pedidos,
            key = { pedido -> pedido.idPedido }
        ) { pedido ->
            PedidoCard(
                pedido = pedido,
                onClick = { onPedidoClick(pedido) }
            )
        }

        // Espacio adicional al final
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ✅ Card de pedido
@Composable
private fun PedidoCard(
    pedido: Pedido,
    onClick: () -> Unit
) {
    val confirmados = pedido.detallesPedido.count { it.confirmado }
    val total = pedido.detallesPedido.size
    val progreso = if (total > 0) confirmados.toFloat() / total else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pedido #${pedido.idPedido}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        pedido.proveedor,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Fecha: ${pedido.fechaPedido}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Badge(
                    containerColor = when (pedido.estadoNombre.lowercase()) {
                        "cerrado", "completado" -> MaterialTheme.colorScheme.primary
                        "abierto", "pendiente" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                ) {
                    Text(
                        pedido.estadoNombre.uppercase(),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progreso
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Productos confirmados:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$confirmados / $total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
        }
    }
}