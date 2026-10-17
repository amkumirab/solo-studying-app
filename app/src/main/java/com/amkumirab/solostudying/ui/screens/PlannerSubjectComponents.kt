package com.amkumirab.solostudying.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amkumirab.solostudying.domain.planner.SubjectColor
import com.amkumirab.solostudying.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode

internal fun subjectPriorityLabel(priority: Int) = listOf("Low", "Moderate", "Normal", "High", "Critical")[priority - 1]
internal fun subjectHours(minutes: Int) = BigDecimal(minutes).divide(BigDecimal(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
internal fun SubjectColor.tint(): Color = when (this) {
    SubjectColor.Cyan -> NeonBlueAccent
    SubjectColor.Gold -> RpgGold
    SubjectColor.Rose -> RpgRuby
    SubjectColor.Green -> RpgEmerald
}

@Composable
internal fun SubjectPrioritySelector(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Priority $value of 5 · ${subjectPriorityLabel(value)}", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { priority ->
                FilterChip(selected = value == priority, onClick = { onChange(priority) }, enabled = enabled,
                    label = { Text(priority.toString()) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .testTag("subject_priority_$priority").semantics { contentDescription = "Priority $priority: ${subjectPriorityLabel(priority)}" })
            }
        }
    }
}

@Composable
internal fun SubjectColorSelector(value: SubjectColor, onChange: (SubjectColor) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Subject color", style = MaterialTheme.typography.titleSmall)
        SubjectColor.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { color ->
                    FilterChip(selected = value == color, onClick = { onChange(color) }, enabled = enabled,
                        label = { Text(color.name) }, leadingIcon = { Box(Modifier.size(14.dp).background(color.tint(), MaterialTheme.shapes.small)) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("subject_color_${color.name}"))
                }
            }
        }
    }
}

@Composable
internal fun PlannerSubjectTitle(name: String, color: SubjectColor?, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (color != null) Box(Modifier.padding(top = 4.dp).size(12.dp).background(color.tint(), MaterialTheme.shapes.small))
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
        if (color != null) Text(color.name, style = MaterialTheme.typography.bodySmall)
    }
}
