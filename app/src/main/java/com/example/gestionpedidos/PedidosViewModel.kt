package com.example.gestionpedidos

// ==========================
// File: PedidosViewModel.kt
// ==========================

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.type
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
//import androidx.datastore.core.use
//import androidx.paging.map
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.io.path.outputStream

class PedidosViewModel : ViewModel() {
    // API + estados visibles por Compose
    var apiUrl by mutableStateOf("")
    var pedidos by mutableStateOf<List<Pedido>>(emptyList())
    var compras by mutableStateOf<List<Compra>>(emptyList())

    // Declara un StateFlow PRIVADO y mutable para uso interno
    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    private val _proveedores = MutableStateFlow<List<Proveedor>>(emptyList())

    // Expón un StateFlow PÚBLICO e inmutable para que la UI lo observe
    val productos: StateFlow<List<Producto>> = _productos.asStateFlow()
    val proveedores: StateFlow<List<Proveedor>> = _proveedores.asStateFlow()

    // --- ESTADOS DEL PEDIDO EN CURSO (¡NUEVO!) ---
    private val _proveedorSeleccionado = MutableStateFlow<Proveedor?>(null)
    val proveedorSeleccionado: StateFlow<Proveedor?> = _proveedorSeleccionado.asStateFlow()

    private val _detallesPedido = MutableStateFlow<List<DetallePedido>>(emptyList())
    val detallesPedido: StateFlow<List<DetallePedido>> = _detallesPedido.asStateFlow()

    var comprasEnviadas by mutableStateOf<List<Compra>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var filtroProveedor by mutableStateOf<String?>(null)
    var filtroEstado by mutableStateOf<String?>(null)
    var busqueda by mutableStateOf("")

    // Estados de selección / navegación (fuente única de verdad)
    var selectedPedidoId by mutableStateOf<Int?>(null)
    var selectedDetalleId by mutableStateOf<Int?>(null)
    var currentCompra by mutableStateOf<Compra?>(null)

    private val gson = Gson()

    // canal para enviar eventos de Snackbar a la UI
    private val _snackbarChannel = Channel<String>()
    val snackbarFlow = _snackbarChannel.receiveAsFlow()


