package id.usecase.noted.presentation.note.editor.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import id.usecase.noted.presentation.components.content.InfoRow
import id.usecase.noted.presentation.components.content.InfoRowCompact
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.FloatingPermissionRequestCard
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
import id.usecase.noted.ui.theme.NotedTheme

@Composable
fun NoteLocationPickerScreenRoot(
    initialLatitude: Double?,
    initialLongitude: Double?,
    initialLabel: String?,
    onTagLocation: (latitude: Double, longitude: Double, label: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapsApiKey = remember(context) {
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        }.getOrDefault("")
    }
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            context.hasLocationPermission(),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasLocationPermission = context.hasLocationPermission()
    }
    val requestLocationPermission = remember(permissionLauncher) {
        {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
    val initialLocation = remember(initialLatitude, initialLongitude, initialLabel) {
        if (initialLatitude != null && initialLongitude != null) {
            PickedLocation(
                latLng = LatLng(initialLatitude, initialLongitude),
                label = initialLabel?.ifBlank { null } ?: formatCoordinate(initialLatitude, initialLongitude),
                shouldReverseGeocode = false,
            )
        } else {
            null
        }
    }
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    val defaultCenter = remember { LatLng(-6.200000, 106.816666) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialLocation?.latLng ?: defaultCenter,
            if (initialLocation == null) 12f else 15f,
        )
    }

    LaunchedEffect(initialLocation) {
        if (initialLocation != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLocation.latLng, 15f)
        }
    }

    LaunchedEffect(initialLocation, hasLocationPermission, mapsApiKey) {
        if (mapsApiKey.isBlank() || initialLocation != null) {
            return@LaunchedEffect
        }

        if (!hasLocationPermission) {
            requestLocationPermission()
            return@LaunchedEffect
        }

        val location = runCatching {
            fusedLocationClient.awaitCurrentLocation()
        }.getOrNull() ?: return@LaunchedEffect
        val latLng = LatLng(location.latitude, location.longitude)

        selectedLocation = PickedLocation(
            latLng = latLng,
            label = formatCoordinate(latLng.latitude, latLng.longitude),
            shouldReverseGeocode = true,
        )

        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            latLng,
            15f,
        )
    }

    LaunchedEffect(selectedLocation?.latLng, selectedLocation?.shouldReverseGeocode) {
        val picked = selectedLocation ?: return@LaunchedEffect
        if (!picked.shouldReverseGeocode) {
            return@LaunchedEffect
        }

        val latLng = picked.latLng
        val label = reverseGeocodeLabel(
            context = context,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
        )
        selectedLocation = selectedLocation?.copy(
            label = label,
            shouldReverseGeocode = false,
        )
    }

    NoteLocationPickerScreen(
        uiState = NoteLocationPickerUiState(
            isMapsApiKeyMissing = mapsApiKey.isBlank(),
            hasLocationPermission = hasLocationPermission,
            selectedLocationLabel = selectedLocation?.label,
            selectedCoordinate = selectedLocation?.latLng?.let {
                formatCoordinate(it.latitude, it.longitude)
            },
        ),
        onRequestLocationPermission = requestLocationPermission,
        onTagLocation = {
            selectedLocation?.let { picked ->
                onTagLocation(
                    picked.latLng.latitude,
                    picked.latLng.longitude,
                    picked.label,
                )
            }
        },
        onNavigateBack = onNavigateBack,
        mapContent = { mapModifier ->
            GoogleMap(
                modifier = mapModifier,
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    selectedLocation = PickedLocation(
                        latLng = latLng,
                        label = formatCoordinate(latLng.latitude, latLng.longitude),
                        shouldReverseGeocode = true,
                    )
                },
            ) {
                selectedLocation?.let { picked ->
                    Marker(
                        state = MarkerState(position = picked.latLng),
                        title = picked.label,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun NoteLocationPickerScreen(
    uiState: NoteLocationPickerUiState,
    onRequestLocationPermission: () -> Unit,
    onTagLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    mapContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NotedTopAppBar(
                title = "Tag Lokasi",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isMapsApiKeyMissing) {
                ErrorState(
                    message = "Google Maps API key belum diset. Tambahkan MAPS_API_KEY di gradle properties.",
                    onRetry = onNavigateBack,
                    retryButtonText = "Kembali",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
                return@Box
            }

            mapContent(Modifier.fillMaxSize())

            if (!uiState.hasLocationPermission) {
                FloatingPermissionRequestCard(
                    title = "Akses Lokasi",
                    message = "Izin lokasi dibutuhkan untuk menentukan posisi saat ini di peta.",
                    actionLabel = "Izinkan Lokasi",
                    onActionClick = onRequestLocationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (uiState.selectedLocationLabel != null && uiState.selectedCoordinate != null) {
                        InfoRow(
                            label = "Lokasi",
                            value = uiState.selectedLocationLabel,
                            showDivider = true,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRowCompact(
                            label = "Koordinat",
                            value = uiState.selectedCoordinate,
                        )
                    } else {
                        Text("Ketuk peta untuk memilih lokasi yang ingin ditandai.")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTagLocation,
                        enabled = uiState.hasSelectedLocation,
                    ) {
                        Text("Tag Lokasi Ini")
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPreviewPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFBFD8D2),
                        Color(0xFFA8C8E8),
                        Color(0xFFE8DDBF),
                    ),
                ),
            ),
    )
}

@Preview(showBackground = true, name = "Selected Location")
@Composable
private fun NoteLocationPickerScreenSelectedPreview() {
    NotedTheme {
        NoteLocationPickerScreen(
            uiState = NoteLocationPickerUiState(
                hasLocationPermission = true,
                selectedLocationLabel = "Jl. Sudirman No. 12, Jakarta",
                selectedCoordinate = "-6.20880, 106.84560",
            ),
            onRequestLocationPermission = {},
            onTagLocation = {},
            onNavigateBack = {},
            mapContent = { mapModifier ->
                MapPreviewPlaceholder(modifier = mapModifier)
            },
        )
    }
}

@Preview(showBackground = true, name = "Permission Prompt")
@Composable
private fun NoteLocationPickerScreenPermissionPromptPreview() {
    NotedTheme {
        NoteLocationPickerScreen(
            uiState = NoteLocationPickerUiState(
                hasLocationPermission = false,
            ),
            onRequestLocationPermission = {},
            onTagLocation = {},
            onNavigateBack = {},
            mapContent = { mapModifier ->
                MapPreviewPlaceholder(modifier = mapModifier)
            },
        )
    }
}

@Preview(showBackground = true, name = "Missing API Key")
@Composable
private fun NoteLocationPickerScreenApiKeyMissingPreview() {
    NotedTheme {
        NoteLocationPickerScreen(
            uiState = NoteLocationPickerUiState(
                isMapsApiKeyMissing = true,
                hasLocationPermission = false,
            ),
            onRequestLocationPermission = {},
            onTagLocation = {},
            onNavigateBack = {},
            mapContent = { mapModifier ->
                MapPreviewPlaceholder(modifier = mapModifier)
            },
        )
    }
}

data class NoteLocationPickerUiState(
    val isMapsApiKeyMissing: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val selectedLocationLabel: String? = null,
    val selectedCoordinate: String? = null,
) {
    val hasSelectedLocation: Boolean
        get() = selectedLocationLabel != null && selectedCoordinate != null
}

private data class PickedLocation(
    val latLng: LatLng,
    val label: String,
    val shouldReverseGeocode: Boolean,
)

private fun formatCoordinate(
    latitude: Double,
    longitude: Double,
): String {
    return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

private fun android.content.Context.hasLocationPermission(): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return hasFineLocation || hasCoarseLocation
}

@SuppressLint("MissingPermission")
private suspend fun com.google.android.gms.location.FusedLocationProviderClient.awaitCurrentLocation(): android.location.Location? {
    return suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
}

private suspend fun reverseGeocodeLabel(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
): String {
    val fallbackLabel = formatCoordinate(latitude, longitude)
    if (!Geocoder.isPresent()) {
        return fallbackLabel
    }

    return withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val label = addresses.firstOrNull()?.getAddressLine(0)
                            ?: addresses.firstOrNull()?.featureName
                            ?: fallbackLabel
                        if (continuation.isActive) {
                            continuation.resume(label)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
                    ?: addresses?.firstOrNull()?.featureName
                    ?: fallbackLabel
            }
        }.getOrDefault(fallbackLabel)
    }
}
