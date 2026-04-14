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

package com.movtery.layer_controller.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.MIN_SIZE_DP
import com.movtery.layer_controller.data.TextAlignment
import com.movtery.layer_controller.event.EventHandler
import com.movtery.layer_controller.observable.*
import com.movtery.layer_controller.utils.buttonContentColorAsState
import com.movtery.layer_controller.utils.buttonFontSizeAsState
import com.movtery.layer_controller.utils.buttonSize
import com.movtery.layer_controller.utils.buttonStyle
import com.movtery.layer_controller.utils.editMode
import com.movtery.layer_controller.utils.snap.GuideLine
import com.movtery.layer_controller.utils.snap.SnapMode
import kotlin.math.max
import kotlin.math.roundToInt

private data class ButtonTextStyle(
    val text: ObservableTranslatableString,
    val textAlignment: TextAlignment,
    val textBold: Boolean,
    val textItalic: Boolean,
    val textUnderline: Boolean
)

/**
 * 基础文本控件
 * @param allStyles 当前控制布局所有的样式（用于加载控件的样式）
 * @param enableSnap 编辑模式下，是否开启吸附功能
 * @param snapMode 吸附模式
 * @param localSnapRange 局部吸附范围（仅在Local模式下有效）
 * @param getOtherWidgets 获取其他控件的信息，在编辑模式下，用于计算吸附位置
 * @param snapThresholdValue 吸附距离阈值
 * @param eventHandler 事件处理器
 * @param drawLine 绘制吸附参考线
 * @param onLineCancel 取消吸附参考线
 */
@Composable
internal fun TextButton(
    isEditMode: Boolean,
    data: ObservableWidget,
    allStyles: List<ObservableButtonStyle>,
    screenSize: IntSize,
    isDark: Boolean = isSystemInDarkTheme(),
    visible: Boolean = true,
    enableSnap: Boolean = false,
    snapMode: SnapMode = SnapMode.FullScreen,
    localSnapRange: Dp = 50.dp,
    getOtherWidgets: () -> List<ObservableWidget>,
    snapThresholdValue: Dp,
    eventHandler: EventHandler? = null,
    drawLine: (ObservableWidget, List<GuideLine>) -> Unit = { _, _ -> },
    onLineCancel: (ObservableWidget) -> Unit = {},
    isPressed: Boolean,
    showResizeCursor: Boolean = false,
    onTapInEditMode: () -> Unit = {},
    onDragFinished: () -> Unit = {}
) {
    if (visible) {
        val styleId = data.styleId
        val style = allStyles
            .takeIf { data.styleId != null }
            ?.find { it.uuid == styleId }
            ?: DefaultObservableButtonStyle

        val locale = LocalConfiguration.current.locales[0]
        val density = LocalDensity.current.density

        Box(
            modifier = Modifier
                .buttonSize(data, screenSize)
                .editMode(
                    isEditMode = isEditMode,
                    data = data,
                    screenSize = screenSize,
                    enableSnap = enableSnap,
                    snapMode = snapMode,
                    localSnapRange = localSnapRange,
                    getOtherWidgets = getOtherWidgets,
                    snapThresholdValue = snapThresholdValue,
                    drawLine = drawLine,
                    onLineCancel = onLineCancel,
                    onTapInEditMode = onTapInEditMode,
                    onDragFinished = onDragFinished
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .buttonStyle(
                        style = style,
                        isDark = isDark,
                        isPressed = isPressed
                    ),
                contentAlignment = Alignment.Center
            ) {
                val color by buttonContentColorAsState(
                    style = style,
                    isDark = isDark,
                    isPressed = isPressed
                )
                val fontSize by buttonFontSizeAsState(
                    style = style,
                    isDark = isDark,
                    isPressed = isPressed
                )
                val buttonTextStyle = when (data) {
                    is ObservableNormalData -> ButtonTextStyle(
                        text = data.text,
                        textAlignment = data.textAlignment,
                        textBold = data.textBold,
                        textItalic = data.textItalic,
                        textUnderline = data.textUnderline
                    )
                    is ObservableTextData -> ButtonTextStyle(
                        text = data.text,
                        textAlignment = data.textAlignment,
                        textBold = data.textBold,
                        textItalic = data.textItalic,
                        textUnderline = data.textUnderline
                    )
                    else -> error("Unknown widget type")
                }
                RtLText(
                    text = buttonTextStyle.text.translate(locale),
                    color = color,
                    fontSize = fontSize.sp,
                    textAlign = buttonTextStyle.textAlignment.textAlign,
                    fontWeight = if (buttonTextStyle.textBold) FontWeight.Bold else null,
                    fontStyle = if (buttonTextStyle.textItalic) FontStyle.Italic else null,
                    textDecoration = if (buttonTextStyle.textUnderline) TextDecoration.Underline else null,
                    style = LocalTextStyle.current.copy(
                        lineHeight = (fontSize * 1.1).sp
                    )
                )
            }

            DisposableEffect(Unit) {
                data.onCompositionStart(eventHandler)
                onDispose {
                    data.onCompositionDispose(eventHandler)
                }
            }

            if (isEditMode && showResizeCursor) {
                ResizeCursor(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    onDrag = { dragAmount ->
                        data.resizeByDrag(
                            dragAmount = dragAmount,
                            screenSize = screenSize,
                            density = density
                        )
                    }
                )
            }
        }
    } else {
        //虚假的控件，使用一个空的组件，只是让Layout有东西能测
        Spacer(
            modifier = Modifier.buttonSize(data, screenSize)
        )
    }
}

