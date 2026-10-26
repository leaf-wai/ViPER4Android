package com.llsl.viper4android.ui.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 轻量操作结果反馈提示
 */
fun Context.showToast(message: String, long: Boolean = false) {
    Toast.makeText(
        this,
        message,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
    ).show()
}

class PlatformToast(
    private val context: Context,
) {
    fun showToast(message: String, long: Boolean = false) {
        context.showToast(message, long)
    }
}

@Composable
fun rememberPlatformToast(): PlatformToast {
    val context = LocalContext.current
    return remember(context) { PlatformToast(context) }
}

