package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.RestaurantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RestaurantViewModel(private val repository: RestaurantRepository) : ViewModel() {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCuisine = MutableStateFlow<String?>(null)
    val onlyAvailable = MutableStateFlow(false)

    // User's virtual location (Center of Moscow for demo)
    val userLatitude = MutableStateFlow(55.7512)
    val userLongitude = MutableStateFlow(37.6184)

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
