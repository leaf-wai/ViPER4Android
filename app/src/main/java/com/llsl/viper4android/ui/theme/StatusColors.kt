package com.llsl.viper4android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全局语义色 Token，集中管理运行状态、健康度、告警、日志等级与选中容器色，
 * 避免各屏幕散落硬编码 Color(0xFF...)。
 */
object StatusColors {
    val healthy: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF57AB5A) else Color(0xFF2E7D32)

    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFFA726) else Color(0xFFEF6C00)

    val danger: Color
        @Composable
        @ReadOnlyComposable
        get() = MiuixTheme.colorScheme.error

    val neutral: Color
        @Composable
        @ReadOnlyComposable
        get() = MiuixTheme.colorScheme.onSurfaceVariantSummary

    val actionButton: Color
        @Composable
        @ReadOnlyComposable
        get() = MiuixTheme.colorScheme.secondaryContainer

    val selectedNodeContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1F3652) else Color(0xFFE3F0FF)

    @Composable
    @ReadOnlyComposable
    fun runState(active: Boolean): Color = if (active) healthy else neutral

    @Composable
    @ReadOnlyComposable
    fun delay(ms: Int): Color = when {
        ms <= 0 -> neutral
        ms < 200 -> healthy
        ms < 500 -> warning
        else -> danger
    }

    val logSourceApp: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF66BB6A) else Color(0xFF2E7D32)

    val logSourceDriver: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFAB47BC) else Color(0xFF7B1FA2)

    val logInfo: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF42A5F5) else Color(0xFF1565C0)

    val logWarn: Color
        @Composable
        @ReadOnlyComposable
        get() = warning

    val logDebug: Color
        @Composable
        @ReadOnlyComposable
        get() = neutral

    val logError: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFEF5350) else Color(0xFFC62828)

    val alertNote: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF539BF5) else Color(0xFF1976D2)

    val alertTip: Color
        @Composable
        @ReadOnlyComposable
        get() = healthy

    val alertImportant: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF986EE2) else Color(0xFF6A1B9A)

    val alertWarning: Color
        @Composable
        @ReadOnlyComposable
        get() = warning

    val alertCaution: Color
        @Composable
        @ReadOnlyComposable
        get() = logError
}
