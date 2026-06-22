package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Restaurant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Table Reserve", appName)
  }

  @Test
  fun `verify mock restaurants details`() {
    val list = Restaurant.getMockRestaurants()
    assertEquals(6, list.size)
    assertEquals("The Rustic Olive", list[0].name)
    assertEquals("Ginger & Soy", list[1].name)
  }
}
