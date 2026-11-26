package com.llsl.viper4android.ui.screens.settings

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.effect.BgEffectBackground
import com.llsl.viper4android.ui.components.effect.BgEffectConfig
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.screens.main.UpdateState
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import com.llsl.viper4android.ui.util.rememberPlatformToast
import com.llsl.viper4android.utils.ReleaseInfo
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

private const val REPO_URL = "https://github.com/leaf-wai/ViPER4Android"
private const val DRIVER_REPO_URL = "https://github.com/likelikeslike/ViPERFX_RE"
private const val ISSUES_URL = "https://github.com/leaf-wai/ViPER4Android/issues"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Immutable
private data class ThirdPartyLibrary(
    val name: String,
    val license: String,
    val url: String,
)

private val ThirdPartyLibraries = listOf(
    ThirdPartyLibrary("Miuix", "Apache-2.0", "https://github.com/compose-miuix-ui/miuix"),
    ThirdPartyLibrary("Jetpack Compose & AndroidX", "Apache-2.0", "https://developer.android.com/jetpack"),
    ThirdPartyLibrary("Dagger Hilt", "Apache-2.0", "https://dagger.dev/hilt"),
    ThirdPartyLibrary("OkHttp", "Apache-2.0", "https://square.github.io/okhttp"),
    ThirdPartyLibrary("Kotlin Coroutines", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
)

/**
 * 关于二级页面，参考 miuix 官方 AboutPage 示例实现 HyperOS 动态光晕背景、Hero 视差折叠与
 * 前景毛玻璃标题，并遵循 AGENTS.md 的顶栏、缺口与卡片规范。
 */
@Composable
fun AboutScreen(
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadInstall: (ReleaseInfo, () -> Unit) -> Unit,
    onDismissUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val platform = rememberPlatformToast()
    val openUrlFailedToast = stringResource(R.string.about_open_url_failed)

    val versionInfo = remember(context) {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            (info.versionName ?: "") to info.longVersionCode
        } catch (_: Exception) {
            "" to 0L
        }
    }
    val appVersionName = versionInfo.first
    val appVersionCode = versionInfo.second

    val openUrl: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }.onFailure {
            platform.showToast(openUrlFailedToast, long = false)
        }
    }

    AboutScreenContent(
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        updateState = updateState,
        onCheckUpdate = onCheckUpdate,
        onDownloadInstall = onDownloadInstall,
        onViewReleaseOnGithub = { htmlUrl ->
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, htmlUrl.toUri())
                context.startActivity(intent)
            }.onFailure {
                platform.showToast(openUrlFailedToast, long = false)
            }
            onDismissUpdate()
        },
        onDismissUpdate = onDismissUpdate,
        onOpenUrl = openUrl,
        onShowToast = { msg -> platform.showToast(msg, long = false) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AboutScreenContent(
    appVersionName: String,
    appVersionCode: Long,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadInstall: (ReleaseInfo, () -> Unit) -> Unit,
    onViewReleaseOnGithub: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLicensesDialog by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    val scrollProgress by remember(lazyListState) {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val backdrop = rememberBlurBackdrop()
    // 仅在 Hero 折叠临界点翻转布尔值，避免滚动期间每帧驱动顶栏重组
    val collapsed by remember { derivedStateOf { scrollProgress >= 1f } }
    val blurActive by remember(backdrop) { derivedStateOf { backdrop != null && collapsed } }
    val showTitle by remember { derivedStateOf { scrollProgress >= 0.45f } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val barColor = if (blurActive) {
                Color.Transparent
            } else if (collapsed) {
                MiuixTheme.colorScheme.surface
            } else {
                Color.Transparent
            }
            val titleAlpha by animateFloatAsState(
                targetValue = if (showTitle) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "about_top_bar_title_alpha",
            )
            BlurredBar(
                backdrop = backdrop,
                blurActive = blurActive,
                modifier = Modifier.background(barColor),
            ) {
                // AboutScreen 固定使用 SmallTopAppBar，且内容侧已自行处理水平缺口，仅对顶栏关闭默认水平 inset 并补 horizontalCutoutPadding
                SmallTopAppBar(
                    title = stringResource(R.string.about_title),
                    modifier = Modifier.horizontalCutoutPadding(),
                    color = Color.Transparent,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                    scrollBehavior = scrollBehavior,
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            AboutContent(
                topPadding = innerPadding.calculateTopPadding(),
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                updateState = updateState,
                showLicensesDialog = showLicensesDialog,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                scrollProgressProvider = { scrollProgress },
                onCheckUpdate = onCheckUpdate,
                onOpenUrl = onOpenUrl,
                onOpenLicenses = { showLicensesDialog = true },
            )
        }
    }

    if (showLicensesDialog) {
        ThirdPartyLicensesDialog(
            onOpenUrl = onOpenUrl,
            onDismiss = { showLicensesDialog = false },
        )
    }

    val releaseInfo = updateState.release
    if (releaseInfo != null) {
        val noAssetStr = stringResource(R.string.update_no_apk_asset)
        UpdateDialog(
            release = releaseInfo,
            currentVersion = appVersionName,
            upToDate = updateState.upToDate,
            downloading = updateState.downloading,
            downloadProgress = updateState.downloadProgress,
            onDownloadInstall = {
                onDownloadInstall(releaseInfo) {
                    onShowToast(noAssetStr)
                }
            },
            onViewOnGithub = { onViewReleaseOnGithub(releaseInfo.htmlUrl) },
            onDismiss = onDismissUpdate,
        )
    }

    val updateError = updateState.error
    if (updateError != null) {
        val errorMsg = stringResource(R.string.update_check_failed, updateError)
        LaunchedEffect(updateError) {
            onShowToast(errorMsg)
            onDismissUpdate()
        }
    }
}

@Composable
private fun AboutContent(
    topPadding: Dp,
    appVersionName: String,
    appVersionCode: Long,
    updateState: UpdateState,
    showLicensesDialog: Boolean,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgressProvider: () -> Float,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val density = LocalDensity.current
    val heroBackdrop = rememberBlurBackdrop()
    val isDark = isSystemInDarkTheme()
    val logoBlend = remember(isDark) { BgEffectConfig.logoBlendColors(isDark) }
    val dstOutPaint = remember { Paint().apply { blendMode = BlendMode.DstOut } }

    val logoHeightPx = remember { mutableIntStateOf(0) }
    val viewportHeightPx = remember { mutableIntStateOf(0) }
    val minContentHeight = remember(viewportHeightPx.intValue, topPadding, density) {
        with(density) {
            (viewportHeightPx.intValue - topPadding.roundToPx()).coerceAtLeast(0).toDp()
        }
    }

    BgEffectBackground(
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (heroBackdrop != null) Modifier.layerBackdrop(heroBackdrop) else Modifier,
        alpha = { 1f - scrollProgressProvider() },
    ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalCutoutPadding()
                        .padding(top = topPadding + 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer {
                                val iconProgress = ((scrollProgressProvider() - 0.35f) / 0.15f).coerceIn(0f, 1f)
                                alpha = 1f - iconProgress
                                scaleX = 1f - iconProgress * 0.05f
                                scaleY = 1f - iconProgress * 0.05f
                            }
                            .then(
                                if (heroBackdrop != null) {
                                    // 与下方标题共用同一套前景毛玻璃着色（DstIn），以挖空后的 squircle 作为 alpha 遮罩
                                    Modifier.textureBlur(
                                        backdrop = heroBackdrop,
                                        shape = RectangleShape,
                                        blurRadius = 100f,
                                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                        colors = BlurDefaults.blurColors(blendColors = logoBlend),
                                        contentBlendMode = BlendMode.DstIn,
                                    )
                                } else {
                                    Modifier.graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                },
                            ),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .squircleBackground(
                                    color = MiuixTheme.colorScheme.onBackground,
                                    cornerRadius = 24.dp,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_monochrome),
                                contentDescription = stringResource(R.string.app_name),
                                tint = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithContent {
                                        // 避免使用中间 graphicsLayer(RenderNode)，直接在当前合成层上用 DstOut 挖空 V 字使底层光晕透出
                                        drawIntoCanvas { canvas ->
                                            canvas.saveLayer(Rect(Offset.Zero, size), dstOutPaint)
                                            scale(scale = 1.45f) {
                                                this@drawWithContent.drawContent()
                                            }
                                            canvas.restore()
                                        }
                                    },
                            )
                        }
                    }

                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 5.dp)
                            .graphicsLayer {
                                val titleProgress = ((scrollProgressProvider() - 0.20f) / 0.15f).coerceIn(0f, 1f)
                                alpha = 1f - titleProgress
                                scaleX = 1f - titleProgress * 0.05f
                                scaleY = 1f - titleProgress * 0.05f
                            }
                            .then(
                                if (heroBackdrop != null) {
                                    Modifier.textureBlur(
                                        backdrop = heroBackdrop,
                                        shape = RoundedCornerShape(16.dp),
                                        blurRadius = 100f,
                                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                        colors = BlurDefaults.blurColors(blendColors = logoBlend),
                                        contentBlendMode = BlendMode.DstIn,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        text = stringResource(R.string.app_name),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 35.sp,
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val versionProgress = ((scrollProgressProvider() - 0.05f) / 0.15f).coerceIn(0f, 1f)
                                alpha = 1f - versionProgress
                                scaleX = 1f - versionProgress * 0.05f
                                scaleY = 1f - versionProgress * 0.05f
                            },
                        text = stringResource(R.string.about_version_format, appVersionName, appVersionCode),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = topPadding),
                ) {
                    // 透明占位区匹配 Hero 高度，上滑时同步驱动背景淡出与标题栏显隐
                    item(key = "logoSpacer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .layout { measurable, constraints ->
                                    val totalHeight = (
                                        logoHeightPx.intValue -
                                            topPadding.roundToPx() +
                                            108.dp.roundToPx()
                                        ).coerceAtLeast(0)
                                    val placeable = measurable.measure(
                                        constraints.copy(minHeight = totalHeight, maxHeight = totalHeight),
                                    )
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                },
                        )
                    }

                    // 视差 + textureBlur 页保持单 item + Card；使用 heightIn(min = 视口高) 兼顾竖屏折叠行程与横屏防裁切
                    item(key = "about") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minContentHeight)
                                .horizontalCutoutPadding(),
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp),
                            ) {
                                ArrowPreference(
                                    title = stringResource(R.string.about_check_update),
                                    endActions = {
                                        Text(
                                            text = if (updateState.checking) {
                                                stringResource(R.string.update_checking)
                                            } else {
                                                appVersionName
                                            },
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    holdDownState = updateState.release != null,
                                    onClick = {
                                        if (!updateState.checking) {
                                            onCheckUpdate()
                                        }
                                    },
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp),
                            ) {
                                ArrowPreference(
                                    title = stringResource(R.string.about_view_source),
                                    endActions = {
                                        Text(
                                            text = "GitHub",
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { onOpenUrl(REPO_URL) },
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.about_driver_repo),
                                    endActions = {
                                        Text(
                                            text = "ViPERFX_RE",
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { onOpenUrl(DRIVER_REPO_URL) },
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.about_report_issue),
                                    endActions = {
                                        Text(
                                            text = "GitHub Issues",
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { onOpenUrl(ISSUES_URL) },
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp),
                            ) {
                                ArrowPreference(
                                    title = stringResource(R.string.about_license),
                                    endActions = {
                                        Text(
                                            text = "GPL-3.0",
                                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    },
                                    onClick = { onOpenUrl(LICENSE_URL) },
                                )
                                ArrowPreference(
                                    title = stringResource(R.string.about_open_source_licenses),
                                    holdDownState = showLicensesDialog,
                                    onClick = onOpenLicenses,
                                )
                            }

                            Spacer(
                                modifier = Modifier
                                    .height(24.dp)
                                    .navigationBarsPadding(),
                            )
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val relaxed = constraints.copy(minWidth = 0, minHeight = 0)
            val logoPlaceable = measurables[0].measure(relaxed)
            if (logoHeightPx.intValue != logoPlaceable.height) {
                logoHeightPx.intValue = logoPlaceable.height
            }
            val listPlaceable = measurables[1].measure(relaxed)
            val width = constraints.constrainWidth(maxOf(logoPlaceable.width, listPlaceable.width))
            val height = constraints.constrainHeight(maxOf(logoPlaceable.height, listPlaceable.height))
            if (viewportHeightPx.intValue != height) {
                viewportHeightPx.intValue = height
            }
            layout(width, height) {
                logoPlaceable.placeRelative(0, 0)
                listPlaceable.placeRelative(0, 0)
            }
        }
    }
}

@Composable
private fun ThirdPartyLicensesDialog(
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val rowMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    WindowDialog(
        title = stringResource(R.string.about_open_source_licenses),
        show = true,
        onDismissRequest = onDismiss,
        insideMargin = DpSize(0.dp, 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                ThirdPartyLibraries.forEach { lib ->
                    ArrowPreference(
                        title = lib.name,
                        summary = lib.license,
                        insideMargin = rowMargin,
                        onClick = { onOpenUrl(lib.url) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

@Preview(name = "About Screen", showBackground = true)
@Composable
private fun AboutScreenPreview() {
    ViperTheme {
        AboutScreenContent(
            appVersionName = "1.0.0",
            appVersionCode = 100L,
            updateState = UpdateState(),
            onCheckUpdate = {},
            onDownloadInstall = { _, _ -> },
            onViewReleaseOnGithub = {},
            onDismissUpdate = {},
            onOpenUrl = {},
            onShowToast = {},
            onBack = {},
        )
    }
}

@Preview(name = "About Screen - Checking Update", showBackground = true)
@Composable
private fun AboutScreenCheckingUpdatePreview() {
    ViperTheme {
        AboutScreenContent(
            appVersionName = "1.0.0",
            appVersionCode = 100L,
            updateState = UpdateState(checking = true),
            onCheckUpdate = {},
            onDownloadInstall = { _, _ -> },
            onViewReleaseOnGithub = {},
            onDismissUpdate = {},
            onOpenUrl = {},
            onShowToast = {},
            onBack = {},
        )
    }
}
