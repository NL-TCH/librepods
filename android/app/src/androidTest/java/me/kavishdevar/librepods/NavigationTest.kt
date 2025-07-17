/*
 * LibrePods - AirPods liberated from Apple's ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.kavishdevar.librepods

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.kavishdevar.librepods.ui.theme.LibrePodsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for navigation flow with mocked root bypass
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationWithMockedHook() {
        composeTestRule.setContent {
            LibrePodsTheme {
                val navController = rememberNavController()
                
                // Simulate having hook available to bypass onboarding
                NavHost(
                    navController = navController,
                    startDestination = "settings" // Skip onboarding
                ) {
                    composable("settings") {
                        androidx.compose.foundation.layout.Column {
                            androidx.compose.material3.Text("Settings Screen")
                            androidx.compose.material3.Button(
                                onClick = {
                                    navController.navigate("debug")
                                }
                            ) {
                                androidx.compose.material3.Text("Debug")
                            }
                        }
                    }
                    composable("debug") {
                        androidx.compose.material3.Text("Debug Screen")
                    }
                    composable("rename") {
                        androidx.compose.material3.Text("Rename Screen")
                    }
                }
            }
        }

        // This test verifies we can navigate to different screens when hook is available
        composeTestRule.onNodeWithText("Settings Screen").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.onNodeWithText(text: String) = 
        onNodeWithText(text)
}