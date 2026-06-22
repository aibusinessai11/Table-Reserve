package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.repository.RestaurantRepository
import com.example.ui.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RestaurantViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize database, repository and ViewModel (constructor injection)
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = RestaurantRepository(database.appDao())
    val viewModel = RestaurantViewModel(repository)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
