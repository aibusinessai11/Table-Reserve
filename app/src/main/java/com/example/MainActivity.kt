package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.repository.RestaurantRepository
import com.example.ui.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RestaurantViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: RestaurantViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            setupLocationListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database, repository and ViewModel (constructor injection)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = RestaurantRepository(database.appDao())
        viewModel = RestaurantViewModel(repository)

        checkLocationPermissions()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationListener()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    fun requestGPSLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            android.widget.Toast.makeText(this, "Пожалуйста, включите GPS / Геолокацию в настройках вашего устройства", android.widget.Toast.LENGTH_LONG).show()
        } else {
            android.widget.Toast.makeText(this, "Запрос GPS координат...", android.widget.Toast.LENGTH_SHORT).show()
        }
        checkLocationPermissions()
    }

    private fun setupLocationListener() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            if (bestLocation != null) {
                updateModelLocation(bestLocation)
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateModelLocation(location)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            // Request updates from both providers securely if they are active
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateModelLocation(location: Location) {
        viewModel.updateLocation(location.latitude, location.longitude)
        
        try {
            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Мой Город"
                        val country = address.countryName ?: "Россия"
                        viewModel.updateLocationName("$city, $country")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Мой Город"
                    val country = address.countryName ?: "Россия"
                    viewModel.updateLocationName("$city, $country")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If built-in platform Geocoder fails (e.g. mock or emulator), trigger fallback OSM network geocoding
            viewModel.fetchCityNameFromCoordinates(location.latitude, location.longitude)
        }
    }
}
