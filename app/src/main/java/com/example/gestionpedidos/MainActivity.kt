package com.example.gestionpedidos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.example.gestionpedidos.ui.*
import com.example.gestionpedidos.ui.theme.GestionPedidosTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ✅ Usar ViewModelFactory con context
    private val viewModel: PedidosViewModel by viewModels {
        PedidosViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar configuración y datos iniciales
        lifecycleScope.launch {
            viewModel.iniciarCargaDeDatos(forzarActualizacion = false)
        }

        setContent {
            GestionPedidosTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavegacion(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavegacion(viewModel: PedidosViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "configuracion"
    ) {

        // ===============================
        // CONFIGURACIÓN INICIAL
        // ===============================
        composable("configuracion") {
            PantallaConfiguracion(
                viewModel = viewModel,
                onContinuar = {
                    navController.navigate("compras") {
                        popUpTo("configuracion") { inclusive = true }
                    }
                }
            )
        }

        // ===============================
        // MENÚ PRINCIPAL DE COMPRAS
        // ===============================
        composable("compras") {
            PantallaCompras(
                viewModel = viewModel,
                onVerLista = { navController.navigate("listaPedidos") },
                onVerHistorial = { navController.navigate("historial") },
                onVerPedidosPendientes = { navController.navigate("pedidosPendientes") },
                onVerComprasPendientes = { navController.navigate("pendientes") },
                onConfiguracion = { navController.navigate("configuracion") },
                onCrearPedido = { navController.navigate("crearPedido") }
            )
        }

        // ===============================
        // LISTA DE PEDIDOS
        // ===============================
        composable("listaPedidos") {
            PantallaListaPedidos(
                viewModel = viewModel,
                onPedidoSeleccionado = { pedido ->
                    viewModel.selectPedido(pedido)
                    viewModel.iniciarCompraDesdePedido(pedido)
                    navController.navigate("detallePedido")
                },
                onVolver = { navController.popBackStack() }
            )
        }

        // ===============================
        // DETALLE DE UN PEDIDO
        // ===============================
        composable("detallePedido") {

            var pedido by remember { mutableStateOf<Pedido?>(null) }
            val compra by viewModel.currentCompra.collectAsState()

            LaunchedEffect(Unit) {
                pedido = viewModel.getSelectedPedido()
            }

            if (pedido != null && compra != null) {
                PantallaDetallePedido(
                    pedido = pedido!!,
                    compraActual = compra!!,
                    onDetalleSeleccionado = { detalle ->
                        viewModel.selectDetalle(detalle)
                        navController.navigate("formularioProducto")
                    },
                    onEditarDetalle = { detalle ->
                        viewModel.selectDetalle(detalle)
                        navController.navigate("editarProducto")
                    },
                    onEliminarDetalle = { detalle ->
                        viewModel.eliminarDetalleDeCompra(detalle)
                    },
                    onVolver = { navController.popBackStack() },
                    onFinalizarCompra = {
                        viewModel.finalizarCompra(it)
                        navController.navigate("compras") {
                            popUpTo("compras") { inclusive = false }
                        }
                    }
                )
            } else {
                // Mostrar loading mientras carga
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ===============================
        // FORMULARIO PARA CONFIRMAR PRODUCTO
        // ===============================
        composable("formularioProducto") {
            val selectedDetalleId by viewModel.selectedDetalleId.collectAsState()
            var detalle by remember { mutableStateOf<DetallePedido?>(null) }

            LaunchedEffect(selectedDetalleId) {
                selectedDetalleId?.let { detalleId ->
                    val pedido = viewModel.getSelectedPedido()
                    detalle = pedido?.detallesPedido?.find { it.productoId == detalleId }
                }
            }

            if (detalle != null) {
                PantallaFormularioProducto(
                    detalle = detalle!!,
                    onConfirmar = { cantidad, precio ->
                        viewModel.confirmarProducto(detalle!!, cantidad, precio)
                        navController.popBackStack()
                    },
                    onVolver = { navController.popBackStack() }
                )
            } else {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ===============================
        // EDICIÓN DE PRODUCTO CONFIRMADO
        // ===============================
        composable("editarProducto") {
            val selectedDetalleId by viewModel.selectedDetalleId.collectAsState()
            val compra by viewModel.currentCompra.collectAsState()
            var detalle by remember { mutableStateOf<DetallePedido?>(null) }

            LaunchedEffect(selectedDetalleId) {
                selectedDetalleId?.let { detalleId ->
                    val pedido = viewModel.getSelectedPedido()
                    detalle = pedido?.detallesPedido?.find { it.productoId == detalleId }
                }
            }

            if (detalle != null && compra != null) {
                PantallaEditarProducto(
                    compraActual = compra!!,
                    detalle = detalle!!,
                    onGuardar = { cantidad, precio ->
                        viewModel.actualizarProductoEnCompra(detalle!!, cantidad, precio)
                        navController.popBackStack()
                    },
                    onVolver = { navController.popBackStack() }
                )
            } else {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // ===============================
        // COMPRAS PENDIENTES
        // ===============================
        composable("pendientes") {
            PantallaComprasPendientes(
                viewModel = viewModel,
                onEliminarCompra = { viewModel.eliminarCompra(it) },
                onVolver = { navController.popBackStack() }
            )
        }

        // ===============================
        // HISTORIAL DE COMPRAS ENVIADAS
        // ===============================
        composable("historial") {
            PantallaHistorial(
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        // ===============================
        // NUEVO FLUJO DE PEDIDOS
        // ===============================
        composable("crearPedido") {
            PantallaCrearPedido(
                viewModel = viewModel,
                onAgregarProductoClick = {
                    navController.navigate("seleccionarProducto")
                },
                onGuardarPedido = {
                    navController.popBackStack()
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }

        composable("seleccionarProducto") {
            PantallaSeleccionarProducto(
                viewModel = viewModel,
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable("escanearCodigo") {
            PantallaEscanearCodigo(
                onCodigoDetectado = { codigo ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("codigoEscaneado", codigo)
                    navController.popBackStack()
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable("pedidosPendientes") {
            PantallaPedidosPendientes(
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }

    }
}