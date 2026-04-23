package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.data.model.ProjectNodeApiModel
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.model.StarredProject
import com.drupaltracker.app.ui.viewmodel.SearchMode
import com.drupaltracker.app.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabbedScreen(
    state: UiState,
    initialTab: Int = 0,
    onSetSearchMode: (SearchMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    onProjectClick: (ProjectNodeApiModel) -> Unit,
    onStarProject: (ProjectNodeApiModel) -> Unit,
    onUnstarProject: (String) -> Unit,
    isProjectStarred: (String) -> Boolean,
    isIssueStarred: (String) -> Boolean,
    onStarIssue: (IssueApiModel) -> Unit,
    onUnstarIssue: (String) -> Unit,
    onSummarizeIssue: (IssueApiModel) -> Unit,
    onOpenStarredProject: (StarredProject) -> Unit,
    onSummarizeStarredIssue: (StarredIssue) -> Unit,
    onIssueClick: (IssueApiModel) -> Unit,
    onCloseSummary: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DrupalTracker") },
                actions = {
                    BadgedBox(
                        badge = {
                            if (state.unreadNotifCount > 0) {
                                Badge { Text(state.unreadNotifCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Search") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Starred") }
                )
            }

            when (selectedTab) {
                0 -> SearchTabContent(
                    state = state,
                    onSetSearchMode = onSetSearchMode,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onLoadMore = onLoadMoreSearch,
                    onProjectClick = onProjectClick,
                    onStarProject = onStarProject,
                    onUnstarProject = onUnstarProject,
                    isProjectStarred = isProjectStarred,
                    isIssueStarred = isIssueStarred,
                    onStarIssue = onStarIssue,
                    onUnstarIssue = onUnstarIssue,
                    onSummarizeIssue = onSummarizeIssue,
                    onIssueClick = onIssueClick
                )
                1 -> StarredTabContent(
                    state = state,
                    onOpenProject = onOpenStarredProject,
                    onUnstarProject = onUnstarProject,
                    onUnstarIssue = onUnstarIssue,
                    onSummarizeStarredIssue = onSummarizeStarredIssue,
                    onIssueClick = onIssueClick
                )
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
