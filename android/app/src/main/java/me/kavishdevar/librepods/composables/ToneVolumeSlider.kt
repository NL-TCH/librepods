/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneVolumeSlider() {
    val service = ServiceManager.getService()!!
    val sliderValueFromAACP = service.aacpManager.controlCommandStatusList.find {
        it.identifier == AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME
    }?.value?.takeIf { it.isNotEmpty() }?.get(0)
    val sliderValue = remember { mutableFloatStateOf(
        sliderValueFromAACP?.toFloat() ?: -1f
    ) }
    val listener = object : AACPManager.ControlCommandListener {
        override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
            if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME.value) {
                val newValue = controlCommand.value.takeIf { it.isNotEmpty() }?.get(0)?.toFloat()
                if (newValue != null) {
                    sliderValue.floatValue = newValue
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        service.aacpManager.registerControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME, listener)
    }
    DisposableEffect(Unit) {
        onDispose {
            service.aacpManager.unregisterControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME, listener)
        }
    }
    Log.d("ToneVolumeSlider", "Slider value: ${sliderValue.floatValue}")

    val isDarkTheme = isSystemInDarkTheme()

    val trackColor = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFF929491)
    val activeTrackColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF3C6DF5)
    val thumbColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
    val labelTextColor = if (isDarkTheme) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\uDBC0\uDEA1",
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.sf_pro)),
                fontWeight = FontWeight.Light,
                color = labelTextColor
            ),
            modifier = Modifier.padding(start = 4.dp)
        )
        Slider(
            value = sliderValue.floatValue,
            onValueChange = {
                sliderValue.floatValue = snapIfClose(it, listOf(100f))
            },
            valueRange = 0f..125f,
            onValueChangeFinished = {
                sliderValue.floatValue = snapIfClose(sliderValue.floatValue.roundToInt().toFloat(), listOf(100f))
                service.aacpManager.sendControlCommand(
                    identifier = AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME.value,
                    value = byteArrayOf(sliderValue.floatValue.toInt().toByte(),
                       0x50.toByte()
                    )
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = trackColor
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(4.dp, CircleShape)
                        .background(thumbColor, CircleShape)
                )
            },
            track = {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    contentAlignment = Alignment.CenterStart
                )
                {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(trackColor, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderValue.floatValue / 125)
                            .height(4.dp)
                            .background(activeTrackColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        )
        Text(
            text = "\uDBC0\uDEA9",
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.sf_pro)),
                fontWeight = FontWeight.Light,
                color = labelTextColor
            ),
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Preview
@Composable
fun ToneVolumeSliderPreview() {
    ToneVolumeSlider()
}

private fun snapIfClose(value: Float, points: List<Float>, threshold: Float = 0.05f): Float {
    val nearest = points.minByOrNull { kotlin.math.abs(it - value) } ?: value
    return if (kotlin.math.abs(nearest - value) <= threshold) nearest else value
}