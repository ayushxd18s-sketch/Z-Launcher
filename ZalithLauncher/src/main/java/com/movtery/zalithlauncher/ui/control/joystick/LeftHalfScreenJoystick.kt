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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
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

/**
 * 左半屏动态摇杆组件
 * 使用 PointerEventPass.Final 实现不阻塞底层按钮的探测
 */
@Composable
fun LeftHalfScreenJoystick(
    modifier: Modifier = Modifier,
    screenSize: IntSize,
    style: ObservableJoystickStyle,
    size: Dp,
    deadZoneRatio: Float,
    canLock: Boolean,
    isOccupiedPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onDirectionChanged: (JoystickDirection) -> Unit,
    onLock: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val joystickSizePx = with(density) { size.toPx() }
    
    // 内部状态
    var joystickVisible by remember { mutableStateOf(false) }
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) } // 摇杆相对于中心的偏移
    var isLocked by remember { mutableStateOf(false) }
    var internalCanLock by remember { mutableStateOf(false) }
    
    // 状态回调支持
    val currentOnDirectionChanged by rememberUpdatedState(onDirectionChanged)
    val currentOnLock by rememberUpdatedState(onLock)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(screenSize, joystickSizePx, deadZoneRatio, canLock) {
                handleJoystickTouch(
                    screenSize = screenSize,
                    joystickSizePx = joystickSizePx,
                    deadZoneRatio = deadZoneRatio,
                    canLock = canLock,
                    isOccupiedPointer = isOccupiedPointer,
                    onOccupiedPointer = onOccupiedPointer,
                    onReleasePointer = onReleasePointer,
                    onUpdateState = { visible, center, offset, locked, canLockState, direction ->
                        joystickVisible = visible
                        joystickCenter = center
                        joystickOffset = offset
                        
                        if (isLocked != locked) {
                            isLocked = locked
                            currentOnLock(locked)
                        }
                        
                        internalCanLock = canLockState
                        currentOnDirectionChanged(direction)
                    }
                )
            }
    ) {
        if (joystickVisible) {
            val visualOffset = remember(joystickCenter, joystickSizePx) {
                IntOffset(
                    (joystickCenter.x - joystickSizePx / 2).roundToInt(),
                    (joystickCenter.y - joystickSizePx / 2).roundToInt()
                )
            }
            
            StatelessStyleableJoystick(
                modifier = Modifier.offset { visualOffset },
                style = style,
                size = size,
                joystickOffset = joystickOffset,
                isLocked = isLocked,
                internalCanLock = internalCanLock
            )
        }
    }
}

private suspend fun PointerInputScope.handleJoystickTouch(
    screenSize: IntSize,
    joystickSizePx: Float,
    deadZoneRatio: Float,
    canLock: Boolean,
    isOccupiedPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onUpdateState: (Boolean, Offset, Offset, Boolean, Boolean, JoystickDirection) -> Unit
) {
    val centerPoint = Offset(joystickSizePx / 2, joystickSizePx / 2)
    val deadZoneRadius = joystickSizePx * deadZoneRatio / 2
    val lockThresholdPx = joystickSizePx * 0.3f // 锁定判定阈值
    val lockPositionOffset = Offset(0f, -joystickSizePx / 2) // 锁定的绝对位置偏移 (相对于中心)

    coroutineScope {
        awaitPointerEventScope {
            var activePointerId: PointerId? = null
            var center = Offset.Zero
            var currentLocked = false
            var canLockTriggered = false
            
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                
                if (activePointerId == null) {
                    val downChange = event.changes.find { 
                        it.changedToDown() && 
                        it.type == PointerType.Touch &&
                        !it.isConsumed 
                    }
                    
                    if (downChange != null) {
                        val pos = downChange.position
                        // 即使容器已经限制了 50%，这里再做一次逻辑检查作为双重保险
                        if (pos.x >= 0 && pos.x < screenSize.width && !isOccupiedPointer(downChange.id)) {
                            activePointerId = downChange.id
                            
                            val halfSize = joystickSizePx / 2
                            center = Offset(
                                pos.x.coerceIn(halfSize, screenSize.width - halfSize),
                                pos.y.coerceIn(halfSize, screenSize.height - halfSize)
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
                            // 抬起处理：如果满足锁定条件，则进入锁定状态
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
                            val currentPos = change.position
                            val relativeOffset = currentPos - center
                            
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
                    
                    // 特殊情况：如果所有手指都抬起了
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
