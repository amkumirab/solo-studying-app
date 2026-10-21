package com.amkumirab.solostudying.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.amkumirab.solostudying.domain.planner.StudyBlock
import com.amkumirab.solostudying.domain.planner.clockRange
import java.time.LocalDate

@Composable
internal fun PlannerSessionTime(block: StudyBlock, priority: Int, modifier: Modifier = Modifier) {
    val clock = block.clockRange() ?: return
    Text("$clock · ${block.minutes} min · Priority $priority", modifier = modifier,
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Preview(widthDp = 288, showBackground = true)
@Composable
private fun SessionTimePreview() {
    MaterialTheme { PlannerSessionTime(StudyBlock(1, 1, LocalDate.of(2026, 10, 19), 25, 1020), 5) }
}
