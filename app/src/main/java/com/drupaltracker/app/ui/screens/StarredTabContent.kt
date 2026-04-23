package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.model.StarredProject
import com.drupaltracker.app.ui.navigation.Screen
import com.drupaltracker.app.ui.viewmodel.UiState

@Composable
fun StarredTabContent(
    state: UiState,
    onOpenProject: (StarredProject) -> Unit,
    onUnstarProject: (String) -> Unit,
    onUnstarIssue: (String) -> Unit,
    onSummarizeStarredIssue: (StarredIssue) -> Unit,
    onIssueClick: (IssueApiModel) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Projects (${state.starredProjects.size})") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Issues (${state.starredIssues.size})") }
            )
        }

        when (selectedSubTab) {
            0 -> StarredProjectsSubTab(
                projects = state.starredProjects,
                onOpenProject = onOpenProject,
                onUnstar = onUnstarProject
            )
            1 -> StarredIssuesSubTab(
                issues = state.starredIssues,
                isIssueStarred = { true },  // all shown here are starred
                onUnstar = onUnstarIssue,
                onSummarize = onSummarizeStarredIssue,
                onIssueClick = onIssueClick
            )
        }
    }
}

@Composable
private fun StarredProjectsSubTab(
    projects: List<StarredProject>,
    onOpenProject: (StarredProject) -> Unit,
    onUnstar: (String) -> Unit
) {
    if (projects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No starred projects yet.\nSearch for a project and star it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(projects, key = { it.nid }) { project ->
            ListItem(
                headlineContent = { Text(project.title, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Text(project.machineName, style = MaterialTheme.typography.bodySmall)
                },
                trailingContent = {
                    IconButton(onClick = { onUnstar(project.nid) }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Unstar",
                            tint = Color(0xFFFFC107)
                        )
                    }
                },
                modifier = Modifier.clickable { onOpenProject(project) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun StarredIssuesSubTab(
    issues: List<StarredIssue>,
    isIssueStarred: (String) -> Boolean,
    onUnstar: (String) -> Unit,
    onSummarize: (StarredIssue) -> Unit,
    onIssueClick: (IssueApiModel) -> Unit
) {
    if (issues.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No starred issues yet.\nStar an issue to track it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(issues, key = { it.nid }) { starred ->
            val issueApiModel = IssueApiModel(
                nid = starred.nid,
                title = starred.title,
                status = starred.status,
                priority = starred.priority,
                changed = starred.changed.toString(),
                commentCount = starred.commentCount.toString(),
                url = starred.url
            )
            IssueCard(
                issue = issueApiModel,
                isStarred = true,
                projectNid = starred.projectNid,
                projectTitle = starred.projectTitle,
                onClick = { onIssueClick(issueApiModel) },
                onStar = { /* already starred */ },
                onUnstar = { onUnstar(starred.nid) },
                onSummarize = { onSummarize(starred) }
            )
            HorizontalDivider()
        }
    }
}
