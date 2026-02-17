package id.usecase.noted.presentation.note.editor.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed interface CameraIntent {
    data class Initialize(
        val context: Context,
        val lifecycleOwner: LifecycleOwner,
    ) : CameraIntent

    data object ToggleFlash : CameraIntent
    data object SwitchCamera : CameraIntent
    data class SetZoom(val zoomRatio: Float) : CameraIntent
    data class SetExposure(val exposure: Int) : CameraIntent
    data class SetTimer(val seconds: Int?) : CameraIntent
    data object ToggleGrid : CameraIntent
    data object ToggleLevel : CameraIntent
    data object ToggleBurstMode : CameraIntent
    data object CapturePhoto : CameraIntent
    data class ConfirmPhoto(val uri: String) : CameraIntent
    data class AttachPreviewSurface(val surfaceProvider: Preview.SurfaceProvider) : CameraIntent
    data class TapToFocus(val meteringPoint: MeteringPoint) : CameraIntent
    data object RetakePhoto : CameraIntent
    data object Cleanup : CameraIntent
}

enum class FlashMode {
    OFF,
    ON,
    AUTO,
}

data class CameraState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val flashMode: FlashMode = FlashMode.OFF,
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val zoomRatio: Float = 1.0f,
    val maxZoom: Float = 1.0f,
    val exposure: Int = 0,
    val minExposure: Int = -10,
    val maxExposure: Int = 10,
    val timerSeconds: Int? = null,
    val isGridEnabled: Boolean = false,
    val isLevelEnabled: Boolean = false,
    val isBurstMode: Boolean = false,
    val capturedPhotoUri: String? = null,
    val isCapturing: Boolean = false,
)

sealed interface CameraEffect {
    data class ShowMessage(val message: String) : CameraEffect
    data class PhotoCaptured(val uri: String) : CameraEffect
}

class CameraViewModel : ViewModel() {
    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val _effect = Channel<CameraEffect>(Channel.BUFFERED)
    val effect: Flow<CameraEffect> = _effect.receiveAsFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var previewSurfaceProvider: Preview.SurfaceProvider? = null
    private var cameraExecutor: ExecutorService? = null

    private var context: Context? = null
    private var lifecycleOwner: LifecycleOwner? = null

    fun onIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.Initialize -> initializeCamera(
                context = intent.context,
                lifecycleOwner = intent.lifecycleOwner,
            )

