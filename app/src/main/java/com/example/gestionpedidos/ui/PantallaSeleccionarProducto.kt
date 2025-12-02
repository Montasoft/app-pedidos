package com.example.gestionpedidos.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gestionpedidos.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionarProducto(
    viewModel: PedidosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val productos by viewModel.productos.collectAsState()

    // Extraer las categorías de la lista de productos actual
    val categorias = remember(productos) {
        productos.map { it.categoriaNombre }.distinct().sorted()
    }

    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    // Cargar productos al iniciar (desde local si existe, sino desde servidor)
    LaunchedEffect(Unit) {
        if (productos.isEmpty()) {
            viewModel.cargarProductos(context, forzarActualizacion = false)

            // Mostrar feedback al usuario
            if (viewModel.errorMessage != null) {
                snackbarHostState.showSnackbar("❌ ${viewModel.errorMessage}")
            } else if (productos.isEmpty()) {
                snackbarHostState.showSnackbar("⚠️ No hay productos cargados")
            }
        }
    }

    // 🧩 Escuchar el resultado del escáner
    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.savedStateHandle.get<String>("codigoEscaneado")?.let { codigo ->
                busqueda = codigo
                entry.savedStateHandle.remove<String>("codigoEscaneado")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar productos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para actualizar catálogo desde servidor
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("🔄 Actualizando catálogo...")
                                viewModel.cargarProductos(context, forzarActualizacion = true)
                                if (viewModel.errorMessage == null) {
                                    snackbarHostState.showSnackbar("✅ Catálogo actualizado")
                                } else {
                                    snackbarHostState.showSnackbar("❌ ${viewModel.errorMessage}")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar catálogo")
                    }
                }
            )
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
                    Text("Cargando productos...", color = MaterialTheme.colorScheme.primary)
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

            // 🔍 Campo de búsqueda + botón escáner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar producto") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                    modifier = Modifier.weight(1f)
                )

                Button(onClick = { navController.navigate("escanearCodigo") }) {
                    Text("📷")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 🔽 Filtro por categoría
            var expanded by remember { mutableStateOf(false) }

            Box {
                OutlinedTextField(
                    value = categoriaSeleccionada ?: "Todas",
                    onValueChange = {},
                    label = { Text("Filtrar por categoría") },
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
                    DropdownMenuItem(
                        text = { Text("Todas las categorías") },
                        onClick = {
                            categoriaSeleccionada = null
                            expanded = false
                        }
                    )
                    categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                categoriaSeleccionada = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mostrar contador de productos
            Text(
                text = "Total productos: ${productos.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 📦 Filtrar productos por texto / categoría
            val productosFiltrados = productos.filter {
                (categoriaSeleccionada == null || it.categoriaNombre == categoriaSeleccionada) &&
                        (busqueda.isBlank() ||
                                it.nombre.contains(busqueda, ignoreCase = true) ||
                                it.codigoBarras?.contains(busqueda, ignoreCase = true) == true)
            }

            if (productos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "No hay productos disponibles",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("🔄 Descargando catálogo...")
                                    viewModel.cargarProductos(context, forzarActualizacion = true)
                                }
                            }
                        ) {
                            Text("Cargar catálogo")
                        }
                    }
                }
            } else if (productosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se encontraron productos con esos filtros",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Mostrando ${productosFiltrados.size} productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = productosFiltrados,
                        key = { it.id }
                    ) { producto ->
                        ProductoItem(
                            producto = producto,
                            onAgregar = { cantidad, costo ->
                                val detalle = DetallePedido(
                                    productoId = producto.id,
                                    productoNombre = producto.nombre,
                                    cantidadPedida = cantidad,
                                    precioEsperado = costo,
                                    presentacionId = producto.presentacionId ,
                                    codigoDeBarras = producto.codigoBarras ?: ""
                                )
                                // notificamos al ViewModel que agreague el producto a la lista
                                viewModel.agregarProductoAlPedido(detalle)
                                // navegamos hacia atras
                                navController.popBackStack()
                                scope.launch {
                                    snackbarHostState.showSnackbar("✅ ${producto.nombre} agregado")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductoItem(
    producto: Producto,
    onAgregar: (Double, Double) -> Unit
) {
    var cantidad by remember { mutableStateOf(1.0) }
    var cantidadText by remember { mutableStateOf(TextFieldValue("1")) }
    var costoText by remember { mutableStateOf(TextFieldValue(producto.costo.toString())) }

    // Interacción para seleccionar todo el texto al hacer clic
    val cantidadInteraction = remember { MutableInteractionSource() }
    val costoInteraction = remember { MutableInteractionSource() }

    val cantidadPressed by cantidadInteraction.collectIsPressedAsState()
    val costoPressed by costoInteraction.collectIsPressedAsState()

    LaunchedEffect(cantidadPressed) {
        if (cantidadPressed) {
            cantidadText = cantidadText.copy(
                selection = TextRange(0, cantidadText.text.length)
            )
        }
    }

    LaunchedEffect(costoPressed) {
        if (costoPressed) {
            costoText = costoText.copy(
                selection = TextRange(0, costoText.text.length)
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Información del producto
            Text(
                producto.nombre,
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Código: ${producto.codigoBarras ?: "N/A"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Costo: $${producto.costo}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Controles de cantidad y costo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Control de cantidad con botones +/-
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cantidad",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (cantidad > 1) {
                                    cantidad--
                                    cantidadText = TextFieldValue(cantidad.toInt().toString())
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Disminuir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = cantidadText,
                            onValueChange = { newValue ->
                                cantidadText = newValue
                                cantidad = newValue.text.toDoubleOrNull() ?: 1.0
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            interactionSource = cantidadInteraction
                        )

                        IconButton(
                            onClick = {
                                cantidad++
                                cantidadText = TextFieldValue(cantidad.toInt().toString())
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Aumentar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Campo de costo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Costo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = costoText,
                        onValueChange = { newValue ->
                            costoText = newValue
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        interactionSource = costoInteraction,
                        prefix = { Text("$") }
                    )
                }

                // Botón flotante de agregar
                FloatingActionButton(
                    onClick = {
                        val cantidadFinal = cantidadText.text.toDoubleOrNull() ?: 1.0
                        val costoFinal = costoText.text.toDoubleOrNull() ?: producto.costo
                        onAgregar(cantidadFinal, costoFinal)

                        // Resetear valores
                        cantidad = 1.0
                        cantidadText = TextFieldValue("1")
                        costoText = TextFieldValue(producto.costo.toString())
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Agregar producto",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}