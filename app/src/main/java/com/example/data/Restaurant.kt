package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey val id: String,
    val name: String,
    val cuisines: String,
    val address: String,
    val distanceMeters: Int,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val averageBill: Int,
    val totalTables: Int,
    val availableTables: Int,
    val imageUrl: String,
    val description: String,
    val loyaltyOffer: String,
    val popularity: Int,
    val city: String = "Москва"
) {
    companion object {
        fun getMockRestaurants(): List<Restaurant> {
            return listOf(
                Restaurant(
                    id = "rest_1",
                    name = "The Rustic Olive",
                    cuisines = "Итальянская • Пицца • Паста",
                    address = "ул. Большая Дмитровка, 12",
                    distanceMeters = 250,
                    latitude = 55.7533,
                    longitude = 37.6191,
                    rating = 4.8,
                    averageBill = 1800,
                    totalTables = 15,
                    availableTables = 4,
                    imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60",
                    description = "Уютный итальянский ресторанчик в самом сердце города. Домашняя паста ручной работы, ароматная пицца из дровяной печи и великолепная карта вин.",
                    loyaltyOffer = "Скидка 10% или бесплатный десерт Тирамису при заказе от 2000₽",
                    popularity = 95
                ),
                Restaurant(
                    id = "rest_2",
                    name = "Ginger & Soy",
                    cuisines = "Паназиатская • Суши • Рамен",
                    address = "Тверской бульвар, 26",
                    distanceMeters = 480,
                    latitude = 55.7490,
                    longitude = 37.6150,
                    rating = 4.6,
                    averageBill = 2100,
                    totalTables = 20,
                    availableTables = 0, // Fully booked right now
                    imageUrl = "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=500&auto=format&fit=crop&q=60",
                    description = "Современный взгляд на азиатскую классику. Авторские роллы, наваристый рамен и фирменная утка по-пекински в изысканной атмосфере.",
                    loyaltyOffer = "Удвоенный кешбэк баллами на все меню суши",
                    popularity = 88
                ),
                Restaurant(
                    id = "rest_3",
                    name = "La Petite Brasserie",
                    cuisines = "Французская • Завтраки • Круассаны",
                    address = "Никитский переулок, 5",
                    distanceMeters = 720,
                    latitude = 55.7565,
                    longitude = 37.6110,
                    rating = 4.7,
                    averageBill = 1500,
                    totalTables = 10,
                    availableTables = 5,
                    imageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop&q=60",
                    description = "Маленький уголок Парижа. Фирменные круассаны, нежнейшие десерты, сытные киши и ароматный кофе. Идеально для завтраков и романтических встреч.",
                    loyaltyOffer = "Кофе в подарок при первом бронировании столика",
                    popularity = 91
                ),
                Restaurant(
                    id = "rest_4",
                    name = "The Green Hearth",
                    cuisines = "Веганская • Вегетарианская • Полезная еда",
                    address = "Кузнецкий Мост, 19с1",
                    distanceMeters = 950,
                    latitude = 55.7480,
                    longitude = 37.6250,
                    rating = 4.5,
                    averageBill = 1200,
                    totalTables = 12,
                    availableTables = 3,
                    imageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500&auto=format&fit=crop&q=60",
                    description = "Ресторан здорового питания. Только свежие органические продукты, суперфуды, raw-десерты и безглютеновая выпечка. Зарядитесь энергией природы!",
                    loyaltyOffer = "Имбирный шот в качестве комплимента к каждому бронированию",
                    popularity = 84
                ),
                Restaurant(
                    id = "rest_5",
                    name = "Ugly Burger & Diner",
                    cuisines = "Американская • Бургеры • Стейки",
                    address = "Театральный проезд, 3с3",
                    distanceMeters = 1150,
                    latitude = 55.7610,
                    longitude = 37.6220,
                    rating = 4.4,
                    averageBill = 1400,
                    totalTables = 18,
                    availableTables = 2,
                    imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
                    description = "Настоящий американский дайнер из фильмов. Огромные бургеры с сочной котлетой, огненные стейки, молочные коктейли и хрустящий картофель фри.",
                    loyaltyOffer = "Крылышки в подарок за каждые накопленные 200 баллов",
                    popularity = 80
                ),
                Restaurant(
                    id = "rest_6",
                    name = "Avocado & Friends",
                    cuisines = "Кето • Полезный бранч • Смузи",
                    address = "Петровка, 15",
                    distanceMeters = 350,
                    latitude = 55.7540,
                    longitude = 37.6160,
                    rating = 4.9,
                    averageBill = 1600,
                    totalTables = 8,
                    availableTables = 1,
                    imageUrl = "https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500&auto=format&fit=crop&q=60",
                    description = "Инстаграмный бранч-бар, где авокадо преобладает в каждом блюде. Смузи-боулы, яйца Бенедикт и авторские лимонады.",
                    loyaltyOffer = "Бесплатный смузи на выбор за приглашенного друга",
                    popularity = 97
                )
            )
        }
    }
}