            CameraIntent.ToggleFlash -> toggleFlash()
            CameraIntent.SwitchCamera -> switchCamera()
            is CameraIntent.SetZoom -> setZoom(intent.zoomRatio)
            is CameraIntent.SetExposure -> setExposure(intent.exposure)
            is CameraIntent.SetTimer -> setTimer(intent.seconds)
            CameraIntent.ToggleGrid -> toggleGrid()
            CameraIntent.ToggleLevel -> toggleLevel()
            CameraIntent.ToggleBurstMode -> toggleBurstMode()
            CameraIntent.CapturePhoto -> capturePhoto()
            is CameraIntent.ConfirmPhoto -> confirmPhoto(intent.uri)
            is CameraIntent.AttachPreviewSurface -> attachPreviewSurface(intent.surfaceProvider)
            is CameraIntent.TapToFocus -> tapToFocus(intent.meteringPoint)
            CameraIntent.RetakePhoto -> retakePhoto()
            CameraIntent.Cleanup -> cleanup()
        }
    }

    private fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
    ) {
        if (_state.value.isInitialized || _state.value.isLoading) {
            return
        }

        this.context = context
        this.lifecycleOwner = lifecycleOwner

        _state.update { currentState ->
            currentState.copy(isLoading = true, error = null)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Failed to initialize camera: ${e.message}",
                    )
                }
                sendEffect(CameraEffect.ShowMessage("Camera initialization failed"))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val ctx = context ?: return

        val preview = Preview.Builder()
            .build()
            .also { createdPreview ->
                previewSurfaceProvider?.let(createdPreview::setSurfaceProvider)
                this.preview = createdPreview
            }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
            .also { this.imageCapture = it }

        val cameraSelector = _state.value.cameraSelector

        try {
            provider.unbindAll()

            camera = provider.bindToLifecycle(
                owner,
                cameraSelector,
                preview,
                imageCapture,
            )

            updateCameraCapabilities()

            _state.update { currentState ->
                currentState.copy(
                    isInitialized = true,
                    isLoading = false,
                    error = null,
                )
            }
        } catch (e: Exception) {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    error = "Failed to bind camera: ${e.message}",
                )
            }
            sendEffect(CameraEffect.ShowMessage("Camera binding failed"))
        }
    }

    private fun updateCameraCapabilities() {
        val camera = this.camera ?: return

        camera.cameraInfo.zoomState.value?.let { zoomState ->
            _state.update { currentState ->
                currentState.copy(
                    maxZoom = zoomState.maxZoomRatio,
                    zoomRatio = zoomState.zoomRatio.coerceIn(
                        zoomState.minZoomRatio,
                        zoomState.maxZoomRatio,
                    ),
                )
            }
        }

        camera.cameraInfo.exposureState.let { exposureState ->
            _state.update { currentState ->
                currentState.copy(
                    minExposure = exposureState.exposureCompensationRange.lower,
                    maxExposure = exposureState.exposureCompensationRange.upper,
                    exposure = exposureState.exposureCompensationIndex,
                )
            }
        }
    }

    private fun toggleFlash() {
        val newFlashMode = when (_state.value.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }

        _state.update { currentState ->
            currentState.copy(flashMode = newFlashMode)
        }

        updateFlashMode()
    }

    private fun updateFlashMode() {
        val imageCapture = this.imageCapture ?: return

        val flashMode = when (_state.value.flashMode) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }

        imageCapture.flashMode = flashMode
    }

    private fun switchCamera() {
        val currentSelector = _state.value.cameraSelector
        val newSelector = if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        _state.update { currentState ->
            currentState.copy(
                cameraSelector = newSelector,
                zoomRatio = 1.0f,
                exposure = 0,
            )
        }

        bindCameraUseCases()
    }

    private fun setZoom(zoomRatio: Float) {
        val camera = this.camera ?: return
        val maxZoom = _state.value.maxZoom
        val clampedZoom = zoomRatio.coerceIn(1.0f, maxZoom)

        camera.cameraControl.setZoomRatio(clampedZoom)

        _state.update { currentState ->
            currentState.copy(zoomRatio = clampedZoom)
        }
    }

    private fun setExposure(exposure: Int) {
        val camera = this.camera ?: return
        val minExposure = _state.value.minExposure
        val maxExposure = _state.value.maxExposure
        val clampedExposure = exposure.coerceIn(minExposure, maxExposure)

        camera.cameraControl.setExposureCompensationIndex(clampedExposure)

        _state.update { currentState ->
            currentState.copy(exposure = clampedExposure)
        }
    }

    private fun setTimer(seconds: Int?) {
        val validTimer = seconds?.takeIf { it in listOf(3, 5, 10) }

        _state.update { currentState ->
            currentState.copy(timerSeconds = validTimer)
        }
    }

    private fun toggleGrid() {
        _state.update { currentState ->
            currentState.copy(isGridEnabled = !currentState.isGridEnabled)
        }
    }

    private fun toggleLevel() {
        _state.update { currentState ->
            currentState.copy(isLevelEnabled = !currentState.isLevelEnabled)
        }
    }

    private fun toggleBurstMode() {
        _state.update { currentState ->
            currentState.copy(isBurstMode = !currentState.isBurstMode)
        }
    }

    private fun capturePhoto() {
        val imageCapture = this.imageCapture ?: return
        val ctx = context ?: return

        if (_state.value.isCapturing) {
            return
        }

        _state.update { currentState ->
            currentState.copy(isCapturing = true, error = null)
        }

        val timerSeconds = _state.value.timerSeconds

        viewModelScope.launch {
            if (timerSeconds != null && timerSeconds > 0) {
                kotlinx.coroutines.delay(timerSeconds * 1000L)
            }

            takePicture(imageCapture, ctx)
        }
    }

    private fun takePicture(imageCapture: ImageCapture, context: Context) {
        val photoFile = createTempFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    _state.update { currentState ->
                        currentState.copy(
                            isCapturing = false,
                            error = "Photo capture failed: ${exception.message}",
                        )
                    }
                    sendEffect(CameraEffect.ShowMessage("Failed to capture photo"))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri?.toString() ?: photoFile.absolutePath
                    _state.update { currentState ->
                        currentState.copy(
                            isCapturing = false,
                            capturedPhotoUri = savedUri,
                        )
                    }
                }
            },
        )
    }

    private fun createTempFile(context: Context): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val storageDir = context.cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir,
        )
    }

    private fun confirmPhoto(uri: String) {
        _state.update { currentState ->
            currentState.copy(capturedPhotoUri = null)
        }
        sendEffect(CameraEffect.PhotoCaptured(uri))
    }

    private fun attachPreviewSurface(surfaceProvider: Preview.SurfaceProvider) {
        previewSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }

    private fun tapToFocus(meteringPoint: MeteringPoint) {
        val currentCamera = camera ?: return
        val action = FocusMeteringAction.Builder(
            meteringPoint,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        currentCamera.cameraControl.startFocusAndMetering(action)
    }

    private fun retakePhoto() {
        _state.update { currentState ->
            currentState.copy(capturedPhotoUri = null)
        }
    }

    private fun cleanup() {
        cameraExecutor?.shutdown()
        cameraExecutor = null

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }

        cameraProvider = null
        camera = null
        imageCapture = null
        preview = null
        previewSurfaceProvider = null
        context = null
        lifecycleOwner = null

        _state.update { currentState ->
            currentState.copy(
                isInitialized = false,
                isLoading = false,
                error = null,
                capturedPhotoUri = null,
                isCapturing = false,
            )
        }
    }

    private fun sendEffect(effect: CameraEffect) {
        _effect.trySend(effect)
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
