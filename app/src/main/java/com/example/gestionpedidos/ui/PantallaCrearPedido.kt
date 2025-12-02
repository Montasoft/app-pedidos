package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.*
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearPedido(
    viewModel: PedidosViewModel,
    onAgregarProductoClick: () -> Unit,
    onGuardarPedido: (Pedido) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- Leemos el estado DIRECTAMENTE del ViewModel ---
    val proveedores by viewModel.proveedores.collectAsState()
    val proveedorSeleccionado by viewModel.proveedorSeleccionado.collectAsState()
    val detallesPedido by viewModel.detallesPedido.collectAsState()

    val totalPedido by remember(detallesPedido) { // Recalcular cuando cambien los detalles
        derivedStateOf {
            detallesPedido.sumOf { it.cantidadPedida * it.precioEsperado }
        }
    }

    // 🔹 Cargar proveedores y productos al abrir la pantalla
    LaunchedEffect(Unit) {

        if (viewModel.apiUrl.isNotBlank()){
            viewModel.iniciarCargaDeDatos(context)
        } else{
            snackbarHostState.showSnackbar("No se ha configurado la URL del servidor.")
    }


    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo Pedido") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarProductoClick) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando datos del servidor...", color = MaterialTheme.colorScheme.primary)
                }
            }
            return@Scaffold
        }

        viewModel.errorMessage?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ $it",
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = {
                       viewModel.iniciarCargaDeDatos(context, forzarActualizacion = true)
                    }) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            // -------------------
            // Selector de proveedor
            // -------------------
            Text("Proveedor", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

            Box {
                OutlinedTextField(
                    value = proveedorSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    label = { Text("Seleccionar proveedor") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "▲" else "▼")
                        }
                    }
                )

                DropdownMenu(
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
                                    viewModel.seleccionarProveedor(proveedor)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -------------------
            // Lista de productos agregados
            // -------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Productos agregados", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${detallesPedido.size} items",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))

            if (detallesPedido.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Presiona + para agregar productos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
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
                                    Text(
                                        "Cantidad: ${detalle.cantidadPedida.toInt()}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Precio: $${detalle.precioEsperado}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Subtotal: $${detalle.cantidadPedida * detalle.precioEsperado}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        // --- Notificamos el evento al ViewModel ---
                                        viewModel.eliminarProductoDelPedido(detalle)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("🗑️ Producto eliminado")
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -------------------
            // Total y guardar
            // -------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                            "$$totalPedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        enabled = proveedorSeleccionado != null && detallesPedido.isNotEmpty() && !viewModel.isLoading,
                        onClick = {
                            // ✅ LLAMAMOS A LA NUEVA FUNCIÓN DEL VIEWMODEL
                            viewModel.guardarPedidoEnServidor(
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Pedido guardado en el servidor")
                                    }
                                    // Opcional: navegar a otra pantalla si es necesario, por ejemplo:
                                    // onPedidoGuardadoExitosamente()
                                },
                                onError = { errorMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("💥 $errorMsg")
                                    }
                                }
                            )
                        }
                    ) {
                        if (viewModel.isLoading) {
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
    }
}