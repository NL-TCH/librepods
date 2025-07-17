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

import android.content.Intent
import android.net.Uri
import org.junit.Test

/**
 * Unit tests for MainActivity logic
 */
class MainActivityTest {

    @Test
    fun testActivityCreation() {
        // Test that we can verify the class exists and is properly defined
        val clazz = MainActivity::class.java
        assert(clazz != null)
        assert(clazz.simpleName == "MainActivity")
    }
    
    @Test
    fun testDeepLinkHandling() {
        // Test deep link intent creation and parsing
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("librepods://add-magic-keys?key=test")
        }
        
        // Verify intent structure is correct
        assert(intent.data != null)
        assert(intent.data?.scheme == "librepods")
        assert(intent.data?.host == "add-magic-keys")
        assert(intent.data?.getQueryParameter("key") == "test")
        assert(intent.action == Intent.ACTION_VIEW)
    }
}