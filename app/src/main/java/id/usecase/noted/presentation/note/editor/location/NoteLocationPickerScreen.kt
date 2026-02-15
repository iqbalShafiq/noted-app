package id.usecase.noted.presentation.note.editor.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NoteLocationPickerScreen(
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
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tag Lokasi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (mapsApiKey.isNotBlank()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedLocation?.let { picked ->
                        Text(text = "Lokasi: ${picked.label}")
                        Text(text = "Titik: ${formatCoordinate(picked.latLng.latitude, picked.latLng.longitude)}")
                    }
                    Button(
                        onClick = {
                            val picked = selectedLocation ?: return@Button
                            onTagLocation(
                                picked.latLng.latitude,
                                picked.latLng.longitude,
                                picked.label,
                            )
                        },
                        enabled = selectedLocation != null,
                    ) {
                        Text("Tag Lokasi Ini")
                    }
                }
            }
        },
    ) { innerPadding ->
        if (mapsApiKey.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Google Maps API key belum diset. Tambahkan MAPS_API_KEY di gradle properties.")
                Button(onClick = onNavigateBack) {
                    Text("Kembali")
                }
            }
        } else {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
        }
    }
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
