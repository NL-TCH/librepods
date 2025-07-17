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

import android.bluetooth.BluetoothDevice
import io.mockk.mockk

/**
 * Mock data providers for testing LibrePods functionality
 */
object MockData {
    
    /**
     * Mock AirPods device data
     */
    fun mockAirPodsDevice(): BluetoothDevice {
        val device = mockk<BluetoothDevice>(relaxed = true)
        io.mockk.every { device.name } returns "Test AirPods Pro"
        io.mockk.every { device.address } returns "AA:BB:CC:DD:EE:FF"
        return device
    }
    
    /**
     * Mock battery levels for testing
     */
    data class MockBatteryLevels(
        val leftBud: Int = 85,
        val rightBud: Int = 90,
        val case: Int = 75
    )
    
    /**
     * Mock noise control modes
     */
    enum class MockNoiseControlMode {
        OFF, NOISE_CANCELLATION, TRANSPARENCY
    }
    
    /**
     * Mock AirPods state for comprehensive testing
     */
    data class MockAirPodsState(
        val isConnected: Boolean = true,
        val batteryLevels: MockBatteryLevels = MockBatteryLevels(),
        val noiseControlMode: MockNoiseControlMode = MockNoiseControlMode.NOISE_CANCELLATION,
        val leftInEar: Boolean = true,
        val rightInEar: Boolean = true,
        val conversationalAwareness: Boolean = false,
        val headTrackingEnabled: Boolean = true,
        val deviceName: String = "Test AirPods Pro"
    )
    
    /**
     * Default mock state for testing
     */
    val defaultMockState = MockAirPodsState()
    
    /**
     * Mock state for disconnected AirPods
     */
    val disconnectedMockState = MockAirPodsState(
        isConnected = false,
        batteryLevels = MockBatteryLevels(0, 0, 0)
    )
    
    /**
     * Mock state for low battery scenario
     */
    val lowBatteryMockState = MockAirPodsState(
        batteryLevels = MockBatteryLevels(15, 20, 5)
    )
    
    /**
     * Mock state for one earbud out
     */
    val oneEarbudOutMockState = MockAirPodsState(
        leftInEar = false,
        rightInEar = true
    )
}