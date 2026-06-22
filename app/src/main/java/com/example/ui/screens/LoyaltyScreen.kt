package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LoyaltyProfile
import com.example.data.LoyaltyReward
import com.example.ui.viewmodel.RestaurantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyScreen(
    viewModel: RestaurantViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.loyaltyProfileState.collectAsState()
    val rewards by viewModel.rewardsState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    val userProfile = profile ?: LoyaltyProfile()

    // Scanning animation for the barcode
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Клуб Привилегий", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        nameInput = userProfile.userName
                        showEditProfileDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Premium Glassmorphic Loyalty Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loyalty_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    val tierColor = when (userProfile.loyaltyTier) {
                        "Gold" -> listOf(Color(0xFFF9A825), Color(0xFFF57F17))
                        "Silver" -> listOf(Color(0xFF78909C), Color(0xFF455A64))
                        else -> listOf(Color(0xFF8d6e63), Color(0xFF4e342e)) // Bronze
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(tierColor))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = userProfile.userName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Карта лояльности • ${userProfile.loyaltyTier} уровень",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }

                                Badge(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = userProfile.loyaltyTier.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "${userProfile.pointsBalance}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "балла накопительного счета",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.CurrencyRuble,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress tracker to next level
                            val nextTier = if (userProfile.loyaltyTier == "Bronze") "Silver" else "Gold"
                            val targetPoints = if (userProfile.loyaltyTier == "Bronze") 500 else 1000
                            val currentPointsValue = userProfile.pointsBalance
                            val fraction = (currentPointsValue.toFloat() / targetPoints.toFloat()).coerceIn(0f, 1f)

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "0 баллов",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "До статуса $nextTier: ${targetPoints - currentPointsValue} б.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$targetPoints б.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Animated Barcode / QR Card for Scans
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Предъявите код на кассе ресторана",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Для накопления баллов или списания вознаграждений",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Vector Procedural Barcode with Scanning Line effect
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(Color.White)
                                .border(1.dp, Color.LightGray)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                // Draw abstract barcodes
                                val seed = userProfile.qrCodeData.hashCode()
                                var currentX = 24f
                                val barSpacing = 4f
                                val endX = width - 24f

                                while (currentX < endX) {
                                    val barWidth = if ((currentX.toInt() + seed) % 5 == 0) 10f else if ((currentX.toInt() + seed) % 3 == 0) 6f else 2f
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(currentX, 10f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, height - 20f)
                                    )
                                    currentX += barWidth + barSpacing
                                }

                                // Interactive Scanning green line
                                val laserY = 10f + (height - 20f) * scanLineFraction
                                drawLine(
                                    color = Color(0xFF00E676),
                                    start = Offset(10f, laserY),
                                    end = Offset(width - 10f, laserY),
                                    strokeWidth = 3f,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = userProfile.qrCodeData,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Loyalty Store header
            item {
                Text(
                    text = "Доступные вознаграждения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Списывайте накопленные баллы на приятные комплименты от заведений",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // List of available rewards redeemable with points
            if (rewards.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Список наград пуст.")
                    }
                }
            } else {
                items(rewards, key = { it.id }) { reward ->
                    RewardStoreItem(
                        reward = reward,
                        currentPoints = userProfile.pointsBalance,
                        onRedeemClick = {
                            viewModel.redeemLoyaltyReward(reward.id, reward.pointsCost)
                        }
                    )
                }
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Редактировать профиль") },
            text = {
                Column {
                    Text("Введите ваше имя для отображения в программе лояльности:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        placeholder = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.updateUserProfile(nameInput)
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun RewardStoreItem(
    reward: LoyaltyReward,
    currentPoints: Int,
    onRedeemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = currentPoints >= reward.pointsCost

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reward.isRedeemed) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (reward.isRedeemed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rewardIcon = when (reward.iconName) {
                        "coffee" -> Icons.Default.LocalCafe
                        "cake" -> Icons.Default.Cake
                        "percent" -> Icons.Default.Percent
                        else -> Icons.Default.Star
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (reward.isRedeemed) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rewardIcon,
                            contentDescription = null,
                            tint = if (reward.isRedeemed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reward.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (reward.isRedeemed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reward.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!reward.isRedeemed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (canAfford) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${reward.pointsCost} б.",
                            color = if (canAfford) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (reward.isRedeemed) {
                // Show redeemed coupon code to waiter
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Покажите официанту купон:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = reward.couponCode,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Активирован",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRedeemClick,
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("redeem_reward_btn_${reward.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (canAfford) "Обменять баллы" else "Недостаточно баллов",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
