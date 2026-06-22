package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.RestaurantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    viewModel: RestaurantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val restaurant by viewModel.selectedRestaurant.collectAsState()
    val isDriving by viewModel.isDrivingMode.collectAsState()

    val r = restaurant ?: return // Safety fallback

    val scrollState = rememberScrollState()

    // Calculated travel speed parameters
    val speedKmh = if (isDriving) 40.0 else 5.0
    val distanceKm = r.distanceMeters / 1000.0
    val durationMinutes = maxOf(1, (distanceKm / speedKmh * 60.0).toInt())

    // Infinite animation parameter simulating navigation progress
    val infiniteTransition = rememberInfiniteTransition(label = "navigation")
    val progressAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeProgress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Маршрут до ${r.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            // Segmented walk vs drive control
            TabRow(
                selectedTabIndex = if (isDriving) 1 else 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                indicator = @Composable { tabPositions ->
                    Box(modifier = Modifier.fillMaxSize()) // Hide default line indicator for card-like tabs
                }
            ) {
                Tab(
                    selected = !isDriving,
                    onClick = { if (isDriving) viewModel.toggleRouteMode() },
                    modifier = Modifier.testTag("tab_walk_mode")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = if (!isDriving) MaterialTheme.colorScheme.primary else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Пешком",
                            fontWeight = FontWeight.Bold,
                            color = if (!isDriving) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
                Tab(
                    selected = isDriving,
                    onClick = { if (!isDriving) viewModel.toggleRouteMode() },
                    modifier = Modifier.testTag("tab_drive_mode")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (isDriving) MaterialTheme.colorScheme.primary else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "На машине",
                            fontWeight = FontWeight.Bold,
                            color = if (isDriving) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            // High Fidelity Custom Vector Map on Canvas
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val roadColor = MaterialTheme.colorScheme.outlineVariant
                    val selectedColor = MaterialTheme.colorScheme.primary

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("route_canvas")
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 1. Draw Neighborhood grids (streets) as abstract lines
                        drawLine(color = roadColor, start = Offset(0f, canvasHeight * 0.3f), end = Offset(canvasWidth, canvasHeight * 0.3f), strokeWidth = 8f)
                        drawLine(color = roadColor, start = Offset(0f, canvasHeight * 0.7f), end = Offset(canvasWidth, canvasHeight * 0.7f), strokeWidth = 8f)
                        drawLine(color = roadColor, start = Offset(canvasWidth * 0.3f, 0f), end = Offset(canvasWidth * 0.3f, canvasHeight), strokeWidth = 8f)
                        drawLine(color = roadColor, start = Offset(canvasWidth * 0.7f, 0f), end = Offset(canvasWidth * 0.7f, canvasHeight), strokeWidth = 8f)

                        // 2. Draw user center dot coord (fixed near bottom-left of canvas grid intersection)
                        val userOffset = Offset(canvasWidth * 0.3f, canvasHeight * 0.7f)
                        // Pulsing outer locator circle
                        drawCircle(color = selectedColor.copy(alpha = 0.3f), radius = 24f + (progressAnim * 12f), center = userOffset)
                        drawCircle(color = selectedColor, radius = 8f, center = userOffset)

                        // 3. Draw Selected Restaurant pin (near top-right of canvas)
                        val restaurantOffset = Offset(canvasWidth * 0.7f, canvasHeight * 0.3f)
                        drawCircle(color = Color(0xFFE53935), radius = 10f, center = restaurantOffset)
                        drawCircle(color = Color.White, radius = 4f, center = restaurantOffset)

                        // 4. Trace the active Route Path (winding along intersection roads)
                        val routePoints = listOf(
                            userOffset,
                            Offset(canvasWidth * 0.3f, canvasHeight * 0.3f),
                            restaurantOffset
                        )

                        val path = Path().apply {
                            moveTo(routePoints[0].x, routePoints[0].y)
                            lineTo(routePoints[1].x, routePoints[1].y)
                            lineTo(routePoints[2].x, routePoints[2].y)
                        }

                        // Drawing route highlighted line
                        drawPath(path = path, color = selectedColor, style = Stroke(width = 6f))

                        // Draw moving dots representing Navigation step progression
                        val midIntersection = routePoints[1]
                        val totalRelativeLength = 1.0f // normalized
                        val currentPoint = if (progressAnim <= 0.5f) {
                            val ratio = progressAnim / 0.5f
                            Offset(
                                x = userOffset.x + (midIntersection.x - userOffset.x) * ratio,
                                y = userOffset.y + (midIntersection.y - userOffset.y) * ratio
                            )
                        } else {
                            val ratio = (progressAnim - 0.5f) / 0.5f
                            Offset(
                                x = midIntersection.x + (restaurantOffset.x - midIntersection.x) * ratio,
                                y = midIntersection.y + (restaurantOffset.y - midIntersection.y) * ratio
                            )
                        }

                        drawCircle(color = Color(0xFFFFB300), radius = 6f, center = currentPoint)
                    }

                    // Floating Card indicating current step
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDriving) Icons.Default.DirectionsCar else Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (progressAnim < 0.5f) "Идем по бульвару..." else "Подходим к ${r.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Metrics header card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ВРЕМЯ В ПУТИ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "~$durationMinutes мин",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Distance Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("РАССТОЯНИЕ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${r.distanceMeters} метров",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step-by-step route text lists
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Пошаговые указания",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                val steps = if (isDriving) {
                    listOf(
                        RouteStep("1", "Начните движение на Тверском переулке на юго-запад", "100 м"),
                        RouteStep("2", "Поверните направо на Тверской бульвар", "500 м", Icons.Default.TurnRight),
                        RouteStep("3", "Поверните направо на переулок и двигайтесь до адреса", "550 м", Icons.Default.TurnRight),
                        RouteStep("4", "Пункт назначения находится справа", r.address, Icons.Default.CheckCircle)
                    )
                } else {
                    listOf(
                        RouteStep("1", "Двигайтесь по Тверскому бульвару на северо-восток", "200 м"),
                        RouteStep("2", "Сверните на пешеходный переход к Кузнецкому Мосту", "150 м", Icons.Default.NorthEast),
                        RouteStep("3", "Пройдите прямо по улице до вашего ресторана за углом", "400 м"),
                        RouteStep("4", "Прибытие в ресторан ${r.name}!", r.address, Icons.Default.Restaurant)
                    )
                }

                steps.forEach { step ->
                    RouteStepItem(step = step)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

data class RouteStep(
    val id: String,
    val text: String,
    val distance: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ArrowUpward
)

@Composable
fun RouteStepItem(step: RouteStep, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = step.distance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
