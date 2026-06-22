package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.RestaurantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RestaurantViewModel(private val repository: RestaurantRepository) : ViewModel() {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCuisine = MutableStateFlow<String?>(null)
    val onlyAvailable = MutableStateFlow(false)

    // User's virtual location
    val userLatitude = MutableStateFlow(55.7512)
    val userLongitude = MutableStateFlow(37.6184)
    val userLocationName = MutableStateFlow("Москва, Россия")

    // Route Mode
    val isDrivingMode = MutableStateFlow(false) // false = Walk, true = Drive

    // Active restaurant for detail/route view
    val selectedRestaurant = MutableStateFlow<Restaurant?>(null)

    // UI state messages
    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    // Core streams
    val restaurantsState: StateFlow<List<Restaurant>> = combine(
        repository.restaurants,
        searchQuery,
        selectedCuisine,
        onlyAvailable
    ) { list, query, cuisine, avail ->
        var filtered = list
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.cuisines.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }
        }
        if (cuisine != null) {
            filtered = filtered.filter { it.cuisines.contains(cuisine) }
        }
        if (avail) {
            filtered = filtered.filter { it.availableTables > 0 }
        }
        filtered
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val reservationsState: StateFlow<List<Reservation>> = repository.reservations
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val loyaltyProfileState: StateFlow<LoyaltyProfile?> = repository.loyaltyProfile
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val rewardsState: StateFlow<List<LoyaltyReward>> = repository.rewards
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        fetchGeoLocationByIp()
    }

    fun updateLocation(lat: Double, lon: Double) {
        userLatitude.value = lat
        userLongitude.value = lon
    }

    fun updateLocationName(name: String) {
        userLocationName.value = name
    }

    fun fetchGeoLocationByIp() {
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://ipapi.co/json/")
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string()
                            if (!bodyString.isNullOrEmpty()) {
                                val json = JSONObject(bodyString)
                                val city = json.optString("city", "Москва")
                                val country = json.optString("country_name", "Россия")
                                val lat = json.optDouble("latitude", 55.7512)
                                val lon = json.optDouble("longitude", 37.6184)
                                
                                withContext(Dispatchers.Main) {
                                    userLatitude.value = lat
                                    userLongitude.value = lon
                                    userLocationName.value = "$city, $country"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCityNameFromCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&accept-language=ru")
                    .header("User-Agent", "TableReserveAndroid")
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string()
                            if (!bodyString.isNullOrEmpty()) {
                                val json = JSONObject(bodyString)
                                val address = json.optJSONObject("address")
                                if (address != null) {
                                    val city = address.optString("city", address.optString("town", address.optString("village", address.optString("state", "Мой Город"))))
                                    val country = address.optString("country", "Россия")
                                    withContext(Dispatchers.Main) {
                                        userLocationName.value = "$city, $country"
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectRestaurant(restaurant: Restaurant?) {
        selectedRestaurant.value = restaurant
    }

    fun toggleRouteMode() {
        isDrivingMode.value = !isDrivingMode.value
    }

    fun bookTable(
        restaurantId: String,
        restaurantName: String,
        date: String,
        time: String,
        guestsCount: Int,
        tableNumber: Int,
        userName: String,
        userPhone: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.createReservation(
                restaurantId, restaurantName, date, time, guestsCount, tableNumber, userName, userPhone
            )
            if (success) {
                _message.emit("Столик успешно забронирован! Начислено бонусов за визит.")
                onSuccess()
            } else {
                _message.emit("Ошибка при бронировании.")
            }
        }
    }

    fun cancelBooking(reservationId: Int, restaurantId: String, pointsEarned: Int) {
        viewModelScope.launch {
            repository.cancelReservation(reservationId, restaurantId, pointsEarned)
            _message.emit("Бронирование отменено.")
        }
    }

    fun redeemLoyaltyReward(rewardId: String, pointsCost: Int) {
        viewModelScope.launch {
            val success = repository.redeemReward(rewardId, pointsCost)
            if (success) {
                _message.emit("Награда успешно получена! Код купона доступен ниже.")
            } else {
                _message.emit("Недостаточно баллов для получения награды.")
            }
        }
    }

    fun updateUserProfile(name: String) {
        viewModelScope.launch {
            repository.updateProfile(name)
            _message.emit("Профиль успешно обновлен.")
        }
    }
}
