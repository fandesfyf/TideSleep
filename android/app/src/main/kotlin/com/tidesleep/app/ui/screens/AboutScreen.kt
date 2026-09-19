package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.ui.components.TideCard
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("关于汐眠") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("汐眠 TideSleep", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "版本 0.1.1-mvp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("免责声明", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = """
汐眠为睡眠辅助工具，非医疗器械。

本产品不提供医疗诊断、治疗或预防疾病的功能。开放环模式下由穿戴设备判断入睡后播放稀疏音频脉冲，不等于论文级 EEG 相位闭环刺激。

如有睡眠障碍，请咨询专业医疗机构。
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("隐私", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "睡眠状态默认在本地处理；云同步（如有）将明示授权。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
