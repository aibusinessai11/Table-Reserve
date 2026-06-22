package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val restaurantId: String,
    val restaurantName: String,
    val date: String,
    val time: String,
    val guestsCount: Int,
    val tableNumber: Int,
    val userName: String,
    val userPhone: String,
    val status: String = "Confirmed", // Confirmed, Completed, Cancelled
    val timestamp: Long = System.currentTimeMillis(),
    val pointsEarned: Int = 50
)
