package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_profile")
data class LoyaltyProfile(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Александр Иванов",
    val pointsBalance: Int = 350,
    val loyaltyTier: String = "Silver", // Bronze, Silver, Gold
    val qrCodeData: String = "LTY-99482-SHIELD"
)

@Entity(tableName = "loyalty_rewards")
data class LoyaltyReward(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconName: String, // "coffee", "cake", "percent", "star"
    val isRedeemed: Boolean = false,
    val couponCode: String = ""
) {
    companion object {
        fun getMockRewards(): List<LoyaltyReward> {
            return listOf(
                LoyaltyReward(
                    id = "rew_1",
                    title = "Фирменный кофе капучино",
                    description = "Ароматный кофе свежей обжарки. Доступно в любом ресторане-партнере за баллы.",
                    pointsCost = 100,
                    iconName = "coffee"
                ),
                LoyaltyReward(
                    id = "rew_2",
                    title = "Нежнейший Тирамису",
                    description = "Классический итальянский десерт по секретному рецепту от шефа.",
                    pointsCost = 180,
                    iconName = "cake"
                ),
                LoyaltyReward(
                    id = "rew_3",
                    title = "Скидка 500 рублей",
                    description = "Скидка 500₽ на весь счет при следующем бронировании (при чеке от 1500₽).",
                    pointsCost = 300,
                    iconName = "percent"
                ),
                LoyaltyReward(
                    id = "rew_4",
                    title = "Авторский лимонад",
                    description = "Освежающий цитрусово-имбирный напиток с веточкой розмарина.",
                    pointsCost = 80,
                    iconName = "star"
                )
            )
        }
    }
}
