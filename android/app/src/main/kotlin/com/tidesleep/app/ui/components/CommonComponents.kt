package com.tidesleep.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tidesleep.app.ui.theme.TideOnSurfaceMuted
import com.tidesleep.app.ui.theme.TideSurface

@Composable
fun TideCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TideSurface),
    ) {
        Column(content = content)
    }
}

@Composable
fun DisclaimerBanner(text: String, modifier: Modifier = Modifier) {
    TideCard(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TideOnSurfaceMuted,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TideOnSurfaceMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
