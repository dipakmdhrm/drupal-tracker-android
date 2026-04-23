package com.drupaltracker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.WatchedProject
import com.drupaltracker.app.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    state: UiState,
    onProjectClick: (WatchedProject) -> Unit,
    onAddClick: () -> Unit,
    onRemoveProject: (WatchedProject) -> Unit,
    onSearchQuery: (String) -> Unit,
    onResolveSearch: () -> Unit,
    onAddSearchResult: () -> Unit,
    onCancelSearch: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drupal Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add project",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Add project search panel
            AnimatedVisibility(visible = state.isSearching) {
                AddProjectPanel(
                    query = state.searchQuery,
                    isLoading = state.isLoading,
                    searchResult = state.searchResult,
                    searchError = state.searchError,
                    onQueryChange = onSearchQuery,
                    onSearch = onResolveSearch,
                    onAdd = onAddSearchResult,
                    onCancel = onCancelSearch
                )
            }

            if (state.projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No projects watched yet", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add a Drupal project", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.projects, key = { it.nid }) { project ->
                        ProjectRow(
                            project = project,
                            onClick = { onProjectClick(project) },
                            onRemove = { onRemoveProject(project) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: WatchedProject,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(project.title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                buildString {
                    append(project.machineName)
                    if (project.filterStatus != null || project.filterPriority != null) {
                        append(" · filtered")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Default.BugReport, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove project?") },
            text = { Text("Stop watching ${project.title}?") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AddProjectPanel(
    query: String,
    isLoading: Boolean,
    searchResult: WatchedProject?,
    searchError: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add Project", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Machine name (e.g. token, views)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = if (isLoading) { { CircularProgressIndicator(Modifier.size(20.dp)) } } else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onSearch, enabled = query.isNotBlank() && !isLoading) {
                    Text("Search")
                }
            }
            if (searchError != null) {
                Text(searchError, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (searchResult != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(searchResult.title, fontWeight = FontWeight.Medium)
                            Text(searchResult.machineName, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onAdd) { Text("Watch") }
                    }
                }
            }
        }
    }
}
