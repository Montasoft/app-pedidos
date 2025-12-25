package com.example.gestionpedidos.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionarProducto(
    viewModel: PedidosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val productos by viewModel.productos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var busqueda by remember { mutableStateOf("") }
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsState()

    val categorias = remember(productos) {
        productos.map { it.categoriaNombre }.distinct().sorted()
    }

    LaunchedEffect(Unit) {
        if (productos.isEmpty()) {
            viewModel.iniciarCargaDeDatos(forzarActualizacion = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

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
                        onCategoriaChange = { viewModel.seleccionarCategoria (it) },
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
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
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

        FiltroCategoria(
            categoriaSeleccionada = categoriaSeleccionada,
            categorias = categorias,
            onCategoriaChange = onCategoriaChange
        )

        Spacer(Modifier.height(12.dp))

        val productosFiltrados = remember(productos, busqueda, categoriaSeleccionada) {
            productos.filter {
                (categoriaSeleccionada == null || it.categoriaNombre == categoriaSeleccionada) &&
                        (busqueda.isBlank() ||
                                it.nombre.contains(busqueda, ignoreCase = true) ||
                                it.codigoBarras?.contains(busqueda, ignoreCase = true) == true)
            }
        }

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
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
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

enum class UltimoEditado {
    CANTIDAD,
    COSTO_UNITARIO,
    SUBTOTAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoItem(
    producto: Producto,
    onAgregar: (Double, Double) -> Unit
) {
    // Estados
    var cantidad by remember { mutableStateOf(TextFieldValue("1")) }
    var costo by remember {
        mutableStateOf(
            TextFieldValue(
                text = producto.costo.toString(),
                selection = TextRange(producto.costo.toString().length)
            )
        )
    }
    var subtotal by remember {
        mutableStateOf(
            TextFieldValue(
                text = producto.costo.toString(),
                selection = TextRange(producto.costo.toString().length)
            )
        )
    }

    var ultimoEditado by remember { mutableStateOf(UltimoEditado.CANTIDAD) }

    // Interaction sources
    val cantidadInteraction = remember { MutableInteractionSource() }
    val costoInteraction = remember { MutableInteractionSource() }
    val subtotalInteraction = remember { MutableInteractionSource() }

    val cantidadFocused by cantidadInteraction.collectIsFocusedAsState()
    val costoFocused by costoInteraction.collectIsFocusedAsState()
    val subtotalFocused by subtotalInteraction.collectIsFocusedAsState()

    // Seleccionar texto SOLO al ganar foco
    LaunchedEffect(cantidadFocused) {
        if (cantidadFocused) {
            cantidad = cantidad.copy(selection = TextRange(0, cantidad.text.length))
        }
    }

    LaunchedEffect(costoFocused) {
        if (costoFocused) {
            costo = costo.copy(selection = TextRange(0, costo.text.length))
        }
    }

    LaunchedEffect(subtotalFocused) {
        if (subtotalFocused) {
            subtotal = subtotal.copy(selection = TextRange(0, subtotal.text.length))
        }
    }

    // Helpers
    fun Double.format2() = String.format("%.2f", this)

    fun actualizarDesdeCantidad(nuevaCantidad: TextFieldValue) {
        cantidad = nuevaCantidad
        val cant = nuevaCantidad.text.toDoubleOrNull() ?: return
        val cost = costo.text.toDoubleOrNull() ?: producto.costo

        if (ultimoEditado == UltimoEditado.SUBTOTAL) {
            val nuevoCosto = if (cant > 0) (subtotal.text.toDoubleOrNull() ?: 0.0) / cant else cost
            val texto = nuevoCosto.format2()
            costo = TextFieldValue(texto, TextRange(texto.length))
        } else {
            val nuevoSubtotal = cant * cost
            val texto = nuevoSubtotal.format2()
            subtotal = TextFieldValue(texto, TextRange(texto.length))
        }
    }

    fun actualizarDesdeCosto(nuevoCosto: TextFieldValue) {
        costo = nuevoCosto
        ultimoEditado = UltimoEditado.COSTO_UNITARIO

        val cant = cantidad.text.toDoubleOrNull() ?: return
        val cost = nuevoCosto.text.toDoubleOrNull() ?: return

        val texto = (cant * cost).format2()
        subtotal = TextFieldValue(texto, TextRange(texto.length))
    }

    fun actualizarDesdeSubtotal(nuevoSubtotal: TextFieldValue) {
        subtotal = nuevoSubtotal
        ultimoEditado = UltimoEditado.SUBTOTAL

        val cant = cantidad.text.toDoubleOrNull() ?: return
        val sub = nuevoSubtotal.text.toDoubleOrNull() ?: return
        if (cant <= 0) return

        val texto = (sub / cant).format2()
        costo = TextFieldValue(texto, TextRange(texto.length))
    }

    // Detectar orientación
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp >
            configuration.screenHeightDp


    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(8.dp)) {

            // información del producto
            Text(producto.nombre,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold)

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
                }
                Column {
                    Text(
                        // producto.costo.aPesos(),
                        producto.categoriaNombre,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Controles - Layout responsivo
            if (isLandscape) {
                // Horizontal: todo en una fila
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth() )
                {
                    CantidadField(
                        value = cantidad,
                        interaction = cantidadInteraction,
                        onChange = ::actualizarDesdeCantidad,
                        onPlus = {
                            val v = (cantidad.text.toIntOrNull() ?: 1) + 1
                            actualizarDesdeCantidad(
                                TextFieldValue(
                                    v.toString(),
                                    TextRange(v.toString().length)
                                )
                            )
                        },
                        onMinus = {
                            val v = (cantidad.text.toIntOrNull() ?: 1)
                            if (v > 1) {
                                val n = v - 1
                                actualizarDesdeCantidad(
                                    TextFieldValue(
                                        n.toString(),
                                        TextRange(n.toString().length)
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )

                    MoneyField(
                        label = "Costo unitario",
                        value = costo,
                        interaction = costoInteraction,
                        onChange = ::actualizarDesdeCosto,
                        modifier = Modifier.weight(1f)
                    )

                    MoneyField(
                        label = "Subtotal",
                        value = subtotal,
                        interaction = subtotalInteraction,
                        onChange = ::actualizarDesdeSubtotal,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val cant = cantidad.text.toDoubleOrNull() ?: 1.0
                            val cost = costo.text.toDoubleOrNull() ?: producto.costo
                            onAgregar(cant, cost)
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Agregar")
                    }
                }
            }else{
                // vertical: dos filas
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // primera fila: Cantidad y Costo unitario
                    //*********
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth() )
                    {
                        CantidadField(
                            value = cantidad,
                            interaction = cantidadInteraction,
                            onChange = ::actualizarDesdeCantidad,
                            onPlus = {
                                val v = (cantidad.text.toIntOrNull() ?: 1) + 1
                                actualizarDesdeCantidad(
                                    TextFieldValue(
                                        v.toString(),
                                        TextRange(v.toString().length)
                                    )
                                )
                            },
                            onMinus = {
                                val v = (cantidad.text.toIntOrNull() ?: 1)
                                if (v > 1) {
                                    val n = v - 1
                                    actualizarDesdeCantidad(
                                        TextFieldValue(
                                            n.toString(),
                                            TextRange(n.toString().length)
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        )

                        MoneyField(
                            label = "Costo unitario",
                            value = costo,
                            interaction = costoInteraction,
                            onChange = ::actualizarDesdeCosto,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // segunda fila: Subtotal y Boton Agregar
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth() )
                    {
                        MoneyField(
                            label = "Subtotal",
                            value = subtotal,
                            interaction = subtotalInteraction,
                            onChange = ::actualizarDesdeSubtotal,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val cant = cantidad.text.toDoubleOrNull() ?: 1.0
                                val cost = costo.text.toDoubleOrNull() ?: producto.costo
                                onAgregar(cant, cost)
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Agregar")
                        }
                    }


                    //*********************
                }
            }


        }
    }
}

@Composable
private fun CantidadField(
    value: TextFieldValue,
    interaction: MutableInteractionSource,
    onChange: (TextFieldValue) -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Text("Cantidad", fontSize = 12.sp)
        Modifier.padding(bottom = 4.dp)
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {

            IconButton(onClick = onMinus) {
                Icon(Icons.Default.Remove, null)
            }

            BasicTextField(
                value = value,
                onValueChange = onChange,
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )

            IconButton(onClick = onPlus) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: TextFieldValue,
    interaction: MutableInteractionSource,
    onChange: (TextFieldValue) -> Unit,
    modifier: Modifier
)   {
    Column(modifier) {
        Text(label, fontSize = 12.sp)
        Modifier.padding(
            bottom = 4.dp,
            start = 12.dp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prefijo $
                Text(
                    text = "$",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            }
        }
    }
}


@Composable
private fun BotonAgregar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Filled.Check, "Agregar", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductoItem() {
    val productoDeEjemplo = Producto(
        id = 1,
        nombre = "7 CEREALES VAINILLA X60G",
        costo = 1893.0,
        codigoBarras = "7708624784919",
        presentacionId = 101,
        categoriaNombre = "Viveres",
        precioVenta = 2500.0,
        existencias = 93.0,
        categoriaId = 1,
        subcategoriaId = 1,
        estadoId = 1,
        subcategoriaNombre = "subcategoria",
        estadoNombre = "estado"
    )

    ProductoItem(
        producto = productoDeEjemplo,
        onAgregar = { cantidad, costo ->
            println("Preview: Agregar $cantidad a un costo de $costo")
        }
    )
}