package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tidesleep.app.ui.theme.TideWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScienceAndMiJiaGuideScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("科学与米家向导") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("科学背景", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = """
MIT 研究表明：在慢波峰值时机播放约 50 ms 粉红噪声短脉冲，可增大慢波振幅并增强脑脊液（CSF）波动。

机制链条：听觉脉冲 → 强化慢波 → 血管舒缩泵增强 → CSF 潮汐加强。

参考：Levitt & Lewis et al., Science Translational Medicine (2026)
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "我们目前做到哪一步",
                        style = MaterialTheme.typography.titleMedium,
                        color = TideWarning,
                    )
                    Text(
                        text = """
• 汐眠 P0/P1 为开放环：由穿戴「入睡/醒来」触发，非 EEG 相位闭环。
• 手机喇叭 alone 无法检测慢波峰值。
• 默认稀疏 50 ms 脉冲，非整夜连续粉红噪声。
• 不作「治疗失眠/防痴呆」等医疗宣称。
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            TideCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("米家自动化配置（无代码验证）", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = """
1. 手环/手表绑定「小米运动健康」
2. 米家 App 开启运动健康数据访问
3. 确认穿戴出现在米家设备列表
4. 创建自动化：
   • 若「睡眠状态 = 睡着」→ 通知汐眠 / 播放指定内容
   • 若「醒来」→ 停止播放
5. 注意：判睡可能滞后真实入睡数分钟，请配合 App 内「入睡延迟」设置

前置：米家 10.0+、HyperOS 2+ 手机或蓝牙 Mesh 网关（部分小爱音箱不支持网关）
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideOnSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
