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
        fetchRealNearbyRestaurants(lat, lon)
    }

    fun updateLocationName(name: String) {
        userLocationName.value = name
    }

    fun fetchRealNearbyRestaurants(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                // Fetch restaurants and cafes near the location within 3000 meters. Limit 15 results
                val overpassQuery = "[out:json];(node[\"amenity\"=\"restaurant\"](around:3000,$lat,$lon);node[\"amenity\"=\"cafe\"](around:3000,$lat,$lon););out 15;"
                val request = Request.Builder()
                    .url("https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(overpassQuery, "UTF-8")}")
                    .build()

                val realRestaurants = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val list = mutableListOf<Restaurant>()
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string()
                            if (!bodyString.isNullOrEmpty()) {
                                val json = JSONObject(bodyString)
                                val elements = json.optJSONArray("elements")
                                if (elements != null && elements.length() > 0) {
                                    val imagePool = listOf(
                                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?w=500&auto=format&fit=crop&q=60",
                                        "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=500&auto=format&fit=crop&q=60"
                                    )
                                    for (i in 0 until elements.length()) {
                                        val elem = elements.getJSONObject(i)
                                        val nodeLat = elem.optDouble("lat", lat)
                                        val nodeLon = elem.optDouble("lon", lon)
                                        val idVal = elem.optLong("id")
                                        val tags = elem.optJSONObject("tags") ?: continue
                                        val rawName = tags.optString("name")
                                        if (rawName.isNullOrEmpty()) continue

                                        val rawCuisine = tags.optString("cuisine", "cafe")
                                        val cuisineName = mapCuisine(rawCuisine)
                                        
                                        val street = tags.optString("addr:street", "")
                                        val house = tags.optString("addr:housenumber", "")
                                        val addressStr = if (street.isNotEmpty()) {
                                            "ул. $street, $house"
                                        } else {
                                            "В районе вашего положения"
                                        }

                                        val distResults = FloatArray(1)
                                        try {
                                            android.location.Location.distanceBetween(lat, lon, nodeLat, nodeLon, distResults)
                                        } catch (e: Exception) {
                                            distResults[0] = (Math.sqrt(Math.pow(nodeLat - lat, 2.0) + Math.pow(nodeLon - lon, 2.0)) * 111000).toFloat()
                                        }
                                        val distance = distResults[0].toInt()

                                        val rRating = ((43..49).random() / 10.0)
                                        val rBill = (8..28).random() * 100
                                        val rTables = (10..22).random()
                                        val avTables = (1..6).random()
                                        val img = imagePool[i % imagePool.size]

                                        list.add(
                                            Restaurant(
                                                id = "osm_$idVal",
                                                name = rawName,
                                                cuisines = cuisineName,
                                                address = addressStr,
                                                distanceMeters = distance,
                                                latitude = nodeLat,
                                                longitude = nodeLon,
                                                rating = rRating,
                                                averageBill = rBill,
                                                totalTables = rTables,
                                                availableTables = avTables,
                                                imageUrl = img,
                                                description = "Аутентичный уголок вкуса поблизости от вас с уютной атмосферой, высоким рейтингом и приветливым персоналом.",
                                                loyaltyOffer = "Скидка по золотой карте и +50 кешбэк баллов за визит",
                                                popularity = (82..98).random()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        list
                    }
                }

                if (realRestaurants.isNotEmpty()) {
                    val sorted = realRestaurants.sortedBy { it.distanceMeters }
                    repository.updateRestaurants(sorted)
                } else {
                    // Fallback: If Overpass API is empty or failed, update mock restaurant distances relative to new location
                    val currentList = repository.restaurants.first()
                    val updatedMockList = currentList.map { mockRest ->
                        val distResults = FloatArray(1)
                        try {
                            android.location.Location.distanceBetween(lat, lon, mockRest.latitude, mockRest.longitude, distResults)
                        } catch (e: Exception) {
                            distResults[0] = (Math.sqrt(Math.pow(mockRest.latitude - lat, 2.0) + Math.pow(mockRest.longitude - lon, 2.0)) * 111000).toFloat()
                        }
                        mockRest.copy(distanceMeters = distResults[0].toInt())
                    }
                    if (updatedMockList.isNotEmpty()) {
                        repository.updateRestaurants(updatedMockList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mapCuisine(cuisineTag: String): String {
        val tag = cuisineTag.lowercase()
        return when {
            tag.contains("italian") || tag.contains("pizza") || tag.contains("pasta") -> "Итальянская кухня"
            tag.contains("asian") || tag.contains("japanese") || tag.contains("sushi") || tag.contains("chinese") -> "Паназиатская кухня"
            tag.contains("french") -> "Французская кухня"
            tag.contains("burger") || tag.contains("american") || tag.contains("fast_food") -> "Американская кухня"
            tag.contains("cafe") || tag.contains("coffee") || tag.contains("bakery") || tag.contains("tea") -> "Кофейня • Десерты"
            tag.contains("german") || tag.contains("beer") -> "Баварский паб"
            tag.contains("georgian") -> "Грузинская кухня"
            tag.contains("russian") -> "Русская кухня"
            tag.contains("grill") || tag.contains("steak") || tag.contains("meat") || tag.contains("bbq") -> "Гриль • Стейк-хаус"
            tag.contains("mexican") -> "Мексиканская кухня"
            tag.contains("vietnamese") -> "Вьетнамская кухня"
            tag.contains("turkish") || tag.contains("kebab") -> "Турецкая кухня"
            else -> "Европейская кухня"
        }
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
                                    fetchRealNearbyRestaurants(lat, lon)
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
