package com.llsl.viper4android.ui.screens.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.components.DialogCard
import com.llsl.viper4android.ui.components.InfoRow
import com.llsl.viper4android.ui.components.RowDivider
import com.llsl.viper4android.ui.screens.main.DriverStatus
import com.llsl.viper4android.ui.theme.StatusColors
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun DriverStatusDialog(
    driverStatus: DriverStatus,
    onDismiss: () -> Unit,
    show: Boolean = true,
) {
    WindowDialog(
        title = stringResource(R.string.menu_driver_status),
        show = show,
        onDismissRequest = onDismiss,
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
                if (!driverStatus.installed) {
                    Text(
                        text = stringResource(R.string.driver_not_found),
                        style = MiuixTheme.textStyles.body1,
                        color = StatusColors.danger,
                    )
                } else {
                    DialogCard {
                        InfoRow(
                            label = stringResource(R.string.driver_version_code),
                            value = driverStatus.versionCode.toString(),
                        )
                        RowDivider()
                        InfoRow(
                            label = stringResource(R.string.driver_version_name),
                            value = driverStatus.versionName,
                        )
                        RowDivider()
                        InfoRow(
                            label = stringResource(R.string.driver_architecture),
                            value = driverStatus.architecture,
                        )
                        RowDivider()
                        InfoRow(
                            label = stringResource(R.string.driver_streaming),
                            value = if (driverStatus.streaming) {
                                stringResource(R.string.status_active)
                            } else {
                                stringResource(R.string.status_inactive)
                            },
                        )
                        RowDivider()
                        InfoRow(
                            label = stringResource(R.string.driver_sampling_rate),
                            value = if (driverStatus.samplingRate > 0) {
                                "${driverStatus.samplingRate} Hz"
                            } else {
                                stringResource(R.string.status_unknown)
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
