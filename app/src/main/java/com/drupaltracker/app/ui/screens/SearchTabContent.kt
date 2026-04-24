package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.data.model.ProjectNodeApiModel
import com.drupaltracker.app.ui.viewmodel.SearchMode
import com.drupaltracker.app.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTabContent(
    state: UiState,
    onSetSearchMode: (SearchMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onProjectClick: (ProjectNodeApiModel) -> Unit,
    onStarProject: (ProjectNodeApiModel) -> Unit,
    onUnstarProject: (String) -> Unit,
    isProjectStarred: (String) -> Boolean,
    isIssueStarred: (String) -> Boolean,
    onStarIssue: (IssueApiModel) -> Unit,
    onUnstarIssue: (String) -> Unit,
    onSummarizeIssue: (IssueApiModel) -> Unit,
    onIssueClick: (IssueApiModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.searchMode == SearchMode.BY_PROJECT,
                onClick = { onSetSearchMode(SearchMode.BY_PROJECT) },
                label = { Text("By Project") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.searchMode == SearchMode.BY_ISSUE,
                onClick = { onSetSearchMode(SearchMode.BY_ISSUE) },
                label = { Text("By Issue") },
                modifier = Modifier.weight(1f)
            )
        }

        // Search field
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    if (state.searchMode == SearchMode.BY_PROJECT) "Machine name (e.g. pathauto) or title keywords"
                    else "Issue title (contains) or NID number"
                )
            },
            trailingIcon = {
                if (state.searchIsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Error
        state.searchError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Results
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            when (state.searchMode) {
                SearchMode.BY_PROJECT -> {
                    items(state.projectSearchResults, key = { it.nid }) { project ->
                        ProjectSearchResultCard(
                            project = project,
                            isStarred = isProjectStarred(project.nid),
                            onClick = { onProjectClick(project) },
                            onStar = { onStarProject(project) },
                            onUnstar = { onUnstarProject(project.nid) }
                        )
                        HorizontalDivider()
                    }
                }
                SearchMode.BY_ISSUE -> {
                    items(state.issueSearchResults, key = { it.nid }) { issue ->
                        IssueCard(
                            issue = issue,
                            isStarred = isIssueStarred(issue.nid),
                            onClick = { onIssueClick(issue) },
                            onStar = { onStarIssue(issue) },
                            onUnstar = { onUnstarIssue(issue.nid) },
                            onSummarize = { onSummarizeIssue(issue) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (state.searchHasMore) {
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

@Composable
private fun ProjectSearchResultCard(
    project: ProjectNodeApiModel,
    isStarred: Boolean,
    onClick: () -> Unit,
    onStar: () -> Unit,
    onUnstar: () -> Unit
) {
    ListItem(
        headlineContent = { Text(project.title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            project.machineName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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
