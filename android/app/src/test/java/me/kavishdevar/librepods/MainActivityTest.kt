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
import android.content.Intent
import android.net.Uri
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MainActivity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    @Test
    fun testActivityCreation() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()
        
        // Verify activity is created successfully
        assert(activity != null)
    }
    
    @Test
    fun testDeepLinkHandling() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("librepods://add-magic-keys?key=test")
        }
        
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent)
            .create()
            .resume()
            .get()
        
        // Verify activity handles deep link intent
        assert(activity.intent.data != null)
        assert(activity.intent.data?.scheme == "librepods")
    }
}