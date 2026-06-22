package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.RestaurantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantViewModel,
    onBack: () -> Unit,
    onSuccessBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val restaurant by viewModel.selectedRestaurant.collectAsState()
    val loyaltyProfile by viewModel.loyaltyProfileState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (restaurant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пожалуйста, сначала выберите ресторан.")
        }
        return
    }

    val r = restaurant!!
    val scrollState = rememberScrollState()

    // Dialog state
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedTableNumber by remember { mutableStateOf(1) }
    var selectedTableSeats by remember { mutableStateOf(2) }

    // Dialog input fields
    var guestCountInput by remember { mutableStateOf("2") }
    val defaultName = loyaltyProfile?.userName ?: "Александр Иванов"
    var nameInput by remember { mutableStateOf(defaultName) }
    var phoneInput by remember { mutableStateOf("+7 (999) 123-45-67") }

    // Update name input when loyalty changes
    LaunchedEffect(loyaltyProfile) {
        if (loyaltyProfile != null) {
            nameInput = loyaltyProfile!!.userName
        }
    }

    // Interactive custom tables layout (8 tables)
    // Table model: number, seats count, isAvailable
    // Deterministic layout based on restaurant ID
    val seatingList = remember(r.id, r.availableTables) {
        List(8) { index ->
            val number = index + 1
            val seats = if (index % 3 == 0) 4 else if (index % 4 == 0) 6 else 2
            val isAvailable = index < r.availableTables
            SeatingTable(number, seats, isAvailable)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(r.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Visual header image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(r.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = r.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Info badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(r.cuisines) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(r.rating.toString())
                            }
                        }
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text("чек ${r.averageBill}₽") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = r.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "О ресторане",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = r.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Loyalty special program card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Программа лояльности",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = r.loyaltyOffer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Table Plan instructions
                Text(
                    text = "Интерактивная схема столиков",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Выберите свободный столик на схеме зала ниже для мгновенного бронирования:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Table grid rendering container of size 2 columns, height fixed to view comfortably
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(seatingList) { table ->
                            TableLayoutItem(
                                table = table,
                                onClick = {
                                    if (table.isAvailable) {
                                        selectedTableNumber = table.number
                                        selectedTableSeats = table.seatsCount
                                        guestCountInput = table.seatsCount.toString()
                                        showBookingDialog = true
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Status Legends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Свободен", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Занят", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Safety Booking Trigger button in case they just want general booking
                Button(
                    onClick = {
                        val firstAvailTable = seatingList.firstOrNull { it.isAvailable }
                        if (firstAvailTable != null) {
                            selectedTableNumber = firstAvailTable.number
                            selectedTableSeats = firstAvailTable.seatsCount
                            guestCountInput = firstAvailTable.seatsCount.toString()
                        } else {
                            selectedTableNumber = 1
                            selectedTableSeats = 2
                            guestCountInput = "2"
                        }
                        showBookingDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reserve_any_table_btn"),
                    enabled = r.availableTables > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdded, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (r.availableTables > 0) "Быстрое бронирование" else "Ресторан заполнен",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Booking detail pop-up modal dialog
    if (showBookingDialog) {
        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Резерв стола №$selectedTableNumber")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "В выбранном зале столик №$selectedTableNumber рассчитан на $selectedTableSeats человек. Пожалуйста, подтвердите данные:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = guestCountInput,
                        onValueChange = { guestCountInput = it },
                        label = { Text("Количество гостей") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var dateInput by remember { mutableStateOf("Сегодня, 21 июня") }
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Дата визита") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var timeInput by remember { mutableStateOf("19:30") }
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Время визита") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Ваша имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Номер телефона") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Loyalty Reward estimation indicator
                    val estimatedPoints = (guestCountInput.toIntOrNull() ?: 2) * 10 + 30
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Начислим +$estimatedPoints баллов за это бронирование!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val guests = guestCountInput.toIntOrNull() ?: selectedTableSeats
                        viewModel.bookTable(
                            restaurantId = r.id,
                            restaurantName = r.name,
                            date = "Сегодня",
                            time = guestCountInput, // we can use the visual fields
                            guestsCount = guests,
                            tableNumber = selectedTableNumber,
                            userName = nameInput,
                            userPhone = phoneInput,
                            onSuccess = {
                                showBookingDialog = false
                                onSuccessBooking()
                            }
                        )
                    },
                    modifier = Modifier.testTag("dialog_confirm_booking_btn")
                ) {
                    Text("Забронировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingDialog = false }) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// Model representing visual floor-plan tables
data class SeatingTable(
    val number: Int,
    val seatsCount: Int,
    val isAvailable: Boolean
)

@Composable
fun TableLayoutItem(
    table: SeatingTable,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (table.isAvailable) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val outlineColor = if (table.isAvailable) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Gray.copy(alpha = 0.3f)
    }

    val textColor = if (table.isAvailable) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(1.dp, outlineColor, RoundedCornerShape(10.dp))
            .clickable(enabled = table.isAvailable, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TableRestaurant,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Стол №${table.number}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(outlineColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${table.seatsCount} мест",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
