// ==========================
// File: PedidosViewModel.kt
// Package: com.example.gestionpedidos
// ==========================

package com.example.gestionpedidos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionpedidos.data.local.AppDatabase
import com.example.gestionpedidos.data.local.dao.*
import com.example.gestionpedidos.data.local.entities.*
import com.example.gestionpedidos.data.mappers.* // Importa los mappers de API
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.outputStream

class PedidosViewModel(private val context: Context) : ViewModel() {

    // ============================================
    // DATABASE & DAOs
    // ============================================
    private val database = AppDatabase.getDatabase(context)
    private val pedidoDao = database.pedidoDao()
    private val detallePedidoDao = database.detallePedidoDao()
    private val productoDao = database.productoDao()
    private val presentacionDao = database.presentacionDao()
    private val proveedorDao = database.proveedorDao()
    private val compraDao = database.compraDao()
    private val detalleCompraDao = database.detalleCompraDao()

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // ============================================
    // FLOWS DESDE LA BASE DE DATOS
    // ============================================

    // Productos desde Room (reactivo)
    val productos: StateFlow<List<Producto>> = productoDao.getAllProductosConPresentaciones()
        .map { lista -> lista.map { it.aProductoDeUI() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Proveedores desde Room (reactivo)
    val proveedores: StateFlow<List<Proveedor>> = proveedorDao.getAllProveedores()
        .map { lista -> lista.map { it.aProveedorDeUI() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pedidos desde Room (reactivo)
    val pedidosFlow: StateFlow<List<Pedido>> = pedidoDao.getAllPedidosConDetalles()
        .map { lista -> lista.map { it.aPedidoDeUI() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Compras pendientes desde Room (reactivo)
    val comprasFlow: StateFlow<List<Compra>> = compraDao.getAllComprasConDetalles()
        .map { lista -> lista.map { it.aCompraDeUI() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ============================================
    // ESTADO DE LA UI
    // ============================================

    private val _apiUrl = MutableStateFlow("")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _proveedorSeleccionado = MutableStateFlow<Proveedor?>(null)
    val proveedorSeleccionado: StateFlow<Proveedor?> = _proveedorSeleccionado.asStateFlow()

    private val _detallesPedido = MutableStateFlow<List<DetallePedido>>(emptyList())
    val detallesPedido: StateFlow<List<DetallePedido>> = _detallesPedido.asStateFlow()

    private val _snackbarChannel = Channel<String>()
    val snackbarFlow = _snackbarChannel.receiveAsFlow()

    // Estados de selección
    private val _selectedPedidoId = MutableStateFlow<Int?>(null)
    val selectedPedidoId: StateFlow<Int?> = _selectedPedidoId.asStateFlow()

    private val _selectedDetalleId = MutableStateFlow<Int?>(null)
    val selectedDetalleId: StateFlow<Int?> = _selectedDetalleId.asStateFlow()

    private val _currentCompra = MutableStateFlow<Compra?>(null)
    val currentCompra: StateFlow<Compra?> = _currentCompra.asStateFlow()

    // ESTADO PARA LA CATEGORÍA
    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)
    val categoriaSeleccionada: StateFlow<String?> = _categoriaSeleccionada

    //  FUNCIÓN PARA ACTUALIZAR LA CATEGORÍA
    fun seleccionarCategoria(categoria: String?) {
        _categoriaSeleccionada.value = categoria
    }

    // ✅ FUNCIÓN PARA LIMPIAR ESTADOS
    // Esta función se puede llamar cuando un pedido se guarda o se cancela,
    // para que la siguiente vez que se cree un pedido, todo esté desde cero.
    fun limpiarSeleccionDeFiltros() {
        _categoriaSeleccionada.value = null
        // Aquí también podrías limpiar la búsqueda si la mueves al ViewModel
    }


    // Filtros
    private val _filtroProveedor = MutableStateFlow<String?>(null)
    val filtroProveedor: StateFlow<String?> = _filtroProveedor.asStateFlow()

    private val _filtroEstado = MutableStateFlow<String?>(null)
    val filtroEstado: StateFlow<String?> = _filtroEstado.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    init {
        // Cargar configuración guardada
        _apiUrl.value = prefs.getString("api_url", "") ?: ""
    }

    // ============================================
    // CONFIGURACIÓN
    // ============================================

    fun setApiUrl(url: String) {
        _apiUrl.value = url
        prefs.edit().putString("api_url", url).apply()
    }

    object EstadoPedido {
        const val PENDIENTE_ENVIO = "pendiente_envio"
        const val ENVIADO = "enviado"
        const val CERRADO = "cerrado"
    }

    // ============================================
    // CARGA INICIAL DE DATOS
    // ============================================

    fun iniciarCargaDeDatos(forzarActualizacion: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null

            // Los datos YA se están observando desde Room via Flow
            // Solo necesitamos sincronizar con el servidor

            val proveedoresJob = async { sincronizarProveedores(forzarActualizacion) }
            val productosJob = async { sincronizarProductos(forzarActualizacion) }
            val pedidosJob = async { sincronizarPedidos() }

            val proveedoresOK = proveedoresJob.await()
            val productosOK = productosJob.await()
            val pedidosOK = pedidosJob.await()

            if (!proveedoresOK && !productosOK && !pedidosOK) {
                _snackbarChannel.send("⚠️ No se pudo conectar al servidor. Mostrando datos locales.")
            }

            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // SINCRONIZACIÓN CON SERVIDOR
    // ============================================

    private suspend fun sincronizarProveedores(forzar: Boolean = false): Boolean {
        return try {
            val ultimaSync = prefs.getLong("ultima_sync_proveedores", 0L)
            val ahora = System.currentTimeMillis()
            val hanPasado24h = (ahora - ultimaSync) > (24 * 60 * 60 * 1000)

            // Si no es forzado y no han pasado 24h, no sincronizar
            if (!forzar && !hanPasado24h) {
                return true
            }

            if (_apiUrl.value.isBlank()) {
                return false
            }

            val proveedoresResponse = withContext(Dispatchers.IO) {
                val url = URL("${_apiUrl.value.trimEnd('/')}/proveedores")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    // Usar el tipo del paquete raíz
                    gson.fromJson(json, Array<com.example.gestionpedidos.Proveedor>::class.java).toList()
                } else {
                    throw Exception("Error HTTP $responseCode")
                }
            }

            // Guardar en Room
            withContext(Dispatchers.IO) {
                val entities = proveedoresResponse.map { it.aProveedorEntity() }
                proveedorDao.insertProveedores(entities)
            }

            prefs.edit().putLong("ultima_sync_proveedores", ahora).apply()
            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun sincronizarProductos(forzar: Boolean = false): Boolean {
        return try {
            val ultimaSync = prefs.getLong("ultima_sync_productos", 0L)
            val ahora = System.currentTimeMillis()
            val hanPasado24h = (ahora - ultimaSync) > (24 * 60 * 60 * 1000)

            if (!forzar && !hanPasado24h) {
                return true
            }

            if (_apiUrl.value.isBlank()) {
                return false
            }

            val productosResponse = withContext(Dispatchers.IO) {
                val url = URL("${_apiUrl.value.trimEnd('/')}/productos")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    // Usar el tipo del paquete raíz
                    val type = object : TypeToken<List<com.example.gestionpedidos.ProductoResponse>>() {}.type
                    gson.fromJson<List<com.example.gestionpedidos.ProductoResponse>>(json, type)
                } else {
                    throw Exception("Error HTTP $responseCode")
                }
            }

            // Guardar en Room (productos + presentaciones)
            withContext(Dispatchers.IO) {
                productosResponse.forEach { productoResponse ->
                    productoResponse.saveToDatabase(productoDao, presentacionDao)
                }
            }

            prefs.edit().putLong("ultima_sync_productos", ahora).apply()
            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun sincronizarPedidos(): Boolean {
        return try {
            if (_apiUrl.value.isBlank()) {
                return false
            }

            val pedidosResponse = withContext(Dispatchers.IO) {
                val url = URL("${_apiUrl.value.trimEnd('/')}/pedidos")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    // Usar el tipo del paquete raíz
                    val type = object : TypeToken<List<com.example.gestionpedidos.PedidoResponse>>() {}.type
                    gson.fromJson<List<com.example.gestionpedidos.PedidoResponse>>(json, type)
                } else {
                    throw Exception("Error HTTP $responseCode")
                }
            }

            // Guardar en Room
            withContext(Dispatchers.IO) {
                pedidosResponse.forEach { pedidoResponse ->
                    pedidoResponse.saveToDatabase(pedidoDao, detallePedidoDao)
                }
            }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ============================================
    // GESTIÓN DE PEDIDOS
    // ============================================

    fun pedidosFiltrados(): StateFlow<List<Pedido>> {
        return combine(
            pedidosFlow,
            filtroProveedor,
            filtroEstado,
            busqueda
        ) { pedidos, proveedor, estado, busquedaTxt ->
            var resultado = pedidos

            proveedor?.let { prov ->
                resultado = resultado.filter { it.proveedor == prov }
            }

            estado?.let { est ->
                resultado = resultado.filter { it.estadoNombre == est }
            }

            if (busquedaTxt.isNotBlank()) {
                resultado = resultado.filter { pedido ->
                    val proveedorCoincide = pedido.proveedor.contains(busquedaTxt, ignoreCase = true)
                    val detalleCoincide = pedido.detallesPedido.any { detalle ->
                        detalle.productoNombre.contains(busquedaTxt, ignoreCase = true) ||
                                (detalle.codigoDeBarras?.contains(busquedaTxt, ignoreCase = true) ?: false)
                    }
                    proveedorCoincide || detalleCoincide
                }
            }

            resultado
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun actualizarEstadoPedido(pedidoId: Int, nuevoEstado: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pedido = pedidoDao.getPedido(pedidoId)
            pedido?.let {
                pedidoDao.updatePedido(it.copy(estado = nuevoEstado))
            }
        }
    }

    fun selectPedido(pedido: Pedido) {
        _selectedPedidoId.value = pedido.idPedido
        _currentCompra.value = Compra(
            idCompra = 0,
            fechaCompra = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            proveedor = pedido.proveedor,
            detallesCompra = mutableListOf()
        )
        _selectedDetalleId.value = null
    }

    suspend fun getSelectedPedido(): Pedido? {
        val id = _selectedPedidoId.value ?: return null
        return withContext(Dispatchers.IO) {
            pedidoDao.getPedidoConDetalles(id)?.aPedidoDeUI()
        }
    }

    fun selectDetalle(detalle: DetallePedido) {
        _selectedDetalleId.value = detalle.productoId
    }

    fun proveedoresUnicos(): List<String> {
        return pedidosFlow.value.map { it.proveedor }.distinct().sorted()
    }

    // ============================================
    // GESTIÓN DE COMPRAS
    // ============================================

    fun agregarCompra(compra: Compra) {
        viewModelScope.launch(Dispatchers.IO) {
            val compraEntity = compra.aCompraEntity()
            val compraId = compraDao.insertCompra(compraEntity).toInt()

            // Guardar detalles
            compra.detallesCompra.forEach { detalle ->
                detalleCompraDao.insertDetalle(detalle.aDetalleCompraEntity(compraId))
            }
        }
    }

    fun eliminarCompra(compra: Compra) {
        viewModelScope.launch(Dispatchers.IO) {
            compraDao.deleteCompra(compra.aCompraEntity())
        }
    }

    fun iniciarCompraDesdePedido(pedido: Pedido) {
        _currentCompra.value = Compra(
            idCompra = 0,
            fechaCompra = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            proveedor = pedido.proveedor,
            detallesCompra = mutableListOf()
        )
    }

    fun confirmarProducto(detalle: DetallePedido, cantidad: Double, precio: Double) {
        val compra = _currentCompra.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // Marcar como confirmado en Room
            val pedidoId = _selectedPedidoId.value ?: return@launch
            val detalles = detallePedidoDao.getDetallesPorPedido(pedidoId)
            val detalleEntity = detalles.find { it.productoId == detalle.productoId }

            detalleEntity?.let {
                detallePedidoDao.updateDetalle(it.copy(confirmado = true))
            }
        }

        // Actualizar compra local
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

    fun actualizarProductoEnCompra(detalle: DetallePedido, cantidad: Double, precio: Double) {
        val compra = _currentCompra.value ?: return
        val existente = compra.detallesCompra.find { it.productoId == detalle.productoId }

        if (existente != null) {
            existente.cantidadCompra = cantidad
            existente.precioUnitario = precio
        } else {
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
    }

    fun eliminarDetalleDeCompra(detalle: DetallePedido) {
        val compra = _currentCompra.value ?: return
        compra.detallesCompra.removeAll { it.productoId == detalle.productoId }

        viewModelScope.launch(Dispatchers.IO) {
            val pedidoId = _selectedPedidoId.value ?: return@launch
            val detalles = detallePedidoDao.getDetallesPorPedido(pedidoId)
            val detalleEntity = detalles.find { it.productoId == detalle.productoId }

            detalleEntity?.let {
                detallePedidoDao.updateDetalle(it.copy(confirmado = false))
            }
        }
    }

    fun finalizarCompra(compra: Compra) {
        viewModelScope.launch(Dispatchers.IO) {
            // Guardar compra en Room
            val compraEntity = compra.aCompraEntity()
            val compraId = compraDao.insertCompra(compraEntity).toInt()

            compra.detallesCompra.forEach { detalle ->
                detalleCompraDao.insertDetalle(detalle.aDetalleCompraEntity(compraId))
            }

            // Verificar si el pedido está completo
            val pedidoId = _selectedPedidoId.value
            if (pedidoId != null) {
                val detalles = detallePedidoDao.getDetallesPorPedido(pedidoId)
                val todosConfirmados = detalles.all { it.confirmado }

                if (todosConfirmados) {
                    val pedido = pedidoDao.getPedido(pedidoId)
                    pedido?.let {
                        pedidoDao.updatePedido(it.copy(estado = "cerrado"))
                    }
                }
            }
        }

        // Limpiar estado
        _currentCompra.value = null
        _selectedPedidoId.value = null
        _selectedDetalleId.value = null
    }

    // ============================================
    // GESTIÓN DE NUEVO PEDIDO
    // ============================================

    fun seleccionarProveedor(proveedor: Proveedor) {
        _proveedorSeleccionado.value = proveedor
    }

    fun agregarProductoAlPedido(detalle: DetallePedido) {
        val listaActual = _detallesPedido.value.toMutableList()
        val productoExistenteIndex = listaActual.indexOfFirst { it.productoId == detalle.productoId }

        if (productoExistenteIndex != -1) {
            listaActual[productoExistenteIndex] = detalle
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
    // ✅ Guardar pedido localmente en Room
    fun guardarPedidoLocal(pedido: Pedido) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaActual = sdf.format(Date())
                val calendario = Calendar.getInstance()
                calendario.add(Calendar.DAY_OF_YEAR, 7)
                val fechaEntrega = sdf.format(calendario.time)

                val pedidoEntity = PedidoEntity(
                    id = 0,
                    fechaPedido = fechaActual,
                    fechaEntregaEsperada = fechaEntrega,
                    fechaEntregaReal = null,
                    proveedorId = _proveedorSeleccionado.value?.id ?: 0,
                    proveedorNombre = pedido.proveedor,
                    estado = EstadoPedido.PENDIENTE_ENVIO,
                    observaciones = null,
                    flete = 0.0,
                    totalNeto = pedido.detallesPedido.sumOf {
                        it.cantidadPedida * it.precioEsperado
                    }
                )

                // ✅ INSERTAR PEDIDO
                val pedidoId = pedidoDao.insertPedido(pedidoEntity).toInt()

                // ✅ INSERTAR DETALLES CON EL ID CORRECTO
                val detalles = pedido.detallesPedido.map { detalle ->
                    DetallePedidoEntity(
                        pedidoId = pedidoId,
                        productoId = detalle.productoId,
                        productoNombre = detalle.productoNombre,
                        presentacionId = detalle.presentacionId,
                        presentacionNombre = null,
                        codigoBarras = detalle.codigoDeBarras,
                        cantidadPedida = detalle.cantidadPedida,
                        precioUnitario = detalle.precioEsperado,
                        iva = 0.0,
                        cantidadRecibida = null,
                        subtotalProducto = detalle.cantidadPedida * detalle.precioEsperado,
                        costoUnitarioFinal = detalle.precioEsperado,
                        confirmado = false
                    )
                }

                detallePedidoDao.insertDetalles(detalles)

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al guardar pedido local: ${e.message}"
            }
        }
    }

    // ✅ NUEVA: Intentar enviar pedido al servidor
    fun intentarEnviarPedidoAlServidor(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onSinConexion: () -> Unit
    ) {
        val prov = _proveedorSeleccionado.value
        if (prov == null) {
            onError("No se ha seleccionado un proveedor.")
            return
        }
        if (_detallesPedido.value.isEmpty()) {
            onError("No hay productos en el pedido.")
            return
        }

        // Verificar si hay URL configurada
        if (_apiUrl.value.isBlank()) {
            onSinConexion()
            limpiarPedidoActual()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaActual = sdf.format(Date())
                val calendario = Calendar.getInstance()
                calendario.add(Calendar.DAY_OF_YEAR, 7)
                val fechaEntrega = sdf.format(calendario.time)

                val detallesRequest = _detallesPedido.value.map { detalleUI ->
                    com.example.gestionpedidos.DetallePedidoRequest(
                        productoId = detalleUI.productoId,
                        presentacionId = detalleUI.presentacionId,
                        cantidadPedida = detalleUI.cantidadPedida,
                        precioUnitario = detalleUI.precioEsperado,
                        neto = detalleUI.precioEsperado * detalleUI.cantidadPedida
                    )
                }

                val pedidoRequest = com.example.gestionpedidos.PedidoRequest(
                    fechaPedido = fechaActual,
                    fechaEntregaEsperada = fechaEntrega,
                    proveedorId = prov.id,
                    detalles = detallesRequest,
                    estado = "requerido",
                    neto = detallesRequest.sumOf { it.precioUnitario * it.cantidadPedida }
                )

                withContext(Dispatchers.IO) {
                    val url = URL("${_apiUrl.value.trimEnd('/')}/pedidos")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 10000 // 10 segundos timeout
                    connection.readTimeout = 10000

                    val jsonBody = gson.toJson(pedidoRequest)
                    connection.outputStream.use { os ->
                        val input = jsonBody.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        throw Exception("Error $responseCode: $errorBody")
                    }

                    // ✅ Si llegamos aquí, el pedido se envió exitosamente
                    // Actualizar el estado en Room a "enviado"
                    val pedidosLocales = pedidoDao.getAllPedidosConDetalles().first()
                    val pedidoPendiente = pedidosLocales.find {
                        it.pedido.estado == EstadoPedido.PENDIENTE_ENVIO &&
                                it.pedido.proveedorId == prov.id
                    }

                    pedidoPendiente?.let {
                        pedidoDao.updatePedido(it.pedido.copy(estado = "enviado"))
                    }
                }

                limpiarPedidoActual()
                sincronizarPedidos() // Recargar desde servidor
                onSuccess()

            } catch (e: java.net.SocketTimeoutException) {
                // Timeout = sin conexión
                limpiarPedidoActual()
                onSinConexion()
            } catch (e: java.net.UnknownHostException) {
                // No se puede resolver el host = sin conexión
                limpiarPedidoActual()
                onSinConexion()
            } catch (e: Exception) {
                e.printStackTrace()
                limpiarPedidoActual()
                onError(e.message ?: "Error desconocido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NUEVA: Obtener pedidos pendientes de envío
    val pedidosPendientesEnvio: StateFlow<List<Pedido>> = pedidoDao.getAllPedidosConDetalles()
        .map { lista ->
            lista
                .filter { it.pedido.estado == EstadoPedido.PENDIENTE_ENVIO }
                .map { it.aPedidoDeUI() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ✅ NUEVA: Reintentar envío de pedidos pendientes
    fun reintentarEnvioPedidosPendientes() {
        viewModelScope.launch {
            val pedidosAEnviar = pedidosPendientesEnvio.value

            if (pedidosAEnviar.isEmpty()) {
                _snackbarChannel.send("No hay pedidos pendientes de envío.")
                return@launch
            }

            if (_apiUrl.value.isBlank()) {
                _snackbarChannel.send("⚠️ No hay URL de servidor. No se puede enviar.")
                return@launch
            }

            _isLoading.value = true
            var exitosos = 0
            var fallidos = 0

            pedidosAEnviar.forEach { pedido ->
                try {
                    // Reutilizamos la lógica de envío
                    val detallesRequest = pedido.detallesPedido.map { detalleUI ->
                        com.example.gestionpedidos.DetallePedidoRequest(
                            productoId = detalleUI.productoId,
                            presentacionId = detalleUI.presentacionId,
                            cantidadPedida = detalleUI.cantidadPedida,
                            precioUnitario = detalleUI.precioEsperado,
                            neto = detalleUI.precioEsperado * detalleUI.cantidadPedida
                        )
                    }

                    val pedidoRequest = com.example.gestionpedidos.PedidoRequest(
                        fechaPedido = pedido.fechaPedido,
                        fechaEntregaEsperada = pedido.fechaEntregaEsperada,
                        proveedorId = pedido.proveedorId,
                        detalles = detallesRequest,
                        estado = "requerido",
                        neto = pedido.totalNeto
                    )

                    // Envío en un contexto de IO
                    withContext(Dispatchers.IO) {
                        val url = URL("${_apiUrl.value.trimEnd('/')}/pedidos")
                        val connection = url.openConnection() as HttpURLConnection
                        // ... (Configuración de la conexión POST, igual que en intentarEnviarPedidoAlServidor)
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        // ... etc ...

                        val jsonBody = gson.toJson(pedidoRequest)
                        connection.outputStream.use { os ->
                            val input = jsonBody.toByteArray(Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }

                        val responseCode = connection.responseCode
                        if (responseCode !in 200..299) {
                            throw Exception("Falló el envío para el pedido del proveedor ${pedido.proveedor}")
                        }
                    }

                    // Si tiene éxito, actualizamos su estado en Room
                    pedidoDao.updatePedido(
                        pedidoDao.getPedido(pedido.idPedido)!!.copy(estado = "enviado")
                    )
                    exitosos++

                } catch (e: Exception) {
                    e.printStackTrace()
                    fallidos++
                }
            }

            _isLoading.value = false

            if (exitosos > 0) {
                _snackbarChannel.send("✅ $exitosos pedidos enviados correctamente.")
                sincronizarPedidos() // Sincronizamos la lista principal
            }
            if (fallidos > 0) {
                _snackbarChannel.send("⚠️ $fallidos pedidos no se pudieron enviar. Revisa tu conexión.")
            }
        }
    }

    fun guardarPedidoEnServidor(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Esta función ahora solo llama a la nueva lógica
        intentarEnviarPedidoAlServidor(
            onSuccess = onSuccess,
            onError = onError,
            onSinConexion = onSuccess // También consideramos éxito si se guarda local
        )
    }

    // ============================================
    // FILTROS
    // ============================================

    fun setFiltroProveedor(proveedor: String?) {
        _filtroProveedor.value = proveedor
    }

    fun setFiltroEstado(estado: String?) {
        _filtroEstado.value = estado
    }

    fun setBusqueda(texto: String) {
        _busqueda.value = texto
    }
}