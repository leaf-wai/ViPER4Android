package com.llsl.viper4android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(text) { AnnotatedString.fromHtml(text) }

    Text(
        text = annotated,
        modifier = modifier,
        style = MiuixTheme.textStyles.main,
    )
}
