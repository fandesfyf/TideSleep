package com.tidesleep.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted

@Composable
fun OnboardingDialog(
    onAccept: () -> Unit,
    onPlayCalibration: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* 必须勾选后才能继续 */ },
        title = { Text("欢迎使用汐眠") },
        text = {
            Column {
                Text(
                    text = "汐眠为睡眠辅助工具，非医疗器械。无 EEG 时为开放环（入睡触发），不等于论文级相位闭环。不提供医疗诊断或治疗建议。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideOnSurfaceMuted,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onPlayCalibration,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("试听一发脉冲")
                }
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(
                        text = "我已阅读并理解上述说明",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = checked,
            ) {
                Text("开始使用")
            }
        },
    )
}
