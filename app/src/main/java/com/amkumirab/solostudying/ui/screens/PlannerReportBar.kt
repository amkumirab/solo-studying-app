package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun PlannerReportBar(
    label: String, minutes: Int, scaleMinutes: Int, isSelected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
        .semantics { selected = isSelected }, colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        )) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text("${plannerTimeLabel(minutes)} planned${if (isSelected) " · Selected" else ""}", style = MaterialTheme.typography.bodyMedium)
            val fraction = if (scaleMinutes > 0) (minutes.toFloat() / scaleMinutes).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (fraction > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(tint))
            }
        }
    }
}
