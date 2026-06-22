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
    val searchRadiusKm = MutableStateFlow(10) // default 10 km (options: 10, 20, 30, 50)

    // Google Account Integration state
    val isGoogleConnected = MutableStateFlow(false)
    val googleUserEmail = MutableStateFlow("aibusinessai11@gmail.com")
    val googleUserName = MutableStateFlow("Google AI Business")
    val googleProfilePic = MutableStateFlow("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80")
    val onlyGoogleSavedPlaces = MutableStateFlow(false)

    // User's virtual location
    val userLatitude = MutableStateFlow(55.7512)
    val userLongitude = MutableStateFlow(37.6184)
    val userLocationName = MutableStateFlow("Москва, Россия")

    // Route Mode
    val isDrivingMode = MutableStateFlow(false) // false = Walk, true = Drive

    val isLoadingRestaurants = MutableStateFlow(false)

    // Active restaurant for detail/route view
    val selectedRestaurant = MutableStateFlow<Restaurant?>(null)

    // UI state messages
    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    // Core streams
    data class SearchFilterState(
        val onlyAvailable: Boolean,
        val radiusKm: Int,
        val lat: Double,
        val lon: Double,
        val onlyGoogleSavedPlaces: Boolean,
        val isGoogleConnected: Boolean
    )

    private val filterStateFlow = combine(
        combine(onlyAvailable, searchRadiusKm, ::Pair),
        combine(userLatitude, userLongitude, ::Pair),
        combine(onlyGoogleSavedPlaces, isGoogleConnected, ::Pair)
    ) { p1, p2, p3 ->
        SearchFilterState(
            onlyAvailable = p1.first,
            radiusKm = p1.second,
            lat = p2.first,
            lon = p2.second,
            onlyGoogleSavedPlaces = p3.first,
            isGoogleConnected = p3.second
        )
    }

    val restaurantsState: StateFlow<List<Restaurant>> = combine(
        repository.restaurants,
        searchQuery,
        selectedCuisine,
        filterStateFlow
    ) { list, query, cuisine, searchState ->
        val lat = searchState.lat
        val lon = searchState.lon
        val radiusKm = searchState.radiusKm
        val avail = searchState.onlyAvailable
        val onlyGoogle = searchState.onlyGoogleSavedPlaces
        val isConnected = searchState.isGoogleConnected

        var filtered = list.map { r ->
            val dist = calculateDistance(lat, lon, r.latitude, r.longitude)
            r.copy(distanceMeters = dist)
        }

        // Filter by radius (convert km to meters)
        val radiusMeters = radiusKm * 1000
        filtered = filtered.filter { it.distanceMeters <= radiusMeters }

        if (onlyGoogle) {
            if (isConnected) {
                // Filter to restaurants designated as Google Saved
                filtered = filtered.filter { r ->
                    r.id.hashCode() % 3 == 0
                }
            } else {
                filtered = emptyList()
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.cuisines.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }
        }
        if (cuisine != null && cuisine != "Все") {
            filtered = filtered.filter { it.cuisines.contains(cuisine) }
        }
        var sortedResult = filtered
        if (avail) {
            // Sort available first, but always keep all of them in the list (even closed/with 0 available tables)
            sortedResult = sortedResult.sortedWith(compareByDescending<Restaurant> { it.availableTables > 0 })
        } else {
            sortedResult = sortedResult.sortedBy { it.distanceMeters }
        }
        sortedResult
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toInt()
    }

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

    fun changeSearchRadius(radiusKm: Int) {
        searchRadiusKm.value = radiusKm
        fetchRealNearbyRestaurants(userLatitude.value, userLongitude.value)
    }

    fun fetchRealNearbyRestaurants(lat: Double, lon: Double) {
        isLoadingRestaurants.value = true
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                val radiusM = searchRadiusKm.value * 1000
                // Fetch restaurants, cafes, bars, pubs etc near the location within selected radius. Limit 120 results
                val overpassQuery = "[out:json];(nwr[\"amenity\"~\"restaurant|cafe|bar|pub|fast_food\"](around:$radiusM,$lat,$lon););out center 120;"
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
                                        var nodeLat = elem.optDouble("lat", Double.NaN)
                                        var nodeLon = elem.optDouble("lon", Double.NaN)
                                        if (nodeLat.isNaN() || nodeLon.isNaN()) {
                                            val centerObj = elem.optJSONObject("center")
                                            if (centerObj != null) {
                                                nodeLat = centerObj.optDouble("lat")
                                                nodeLon = centerObj.optDouble("lon")
                                            }
                                        }
                                        if (nodeLat.isNaN() || nodeLon.isNaN()) {
                                            nodeLat = lat
                                            nodeLon = lon
                                        }

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
                                        val avTables = (0..6).random()
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

                val sorted = realRestaurants.sortedBy { it.distanceMeters }
                if (sorted.isNotEmpty()) {
                    repository.updateRestaurants(sorted)
                } else {
                    val fallback = Restaurant.getMockRestaurants().mapIndexed { idx, r ->
                        val offsetLat = (idx - 3) * 0.005 + 0.003
                        val offsetLon = (idx - 3) * 0.005 - 0.003
                        r.copy(
                            latitude = lat + offsetLat,
                            longitude = lon + offsetLon
                        )
                    }
                    repository.updateRestaurants(fallback)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val fallback = Restaurant.getMockRestaurants().mapIndexed { idx, r ->
                        val offsetLat = (idx - 3) * 0.005 + 0.003
                        val offsetLon = (idx - 3) * 0.005 - 0.003
                        r.copy(
                            latitude = lat + offsetLat,
                            longitude = lon + offsetLon
                        )
                    }
                    repository.updateRestaurants(fallback)
                } catch (dbEx: Exception) {
                    dbEx.printStackTrace()
                }
            } finally {
                isLoadingRestaurants.value = false
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
            var success = false
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
                                success = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (!success) {
                withContext(Dispatchers.Main) {
                    fetchRealNearbyRestaurants(userLatitude.value, userLongitude.value)
                }
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
