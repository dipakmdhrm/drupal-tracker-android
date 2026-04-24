package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.data.model.toPriorityLabel
import com.drupaltracker.app.data.model.toStatusLabel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IssueCard(
    issue: IssueApiModel,
    isStarred: Boolean,
    projectNid: String = "",
    projectTitle: String = "",
    onClick: () -> Unit = {},
    onStar: () -> Unit,
    onUnstar: () -> Unit,
    onSummarize: () -> Unit
) {
    val changedTs = issue.changed.toLongOrNull() ?: 0L
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val date = if (changedTs > 0) dateFormat.format(Date(changedTs * 1000)) else "—"
    val commentCount = issue.commentCount.toIntOrNull() ?: 0

    ListItem(
        headlineContent = {
            Text(issue.title, fontWeight = FontWeight.Medium, maxLines = 2)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(issue.status.toStatusLabel())
                    PriorityChip(issue.priority)
                }
                Text(
                    "Changed $date · $commentCount comments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSummarize,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Summary", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { if (isStarred) onUnstar() else onStar() }) {
                Icon(
                    if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isStarred) "Unstar" else "Star",
                    tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun StatusChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun PriorityChip(priority: String) {
    val color = when (priority) {
        "400" -> Color(0xFFD32F2F)
        "300" -> Color(0xFFF57C00)
        "200" -> MaterialTheme.colorScheme.primary
        else  -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            priority.toPriorityLabel(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
