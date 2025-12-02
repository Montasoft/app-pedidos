package com.example.gestionpedidos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestionpedidos.PedidosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    viewModel: PedidosViewModel,
    onContinuar: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración API") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = viewModel.apiUrl,
                onValueChange = { viewModel.apiUrl = it },
                label = { Text("URL del servidor API") },
                placeholder = { Text("http://192.168.0.10:8080/api") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.apiUrl.isNotBlank()
            ) {
                Text("Continuar")
            }
        }
    }
}