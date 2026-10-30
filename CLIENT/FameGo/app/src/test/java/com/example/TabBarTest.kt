package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.FameGoTabBar
import com.example.ui.components.fameGoClientTabs
import com.example.ui.components.fameGoCrewTabs
import com.example.ui.theme.FameGoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TabBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `tab bar renders all five client tabs`() {
    composeTestRule.setContent {
      FameGoTheme {
        FameGoTabBar(
          tabs = fameGoClientTabs(),
          selectedRoute = "home",
          onSelect = {}
        )
      }
    }
    composeTestRule.onNodeWithTag("famego_tab_bar").assertExists()
    composeTestRule.onNodeWithTag("tab_home").assertExists()
    composeTestRule.onNodeWithTag("tab_bookings").assertExists()
    composeTestRule.onNodeWithTag("tab_book").assertExists()
    composeTestRule.onNodeWithTag("tab_notifications").assertExists()
    composeTestRule.onNodeWithTag("tab_profile").assertExists()
  }

  @Test
  fun `tapping book tab selects book route`() {
    var selected: String? = null
    composeTestRule.setContent {
      FameGoTheme {
        FameGoTabBar(
          tabs = fameGoClientTabs(),
          selectedRoute = "home",
          onSelect = { selected = it }
        )
      }
    }
    composeTestRule.onNodeWithTag("tab_book").performClick()
    composeTestRule.waitForIdle()
    assertEquals("book", selected)
  }

  @Test
  fun `tapping alerts tab selects notifications route`() {
    var selected: String? = null
    composeTestRule.setContent {
      FameGoTheme {
        FameGoTabBar(
          tabs = fameGoClientTabs(),
          selectedRoute = "home",
          onSelect = { selected = it }
        )
      }
    }
    composeTestRule.onNodeWithTag("tab_notifications").performClick()
    composeTestRule.waitForIdle()
    assertEquals("notifications", selected)
  }

  @Test
  fun `crew bar renders jobs instead of book`() {
    composeTestRule.setContent {
      FameGoTheme {
        FameGoTabBar(
          tabs = fameGoCrewTabs(),
          selectedRoute = "home",
          onSelect = {}
        )
      }
    }
    composeTestRule.onNodeWithTag("tab_bookings").assertExists()
    composeTestRule.onNodeWithTag("tab_book").assertDoesNotExist()
  }
}
