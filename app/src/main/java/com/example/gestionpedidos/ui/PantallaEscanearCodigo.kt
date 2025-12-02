package com.example.gestionpedidos.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscanearCodigo(
    onCodigoDetectado: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tienePermiso = granted
    }

    LaunchedEffect(Unit) {
        if (!tienePermiso) solicitarPermiso.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear código") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->

        if (!tienePermiso) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permiso de cámara denegado")
            }
            return@Scaffold
        }

        AndroidView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // ✅ CORRECCIÓN: Usamos una clase dedicada para el analizador
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor, BarcodeAnalyzer(onCodigoDetectado))
                        }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("CAMERA_ERROR", "Fallo al vincular los casos de uso de la cámara", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }
}

// ✅ --- CLASE ANALIZADORA DEDICADA Y OPTIMIZADA ---
@SuppressLint("UnsafeOptInUsageError")
private class BarcodeAnalyzer(
    private val onCodigoDetectado: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // 1. El cliente del escáner se crea UNA SOLA VEZ.
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    // 2. Bandera para procesar el código UNA SOLA VEZ.
    @Volatile
    private var isScanning = true

    override fun analyze(imageProxy: ImageProxy) {
        // Si ya hemos procesado un código, ignoramos nuevos fotogramas.
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        // Desactivamos el escaneo para no procesar más resultados
                        isScanning = false
                        // Obtenemos el valor real del código de barras
                        barcodes.first().rawValue?.let { codigo ->
                            Log.d("ESCANEO", "Detectado: $codigo")
                            onCodigoDetectado(codigo)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("ESCANEO", "Error al analizar: ${it.message}")
                }
                .addOnCompleteListener {
                    // ‼️ CRÍTICO: Siempre cerrar el proxy para liberar la cámara.
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
