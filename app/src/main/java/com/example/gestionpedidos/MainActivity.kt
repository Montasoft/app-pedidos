package com.example.gestionpedidos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.gestionpedidos.ui.*
import com.example.gestionpedidos.ui.theme.GestionPedidosTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PedidosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val pedido = viewModel.getSelectedPedido()
            val compra = viewModel.getCompraActual()
            if (pedido != null && compra != null) {
                PantallaDetallePedido(
                    pedido = pedido,
                    compraActual = compra,
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
                Text("Error: No hay pedido seleccionado")
            }
        }

        // ===============================
        // FORMULARIO PARA CONFIRMAR PRODUCTO
        // ===============================
        composable("formularioProducto") {
            val detalle = viewModel.getSelectedDetalle()
            if (detalle != null) {
                PantallaFormularioProducto(
                    detalle = detalle,
                    onConfirmar = { cantidad, precio ->
                        viewModel.confirmarProducto(detalle, cantidad, precio)
                        navController.popBackStack()
                    },
                    onVolver = { navController.popBackStack() }
                )
            } else {
                Text("No hay producto seleccionado.")
            }
        }

        // ===============================
        // EDICIÓN DE PRODUCTO CONFIRMADO
        // ===============================
        composable("editarProducto") {
            val detalle = viewModel.getSelectedDetalle()
            val compra = viewModel.getCompraActual()
            if (detalle != null && compra != null) {
                PantallaEditarProducto(
                    compraActual = compra,
                    detalle = detalle,
                    onGuardar = { cantidad, precio ->
                        viewModel.actualizarProductoEnCompra(detalle, cantidad, precio)
                        navController.popBackStack()
                    },
                    onVolver = { navController.popBackStack() }
                )
            } else {
                Text("Error: No hay producto seleccionado")
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
                onGuardarPedido = { pedido ->
                    //TODO: Guardar el pedido en el servidor
                    viewModel.pedidos = viewModel.pedidos + pedido
                    navController.popBackStack()
                },
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
    }
}
