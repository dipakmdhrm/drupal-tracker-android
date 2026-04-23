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
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.notifications.NotificationHelper
import com.drupaltracker.app.service.PollingForegroundService
import com.drupaltracker.app.ui.navigation.Screen
import com.drupaltracker.app.ui.screens.IssueWebViewScreen
import com.drupaltracker.app.ui.screens.MainTabbedScreen
import com.drupaltracker.app.ui.screens.NotificationStreamScreen
import com.drupaltracker.app.ui.screens.ProjectIssueListScreen
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

        // Handle cold-start intent from notification
        handleNotificationIntent(intent)

        setContent {
            DrupalTrackerTheme {
                val state by viewModel.state.collectAsState()

                val onIssueClick: (IssueApiModel) -> Unit = { issue ->
                    val url = issue.url.ifBlank { "https://www.drupal.org/node/${issue.nid}" }
                    viewModel.navigateToIssue(url, issue.title)
                }

                when (val screen = state.screen) {
                    is Screen.IssueWebView -> {
                        IssueWebViewScreen(
                            url = screen.url,
                            title = screen.title,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    is Screen.SearchTab, is Screen.StarredTab -> {
                        MainTabbedScreen(
                            state = state,
                            initialTab = if (screen is Screen.StarredTab) 1 else 0,
                            onSetSearchMode = { viewModel.setSearchMode(it) },
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onSearch = { viewModel.executeSearch() },
                            onLoadMoreSearch = { viewModel.loadMoreSearchResults() },
                            onProjectClick = { project ->
                                viewModel.openProject(
                                    project.nid,
                                    project.title,
                                    project.machineName ?: project.nid,
                                    Screen.SourceTab.SEARCH
                                )
                            },
                            onStarProject = { viewModel.starProject(it) },
                            onUnstarProject = { viewModel.unstarProject(it) },
                            isProjectStarred = { viewModel.isProjectStarred(it) },
                            isIssueStarred = { viewModel.isIssueStarred(it) },
                            onStarIssue = { issue ->
                                viewModel.starIssue(issue)
                            },
                            onUnstarIssue = { viewModel.unstarIssue(it) },
                            onSummarizeIssue = { issue ->
                                viewModel.summarizeIssue(
                                    nid = issue.nid,
                                    title = issue.title,
                                    status = issue.status,
                                    priority = issue.priority,
                                    commentCount = issue.commentCount.toIntOrNull() ?: 0,
                                    cachedSummary = null,
                                    summarizedCommentCount = 0
                                )
                            },
                            onOpenStarredProject = { starredProject ->
                                viewModel.openProject(
                                    starredProject.nid,
                                    starredProject.title,
                                    starredProject.machineName,
                                    Screen.SourceTab.STARRED
                                )
                            },
                            onSummarizeStarredIssue = { starred ->
                                viewModel.summarizeIssue(
                                    nid = starred.nid,
                                    title = starred.title,
                                    status = starred.status,
                                    priority = starred.priority,
                                    commentCount = starred.commentCount,
                                    cachedSummary = starred.cachedSummary,
                                    summarizedCommentCount = starred.summarizedCommentCount
                                )
                            },
                            onIssueClick = onIssueClick,
                            onNotificationsClick = {
                                viewModel.clearUnreadCount()
                                viewModel.loadNotifications(reset = true)
                                viewModel.navigate(Screen.NotificationStream)
                            },
                            onSettingsClick = { viewModel.navigate(Screen.Settings) }
                        )
                    }
                    is Screen.ProjectIssues -> {
                        ProjectIssueListScreen(
                            projectScreen = screen,
                            state = state,
                            onBack = { viewModel.navigateBack() },
                            onKeywordChange = { viewModel.onProjectIssueKeywordChange(it) },
                            onSearchIssues = { viewModel.searchProjectIssues() },
                            onLoadMore = { viewModel.loadMoreProjectIssues() },
                            isIssueStarred = { viewModel.isIssueStarred(it) },
                            onStarIssue = { issue, projectNid, projectTitle ->
                                viewModel.starIssue(issue, projectNid, projectTitle)
                            },
                            onUnstarIssue = { viewModel.unstarIssue(it) },
                            onSummarizeIssue = { issue ->
                                val starred = state.starredIssues.find { it.nid == issue.nid }
                                viewModel.summarizeIssue(
                                    nid = issue.nid,
                                    title = issue.title,
                                    status = issue.status,
                                    priority = issue.priority,
                                    commentCount = issue.commentCount.toIntOrNull() ?: 0,
                                    cachedSummary = starred?.cachedSummary,
                                    summarizedCommentCount = starred?.summarizedCommentCount ?: 0
                                )
                            },
                            onCloseSummary = { viewModel.closeSummary() },
                            onIssueClick = onIssueClick
                        )
                    }
                    is Screen.NotificationStream -> {
                        NotificationStreamScreen(
                            state = state,
                            onBack = { viewModel.navigateBack() },
                            onLoadMore = { viewModel.loadMoreNotifications() }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            currentApiKey = state.geminiApiKey,
                            notificationSettings = state.notificationSettings,
                            onSaveApiKey = { viewModel.saveApiKey(it) },
                            onSaveNotificationSettings = { viewModel.saveNotificationSettings(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_NOTIFICATION_STREAM, false) == true) {
            viewModel.clearUnreadCount()
            viewModel.loadNotifications(reset = true)
            viewModel.navigate(Screen.NotificationStream)
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
