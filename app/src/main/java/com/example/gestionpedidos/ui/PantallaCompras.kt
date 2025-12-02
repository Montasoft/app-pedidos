package com.example.gestionpedidos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.PedidosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCompras(
    viewModel: PedidosViewModel,
    onVerLista: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerComprasPendientes: () -> Unit,
    onConfiguracion: () -> Unit,
    onCrearPedido: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading by remember { derivedStateOf { viewModel.isLoading } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Compras") },
                actions = {
                    IconButton(onClick = onConfiguracion) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Bienvenido 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Selecciona una opción para gestionar tus pedidos",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 🔹 Descargar pedidos con indicador de carga
                item {
                    OpcionMenu(
                        titulo = "🔄 Descargar Pedidos",
                        descripcion = "Cargar la lista de pedidos desde el servidor",
                        icono = Icons.Default.CloudDownload,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Descargando pedidos...")
                                viewModel.cargarPedidos(context)

                                // Espera a que termine el proceso
                                if (viewModel.errorMessage != null) {
                                    snackbarHostState.showSnackbar("❌ ${viewModel.errorMessage}")
                                } else {
                                    snackbarHostState.showSnackbar("✅ Pedidos actualizados correctamente")
                                }
                            }
                        }
                    )
                }

                item { OpcionMenu("📦 Lista de Pedidos", "Ver pedidos abiertos o cerrados", Icons.AutoMirrored.Filled.List, onVerLista) }
                item { OpcionMenu("🕒 Pendientes", "Compras en curso por enviar", Icons.Default.Schedule, onVerComprasPendientes) }
                item { OpcionMenu("📜 Historial", "Pedidos y compras enviados", Icons.Default.History, onVerHistorial) }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 🟢 Botón principal para crear nuevo pedido
                item {
                    Button(
                        onClick = onCrearPedido,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Nuevo Pedido",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Crear Nuevo Pedido",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 🔸 Indicador de carga centrado
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Descargando pedidos...",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpcionMenu(
    titulo: String,
    descripcion: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.9f),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = titulo,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = descripcion,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
