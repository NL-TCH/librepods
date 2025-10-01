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

package me.kavishdevar.librepods.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.composables.StyledSlider
import me.kavishdevar.librepods.composables.LoudSoundReductionSwitch
import me.kavishdevar.librepods.composables.NavigationButton
import me.kavishdevar.librepods.composables.SinglePodANCSwitch
import me.kavishdevar.librepods.composables.StyledSwitch
import me.kavishdevar.librepods.composables.VolumeControlSwitch
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.ATTHandles
import me.kavishdevar.librepods.utils.RadareOffsetFinder
import me.kavishdevar.librepods.utils.TransparencySettings
import me.kavishdevar.librepods.utils.parseTransparencySettingsResponse
import me.kavishdevar.librepods.utils.sendTransparencySettings
import java.io.IOException
import kotlin.io.encoding.ExperimentalEncodingApi

private var phoneMediaDebounceJob: Job? = null
private var toneVolumeDebounceJob: Job? = null
private const val TAG = "AccessibilitySettings"

@SuppressLint("DefaultLocale")
@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun AccessibilitySettingsScreen(navController: NavController) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val verticalScrollState = rememberScrollState()
    val hazeState = remember { HazeState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val aacpManager = remember { ServiceManager.getService()?.aacpManager }
    val isSdpOffsetAvailable =
        remember { mutableStateOf(RadareOffsetFinder.isSdpOffsetAvailable()) }

    val trackColor = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFF929491)
    val activeTrackColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF3C6DF5)
    val thumbColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)

    Scaffold(
        containerColor = if (isSystemInDarkTheme()) Color(
            0xFF000000
        ) else Color(
            0xFFF2F2F7
        ),
        topBar = {
            val darkMode = isSystemInDarkTheme()
            val mDensity = remember { mutableFloatStateOf(1f) }

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.accessibility),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (darkMode) Color.White else Color.Black,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        )
                    )
                },
                modifier = Modifier
                    .hazeEffect(
                        state = hazeState,
                        style = CupertinoMaterials.thick(),
                        block = fun HazeEffectScope.() {
                            alpha =
                                if (verticalScrollState.value > 60.dp.value * mDensity.floatValue) 1f else 0f
                        })
                    .drawBehind {
                        mDensity.floatValue = density
                        val strokeWidth = 0.7.dp.value * density
                        val y = size.height - strokeWidth / 2
                        if (verticalScrollState.value > 60.dp.value * density) {
                            drawLine(
                                if (darkMode) Color.DarkGray else Color.LightGray,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }
                    },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .hazeSource(hazeState)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

            val enabled = remember { mutableStateOf(false) }
            val amplificationSliderValue = remember { mutableFloatStateOf(0.5f) }
            val balanceSliderValue = remember { mutableFloatStateOf(0.5f) }
            val toneSliderValue = remember { mutableFloatStateOf(0.5f) }
            val ambientNoiseReductionSliderValue = remember { mutableFloatStateOf(0.0f) }
            val conversationBoostEnabled = remember { mutableStateOf(false) }
            val eq = remember { mutableStateOf(FloatArray(8)) }

            val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
            val phoneEQEnabled = remember { mutableStateOf(false) }
            val mediaEQEnabled = remember { mutableStateOf(false) }

            val initialLoadComplete = remember { mutableStateOf(false) }

            val initialReadSucceeded = remember { mutableStateOf(false) }
            val initialReadAttempts = remember { mutableIntStateOf(0) }

            val transparencySettings = remember {
                mutableStateOf(
                    TransparencySettings(
                        enabled = enabled.value,
                        leftEQ = eq.value,
                        rightEQ = eq.value,
                        leftAmplification = amplificationSliderValue.floatValue + (0.5f - balanceSliderValue.floatValue) * amplificationSliderValue.floatValue * 2,
                        rightAmplification = amplificationSliderValue.floatValue + (balanceSliderValue.floatValue - 0.5f) * amplificationSliderValue.floatValue * 2,
                        leftTone = toneSliderValue.floatValue,
                        rightTone = toneSliderValue.floatValue,
                        leftConversationBoost = conversationBoostEnabled.value,
                        rightConversationBoost = conversationBoostEnabled.value,
                        leftAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                        rightAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                        netAmplification = amplificationSliderValue.floatValue,
                        balance = balanceSliderValue.floatValue
                    )
                )
            }

            val transparencyListener = remember {
                object : (ByteArray) -> Unit {
                    override fun invoke(value: ByteArray) {
                        val parsed = parseTransparencySettingsResponse(value)
                        if (parsed != null) {
                            enabled.value = parsed.enabled
                            amplificationSliderValue.floatValue = parsed.netAmplification
                            balanceSliderValue.floatValue = parsed.balance
                            toneSliderValue.floatValue = parsed.leftTone
                            ambientNoiseReductionSliderValue.floatValue =
                                parsed.leftAmbientNoiseReduction
                            conversationBoostEnabled.value = parsed.leftConversationBoost
                            eq.value = parsed.leftEQ.copyOf()
                            Log.d(TAG, "Updated transparency settings from notification")
                        } else {
                            Log.w(TAG, "Failed to parse transparency settings from notification")
                        }
                    }
                }
            }

            val pressSpeedOptions = mapOf(
                0.toByte() to "Default",
                1.toByte() to "Slower",
                2.toByte() to "Slowest"
            )
            val selectedPressSpeedValue =
                aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL }?.value?.takeIf { it.isNotEmpty() }
                    ?.get(0)
            var selectedPressSpeed by remember {
                mutableStateOf(
                    pressSpeedOptions[selectedPressSpeedValue] ?: pressSpeedOptions[0]
                )
            }
            val selectedPressSpeedListener = object : AACPManager.ControlCommandListener {
                override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
                    if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL.value) {
                        val newValue = controlCommand.value.takeIf { it.isNotEmpty() }?.get(0)
                        selectedPressSpeed = pressSpeedOptions[newValue] ?: pressSpeedOptions[0]
                    }
                }
            }
            LaunchedEffect(Unit) {
                aacpManager?.registerControlCommandListener(
                    AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL,
                    selectedPressSpeedListener
                )
            }
            DisposableEffect(Unit) {
                onDispose {
                    aacpManager?.unregisterControlCommandListener(
                        AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL,
                        selectedPressSpeedListener
                    )
                }
            }

            val pressAndHoldDurationOptions = mapOf(
                0.toByte() to "Default",
                1.toByte() to "Slower",
                2.toByte() to "Slowest"
            )
            val selectedPressAndHoldDurationValue =
                aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL }?.value?.takeIf { it.isNotEmpty() }
                    ?.get(0)
            var selectedPressAndHoldDuration by remember {
                mutableStateOf(
                    pressAndHoldDurationOptions[selectedPressAndHoldDurationValue]
                        ?: pressAndHoldDurationOptions[0]
                )
            }
            val selectedPressAndHoldDurationListener = object : AACPManager.ControlCommandListener {
                override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
                    if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL.value) {
                        val newValue = controlCommand.value.takeIf { it.isNotEmpty() }?.get(0)
                        selectedPressAndHoldDuration =
                            pressAndHoldDurationOptions[newValue] ?: pressAndHoldDurationOptions[0]
                    }
                }
            }
            LaunchedEffect(Unit) {
                aacpManager?.registerControlCommandListener(
                    AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL,
                    selectedPressAndHoldDurationListener
                )
            }
            DisposableEffect(Unit) {
                onDispose {
                    aacpManager?.unregisterControlCommandListener(
                        AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL,
                        selectedPressAndHoldDurationListener
                    )
                }
            }

            val volumeSwipeSpeedOptions = mapOf(
                1.toByte() to "Default",
                2.toByte() to "Longer",
                3.toByte() to "Longest"
            )
            val selectedVolumeSwipeSpeedValue =
                aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL }?.value?.takeIf { it.isNotEmpty() }
                    ?.get(0)
            var selectedVolumeSwipeSpeed by remember {
                mutableStateOf(
                    volumeSwipeSpeedOptions[selectedVolumeSwipeSpeedValue]
                        ?: volumeSwipeSpeedOptions[1]
                )
            }
            val selectedVolumeSwipeSpeedListener = object : AACPManager.ControlCommandListener {
                override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
                    if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL.value) {
                        val newValue = controlCommand.value.takeIf { it.isNotEmpty() }?.get(0)
                        selectedVolumeSwipeSpeed =
                            volumeSwipeSpeedOptions[newValue] ?: volumeSwipeSpeedOptions[1]
                    }
                }
            }
            LaunchedEffect(Unit) {
                aacpManager?.registerControlCommandListener(
                    AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL,
                    selectedVolumeSwipeSpeedListener
                )
            }
            DisposableEffect(Unit) {
                onDispose {
                    aacpManager?.unregisterControlCommandListener(
                        AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL,
                        selectedVolumeSwipeSpeedListener
                    )
                }
            }

            // Debounced write for phone/media EQ using AACP manager when values/toggles change
            LaunchedEffect(phoneMediaEQ.value, phoneEQEnabled.value, mediaEQEnabled.value) {
                phoneMediaDebounceJob?.cancel()
                phoneMediaDebounceJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(150)
                    val manager = ServiceManager.getService()?.aacpManager
                    if (manager == null) {
                        Log.w(TAG, "Cannot write EQ: AACPManager not available")
                        return@launch
                    }
                    try {
                        val phoneByte = if (phoneEQEnabled.value) 0x01.toByte() else 0x02.toByte()
                        val mediaByte = if (mediaEQEnabled.value) 0x01.toByte() else 0x02.toByte()
                        Log.d(
                            TAG,
                            "Sending phone/media EQ (phoneEnabled=${phoneEQEnabled.value}, mediaEnabled=${mediaEQEnabled.value})"
                        )
                        manager.sendPhoneMediaEQ(phoneMediaEQ.value, phoneByte, mediaByte)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending phone/media EQ: ${e.message}")
                    }
                }
            }
            val toneVolumeValue = remember { mutableFloatStateOf(
                aacpManager?.controlCommandStatusList?.find {
                    it.identifier == AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME
                }?.value?.takeIf { it.isNotEmpty() }?.get(0)?.toFloat() ?: 75f
            ) }
            LaunchedEffect(toneVolumeValue.floatValue) {
                toneVolumeDebounceJob?.cancel()
                toneVolumeDebounceJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(150)
                    val manager = ServiceManager.getService()?.aacpManager
                    if (manager == null) {
                        Log.w(TAG, "Cannot write tone volume: AACPManager not available")
                        return@launch
                    }
                    try {
                        manager.sendControlCommand(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME.value,
                            value = byteArrayOf(toneVolumeValue.floatValue.toInt().toByte(), 0x50.toByte())
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending tone volume: ${e.message}")
                    }
                }
            }

            Text(
                text = stringResource(R.string.tone_volume).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )
            StyledSlider(
                mutableFloatState = toneVolumeValue,
                onValueChange = {
                    toneVolumeValue.floatValue = it
                },
                valueRange = 0f..125f,
                snapPoints = listOf(100f),
                startIcon = "\uDBC0\uDEA1",
                endIcon = "\uDBC0\uDEA9",
                independent = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SinglePodANCSwitch()
                VolumeControlSwitch()
                LoudSoundReductionSwitch()

                DropdownMenuComponent(
                    label = stringResource(R.string.press_speed),
                    options = listOf(
                        stringResource(R.string.default_option),
                        stringResource(R.string.slower),
                        stringResource(R.string.slowest)
                    ),
                    selectedOption = selectedPressSpeed.toString(),
                    onOptionSelected = { newValue ->
                        selectedPressSpeed = newValue
                        aacpManager?.sendControlCommand(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL.value,
                            value = pressSpeedOptions.filterValues { it == newValue }.keys.firstOrNull()
                                ?: 0.toByte()
                        )
                    },
                    textColor = textColor
                )
                DropdownMenuComponent(
                    label = stringResource(R.string.press_and_hold_duration),
                    options = listOf(
                        stringResource(R.string.default_option),
                        stringResource(R.string.slower),
                        stringResource(R.string.slowest)
                    ),
                    selectedOption = selectedPressAndHoldDuration.toString(),
                    onOptionSelected = { newValue ->
                        selectedPressAndHoldDuration = newValue
                        aacpManager?.sendControlCommand(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL.value,
                            value = pressAndHoldDurationOptions.filterValues { it == newValue }.keys.firstOrNull()
                                ?: 0.toByte()
                        )
                    },
                    textColor = textColor
                )
                DropdownMenuComponent(
                    label = stringResource(R.string.volume_swipe_speed),
                    options = listOf(
                        stringResource(R.string.default_option),
                        stringResource(R.string.longer),
                        stringResource(R.string.longest)
                    ),
                    selectedOption = selectedVolumeSwipeSpeed.toString(),
                    onOptionSelected = { newValue ->
                        selectedVolumeSwipeSpeed = newValue
                        aacpManager?.sendControlCommand(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL.value,
                            value = volumeSwipeSpeedOptions.filterValues { it == newValue }.keys.firstOrNull()
                                ?: 1.toByte()
                        )
                    },
                    textColor = textColor
                )
            }

            NavigationButton(
                to = "transparency_customization",
                name = stringResource(R.string.customize_transparency_mode),
                navController = navController
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.apply_eq_to).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(vertical = 0.dp)
            ) {
                val darkModeLocal = isSystemInDarkTheme()

                val phoneShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                var phoneBackgroundColor by remember {
                    mutableStateOf(
                        if (darkModeLocal) Color(
                            0xFF1C1C1E
                        ) else Color(0xFFFFFFFF)
                    )
                }
                val phoneAnimatedBackgroundColor by animateColorAsState(
                    targetValue = phoneBackgroundColor,
                    animationSpec = tween(durationMillis = 500)
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .background(phoneAnimatedBackgroundColor, phoneShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    phoneBackgroundColor =
                                        if (darkModeLocal) Color(0x40888888) else Color(0x40D9D9D9)
                                    tryAwaitRelease()
                                    phoneBackgroundColor =
                                        if (darkModeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                                    phoneEQEnabled.value = !phoneEQEnabled.value
                                }
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.phone),
                        fontSize = 16.sp,
                        color = textColor,
                        fontFamily = FontFamily(Font(R.font.sf_pro)),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = phoneEQEnabled.value,
                        onCheckedChange = { phoneEQEnabled.value = it },
                        colors = CheckboxDefaults.colors().copy(
                            checkedCheckmarkColor = Color(0xFF007AFF),
                            uncheckedCheckmarkColor = Color.Transparent,
                            checkedBoxColor = Color.Transparent,
                            uncheckedBoxColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .height(24.dp)
                            .scale(1.5f)
                    )
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = Color(0x40888888)
                )

                val mediaShape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                var mediaBackgroundColor by remember {
                    mutableStateOf(
                        if (darkModeLocal) Color(
                            0xFF1C1C1E
                        ) else Color(0xFFFFFFFF)
                    )
                }
                val mediaAnimatedBackgroundColor by animateColorAsState(
                    targetValue = mediaBackgroundColor,
                    animationSpec = tween(durationMillis = 500)
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .background(mediaAnimatedBackgroundColor, mediaShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    mediaBackgroundColor =
                                        if (darkModeLocal) Color(0x40888888) else Color(0x40D9D9D9)
                                    tryAwaitRelease()
                                    mediaBackgroundColor =
                                        if (darkModeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                                    mediaEQEnabled.value = !mediaEQEnabled.value
                                }
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.media),
                        fontSize = 16.sp,
                        color = textColor,
                        fontFamily = FontFamily(Font(R.font.sf_pro)),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = mediaEQEnabled.value,
                        onCheckedChange = { mediaEQEnabled.value = it },
                        colors = CheckboxDefaults.colors().copy(
                            checkedCheckmarkColor = Color(0xFF007AFF),
                            uncheckedCheckmarkColor = Color.Transparent,
                            checkedBoxColor = Color.Transparent,
                            uncheckedBoxColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .height(24.dp)
                            .scale(1.5f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until 8) {
                    val eqPhoneValue =
                        remember(phoneMediaEQ.value[i]) { mutableFloatStateOf(phoneMediaEQ.value[i]) }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", eqPhoneValue.floatValue),
                            fontSize = 12.sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Slider(
                            value = eqPhoneValue.floatValue,
                            onValueChange = { newVal ->
                                eqPhoneValue.floatValue = newVal
                                val newEQ = phoneMediaEQ.value.copyOf()
                                newEQ[i] = eqPhoneValue.floatValue
                                phoneMediaEQ.value = newEQ
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
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
                                Box(
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
                                            .fillMaxWidth(eqPhoneValue.floatValue / 100f)
                                            .height(4.dp)
                                            .background(activeTrackColor, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        )

                        Text(
                            text = stringResource(R.string.band_label, i + 1),
                            fontSize = 12.sp,
                            color = textColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AccessibilityToggle(
    text: String,
    mutableState: MutableState<Boolean>,
    independent: Boolean = false,
    description: String? = null,
    title: String? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    var backgroundColor by remember {
        mutableStateOf(
            if (isDarkTheme) Color(0xFF1C1C1E) else Color(
                0xFFFFFFFF
            )
        )
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 500)
    )
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cornerShape = if (independent) RoundedCornerShape(14.dp) else RoundedCornerShape(0.dp)

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 2.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .background(animatedBackgroundColor, cornerShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            backgroundColor =
                                if (isDarkTheme) Color(0x40888888) else Color(0x40D9D9D9)
                            tryAwaitRelease()
                            backgroundColor =
                                if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                        },
                        onTap = {
                            mutableState.value = !mutableState.value
                        }
                    )
                },
        )
        {
            val rowHeight = if (independent) 55.dp else 50.dp
            val rowPadding = if (independent) 12.dp else 4.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = rowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = textColor
                )
                StyledSwitch(
                    checked = mutableState.value,
                    onCheckedChange = {
                        mutableState.value = it
                    },
                )
            }
        }
        if (description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun DropdownMenuComponent(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            Text(
                text = selectedOption,
                modifier = Modifier.padding(16.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    text = { Text(text = option) }
                )
            }
        }
    }
}