@Composable
private fun ResizeCursor(
    modifier: Modifier = Modifier,
    onDrag: (Offset) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, primaryColor, alpha = 0.9f)
        }
    }
}

private fun ObservableWidget.resizeByDrag(
    dragAmount: androidx.compose.ui.geometry.Offset,
    screenSize: IntSize,
    density: Float
) {
    val size = when (this) {
        is ObservableNormalData -> this.buttonSize
        is ObservableTextData -> this.buttonSize
        else -> return
    }

    val resized = when (size.type) {
        ButtonSize.Type.Dp -> size.copy(
            widthDp = max(MIN_SIZE_DP, size.widthDp + dragAmount.x / density),
            heightDp = max(MIN_SIZE_DP, size.heightDp + dragAmount.y / density)
        )

        ButtonSize.Type.Percentage -> {
            val widthReference = if (size.widthReference == ButtonSize.Reference.ScreenWidth) {
                screenSize.width.toFloat()
            } else {
                screenSize.height.toFloat()
            }
            val heightReference = if (size.heightReference == ButtonSize.Reference.ScreenWidth) {
                screenSize.width.toFloat()
            } else {
                screenSize.height.toFloat()
            }

            val newWidthPercentage = (size.widthPercentage + dragAmount.x / widthReference * 10000f).roundToInt().coerceIn(100, 10000)
            val newHeightPercentage = (size.heightPercentage + dragAmount.y / heightReference * 10000f).roundToInt().coerceIn(100, 10000)
            
            size.copy(
                widthPercentage = newWidthPercentage,
                heightPercentage = newHeightPercentage
            )
        }

        ButtonSize.Type.WrapContent -> size.copy(
            type = ButtonSize.Type.Dp,
            widthDp = max(MIN_SIZE_DP, (internalRenderSize.width / density + dragAmount.x / density)),
            heightDp = max(MIN_SIZE_DP, (internalRenderSize.height / density + dragAmount.y / density))
        )
    }

    when (this) {
        is ObservableNormalData -> this.buttonSize = resized
        is ObservableTextData -> this.buttonSize = resized
    }
}

/**
 * 仅渲染控件外观的组件
 */
@Composable
fun RendererStyleBox(
    style: ObservableButtonStyle,
    modifier: Modifier = Modifier,
    text: String = "",
    isDark: Boolean,
    isPressed: Boolean
) {
    Box(
        modifier = modifier.buttonStyle(
            style = style,
            isDark = isDark,
            isPressed = isPressed
        ),
        contentAlignment = Alignment.Center
    ) {
        val color by buttonContentColorAsState(style = style, isDark = isDark, isPressed = isPressed)
        val fontSize by buttonFontSizeAsState(style = style, isDark = isDark, isPressed = isPressed)
        RtLText(
            text = text,
            color = color,
            fontSize = fontSize.sp
        )
    }
}
