package com.llsl.viper4android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DialogCardCornerRadius = 16.dp
private val DialogButtonSpacing = UiDimens.Medium
private val DialogDividerPadding = UiDimens.Standard
private val DialogEmptyVerticalPadding = UiDimens.IconLarge
private val DialogIconActionPadding = UiDimens.Medium
private val DialogIconActionSpacing = UiDimens.XSmall
private val DialogIndicatorSize = UiDimens.Medium
private val DialogLeadingIconSize = UiDimens.IconLarge
private val DialogListMaxHeight = UiDimens.DialogListMaxHeight
private val DialogRowHorizontalPadding = UiDimens.Standard
private val DialogListItemVerticalPadding = UiDimens.Medium
private val DialogRowVerticalPadding = UiDimens.Large
private val DialogRowValueSpacing = UiDimens.Standard
private val DialogRowIconSpacing = UiDimens.Large
private val DialogSmallIconSize = UiDimens.IconMedium

@Composable
fun DialogSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    SmallTitle(
        text = label,
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = DialogCardCornerRadius,
        insideMargin = PaddingValues(0.dp),
        colors = CardColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        Column { content() }
    }
}

@Composable
fun DialogListCard(
    modifier: Modifier = Modifier,
    maxHeight: Dp = DialogListMaxHeight,
    content: LazyListScope.() -> Unit,
) {
    DialogCard(modifier = modifier) {
        DialogList(maxHeight = maxHeight, content = content)
    }
}

@Composable
fun DialogList(
    modifier: Modifier = Modifier,
    maxHeight: Dp = DialogListMaxHeight,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = maxHeight),
        content = content,
    )
}

@Composable
fun DialogEmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DialogEmptyVerticalPadding),
    )
}

@Composable
fun DialogButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DialogButtonSpacing),
        content = content,
    )
}

@Composable
fun DialogIconActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DialogListItemVerticalPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        content = content,
    )
}

@Composable
fun DialogListItemRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = DialogRowHorizontalPadding, vertical = DialogListItemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DialogRowIconSpacing),
        content = content,
    )
}

@Composable
fun RowDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = DialogDividerPadding, end = DialogDividerPadding),
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    SwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = label,
        modifier = modifier,
        summary = subtitle,
    )
}

@Composable
fun NavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    statusColor: Color? = null,
    holdDownState: Boolean = false,
) {
    ArrowPreference(
        title = label,
        modifier = modifier,
        summary = subtitle,
        onClick = onClick,
        holdDownState = holdDownState,
        startAction = if (leadingIcon != null || statusColor != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.size(DialogLeadingIconSize),
                        )
                    }
                    if (statusColor != null) {
                        if (leadingIcon != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Canvas(modifier = Modifier.size(DialogIndicatorSize)) {
                            drawCircle(statusColor)
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun ActionRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = MiuixIcons.Download,
    holdDownState: Boolean = false,
) {
    ArrowPreference(
        title = label,
        modifier = modifier,
        summary = subtitle,
        onClick = onClick,
        holdDownState = holdDownState,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(DialogSmallIconSize),
            )
        },
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = DialogRowHorizontalPadding, vertical = DialogRowVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(.6f),
        )
        Spacer(modifier = Modifier.width(DialogRowValueSpacing))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(.4f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun IndicatorInfoRow(
    label: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DialogRowHorizontalPadding, vertical = DialogRowVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Canvas(modifier = Modifier.size(DialogIndicatorSize)) {
            drawCircle(indicatorColor)
        }
    }
}

@Composable
fun IconActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
) {
    val effectiveTint = if (enabled) tint else MiuixTheme.colorScheme.disabledOnSecondaryVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(DialogIconActionPadding),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(DialogSmallIconSize),
            tint = effectiveTint,
        )
        Spacer(modifier = Modifier.height(DialogIconActionSpacing))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = effectiveTint,
        )
    }
}
