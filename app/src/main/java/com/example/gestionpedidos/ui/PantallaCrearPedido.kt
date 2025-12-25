package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.PedidosViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearPedido(
    viewModel: PedidosViewModel,
    onAgregarProductoClick: () -> Unit,
    onGuardarPedido: () -> Unit, //  Solo notificar éxito, no pasar el pedido
    onVolver: () -> Unit = {}
) {
    // ✅ Observar StateFlows correctamente
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val apiUrl by viewModel.apiUrl.collectAsState()
    val proveedores by viewModel.proveedores.collectAsState()
    val proveedorSeleccionado by viewModel.proveedorSeleccionado.collectAsState()
    val detallesPedido by viewModel.detallesPedido.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val formato = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    // ✅  Calcular total
    val totalPedido by remember(detallesPedido) {
        derivedStateOf {
            detallesPedido.sumOf { it.cantidadPedida * it.precioEsperado }
        }
    }

    // ✅ Cargar datos solo si es necesario
    LaunchedEffect(Unit) {
        if (apiUrl.isBlank()) {
            snackbarHostState.showSnackbar("⚠️ No se ha configurado la URL del servidor.")
        } else if (proveedores.isEmpty()) {
            viewModel.iniciarCargaDeDatos(forzarActualizacion = false)
        }
    }

    // ✅ Observar el canal de snackbar del ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Pedido") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isLoading && errorMessage == null) {
                FloatingActionButton(
                    onClick = onAgregarProductoClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        // ✅  Manejo de estados con Box y when
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Estado de carga
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando datos del servidor...",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Estado de error
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, // Cambiar por error icon si tienes
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "⚠️ $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.iniciarCargaDeDatos(forzarActualizacion = true)
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                // Contenido principal
                else -> {
                    ContenidoCrearPedido(
                        proveedores = proveedores,
                        proveedorSeleccionado = proveedorSeleccionado,
                        detallesPedido = detallesPedido,
                        totalPedido = totalPedido,
                        formato = formato,
                        isLoading = isLoading,
                        onSeleccionarProveedor = { viewModel.seleccionarProveedor(it) },
                        onEliminarProducto = {
                            viewModel.eliminarProductoDelPedido(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("🗑️ Producto eliminado")
                            }
                        },
                        onGuardarPedido = {
                            viewModel.guardarPedidoEnServidor(
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Pedido guardado correctamente")
                                        // LIMPIAMOS LOS FILTROS PARA EL PRÓXIMO PEDIDO
                                        viewModel.limpiarSeleccionDeFiltros()
                                        onGuardarPedido()
                                    }
                                },
                                onError = { errorMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("❌ Error: $errorMsg")
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

// ✅ Componente separado para mejor organización
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoCrearPedido(
    proveedores: List<com.example.gestionpedidos.Proveedor>,
    proveedorSeleccionado: com.example.gestionpedidos.Proveedor?,
    detallesPedido: List<com.example.gestionpedidos.DetallePedido>,
    totalPedido: Double,
    formato: NumberFormat,
    isLoading: Boolean,
    onSeleccionarProveedor: (com.example.gestionpedidos.Proveedor) -> Unit,
    onEliminarProducto: (com.example.gestionpedidos.DetallePedido) -> Unit,
    onGuardarPedido: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // -------------------
        // Selector de proveedor
        // -------------------
        Text(
            "Proveedor",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = proveedorSeleccionado?.nombre ?: "Seleccionar proveedor",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (proveedores.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay proveedores disponibles") },
                        onClick = { expanded = false }
                    )
                } else {
                    proveedores.forEach { proveedor ->
                        DropdownMenuItem(
                            text = { Text(proveedor.nombre) },
                            onClick = {
                                onSeleccionarProveedor(proveedor)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // -------------------
        // Encabezado de productos
        // -------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Productos agregados",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "${detallesPedido.size} items",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // -------------------
        // Lista de productos
        // -------------------
        if (detallesPedido.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📦",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No hay productos agregados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Presiona el botón + para agregar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = detallesPedido,
                    key = { it.productoId }
                ) { detalle ->
                    ProductoCard(
                        detalle = detalle,
                        formato = formato,
                        onEliminar = { onEliminarProducto(detalle) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // -------------------
        // Total y botón guardar
        // -------------------
        TotalYGuardarCard(
            total = totalPedido,
            formato = formato,
            habilitado = proveedorSeleccionado != null && detallesPedido.isNotEmpty() && !isLoading,
            isLoading = isLoading,
            onGuardar = onGuardarPedido
        )
    }
}

// ✅ Card de producto reutilizable
@Composable
private fun ProductoCard(
    detalle: com.example.gestionpedidos.DetallePedido,
    formato: NumberFormat,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    detalle.productoNombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Cant: ${detalle.cantidadPedida.toInt()}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "×",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formato.format(detalle.precioEsperado),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Subtotal: ${formato.format(detalle.cantidadPedida * detalle.precioEsperado)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ✅ Card de total y botón guardar
@Composable
private fun TotalYGuardarCard(
    total: Double,
    formato: NumberFormat,
    habilitado: Boolean,
    isLoading: Boolean,
    onGuardar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Total del pedido",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    formato.format(total),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Button(
                enabled = habilitado,
                onClick = onGuardar
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Pedido")
                }
            }
        }
    }
}