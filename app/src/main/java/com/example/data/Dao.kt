package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Restaurants
    @Query("SELECT * FROM restaurants ORDER BY distanceMeters ASC")
    fun getAllRestaurants(): Flow<List<Restaurant>>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getRestaurantById(id: String): Restaurant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurants(restaurants: List<Restaurant>)

    @Query("DELETE FROM restaurants")
    suspend fun deleteAllRestaurants()

    @Query("UPDATE restaurants SET availableTables = :available WHERE id = :id")
    suspend fun updateAvailableTables(id: String, available: Int)

    // Reservations
    @Query("SELECT * FROM reservations ORDER BY timestamp DESC")
    fun getAllReservations(): Flow<List<Reservation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: Reservation)

    @Query("DELETE FROM reservations WHERE id = :id")
    suspend fun deleteReservationById(id: Int)

    // Loyalty Profile
    @Query("SELECT * FROM loyalty_profile WHERE id = 1")
    fun getLoyaltyProfileFlow(): Flow<LoyaltyProfile?>

    @Query("SELECT * FROM loyalty_profile WHERE id = 1")
    suspend fun getLoyaltyProfile(): LoyaltyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyProfile(profile: LoyaltyProfile)

    @Query("UPDATE loyalty_profile SET pointsBalance = :points WHERE id = 1")
    suspend fun updatePointsBalance(points: Int)

    // Loyalty Rewards
    @Query("SELECT * FROM loyalty_rewards")
    fun getAllRewards(): Flow<List<LoyaltyReward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<LoyaltyReward>)

    @Query("UPDATE loyalty_rewards SET isRedeemed = :isRedeemed, couponCode = :coupon WHERE id = :id")
    suspend fun updateRewardRedeemed(id: String, isRedeemed: Boolean, coupon: String)
}
