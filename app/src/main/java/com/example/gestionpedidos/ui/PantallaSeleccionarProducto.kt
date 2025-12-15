package com.example.gestionpedidos.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gestionpedidos.*
import kotlinx.coroutines.launch
import com.example.gestionpedidos.utils.aPesos
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionarProducto(
    viewModel: PedidosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ CORRECCIÓN 1: Observar StateFlows
    val productos by viewModel.productos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Estados locales
    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    // Extraer categorías
    val categorias = remember(productos) {
        productos.map { it.categoriaNombre }.distinct().sorted()
    }

    // ✅ CORRECCIÓN 2: Cargar productos sin context
    LaunchedEffect(Unit) {
        if (productos.isEmpty()) {
            viewModel.iniciarCargaDeDatos(forzarActualizacion = false)
        }
    }

    // ✅ CORRECCIÓN 3: Observar canal de snackbar
    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    // 🧩 Escuchar el resultado del escáner
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow<String?>("codigoEscaneado", null)
            ?.collect { codigo ->
                codigo?.let {
                    busqueda = it
                    navController.currentBackStackEntry?.savedStateHandle
                        ?.remove<String>("codigoEscaneado")
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
                    // Botón para actualizar catálogo
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.iniciarCargaDeDatos(forzarActualizacion = true)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar catálogo")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ✅ Manejo de estados con when
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando productos...",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                errorMessage != null && productos.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "⚠️ $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.iniciarCargaDeDatos(forzarActualizacion = true)
                                }
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                else -> {
                    ContenidoSeleccionProducto(
                        productos = productos,
                        busqueda = busqueda,
                        categoriaSeleccionada = categoriaSeleccionada,
                        categorias = categorias,
                        onBusquedaChange = { busqueda = it },
                        onCategoriaChange = { categoriaSeleccionada = it },
                        onEscanear = { navController.navigate("escanearCodigo") },
                        onAgregarProducto = { producto, cantidad, costo ->
                            val detalle = DetallePedido(
                                productoId = producto.id,
                                productoNombre = producto.nombre,
                                cantidadPedida = cantidad,
                                precioEsperado = costo,
                                presentacionId = producto.presentacionId,
                                codigoDeBarras = producto.codigoBarras ?: ""
                            )
                            viewModel.agregarProductoAlPedido(detalle)
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

// ✅ Componente de contenido principal
@Composable
private fun ContenidoSeleccionProducto(
    productos: List<Producto>,
    busqueda: String,
    categoriaSeleccionada: String?,
    categorias: List<String>,
    onBusquedaChange: (String) -> Unit,
    onCategoriaChange: (String?) -> Unit,
    onEscanear: () -> Unit,
    onAgregarProducto: (Producto, Double, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 🔍 Campo de búsqueda + botón escáner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusquedaChange,
                label = { Text("Buscar producto") },
                placeholder = { Text("Nombre o código de barras") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            IconButton(
                onClick = onEscanear,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear código",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 🔽 Filtro por categoría
        FiltroCategoria(
            categoriaSeleccionada = categoriaSeleccionada,
            categorias = categorias,
            onCategoriaChange = onCategoriaChange
        )

        Spacer(Modifier.height(12.dp))

        // 📦 Filtrar productos
        val productosFiltrados = remember(productos, busqueda, categoriaSeleccionada) {
            productos.filter {
                (categoriaSeleccionada == null || it.categoriaNombre == categoriaSeleccionada) &&
                        (busqueda.isBlank() ||
                                it.nombre.contains(busqueda, ignoreCase = true) ||
                                it.codigoBarras?.contains(busqueda, ignoreCase = true) == true)
            }
        }

        // Contador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mostrando ${productosFiltrados.size} de ${productos.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (busqueda.isNotEmpty() || categoriaSeleccionada != null) {
                TextButton(
                    onClick = {
                        onBusquedaChange("")
                        onCategoriaChange(null)
                    }
                ) {
                    Text("Limpiar filtros")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Lista de productos
        when {
            productos.isEmpty() -> {
                EstadoVacioProductos()
            }
            productosFiltrados.isEmpty() -> {
                EstadoSinResultados(
                    onLimpiarFiltros = {
                        onBusquedaChange("")
                        onCategoriaChange(null)
                    }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = productosFiltrados,
                        key = { it.id }
                    ) { producto ->
                        ProductoItem(
                            producto = producto,
                            onAgregar = { cantidad, costo ->
                                onAgregarProducto(producto, cantidad, costo)
                            }
                        )
                    }

                    // Espacio final
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ✅  Componente de filtro de categoría
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroCategoria(
    categoriaSeleccionada: String?,
    categorias: List<String>,
    onCategoriaChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = categoriaSeleccionada ?: "Todas las categorías",
            onValueChange = {},
            label = { Text("Categoría") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todas las categorías") },
                onClick = {
                    onCategoriaChange(null)
                    expanded = false
                },
                leadingIcon = {
                    if (categoriaSeleccionada == null) {
                        Icon(Icons.Default.Check, "Seleccionado")
                    }
                }
            )
            categorias.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onCategoriaChange(cat)
                        expanded = false
                    },
                    leadingIcon = {
                        if (categoriaSeleccionada == cat) {
                            Icon(Icons.Default.Check, "Seleccionado")
                        }
                    }
                )
            }
        }
    }
}

// ✅ Estados vacíos
@Composable
private fun EstadoVacioProductos() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No hay productos disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Sincroniza con el servidor para cargar el catálogo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EstadoSinResultados(onLimpiarFiltros: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No se encontraron productos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onLimpiarFiltros) {
                Text("Limpiar filtros")
            }
        }
    }
}


// ✅ Card de producto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductoItem(
    producto: Producto,
    onAgregar: (Double, Double) -> Unit
) {
    var cantidadText by remember { mutableStateOf(TextFieldValue("1")) }
    var costoText by remember { mutableStateOf(TextFieldValue(producto.costo.toString())) }

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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            // Información del producto
            Text(
                producto.nombre,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Código: ${producto.codigoBarras ?: "N/A"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        producto.categoriaNombre,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    producto.costo.aPesos(),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            // Controles
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cantidad
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cantidad",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        //modifier = Modifier.width(70.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val current = cantidadText.text.toDoubleOrNull() ?: 1.0
                                    if (current > 1) {
                                        cantidadText =
                                            TextFieldValue((current - 1).toInt().toString())
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                "Disminuir",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                        BasicTextField(
                            value = cantidadText,
                            onValueChange = { cantidadText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.4f),
                            singleLine = true,
                            interactionSource = cantidadInteraction,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        ) { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = cantidadText.text,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = cantidadInteraction,
                                contentPadding = PaddingValues(
                                    start = 3.dp,
                                    end = 3.dp,
                                    top = 1.dp,
                                    bottom = 1.dp
                                ),

                                container = {
                                    OutlinedTextFieldDefaults.ContainerBox(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = cantidadInteraction,
                                        colors = OutlinedTextFieldDefaults.colors()
                                    )
                                }
                            )
                        }
                        IconButton(
                            onClick = {
                                val current = cantidadText.text.toDoubleOrNull() ?: 1.0
                                cantidadText = TextFieldValue((current + 1).toInt().toString())
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Aumentar")
                        }

                // Costo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Costo unitario",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                       // modifier = Modifier.padding(bottom = 4.dp)

                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = costoText,
                            onValueChange = { costoText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth(),

                            singleLine = true,
                            interactionSource = costoInteraction,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        ) { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = costoText.text,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = costoInteraction,
                                prefix = { Text("$") },
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 1.dp,
                                    bottom = 1.dp
                                ),
                                container = {
                                    OutlinedTextFieldDefaults.ContainerBox(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = costoInteraction,
                                        colors = OutlinedTextFieldDefaults.colors()
                                    )
                                }
                            )
                        }
                    }
                }

                // Botón agregar
                FilledTonalButton(
                    onClick = {
                        val cantidadFinal = cantidadText.text.toDoubleOrNull() ?: 1.0
                        val costoFinal = costoText.text.toDoubleOrNull() ?: producto.costo
                        onAgregar(cantidadFinal, costoFinal)
                    },
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(
                        start = 1.dp,
                        end = 1.dp,
                        top = 13.dp,
                        bottom = 1.dp
                    )
                ) {
                    Icon(Icons.Filled.Check, "Agregar")
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}


@Preview(showBackground = true) // <-- ¡La anotación mágica!
@Composable
private fun PreviewProductoItem() {
    // 1. Creamos un producto de ejemplo (dummy data)
    val productoDeEjemplo = Producto(
        id = 1,
        nombre = "Producto de Ejemplo Muy Largo para Probar Espacios",
        costo = 12500.50,
        codigoBarras = "7701234567890",
        presentacionId = 101,
        categoriaNombre = "Categoría de Prueba",
        precioVenta = 500.0,
        existencias = 14.0,
        categoriaId = 1,
        subcategoriaId = 1,
        estadoId = 1,
        subcategoriaNombre = "subcategoria",
        estadoNombre = "estado"
    )

    // 2. Llamamos al Composable que queremos previsualizar
    ProductoItem(
        producto = productoDeEjemplo,
        onAgregar = { cantidad, costo ->
            // En la previsualización, las acciones no hacen nada.
            // Simplemente puedes imprimir en consola si quieres.
            println("Preview: Agregar $cantidad a un costo de $costo")
        }
    )
}
