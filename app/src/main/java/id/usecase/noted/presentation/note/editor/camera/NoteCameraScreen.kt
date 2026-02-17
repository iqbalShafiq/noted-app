package id.usecase.noted.presentation.note.editor.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.usecase.noted.presentation.components.camera.CameraControlRow
import id.usecase.noted.presentation.components.camera.CameraExposureSlider
import id.usecase.noted.presentation.components.camera.CameraGridOverlay
import id.usecase.noted.presentation.components.camera.CameraIconButton
import id.usecase.noted.presentation.components.camera.CameraLevelIndicator
import id.usecase.noted.presentation.components.camera.CameraShutterButton
import id.usecase.noted.presentation.components.camera.CameraZoomSlider
import id.usecase.noted.presentation.components.camera.FocusIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCameraScreenRoot(
    onPhotoCaptured: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    
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
            val activity = context as? Activity
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            isPermanentlyDenied = !shouldShowRationale
            onShowMessage("Camera permission is required to take photos")
        } else {
            isPermanentlyDenied = false
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            viewModel.onIntent(
                CameraIntent.Initialize(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CameraEffect.ShowMessage -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is CameraEffect.PhotoCaptured -> {
                    onPhotoCaptured(effect.uri)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onIntent(CameraIntent.Cleanup)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Camera") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (hasCameraPermission) {
                CameraContent(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onPhotoCaptured = { uri ->
                        viewModel.onIntent(CameraIntent.ConfirmPhoto(uri))
                    },
                )
            } else {
                PermissionDeniedContent(
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequestPermission = {
                        if (isPermanentlyDenied) {
                            openAppSettings(context)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraContent(
    state: CameraState,
    onIntent: (CameraIntent) -> Unit,
    onPhotoCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember(context) { PreviewView(context) }
    
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(state.isInitialized) {
        if (state.isInitialized) {
            onIntent(CameraIntent.AttachPreviewSurface(previewView.surfaceProvider))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    focusPoint = offset
                    showFocusIndicator = true
                    onIntent(
                        CameraIntent.TapToFocus(
                            x = offset.x,
                            y = offset.y,
                            viewWidth = previewView.width,
                            viewHeight = previewView.height,
                        ),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (zoom != 1f) {
                        val currentZoom = state.zoomRatio
                        val newZoom = (currentZoom * zoom).coerceIn(1f, state.maxZoom)
                        onIntent(CameraIntent.SetZoom(newZoom))
                    }
                }
            },
            factory = { previewView },
        )

        AnimatedVisibility(
            visible = state.isGridEnabled,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            CameraGridOverlay(
                isVisible = true,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = state.isLevelEnabled,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            CameraLevelIndicator(
                isVisible = true,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp),
            )
        }

        focusPoint?.let { point ->
            FocusIndicator(
                x = point.x,
                y = point.y,
                isVisible = showFocusIndicator,
                modifier = Modifier.fillMaxSize(),
                onAnimationComplete = { showFocusIndicator = false },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
        ) {
            CameraControlRow {
                CameraIconButton(
                    icon = when (state.flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    },
                    contentDescription = "Flash mode: ${state.flashMode.name}",
                    onClick = { onIntent(CameraIntent.ToggleFlash) },
                    isSelected = state.flashMode != FlashMode.OFF,
                )
                
                CameraIconButton(
                    icon = if (state.timerSeconds != null) Icons.Default.Timer else Icons.Default.TimerOff,
                    contentDescription = "Timer: ${state.timerSeconds?.toString() ?: "Off"}",
                    onClick = {
                        val nextTimer = when (state.timerSeconds) {
                            null -> 3
                            3 -> 10
                            else -> null
                        }
                        onIntent(CameraIntent.SetTimer(nextTimer))
                    },
                    isSelected = state.timerSeconds != null,
                )
                
                CameraIconButton(
                    icon = Icons.Default.GridOn,
                    contentDescription = "Grid",
                    onClick = { onIntent(CameraIntent.ToggleGrid) },
                    isSelected = state.isGridEnabled,
                )
                
                CameraIconButton(
                    icon = Icons.Default.Straighten,
                    contentDescription = "Level",
                    onClick = { onIntent(CameraIntent.ToggleLevel) },
                    isSelected = state.isLevelEnabled,
                )
            }
        }

        AnimatedVisibility(
            visible = state.timerSeconds != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            state.timerSeconds?.let { seconds ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = seconds.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraIconButton(
                icon = Icons.Default.BurstMode,
                contentDescription = "Burst mode",
                onClick = { onIntent(CameraIntent.ToggleBurstMode) },
                isSelected = state.isBurstMode,
            )
            
            CameraShutterButton(
                onClick = { onIntent(CameraIntent.CapturePhoto) },
                onLongPress = if (state.isBurstMode) {
                    { onIntent(CameraIntent.CapturePhoto) }
                } else null,
                isCapturing = state.isCapturing,
                isBurstMode = state.isBurstMode,
                size = 80,
            )
            
            CameraIconButton(
                icon = Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch camera",
                onClick = { onIntent(CameraIntent.SwitchCamera) },
            )
        }

        CameraZoomSlider(
            zoomRatio = state.zoomRatio,
            onZoomChange = { zoom -> onIntent(CameraIntent.SetZoom(zoom)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 120.dp)
                .align(Alignment.BottomCenter),
            minZoom = 1.0f,
            maxZoom = state.maxZoom.coerceAtLeast(1.0f),
        )

        CameraExposureSlider(
            exposure = state.exposure,
            onExposureChange = { exposure -> onIntent(CameraIntent.SetExposure(exposure)) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            minExposure = state.minExposure,
            maxExposure = state.maxExposure,
        )

        AnimatedVisibility(
            visible = state.isLoading || state.isCapturing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        state.capturedPhotoUri?.let { uri ->
            PhotoReviewDialog(
                photoUri = uri,
                onDismiss = { onIntent(CameraIntent.RetakePhoto) },
                onConfirm = { photoUri ->
                    onPhotoCaptured(photoUri)
                },
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isPermanentlyDenied) {
                    "Camera permission is permanently denied. Please enable it in app settings."
                } else {
                    "Camera permission is required to take photos."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onRequestPermission,
            ) {
                Text(if (isPermanentlyDenied) "Open Settings" else "Grant Permission")
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
