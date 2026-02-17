package id.usecase.noted.presentation.note.editor.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NoteCameraScreenRoot(
    onPhotoCaptured: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            isPermanentlyDenied = !shouldShowRationale
            onShowMessage("Izin kamera dibutuhkan untuk mengambil foto")
        } else {
            isPermanentlyDenied = false
        }
    }

    val previewView = remember(context) { PreviewView(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(hasCameraPermission, lifecycleOwner) {
        if (!hasCameraPermission) {
            return@LaunchedEffect
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also { preview ->
                        preview.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )
                    imageCapture = capture
                }.onFailure {
                    onShowMessage("Gagal menyalakan kamera")
                }
            },
            mainExecutor,
        )
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        onDispose {
            cameraProvider?.unbindAll()
            imageCapture = null
            cameraProvider = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NotedTopAppBar(
                title = "Kamera",
                onNavigateBack = onNavigateBack,
            )
        },
        floatingActionButton = {
            if (hasCameraPermission) {
                FloatingActionButton(
                    onClick = {
                        val capture = imageCapture
                        if (capture == null) {
                            onShowMessage("Kamera belum siap")
                            return@FloatingActionButton
                        }

                        val directory = File(context.filesDir, "note-images")
                        if (!directory.exists() && !directory.mkdirs()) {
                            onShowMessage("Gagal menyiapkan folder foto")
                            return@FloatingActionButton
                        }
                        val outputFile = File(directory, "note-${System.currentTimeMillis()}.jpg")
                        val output = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                        capture.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onPhotoCaptured(Uri.fromFile(outputFile).toString())
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    onShowMessage("Gagal mengambil foto")
                                }
                            },
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Camera,
                        contentDescription = "Ambil foto",
                    )
                }
            }
        },
    ) { innerPadding ->
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { previewView },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (isPermanentlyDenied) {
                            "Izin kamera diblokir permanen. Buka pengaturan aplikasi untuk mengaktifkan kembali."
                        } else {
                            "Izinkan akses kamera untuk mengambil foto note."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            if (isPermanentlyDenied) {
                                openAppSettings(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ) {
                        Text(if (isPermanentlyDenied) "Buka Pengaturan" else "Izinkan Kamera")
                    }
                }
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
