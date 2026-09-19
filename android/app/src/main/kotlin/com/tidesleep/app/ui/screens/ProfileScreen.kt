package com.tidesleep.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.ui.components.DisclaimerBanner
import com.tidesleep.app.ui.components.SectionHeader

@Composable
fun ProfileScreen(
    onNavigateToSafety: () -> Unit,
    onNavigateToScience: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(title = "我的")

        DisclaimerBanner(
            text = "汐眠为睡眠辅助工具，非医疗器械。不提供医疗诊断或治疗建议。"
        )

        OutlinedButton(onClick = onNavigateToSafety, modifier = Modifier.fillMaxWidth()) {
            Text("安全设置")
        }
        OutlinedButton(onClick = onNavigateToScience, modifier = Modifier.fillMaxWidth()) {
            Text("科学说明与米家向导")
        }
        OutlinedButton(onClick = onNavigateToAbout, modifier = Modifier.fillMaxWidth()) {
            Text("关于汐眠")
        }
    }
}
