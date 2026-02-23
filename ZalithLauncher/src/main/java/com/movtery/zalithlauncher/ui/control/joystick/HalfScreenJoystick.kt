/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.control.joystick

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

class HalfScreenJoystickState {
    var isVisible by mutableStateOf(false)
    var center by mutableStateOf(Offset.Zero)
    var joystickOffset by mutableStateOf(Offset.Zero)
    var isLocked by mutableStateOf(false)
    var internalCanLock by mutableStateOf(false)
}

@Composable
fun rememberHalfScreenJoystickState() = remember { HalfScreenJoystickState() }

@Composable
fun HalfScreenJoystickInput(
    state: HalfScreenJoystickState,
    modifier: Modifier = Modifier,
    screenSize: IntSize,
    size: Dp,
    deadZoneRatio: Float,
    canLock: Boolean,
    isLeftHalf: Boolean,
    offsetX: Int = 0,
    isOccupiedPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onDirectionChanged: (JoystickDirection) -> Unit,
    onLock: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val joystickSizePx = with(density) { size.toPx() }
    
    val currentOnDirectionChanged by rememberUpdatedState(onDirectionChanged)
    val currentOnLock by rememberUpdatedState(onLock)

    var lastReportedDirection by remember { mutableStateOf(JoystickDirection.None) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(screenSize, joystickSizePx, deadZoneRatio, canLock, isLeftHalf, offsetX) {
                handleJoystickTouch(
                    screenSize = screenSize,
                    joystickSizePx = joystickSizePx,
                    deadZoneRatio = deadZoneRatio,
                    canLock = canLock,
                    isLeftHalf = isLeftHalf,
                    offsetX = offsetX,
                    isOccupiedPointer = isOccupiedPointer,
                    onOccupiedPointer = onOccupiedPointer,
                    onReleasePointer = onReleasePointer,
                    onUpdateState = { visible, center, offset, locked, canLockState, direction ->
                        state.isVisible = visible
                        state.center = center
                        state.joystickOffset = offset
                        
                        if (state.isLocked != locked) {
                            state.isLocked = locked
                            currentOnLock(locked)
                        }
                        
                        state.internalCanLock = canLockState
                        
                        if (direction != lastReportedDirection) {
                            lastReportedDirection = direction
                            currentOnDirectionChanged(direction)
                        }
                    }
                )
            }
    )
}

@Composable
fun HalfScreenJoystickVisual(
    state: HalfScreenJoystickState,
    modifier: Modifier = Modifier,
    style: ObservableJoystickStyle,
    size: Dp
) {
    val density = LocalDensity.current
    val joystickSizePx = with(density) { size.toPx() }

    if (state.isVisible) {
        StatelessStyleableJoystick(
            modifier = modifier.absoluteOffset {
                IntOffset(
                    x = (state.center.x - joystickSizePx / 2).roundToInt(),
                    y = (state.center.y - joystickSizePx / 2).roundToInt()
                )
            },
            style = style,
            size = size,
            joystickOffset = { state.joystickOffset },
            isLocked = state.isLocked,
            internalCanLock = state.internalCanLock
        )
    }
}

private suspend fun PointerInputScope.handleJoystickTouch(
    screenSize: IntSize,
    joystickSizePx: Float,
    deadZoneRatio: Float,
    canLock: Boolean,
    isLeftHalf: Boolean,
    offsetX: Int,
    isOccupiedPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onUpdateState: (Boolean, Offset, Offset, Boolean, Boolean, JoystickDirection) -> Unit
) {
    val centerPoint = Offset(joystickSizePx / 2, joystickSizePx / 2)
    val deadZoneRadius = joystickSizePx * deadZoneRatio / 2
    val lockThresholdPx = joystickSizePx * 0.3f
    val lockPositionOffset = Offset(0f, -joystickSizePx / 2)

    coroutineScope {
        awaitPointerEventScope {
            var activePointerId: PointerId? = null
            var center = Offset.Zero
            var currentLocked = false
            var canLockTriggered = false
            
            while (true) {
                val event = awaitPointerEvent()
                
                if (activePointerId == null) {
                    val downChange = event.changes.find { 
                        it.changedToDown() && 
                        it.type == PointerType.Touch &&
                        !it.isConsumed 
                    }
                    
                    if (downChange != null) {
                        val localPos = downChange.position
                        val actualX = localPos.x + offsetX
                        
                        if (actualX >= 0 && actualX < screenSize.width && !isOccupiedPointer(downChange.id)) {
                            activePointerId = downChange.id
                            
                            val halfSize = joystickSizePx / 2
                            center = Offset(
                                actualX.coerceIn(halfSize, screenSize.width - halfSize),
                                localPos.y.coerceIn(halfSize, screenSize.height - halfSize)
                            )
                            
                            currentLocked = false
                            canLockTriggered = false
                            onOccupiedPointer(activePointerId!!)
                            
                            onUpdateState(true, center, Offset.Zero, false, false, JoystickDirection.None)
                            downChange.consume()
                        }
                    }
                } else {
                    val change = event.changes.find { it.id == activePointerId }
                    
                    if (change != null) {
                        if (change.changedToUpIgnoreConsumed()) {
                            if (canLockTriggered) {
                                currentLocked = true
                                val direction = JoystickDirection.North
                                onUpdateState(true, center, lockPositionOffset, true, false, direction)
                            } else {
                                currentLocked = false
                                onUpdateState(false, center, Offset.Zero, false, false, JoystickDirection.None)
                            }
                            
                            onReleasePointer(activePointerId!!)
                            activePointerId = null
                            canLockTriggered = false
                            change.consume()
                        } else if (change.pressed && change.positionChanged()) {
                            val localPos = change.position
                            val actualX = localPos.x + offsetX
                            val actualPos = Offset(actualX, localPos.y)
                            val relativeOffset = actualPos - center
                            
                            if (currentLocked) currentLocked = false
                            
                            val clampedPos = updateJoystickPosition(
                                newPosition = relativeOffset + centerPoint,
                                minX = 0f,
                                maxX = joystickSizePx,
                                minY = 0f,
                                maxY = joystickSizePx
                            )
                            
                            val finalOffset = clampedPos - centerPoint
                            val direction = calculateDirection(clampedPos, centerPoint, deadZoneRadius)
                            
                            canLockTriggered = canLock && 
                                             direction == JoystickDirection.North && 
                                             relativeOffset.y < -lockThresholdPx - (joystickSizePx / 2)
                            
                            onUpdateState(true, center, finalOffset, currentLocked, canLockTriggered, direction)
                            change.consume()
                        }
                    }
                    
                    if (!event.changes.any { it.pressed }) {
                        if (activePointerId != null) {
                            onUpdateState(false, center, Offset.Zero, false, false, JoystickDirection.None)
                            onReleasePointer(activePointerId!!)
                            activePointerId = null
                        }
                    }
                }
            }
        }
    }
}
