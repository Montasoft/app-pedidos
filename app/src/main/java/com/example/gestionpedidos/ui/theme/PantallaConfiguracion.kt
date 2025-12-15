package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestionpedidos.PedidosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: PedidosViewModel,
    onContinuar: () -> Unit
) {
    // ✅ Observar StateFlow en lugar de acceso directo
    val apiUrl by viewModel.apiUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado local para el campo de texto
    var urlInput by remember { mutableStateOf(apiUrl) }
    var mostrarEjemplos by remember { mutableStateOf(false) }

    // Sincronizar cuando cambie el apiUrl del ViewModel
    LaunchedEffect(apiUrl) {
        if (urlInput.isEmpty() && apiUrl.isNotEmpty()) {
            urlInput = apiUrl
        }
    }

    // ✅ Observar canal de snackbar
    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración API") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { mostrarEjemplos = !mostrarEjemplos }
                    ) {
                        Icon(
                            imageVector = if (mostrarEjemplos)
                                Icons.Default.ExpandLess
                            else
                                Icons.Default.ExpandMore,
                            contentDescription = "Ver ejemplos",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ✅ Icono y título
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Configuración del Servidor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ingresa la URL de tu servidor API",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ Campo de URL
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("URL del servidor API") },
                placeholder = { Text("http://192.168.0.10:8080/api") },
                leadingIcon = {
                    Icon(Icons.Default.Language, "URL")
                },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { urlInput = "" }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        "Ejemplo: http://192.168.1.100:8080/api",
                        fontSize = 12.sp
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅  Ejemplos de URLs
            if (mostrarEjemplos) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "💡 Ejemplos de URLs:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        EjemploUrl(
                            "Local (mismo dispositivo)",
                            "http://localhost:8080/api",
                            onClick = { urlInput = "http://localhost:8080/api" }
                        )

                        EjemploUrl(
                            "Red local (LAN)",
                            "http://192.168.1.100:8080/api",
                            onClick = { urlInput = "http://192.168.1.100:8080/api" }
                        )

                        EjemploUrl(
                            "Servidor remoto (HTTPS)",
                            "https://api.miempresa.com/api",
                            onClick = { urlInput = "https://api.miempresa.com/api" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ✅ NUEVO: Botón de probar conexión
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.setApiUrl(urlInput)
                            scope.launch {
                                viewModel.iniciarCargaDeDatos(forzarActualizacion = true)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = urlInput.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.WifiFind,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Probar")
                    }
                }

                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.setApiUrl(urlInput)
                            onContinuar()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = urlInput.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Continuar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ NUEVO: Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            "Requisitos de la URL:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "• Debe comenzar con http:// o https://\n" +
                                    "• Incluir la dirección IP o dominio\n" +
                                    "• Incluir el puerto si es necesario\n" +
                                    "• Terminar con la ruta del API",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ✅ NUEVO: Componente para ejemplos de URL
@Composable
private fun EjemploUrl(
    titulo: String,
    url: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                url,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}