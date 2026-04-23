package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.ui.navigation.Screen
import com.drupaltracker.app.ui.viewmodel.SummarySheet
import com.drupaltracker.app.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectIssueListScreen(
    projectScreen: Screen.ProjectIssues,
    state: UiState,
    onBack: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearchIssues: () -> Unit,
    onLoadMore: () -> Unit,
    isIssueStarred: (String) -> Boolean,
    onStarIssue: (IssueApiModel, String, String) -> Unit,
    onUnstarIssue: (String) -> Unit,
    onSummarizeIssue: (IssueApiModel) -> Unit,
    onCloseSummary: () -> Unit,
    onIssueClick: (IssueApiModel) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(projectScreen.projectTitle)
                        Text(
                            projectScreen.projectMachineName,
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Keyword filter
            OutlinedTextField(
                value = state.projectIssueKeyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Filter issues by keyword…") },
                trailingIcon = {
                    if (state.projectIssueIsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onSearchIssues) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                singleLine = true
            )

            state.projectIssueError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.projectIssues.isEmpty() && !state.projectIssueIsLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No issues found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projectIssues, key = { it.nid }) { issue ->
                        IssueCard(
                            issue = issue,
                            isStarred = isIssueStarred(issue.nid),
                            projectNid = projectScreen.projectNid,
                            projectTitle = projectScreen.projectTitle,
                            onClick = { onIssueClick(issue) },
                            onStar = { onStarIssue(issue, projectScreen.projectNid, projectScreen.projectTitle) },
                            onUnstar = { onUnstarIssue(issue.nid) },
                            onSummarize = { onSummarizeIssue(issue) }
                        )
                        HorizontalDivider()
                    }
                    if (state.projectIssueHasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = onLoadMore, modifier = Modifier.padding(8.dp)) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Summary bottom sheet
    val summarySheet = state.summarySheet
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
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    summarySheet.issueTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider()
                when {
                    summarySheet.loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Text(
                                "Summarizing…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
