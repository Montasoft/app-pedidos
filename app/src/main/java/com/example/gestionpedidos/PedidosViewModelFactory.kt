package com.example.gestionpedidos

// ==========================
// File: PedidosViewModelFactory.kt
// Package: com.example.gestionpedidos
// ==========================

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PedidosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidosViewModel::class.java)) {
            return PedidosViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ============================================
// USO EN TU ACTIVITY/FRAGMENT
// ============================================

/*
// En tu Activity o Fragment:

class MainActivity : AppCompatActivity() {

    private val viewModel: PedidosViewModel by viewModels {
        PedidosViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar URL de API
        viewModel.setApiUrl("https://tu-api.com")

        // Iniciar carga de datos
        viewModel.iniciarCargaDeDatos(forzarActualizacion = false)

        // Observar cambios reactivamente
        lifecycleScope.launch {
            viewModel.productos.collect { productos ->
                // Actualizar UI con productos
                println("Productos: ${productos.size}")
            }
        }

        lifecycleScope.launch {
            viewModel.pedidosFlow.collect { pedidos ->
                // Actualizar UI con pedidos
                println("Pedidos: ${pedidos.size}")
            }
        }

        lifecycleScope.launch {
            viewModel.snackbarFlow.collect { mensaje ->
                // Mostrar snackbar
                Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
*/

// ============================================
// USO CON COMPOSE
// ============================================

/*
@Composable
fun PedidosScreen() {
    val context = LocalContext.current
    val viewModel: PedidosViewModel = viewModel(
        factory = PedidosViewModelFactory(context)
    )

    // Observar estados
    val productos by viewModel.productos.collectAsState()
    val pedidos by viewModel.pedidosFiltrados().collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // UI
    if (isLoading) {
        CircularProgressIndicator()
    }

    LazyColumn {
        items(pedidos) { pedido ->
            PedidoItem(pedido = pedido)
        }
    }
}
*/