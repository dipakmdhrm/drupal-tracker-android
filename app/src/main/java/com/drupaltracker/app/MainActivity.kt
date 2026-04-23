package com.drupaltracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.drupaltracker.app.notifications.NotificationHelper
import com.drupaltracker.app.service.PollingForegroundService
import com.drupaltracker.app.ui.screens.IssueListScreen
import com.drupaltracker.app.ui.screens.ProjectListScreen
import com.drupaltracker.app.ui.screens.SettingsScreen
import com.drupaltracker.app.ui.theme.DrupalTrackerTheme
import com.drupaltracker.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()
        startPollingService()

        setContent {
            DrupalTrackerTheme {
                val state by viewModel.state.collectAsState()
                val selectedProject = state.projects.find { it.nid == state.selectedProjectNid }

                when {
                    state.showSettings -> {
                        SettingsScreen(
                            currentApiKey = state.geminiApiKey,
                            onSave = { viewModel.saveApiKey(it) },
                            onBack = { viewModel.hideSettings() }
                        )
                    }
                    selectedProject != null -> {
                        IssueListScreen(
                            project = selectedProject,
                            issues = state.issuesForSelected,
                            onBack = { viewModel.selectProject(null) },
                            summarySheet = state.summarySheet,
                            onSummarize = { viewModel.summarizeIssue(it) },
                            onCloseSummary = { viewModel.closeSummary() }
                        )
                    }
                    else -> {
                        ProjectListScreen(
                            state = state,
                            onProjectClick = { viewModel.selectProject(it.nid) },
                            onAddClick = { viewModel.setSearching(true) },
                            onRemoveProject = { viewModel.removeProject(it) },
                            onSearchQuery = { viewModel.searchProject(it) },
                            onResolveSearch = { viewModel.resolveProject() },
                            onAddSearchResult = {
                                state.searchResult?.let { viewModel.addProject(it) }
                            },
                            onCancelSearch = { viewModel.setSearching(false) },
                            onSettingsClick = { viewModel.showSettings() }
                        )
                    }
                }
            }
        }
    }

    private fun startPollingService() {
        startForegroundService(Intent(this, PollingForegroundService::class.java))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
