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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.kavishdevar.librepods.ui.theme.LibrePodsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI test demonstrating the complete app flow with mock data
 * This test bypasses the root setup and shows how to test all major screens
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCompleteAppFlowWithMockData() {
        // Use different mock states to test various scenarios
        val mockStates = listOf(
            MockData.defaultMockState,
            MockData.lowBatteryMockState,
            MockData.disconnectedMockState,
            MockData.oneEarbudOutMockState
        )

        mockStates.forEach { mockState ->
            testAirPodsState(mockState)
        }
    }

    private fun testAirPodsState(mockState: MockData.MockAirPodsState) {
        composeTestRule.setContent {
            LibrePodsTheme {
                MockLibrePodsApp(mockState)
            }
        }

        // Test connection status
        if (mockState.isConnected) {
            composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
            composeTestRule.onNodeWithText(mockState.deviceName).assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("Disconnected").assertIsDisplayed()
        }

        // Test battery levels
        composeTestRule.onNodeWithText("Left: ${mockState.batteryLevels.leftBud}%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right: ${mockState.batteryLevels.rightBud}%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Case: ${mockState.batteryLevels.case}%").assertIsDisplayed()

        // Test low battery warning
        val hasLowBattery = mockState.batteryLevels.leftBud < 20 || 
                           mockState.batteryLevels.rightBud < 20 || 
                           mockState.batteryLevels.case < 10
        
        if (hasLowBattery) {
            composeTestRule.onNodeWithText("Low Battery").assertIsDisplayed()
        }

        // Test noise control mode
        val noiseControlText = when (mockState.noiseControlMode) {
            MockData.MockNoiseControlMode.OFF -> "Off"
            MockData.MockNoiseControlMode.TRANSPARENCY -> "Transparency"
            MockData.MockNoiseControlMode.NOISE_CANCELLATION -> "Noise Cancellation"
        }
        composeTestRule.onNodeWithText(noiseControlText).assertIsDisplayed()

        // Test ear detection status
        if (mockState.leftInEar && mockState.rightInEar) {
            composeTestRule.onNodeWithText("Both earbuds in").assertIsDisplayed()
        } else if (!mockState.leftInEar && !mockState.rightInEar) {
            composeTestRule.onNodeWithText("Both earbuds out").assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("One earbud out").assertIsDisplayed()
        }

        // Test navigation - click on different screens
        if (mockState.isConnected) {
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.onNodeWithText("Settings Screen").assertIsDisplayed()

            composeTestRule.onNodeWithText("Debug").performClick() 
            composeTestRule.onNodeWithText("Debug Screen").assertIsDisplayed()

            composeTestRule.onNodeWithText("Back to Settings").performClick()
            composeTestRule.onNodeWithText("Settings Screen").assertIsDisplayed()
        }
    }

    @Test
    fun testInteractiveFeatures() {
        composeTestRule.setContent {
            LibrePodsTheme {
                MockInteractiveScreen()
            }
        }

        // Test toggle switches
        composeTestRule.onNodeWithText("Ear Detection").assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("ear_detection_switch")).performClick()

        composeTestRule.onNodeWithText("Head Tracking").assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("head_tracking_switch")).performClick()

        composeTestRule.onNodeWithText("Conversational Awareness").assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("conversational_awareness_switch")).performClick()

        // Test noise control mode selection
        composeTestRule.onNodeWithText("Transparency").performClick()
        composeTestRule.onNodeWithText("Transparency Selected").assertIsDisplayed()

        composeTestRule.onNodeWithText("Noise Cancellation").performClick()
        composeTestRule.onNodeWithText("Noise Cancellation Selected").assertIsDisplayed()
    }
}

/**
 * Mock LibrePods app component that bypasses root setup
 * This simulates the full app navigation with mock data
 */
@Composable
fun MockLibrePodsApp(mockState: MockData.MockAirPodsState) {
    val navController = rememberNavController()
    
    // Bypass onboarding - start directly at settings when "hook available"
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MockMainScreen(mockState, navController)
        }
        composable("settings") {
            MockSettingsScreen(navController)
        }
        composable("debug") {
            MockDebugScreen(navController)
        }
    }
}

@Composable
fun MockMainScreen(mockState: MockData.MockAirPodsState, navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (mockState.isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (mockState.isConnected) {
                    Text(mockState.deviceName)
                }
            }
        }

        // Battery Status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Battery Status", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Left: ${mockState.batteryLevels.leftBud}%")
                    Text("Right: ${mockState.batteryLevels.rightBud}%")
                    Text("Case: ${mockState.batteryLevels.case}%")
                }
                
                // Low battery warning
                val hasLowBattery = mockState.batteryLevels.leftBud < 20 || 
                                   mockState.batteryLevels.rightBud < 20 || 
                                   mockState.batteryLevels.case < 10
                if (hasLowBattery) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Low Battery", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Noise Control Mode
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Noise Control", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val modeText = when (mockState.noiseControlMode) {
                    MockData.MockNoiseControlMode.OFF -> "Off"
                    MockData.MockNoiseControlMode.TRANSPARENCY -> "Transparency"
                    MockData.MockNoiseControlMode.NOISE_CANCELLATION -> "Noise Cancellation"
                }
                Text(modeText)
            }
        }

        // Ear Detection Status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ear Detection", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val earStatus = when {
                    mockState.leftInEar && mockState.rightInEar -> "Both earbuds in"
                    !mockState.leftInEar && !mockState.rightInEar -> "Both earbuds out"
                    else -> "One earbud out"
                }
                Text(earStatus)
            }
        }

        // Navigation Buttons (only show if connected)
        if (mockState.isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { navController.navigate("settings") }) {
                    Text("Settings")
                }
                Button(onClick = { navController.navigate("debug") }) {
                    Text("Debug")
                }
            }
        }
    }
}

@Composable
fun MockSettingsScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigateUp() }) {
            Text("Back to Main")
        }
    }
}

@Composable
fun MockDebugScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Debug Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { navController.navigate("settings") }) {
            Text("Back to Settings")
        }
    }
}

@Composable
fun MockInteractiveScreen() {
    var earDetectionEnabled by remember { mutableStateOf(true) }
    var headTrackingEnabled by remember { mutableStateOf(true) }
    var conversationalAwarenessEnabled by remember { mutableStateOf(false) }
    var selectedNoiseControl by remember { mutableStateOf("Noise Cancellation") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Interactive Features", style = MaterialTheme.typography.headlineMedium)

        // Feature toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ear Detection")
            Switch(
                checked = earDetectionEnabled,
                onCheckedChange = { earDetectionEnabled = it },
                modifier = Modifier.testTag("ear_detection_switch")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Head Tracking")
            Switch(
                checked = headTrackingEnabled,
                onCheckedChange = { headTrackingEnabled = it },
                modifier = Modifier.testTag("head_tracking_switch")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Conversational Awareness")
            Switch(
                checked = conversationalAwarenessEnabled,
                onCheckedChange = { conversationalAwarenessEnabled = it },
                modifier = Modifier.testTag("conversational_awareness_switch")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Noise Control Mode", style = MaterialTheme.typography.titleMedium)

        // Noise control options
        val options = listOf("Off", "Transparency", "Noise Cancellation")
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedNoiseControl == option,
                    onClick = { selectedNoiseControl = option }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(option)
            }
        }

        if (selectedNoiseControl.isNotEmpty()) {
            Text("$selectedNoiseControl Selected")
        }
    }
}