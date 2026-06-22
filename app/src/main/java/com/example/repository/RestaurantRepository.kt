package com.example.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestaurantRepository(private val appDao: AppDao) {

    private val seedMutex = Mutex()
    private var isSeeded = false

    val restaurants: Flow<List<Restaurant>> = appDao.getAllRestaurants()

    val reservations: Flow<List<Reservation>> = appDao.getAllReservations()

    val loyaltyProfile: Flow<LoyaltyProfile?> = appDao.getLoyaltyProfileFlow()

    val rewards: Flow<List<LoyaltyReward>> = appDao.getAllRewards()

    suspend fun updateRestaurants(newList: List<Restaurant>) = withContext(Dispatchers.IO) {
        if (newList.isNotEmpty()) {
            appDao.deleteAllRestaurants()
            appDao.insertRestaurants(newList)
        }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (isSeeded) return@withContext
        seedMutex.withLock {
            if (isSeeded) return@withLock
            // Seed Loyalty profile
            val profile = appDao.getLoyaltyProfile()
            if (profile == null) {
                appDao.insertLoyaltyProfile(LoyaltyProfile())
            }
            // Seed rewards
            val rewardsList = appDao.getAllRewards().first()
            if (rewardsList.isEmpty()) {
                appDao.insertRewards(LoyaltyReward.getMockRewards())
            }
            // Seed restaurants
            val restaurantsList = appDao.getAllRestaurants().first()
            if (restaurantsList.isEmpty()) {
                appDao.insertRestaurants(Restaurant.getMockRestaurants())
            }
            isSeeded = true
        }
    }

    suspend fun getRestaurantById(id: String): Restaurant? {
        return appDao.getRestaurantById(id)
    }

    suspend fun createReservation(
        restaurantId: String,
        restaurantName: String,
        date: String,
        time: String,
        guestsCount: Int,
        tableNumber: Int,
        userName: String,
        userPhone: String
    ): Boolean {
        // 1. Insert reservation
        val points = 30 + (guestsCount * 10)
        val reservation = Reservation(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            date = date,
            time = time,
            guestsCount = guestsCount,
            tableNumber = tableNumber,
            userName = userName,
            userPhone = userPhone,
            pointsEarned = points
        )
        appDao.insertReservation(reservation)

        // 2. Reduce table count in restaurant
        val restaurant = appDao.getRestaurantById(restaurantId)
        if (restaurant != null) {
            val newAvailable = maxOf(0, restaurant.availableTables - 1)
            appDao.updateAvailableTables(restaurantId, newAvailable)
        }

        // 3. Update loyalty points
        val currentProfile = appDao.getLoyaltyProfile() ?: LoyaltyProfile()
        val newPoints = currentProfile.pointsBalance + points
        val newTier = when {
            newPoints >= 1000 -> "Gold"
            newPoints >= 500 -> "Silver"
            else -> "Bronze"
        }
        appDao.insertLoyaltyProfile(
            currentProfile.copy(
                pointsBalance = newPoints,
                loyaltyTier = newTier
            )
        )
        return true
    }

    suspend fun cancelReservation(reservationId: Int, restaurantId: String, pointsEarned: Int) {
        appDao.deleteReservationById(reservationId)
        
        // Restore table
        val restaurant = appDao.getRestaurantById(restaurantId)
        if (restaurant != null) {
            val newAvailable = minOf(restaurant.totalTables, restaurant.availableTables + 1)
            appDao.updateAvailableTables(restaurantId, newAvailable)
        }
        
        // Revert points
        val currentProfile = appDao.getLoyaltyProfile() ?: return
        val newPoints = maxOf(0, currentProfile.pointsBalance - pointsEarned)
        val newTier = when {
            newPoints >= 1000 -> "Gold"
            newPoints >= 500 -> "Silver"
            else -> "Bronze"
        }
        appDao.insertLoyaltyProfile(
            currentProfile.copy(
                pointsBalance = newPoints,
                loyaltyTier = newTier
            )
        )
    }

    suspend fun redeemReward(rewardId: String, pointsCost: Int): Boolean {
        val currentProfile = appDao.getLoyaltyProfile() ?: return false
        if (currentProfile.pointsBalance < pointsCost) return false

        // Update profile
        val newPoints = currentProfile.pointsBalance - pointsCost
        val newTier = when {
            newPoints >= 1000 -> "Gold"
            newPoints >= 500 -> "Silver"
            else -> "Bronze"
        }
        appDao.insertLoyaltyProfile(
            currentProfile.copy(
                pointsBalance = newPoints,
                loyaltyTier = newTier
            )
        )

        // Update reward redemption
        val couponCode = "RESERVE-" + (1000..9999).random()
        appDao.updateRewardRedeemed(rewardId, true, couponCode)
        return true
    }

    suspend fun updateProfile(userName: String) {
        val currentProfile = appDao.getLoyaltyProfile() ?: LoyaltyProfile()
        appDao.insertLoyaltyProfile(currentProfile.copy(userName = userName))
    }
}
