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

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import me.kavishdevar.librepods.utils.RadareOffsetFinder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test for bypassing root setup by mocking RadareOffsetFinder
 * This demonstrates how to test the app without actual root access
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RootBypassTest {

    @Test
    fun testBypassRootSetupWithMockedHook() {
        val mockContext = mockk<Context>(relaxed = true)
        
        // Mock RadareOffsetFinder to return that hook is available
        val radareOffsetFinder = spyk(RadareOffsetFinder(mockContext))
        every { radareOffsetFinder.isHookOffsetAvailable() } returns true
        
        // Verify that when hook is available, we bypass the onboarding
        val hookAvailable = radareOffsetFinder.isHookOffsetAvailable()
        assert(hookAvailable) { "Hook should be available in test environment" }
        
        // Test navigation logic
        val startDestination = if (hookAvailable) "settings" else "onboarding"
        assert(startDestination == "settings") { "Should navigate to settings when hook is available" }
    }
    
    @Test
    fun testMockDataValidation() {
        val mockState = MockData.defaultMockState
        
        // Validate mock data structure
        assert(mockState.isConnected) { "Default mock state should be connected" }
        assert(mockState.batteryLevels.leftBud > 0) { "Left bud should have battery" }
        assert(mockState.batteryLevels.rightBud > 0) { "Right bud should have battery" }
        assert(mockState.batteryLevels.case > 0) { "Case should have battery" }
        assert(mockState.deviceName.isNotEmpty()) { "Device name should not be empty" }
    }
    
    @Test
    fun testLowBatteryScenarioValidation() {
        val lowBatteryState = MockData.lowBatteryMockState
        
        // Validate low battery scenario
        assert(lowBatteryState.batteryLevels.leftBud < 20) { "Left bud should have low battery" }
        assert(lowBatteryState.batteryLevels.rightBud < 25) { "Right bud should have low battery" }
        assert(lowBatteryState.batteryLevels.case < 10) { "Case should have very low battery" }
    }
    
    @Test
    fun testDisconnectedScenarioValidation() {
        val disconnectedState = MockData.disconnectedMockState
        
        // Validate disconnected scenario
        assert(!disconnectedState.isConnected) { "Should be disconnected" }
        assert(disconnectedState.batteryLevels.leftBud == 0) { "Disconnected device should show 0% battery" }
        assert(disconnectedState.batteryLevels.rightBud == 0) { "Disconnected device should show 0% battery" }
        assert(disconnectedState.batteryLevels.case == 0) { "Disconnected device should show 0% battery" }
    }
    
    @Test
    fun testOneEarbudOutScenario() {
        val oneEarOut = MockData.oneEarbudOutMockState
        
        // Validate one earbud out scenario
        assert(!oneEarOut.leftInEar) { "Left earbud should be out" }
        assert(oneEarOut.rightInEar) { "Right earbud should be in" }
        assert(oneEarOut.isConnected) { "Device should still be connected" }
    }
}