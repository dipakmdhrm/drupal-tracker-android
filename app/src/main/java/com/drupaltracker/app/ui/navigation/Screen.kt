package com.drupaltracker.app.ui.navigation

sealed class Screen {
    object SearchTab : Screen()
    object StarredTab : Screen()
    data class ProjectIssues(
        val projectNid: String,
        val projectTitle: String,
        val projectMachineName: String,
        val sourceTab: SourceTab = SourceTab.SEARCH
    ) : Screen()
    object NotificationStream : Screen()
    object Settings : Screen()
    data class IssueWebView(val url: String, val title: String) : Screen()

    enum class SourceTab { SEARCH, STARRED }
}
