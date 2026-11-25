package com.llsl.viper4android.ui.components.effect

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode

/** 关于页背景着色的设备形态分类（手机 / 宽屏平板）。 */
internal enum class BgDeviceType {
    PHONE,
    PAD,
}

/**
 * 参考 miuix 官方示例的 HyperOS 背景光晕与标题毛玻璃配色预设。
 */
internal object BgEffectConfig {

    internal class Config(
        val points: FloatArray,
        val colors1: FloatArray,
        val colors2: FloatArray,
        val colors3: FloatArray,
        val colorInterpPeriod: Float,
        val lightOffset: Float,
        val saturateOffset: Float,
        val pointOffset: Float,
    )

    private val OS3_PHONE_LIGHT = Config(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            1.0f, 0.9f, 0.94f, 1.0f,
            1.0f, 0.84f, 0.89f, 1.0f,
            0.97f, 0.73f, 0.82f, 1.0f,
            0.64f, 0.65f, 0.98f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.58f, 0.74f, 1.0f, 1.0f,
            1.0f, 0.9f, 0.93f, 1.0f,
            0.74f, 0.76f, 1.0f, 1.0f,
            0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.98f, 0.86f, 0.9f, 1.0f,
            0.6f, 0.73f, 0.98f, 1.0f,
            0.92f, 0.93f, 1.0f, 1.0f,
            0.56f, 0.69f, 1.0f, 1.0f,
        ),
        colorInterpPeriod = 5.0f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )

    private val OS3_PHONE_DARK = Config(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            0.2f, 0.06f, 0.88f, 0.4f,
            0.3f, 0.14f, 0.55f, 0.5f,
            0.0f, 0.64f, 0.96f, 0.5f,
            0.11f, 0.16f, 0.83f, 0.4f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f,
            0.62f, 0.21f, 0.67f, 0.5f,
            0.06f, 0.25f, 0.84f, 0.5f,
            0.0f, 0.2f, 0.78f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.4f,
            0.27f, 0.18f, 0.6f, 0.5f,
            0.66f, 0.26f, 0.62f, 0.5f,
            0.12f, 0.16f, 0.7f, 0.6f,
        ),
        colorInterpPeriod = 8.0f,
        lightOffset = 0.0f,
        saturateOffset = 0.17f,
        pointOffset = 0.4f,
    )

    private val OS3_PAD_LIGHT = Config(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            0.99f, 0.77f, 0.86f, 1.0f,
            0.74f, 0.76f, 1.0f, 1.0f,
            0.72f, 0.74f, 1.0f, 1.0f,
            0.98f, 0.76f, 0.8f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.66f, 0.75f, 1.0f, 1.0f,
            1.0f, 0.86f, 0.91f, 1.0f,
            0.74f, 0.76f, 1.0f, 1.0f,
            0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.97f, 0.79f, 0.85f, 1.0f,
            0.65f, 0.68f, 0.98f, 1.0f,
            0.66f, 0.77f, 1.0f, 1.0f,
            0.72f, 0.73f, 0.98f, 1.0f,
        ),
        colorInterpPeriod = 7.0f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )

    private val OS3_PAD_DARK = Config(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            0.66f, 0.26f, 0.62f, 0.4f,
            0.06f, 0.25f, 0.84f, 0.5f,
            0.0f, 0.64f, 0.96f, 0.5f,
            0.14f, 0.18f, 0.55f, 0.5f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f,
            0.11f, 0.16f, 0.83f, 0.5f,
            0.06f, 0.25f, 0.84f, 0.5f,
            0.66f, 0.26f, 0.62f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.5f,
            0.11f, 0.16f, 0.83f, 0.5f,
            0.66f, 0.26f, 0.62f, 0.5f,
            0.27f, 0.18f, 0.6f, 0.6f,
        ),
        colorInterpPeriod = 7.0f,
        lightOffset = 0.0f,
        saturateOffset = 0.0f,
        pointOffset = 0.2f,
    )

    private val LOGO_BLEND_DARK = listOf(
        BlendColorEntry(Color(0xE6A1A1A1), BlurBlendMode.ColorDodge),
        BlendColorEntry(Color(0x4DE6E6E6), BlurBlendMode.LinearLight),
        BlendColorEntry(Color(0xFF1AF500), BlurBlendMode.Lab),
    )

    private val LOGO_BLEND_LIGHT = listOf(
        BlendColorEntry(Color(0xCC4A4A4A), BlurBlendMode.ColorBurn),
        BlendColorEntry(Color(0xFF4F4F4F), BlurBlendMode.LinearLight),
        BlendColorEntry(Color(0xFF1AF200), BlurBlendMode.Lab),
    )

    fun get(deviceType: BgDeviceType, isDark: Boolean): Config = when (deviceType) {
        BgDeviceType.PHONE -> if (isDark) OS3_PHONE_DARK else OS3_PHONE_LIGHT
        BgDeviceType.PAD -> if (isDark) OS3_PAD_DARK else OS3_PAD_LIGHT
    }

    /** 标题文字前景模糊（DstIn）混合色，统一收口于特效配置避免屏幕层散落硬编码色值。 */
    fun logoBlendColors(isDark: Boolean): List<BlendColorEntry> =
        if (isDark) LOGO_BLEND_DARK else LOGO_BLEND_LIGHT
}