    // ---------------------------------------------------------------------
    // Carga / guardado pedidos (remoto + local)
    // ---------------------------------------------------------------------
    fun cargarPedidos(context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resultado = withContext(Dispatchers.IO) {
                    val url = URL("${apiUrl.trimEnd('/')}/pedidos")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/json")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode = connection.responseCode

                    if (responseCode in 200..299) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        // 1. Gson parsea usando los modelos *Response
                        val tipoLista = object : TypeToken<List<PedidoResponse>>() {}.type
                        val pedidosDesdeApi = gson.fromJson<List<PedidoResponse>>(response, tipoLista)

                        // 2. Mapeas/Conviertes al modelo de UI
                        val listaPedidosUI = pedidosDesdeApi.map { it.aPedidoDeUI() }

                        // 3. Devuelves la lista ya convertida
                        return@withContext listaPedidosUI
                    } else {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Sin detalles"
                        throw Exception("Error HTTP $responseCode: $errorBody")
                    }
                }
                // Si la llamada a la API fue exitosa:
                pedidos = resultado
                guardarPedidosLocal(context)
                errorMessage = null // Aseguramos que no haya mensaje de error

            } catch (e: Exception) {
                e.printStackTrace() // Mantenemos el log para depuración

                // 1. Cargamos la copia local de los pedidos
                cargarPedidosLocal(context)

                // 2. Verificamos si se cargaron datos locales
                if (pedidos.isNotEmpty()) {
                    // Si hay datos, limpiamos el error principal y notificamos por Snackbar
                    errorMessage = null
                    _snackbarChannel.send("⚠️ No se pudo conectar. Mostrando datos locales.")
                } else {
                    // Si no hay conexión Y TAMPOCO hay datos locales, entonces sí mostramos el error principal
                    errorMessage = "Error al cargar pedidos: ${e.message}"
                }

            } finally {
                isLoading = false
            }
        }
    }

    private fun guardarPedidosLocal(context: Context) {
        try {
            val file = File(context.filesDir, "pedidos.json")
            file.writeText(gson.toJson(pedidos))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarPedidosLocal(context: Context) {
        try {
            val file = File(context.filesDir, "pedidos.json")
            if (file.exists()) {
                val json = file.readText()
                pedidos = gson.fromJson(json, Array<Pedido>::class.java).toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------
    // Compras locales (pendientes) y envío
    // ---------------------
    fun agregarCompra(compra: Compra, context: Context) {
        compra.idCompra = (compras.maxOfOrNull { it.idCompra } ?: 0) + 1
        compras = compras + compra
        guardarComprasLocal(context)
    }

    fun eliminarCompra(compra: Compra, context: Context) {
        compras = compras.filter { it.idCompra != compra.idCompra }
        guardarComprasLocal(context)
    }

    private fun guardarComprasLocal(context: Context) {
        try {
            val file = File(context.filesDir, "compras.json")
            file.writeText(gson.toJson(compras))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cargarComprasLocal(context: Context) {
        try {
            val file = File(context.filesDir, "compras.json")
            if (file.exists()) {
                val json = file.readText()
                compras = gson.fromJson(json, Array<Compra>::class.java).toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun enviarCompras(context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                withContext(Dispatchers.IO) {
                    val url = URL("${apiUrl.trimEnd('/')}/compras")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val jsonCompras = gson.toJson(compras)
                    connection.outputStream.bufferedWriter().use { it.write(jsonCompras) }

                    if (connection.responseCode in 200..299) {
                        comprasEnviadas = comprasEnviadas + compras
                        compras = emptyList()
                        guardarComprasLocal(context)
                        guardarHistorialLocal(context)
                    } else {
                        throw Exception("Error: ${connection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error al enviar compras: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun guardarHistorialLocal(context: Context) {
        try {
            val file = File(context.filesDir, "historial.json")
            file.writeText(gson.toJson(comprasEnviadas))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cargarHistorialLocal(context: Context) {
        try {
            val file = File(context.filesDir, "historial.json")
            if (file.exists()) {
                val json = file.readText()
                comprasEnviadas = gson.fromJson(json, Array<Compra>::class.java).toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------
    // Filtrado / utilidades
    // ---------------------
    fun actualizarEstadoPedido(pedidoId: Int, nuevoEstado: String) {
        pedidos = pedidos.map { pedido ->
            if (pedido.idPedido == pedidoId) pedido.copy(estadoNombre = nuevoEstado) else pedido
        }
    }

    fun pedidosFiltrados(): List<Pedido> {
        var resultado = pedidos

        filtroProveedor?.let { proveedor ->
            resultado = resultado.filter { it.proveedor == proveedor }
        }

        filtroEstado?.let { estado ->
            resultado = resultado.filter { it.estadoNombre == estado }
        }

        if (busqueda.isNotBlank()) {
            resultado = resultado.filter { pedido ->
                // Comprobación segura del proveedor
                val proveedorCoincide = pedido.proveedor.contains(busqueda, ignoreCase = true)

                // ✅ CORRECCIÓN: Comprobación segura de los detalles del pedido
                val detalleCoincide = pedido.detallesPedido?.any { detalle ->
                    (detalle.productoNombre?.contains(busqueda, ignoreCase = true) ?: false) ||
                            (detalle.codigoDeBarras?.contains(busqueda, ignoreCase = true) ?: false)
                } ?: false // Si detallesPedido es null, el resultado de la comprobación es 'false'

                proveedorCoincide || detalleCoincide
            }
        }

        return resultado
    }

    // ---------------------
    // Selection helpers (un solo bloque, sin duplicados)
    // ---------------------
    fun selectPedido(pedido: Pedido) {
        selectedPedidoId = pedido.idPedido
        // Crear/actualizar currentCompra cuando se selecciona un pedido
        currentCompra = Compra(
            idCompra = (compras.maxOfOrNull { it.idCompra } ?: 0) + 1,
            fechaCompra = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            proveedor = pedido.proveedor,
            detallesCompra = mutableListOf()
        )
        // limpiar detalle seleccionado
        selectedDetalleId = null
    }

    fun getSelectedPedido(): Pedido? = selectedPedidoId?.let { id -> pedidos.find { it.idPedido == id } }

    fun selectDetalle(detalle: DetallePedido) {
        selectedDetalleId = detalle.productoId
    }

    fun getSelectedDetalle(): DetallePedido? = getSelectedPedido()?.detallesPedido?.find { it.productoId == selectedDetalleId }

    fun getCompraActual(): Compra? = currentCompra

    // ---------------------
    // Lógica de compra: confirmar/editar/eliminar/finalizar
    // ---------------------
    fun iniciarCompraDesdePedido(pedido: Pedido) {
        currentCompra = Compra(
            idCompra = (compras.maxOfOrNull { it.idCompra } ?: 0) + 1,
            fechaCompra = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            proveedor = pedido.proveedor,
            detallesCompra = mutableListOf()
        )
    }

    fun confirmarProducto(detalle: DetallePedido, cantidad: Double, precio: Double) {
        val compra = currentCompra ?: return

        // marcar como confirmado en el pedido (mutación del modelo pedido)
        getSelectedPedido()?.detallesPedido?.find { it.productoId == detalle.productoId }?.let {
            it.confirmado = true
        }

        // crear/actualizar detalleCompra
        compra.detallesCompra.removeAll { it.productoId == detalle.productoId }
        compra.detallesCompra.add(
            DetalleCompra(
                productoId = detalle.productoId,
                productoNombre = detalle.productoNombre,
                codigoDeBarras = detalle.codigoDeBarras ?: "",
                cantidadCompra = cantidad,
                precioUnitario = precio
            )
        )
    }


    fun iniciarCargaDeDatos(context: Context, forzarActualizacion: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null

            // Corrida EN PARALELO en IO
            val proveedoresJob = async { cargarProveedores(context, forzarActualizacion) }
            val productosJob = async { cargarProductos(context, forzarActualizacion) }

            val proveedoresOK = proveedoresJob.await()
            val productosOK = productosJob.await()

            if (!proveedoresOK && !productosOK) {
                errorMessage = "No se pudieron cargar los datos desde el servidor."
            }

            // Volvemos a main SOLO para actualizar UI
            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }




    fun actualizarProductoEnCompra(detalle: DetallePedido, cantidad: Double, precio: Double) {
        val compra = currentCompra ?: return
        val existente = compra.detallesCompra.find { it.productoId == detalle.productoId }

        if (existente != null) {
            // asumimos que DetalleCompra tiene propiedades var para cantidad/precio
            existente.cantidadCompra = cantidad
            existente.precioUnitario = precio
        } else {
            compra.detallesCompra.add(
                DetalleCompra(
                    productoId = detalle.productoId,
                    productoNombre = detalle.productoNombre,
                    codigoDeBarras = detalle.codigoDeBarras?: "",
                    cantidadCompra = cantidad,
                    precioUnitario = precio
                )
            )
        }
    }

    fun eliminarDetalleDeCompra(detalle: DetallePedido) {
        val compra = currentCompra ?: return
        compra.detallesCompra.removeAll { it.productoId == detalle.productoId }
        // marcar como no confirmado en el pedido
        getSelectedPedido()?.detallesPedido?.find { it.productoId == detalle.productoId }?.let {
            it.confirmado = false
        }
    }

    fun finalizarCompra(compra: Compra, context: Context? = null) {
        // Añadir a compras pendientes
        compras = compras + compra

        // Si el pedido asociado está completamente confirmado, cerrar el pedido
        val pedido = getSelectedPedido()
        if (pedido != null) {
            val todosConfirmados = pedido.detallesPedido.all { it.confirmado }
            if (todosConfirmados) {
                actualizarEstadoPedido(pedido.idPedido, "cerrado")
            }
        }

        // persistir compras locales si se proporciona context
        context?.let { guardarComprasLocal(it) }

        // limpiar selección actual
        currentCompra = null
        selectedPedidoId = null
        selectedDetalleId = null
    }

    // eliminar compra pendiente (por id)
    fun eliminarCompra(compra: Compra) {
        compras = compras.filter { it.idCompra != compra.idCompra }
    }


    // =========================================
    // 🔹 GESTIÓN DE PROVEEDORES
    // =========================================

    fun proveedoresUnicos(): List<String> {
        return pedidos.map { it.proveedor }.distinct().sorted()
    }

    fun seleccionarProveedor(proveedor: Proveedor) {
        _proveedorSeleccionado.value = proveedor
    }


    /**
     * Carga los proveedores desde el servidor o desde un archivo local.
     * Si han pasado más de 24 horas desde la última actualización, fuerza la recarga automáticamente.
     */
    suspend fun cargarProveedores(context: Context, forzarActualizacion: Boolean = false): Boolean {
        var resultado = false
        val file = File(context.filesDir, "proveedores.json")

        isLoading = true

        try {

            if (!forzarActualizacion && file.exists()) {
                withContext(Dispatchers.IO) {
                    val json = file.readText()
                    _proveedores.value = gson.fromJson(json, Array<Proveedor>::class.java).toList()
                }
                resultado = true
                return resultado
            }

            if (apiUrl.isBlank()) {
                errorMessage = "No se ha configurado la URL del servidor."
                return false
            }

            val listaUI = withContext(Dispatchers.IO) {
                val url = URL("${apiUrl.trimEnd('/')}/proveedores")
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "GET"

                val code = con.responseCode
                if (code in 200..299) {
                    val json = con.inputStream.bufferedReader().use { it.readText() }
                    gson.fromJson(json, Array<Proveedor>::class.java).toList()
                } else {
                    val err = con.errorStream?.bufferedReader()?.use { it.readText() }
                    throw Exception("Error HTTP $code: $err")
                }
            }

            _proveedores.value = listaUI

            withContext(Dispatchers.IO) {
                file.writeText(gson.toJson(listaUI))
            }

            resultado = true

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error al cargar proveedores: ${e.message}"

            // Cargar copia local si existe
            if (file.exists()) {
                try {
                    withContext(Dispatchers.IO) {
                        val json = file.readText()
                        _proveedores.value = gson.fromJson(json, Array<Proveedor>::class.java).toList()
                    }
                    resultado = true
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    errorMessage = "Error al leer copia local: ${e2.message}"
                    resultado = false
                }
            }
        } finally {
            isLoading = false
        }

        return resultado
    }

    fun guardarProveedoresLocal(context: Context) {
        try {
            val file = File(context.filesDir, "proveedores.json")
            file.writeText(gson.toJson(_proveedores.value))
            println("💾 Proveedores guardados localmente (${_proveedores.value.size} items).")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cargarProveedoresLocal(context: Context) {
        try {
            val file = File(context.filesDir, "proveedores.json")
            if (file.exists()) {
                val json = file.readText()
                _proveedores.value = gson.fromJson(json, Array<Proveedor>::class.java).toList()
                println("📂 Proveedores cargados desde archivo local: ${_proveedores.value.size}")
            } else {
                println("⚠️ No se encontró archivo local de proveedores.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error al cargar proveedores locales: ${e.message}"
        }
    }


// =========================================
// 🔹 GESTIÓN DE PRODUCTOS
// =========================================

    /**
     * Carga los productos desde el servidor o desde un archivo local.
     * Si han pasado más de 24 horas desde la última actualización, fuerza la recarga automáticamente.
     */
    /**
     * Carga los productos desde el servidor o desde un archivo local.
     * Si han pasado más de 24 horas desde la última actualización, fuerza la recarga automáticamente.
     */
    suspend fun cargarProductos(context: Context, forzarActualizacion: Boolean = false): Boolean {
        var resultado = false
        isLoading = true

        val file = File(context.filesDir, "productos.json")
        val prefs = context.getSharedPreferences("cache_info", Context.MODE_PRIVATE)
        val ultimaActualizacion = prefs.getLong("ultima_actualizacion_productos", 0L)
        val ahora = System.currentTimeMillis()
        val hanPasado24h = (ahora - ultimaActualizacion) > (24 * 60 * 60 * 1000)

        try {

            // 1️⃣ Cargar desde archivo local si aplica
            if (!forzarActualizacion && !hanPasado24h && file.exists()) {
                withContext(Dispatchers.IO) {
                    val json = file.readText()
                    _productos.value = gson.fromJson(json, Array<Producto>::class.java).toList()
                }
                resultado = true
                return resultado    // ← ya no afecta el finally
            }

            // 2️⃣ Validar URL
            if (apiUrl.isBlank()) {
                errorMessage = "No se ha configurado la URL del servidor."
                resultado = false
                return resultado
            }

            // 3️⃣ Descargar del servidor
            val productosUI = withContext(Dispatchers.IO) {
                val url = URL("${apiUrl.trimEnd('/')}/productos")
                val con = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val code = con.responseCode

                if (code in 200..299) {
                    val jsonString = con.inputStream.bufferedReader().use { it.readText() }

                    val type = object : TypeToken<List<ProductoResponse>>() {}.type
                    val listaApi = gson.fromJson<List<ProductoResponse>>(jsonString, type)

                    listaApi.map { it.aProductoDeUI() }
                } else {
                    val err = con.errorStream?.bufferedReader()?.use { it.readText() }
                    throw Exception("Error HTTP $code: $err")
                }
            }

            // Guardar estado y archivo
            _productos.value = productosUI

            withContext(Dispatchers.IO) {
                file.writeText(gson.toJson(productosUI))
            }

            prefs.edit { putLong("ultima_actualizacion_productos", ahora) }

            resultado = true

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error al cargar productos: ${e.message}"

            // 4️⃣ Intentar cargar local si existe
            if (file.exists()) {
                try {
                    withContext(Dispatchers.IO) {
                        val json = file.readText()
                        _productos.value = gson.fromJson(json, Array<Producto>::class.java).toList()
                    }
                    resultado = true
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    errorMessage = "Error al leer copia local: ${e2.message}"
                    resultado = false
                }
            }
        } finally {
            isLoading = false
        }

        return resultado
    }


    /**
     * Guarda manualmente la lista actual de productos en el almacenamiento local.
     */
    fun guardarProductosLocal(context: Context) {
        try {
            val file = File(context.filesDir, "productos.json")
            file.writeText(gson.toJson(_productos.value))
            println("💾 Productos guardados localmente (${_productos.value.size} items).")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Carga los productos directamente desde el archivo local, si existe.
     */
    fun cargarProductosLocal(context: Context) {
        try {
            val file = File(context.filesDir, "productos.json")
            if (file.exists()) {
                val json = file.readText()
                _productos.value = gson.fromJson(json, Array<Producto>::class.java).toList()
                println("📂 Productos cargados desde archivo local: ${_productos.value.size}")
            } else {
                println("⚠️ No se encontró archivo local de productos.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error al cargar productos locales: ${e.message}"
        }
    }

    fun agregarProductoAlPedido(detalle: DetallePedido) {
        val listaActual = _detallesPedido.value.toMutableList()
        // Opcional: si el producto ya existe, actualizarlo en lugar de añadirlo
        val productoExistenteIndex = listaActual.indexOfFirst { it.productoId == detalle.productoId }
        if (productoExistenteIndex != -1) {
            listaActual[productoExistenteIndex] = detalle // O sumar cantidades, etc.
        } else {
            listaActual.add(detalle)
        }
        _detallesPedido.value = listaActual
    }

    fun eliminarProductoDelPedido(detalle: DetallePedido) {
        val listaActual = _detallesPedido.value.toMutableList()
        listaActual.remove(detalle)
        _detallesPedido.value = listaActual
    }

    fun limpiarPedidoActual() {
        _proveedorSeleccionado.value = null
        _detallesPedido.value = emptyList()
    }


    //*************************************************************
    fun guardarPedidoEnServidor(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Verificación de pre-condiciones
        val prov = proveedorSeleccionado.value
        if (prov == null) {
            onError("No se ha seleccionado un proveedor.")
            return
        }
        if (detallesPedido.value.isEmpty()) {
            onError("No hay productos en el pedido.")
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // 1. Construir el objeto de la petición (Request)
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val fechaActual = sdf.format(java.util.Date())
                // Para la fecha de entrega, usamos 7 días en el futuro como ejemplo
                val calendario = java.util.Calendar.getInstance()
                calendario.add(java.util.Calendar.DAY_OF_YEAR, 7)
                val fechaEntrega = sdf.format(calendario.time)

                val detallesRequest = detallesPedido.value.map { detalleUI ->
                    DetallePedidoRequest(
                        productoId = detalleUI.productoId,
                        presentacionId = detalleUI.presentacionId,
                        cantidadPedida = detalleUI.cantidadPedida,
                        precioUnitario = detalleUI.precioEsperado,
                        descuentoPreIva = 0.00,
                        iva = 0.00,
                        otrosImpuestos = 0.50,
                        otrosImpuestosDetalle = null,
                        descuentoPosIva = 0.00,
                        flete = 0.00,
                        neto = detalleUI.precioEsperado * detalleUI.cantidadPedida,
                        observacion = null
                    )
                }

                val pedidoRequest = PedidoRequest(
                    fechaPedido = fechaActual,
                    fechaEntregaEsperada = fechaEntrega,
                    proveedorId = prov.id,
                    detalles = detallesRequest,
                    estado = "requerido",
                    flete = 0.0,
                    observaciones = "",
                    neto = detallesRequest.sumOf { it.precioUnitario * it.cantidadPedida }
                )

                // 2. Enviar la petición POST
                val resultado = withContext(Dispatchers.IO) {
                    val url = URL("${apiUrl.trimEnd('/')}/pedidos")
                    val jsonBody = gson.toJson(pedidoRequest)

                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    // Escribir el cuerpo del JSON en la petición
                    connection.outputStream.use { os ->
                        val input = jsonBody.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        // Éxito
                        "Pedido guardado en el servidor correctamente."
                    } else {
                        // Error
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error desconocido"
                        throw Exception("Error $responseCode: $errorBody")
                    }
                }
                // 3. Notificar a la UI en caso de éxito
                limpiarPedidoActual()
                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                // 4. Notificar a la UI en caso de error
                onError("Error al guardar: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
}