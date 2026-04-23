package com.drupaltracker.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.WatchedProject
import com.drupaltracker.app.data.model.toPriorityLabel
import com.drupaltracker.app.data.model.toStatusLabel
import com.drupaltracker.app.ui.viewmodel.SummarySheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueListScreen(
    project: WatchedProject,
    issues: List<SeenIssue>,
    onBack: () -> Unit,
    summarySheet: SummarySheet?,
    onSummarize: (SeenIssue) -> Unit,
    onCloseSummary: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.title)
                        Text(
                            project.machineName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (issues.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No issues tracked yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Issues will appear after the first poll",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(issues, key = { it.nid }) { issue ->
                    IssueRow(
                        issue = issue,
                        onOpen = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issue.url)))
                        },
                        onSummarize = { onSummarize(issue) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Summary bottom sheet
    if (summarySheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseSummary,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(summarySheet.issueTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium)
                HorizontalDivider()
                when {
                    summarySheet.loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Text("Summarizing…", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    summarySheet.error != null -> {
                        Text(
                            summarySheet.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    summarySheet.summary != null -> {
                        Text(summarySheet.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueRow(
    issue: SeenIssue,
    onOpen: () -> Unit,
    onSummarize: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val date = dateFormat.format(Date(issue.changed * 1000))

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
                    "Changed $date · ${issue.commentCount} comments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        },
        trailingContent = {
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun StatusChip(label: String) {
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
private fun PriorityChip(priority: String) {
    val color = when (priority) {
        "400" -> Color(0xFFD32F2F)
        "300" -> Color(0xFFF57C00)
        "200" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
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
