package com.llsl.viper4android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier = this.miuixLayerBackdrop(backdrop)

@Composable
fun rememberBlurBackdrop(
    backgroundColor: Color = MiuixTheme.colorScheme.surface,
): LayerBackdrop? {
    return if (isRuntimeShaderSupported()) {
        // 先绘制不透明底色再录制内容，避免透明区域导致 miuix 模糊着色器将悬浮文字（如 SmallTitle）反预乘放大为整片灰影
        rememberLayerBackdrop {
            drawRect(backgroundColor)
            drawContent()
        }
    } else {
        null
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    Box(
        modifier = modifier.then(
            if (blurActive && backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 60f,
                    colors = BlurDefaults.blurColors(
                        blendColors = listOf(
                            BlendColorEntry(
                                color = surfaceColor.copy(alpha = 0.78f),
                                mode = BlurBlendMode.SrcOver,
                            ),
                        ),
                    ),
                )
            } else {
                Modifier
            },
        ),
    ) {
        content()
    }
}
