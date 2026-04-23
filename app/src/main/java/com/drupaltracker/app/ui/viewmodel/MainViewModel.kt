package com.drupaltracker.app.ui.viewmodel

import android.app.Application
import android.text.Html
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drupaltracker.app.data.api.GeminiClient
import com.drupaltracker.app.data.api.IssueRssFetcher
import com.drupaltracker.app.data.api.RetrofitClient
import com.drupaltracker.app.data.db.AppDatabase
import com.drupaltracker.app.data.model.IssueApiModel
import com.drupaltracker.app.data.model.NotificationRecord
import com.drupaltracker.app.data.model.ProjectNodeApiModel
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.model.StarredProject
import com.drupaltracker.app.data.model.toPriorityLabel
import com.drupaltracker.app.data.model.toProjectNodeApiModel
import com.drupaltracker.app.data.model.toStatusLabel
import com.drupaltracker.app.data.preferences.NotificationSettings
import com.drupaltracker.app.data.preferences.SettingsRepository
import com.drupaltracker.app.ui.navigation.Screen
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SummarySheet(
    val issueNid: String,
    val issueTitle: String,
    val summary: String? = null,
    val error: String? = null,
    val loading: Boolean = true
)

enum class SearchMode { BY_PROJECT, BY_ISSUE }

data class UiState(
    val screen: Screen = Screen.SearchTab,
    // Search tab
    val searchMode: SearchMode = SearchMode.BY_PROJECT,
    val searchQuery: String = "",
    val projectSearchResults: List<ProjectNodeApiModel> = emptyList(),
    val issueSearchResults: List<IssueApiModel> = emptyList(),
    val searchIsLoading: Boolean = false,
    val searchError: String? = null,
    val searchHasMore: Boolean = false,
    val searchPage: Int = 0,
    // Project issue list (drill-down from search or starred)
    val projectIssues: List<IssueApiModel> = emptyList(),
    val projectIssueKeyword: String = "",
    val projectIssuePage: Int = 0,
    val projectIssueHasMore: Boolean = false,
    val projectIssueIsLoading: Boolean = false,
    val projectIssueError: String? = null,
    // Starred
    val starredProjects: List<StarredProject> = emptyList(),
    val starredIssues: List<StarredIssue> = emptyList(),
    // Notification stream
    val notifications: List<NotificationRecord> = emptyList(),
    val notifPage: Int = 0,
    val notifHasMore: Boolean = false,
    val unreadNotifCount: Int = 0,
    // Settings
    val geminiApiKey: String = "",
    val notificationSettings: NotificationSettings = NotificationSettings(),
    // Summary sheet
    val summarySheet: SummarySheet? = null
)

private const val PAGE_SIZE = 10

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settings = SettingsRepository(application)
    private val _state = MutableStateFlow(UiState())
    private val backStack = ArrayDeque<Screen>()
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            db.starredProjectDao().getAllProjects().collect { projects ->
                _state.update { it.copy(starredProjects = projects) }
            }
        }
        viewModelScope.launch {
            db.starredIssueDao().getAllIssues().collect { issues ->
                _state.update { it.copy(starredIssues = issues) }
            }
        }
        viewModelScope.launch {
            settings.apiKeyFlow.collect { key ->
                _state.update { it.copy(geminiApiKey = key) }
            }
        }
        viewModelScope.launch {
            settings.notificationSettingsFlow.collect { ns ->
                _state.update { it.copy(notificationSettings = ns) }
            }
        }
        loadNotifications(reset = true)
    }

    // --- Navigation ---

    fun navigate(screen: Screen) = _state.update { it.copy(screen = screen) }

    fun navigateToIssue(url: String, title: String) {
        backStack.addLast(_state.value.screen)
        _state.update { it.copy(screen = Screen.IssueWebView(url, title)) }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            _state.update { it.copy(screen = backStack.removeLast()) }
            return
        }
        val current = _state.value.screen
        val parent: Screen = when (current) {
            is Screen.ProjectIssues -> when (current.sourceTab) {
                Screen.SourceTab.STARRED -> Screen.StarredTab
                else -> Screen.SearchTab
            }
            is Screen.NotificationStream -> Screen.SearchTab
            is Screen.Settings -> Screen.SearchTab
            else -> Screen.SearchTab
        }
        _state.update { it.copy(screen = parent) }
    }

    // --- Search tab ---

    fun setSearchMode(mode: SearchMode) {
        _state.update {
            it.copy(
                searchMode = mode,
                searchQuery = "",
                projectSearchResults = emptyList(),
                issueSearchResults = emptyList(),
                searchError = null,
                searchHasMore = false,
                searchPage = 0
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }
    }

    fun executeSearch() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) return
        _state.update {
            it.copy(
                searchIsLoading = true,
                searchError = null,
                projectSearchResults = emptyList(),
                issueSearchResults = emptyList(),
                searchPage = 0,
                searchHasMore = false
            )
        }
        viewModelScope.launch {
            try {
                when (_state.value.searchMode) {
                    SearchMode.BY_PROJECT -> {
                        val response = RetrofitClient.jsonApiService.searchProjectsContains(value = query, offset = 0)
                        val projects = response.data.map { it.toProjectNodeApiModel() }
                        _state.update {
                            it.copy(
                                searchIsLoading = false,
                                projectSearchResults = projects,
                                searchHasMore = response.links?.next != null
                            )
                        }
                    }
                    SearchMode.BY_ISSUE -> {
                        if (query.all { it.isDigit() }) {
                            // Direct NID lookup
                            val detail = RetrofitClient.service.getNodeDetail(query)
                            val issue = IssueApiModel(
                                nid = detail.nid,
                                title = detail.title,
                                changed = detail.changed ?: "0"
                            )
                            _state.update {
                                it.copy(
                                    searchIsLoading = false,
                                    issueSearchResults = listOf(issue),
                                    searchHasMore = false
                                )
                            }
                        } else {
                            val results = IssueRssFetcher.searchGlobal(query, page = 0)
                            _state.update {
                                it.copy(
                                    searchIsLoading = false,
                                    issueSearchResults = results,
                                    searchHasMore = results.size >= PAGE_SIZE
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Search error", e)
                _state.update { it.copy(searchIsLoading = false, searchError = "Network error: ${e.message}") }
            }
        }
    }

    fun loadMoreSearchResults() {
        val state = _state.value
        if (!state.searchHasMore || state.searchIsLoading) return
        val nextPage = state.searchPage + 1
        _state.update { it.copy(searchIsLoading = true, searchPage = nextPage) }
        viewModelScope.launch {
            try {
                when (state.searchMode) {
                    SearchMode.BY_PROJECT -> {
                        val offset = state.projectSearchResults.size
                        val response = RetrofitClient.jsonApiService.searchProjectsContains(
                            value = state.searchQuery, offset = offset
                        )
                        val projects = response.data.map { it.toProjectNodeApiModel() }
                        _state.update {
                            it.copy(
                                searchIsLoading = false,
                                projectSearchResults = it.projectSearchResults + projects,
                                searchHasMore = response.links?.next != null
                            )
                        }
                    }
                    SearchMode.BY_ISSUE -> {
                        val results = IssueRssFetcher.searchGlobal(state.searchQuery, page = nextPage)
                        _state.update {
                            it.copy(
                                searchIsLoading = false,
                                issueSearchResults = it.issueSearchResults + results,
                                searchHasMore = results.size >= PAGE_SIZE
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load more error", e)
                _state.update { it.copy(searchIsLoading = false) }
            }
        }
    }

    // --- Project issue list ---

    fun openProject(nid: String, title: String, machineName: String, sourceTab: Screen.SourceTab = Screen.SourceTab.SEARCH) {
        _state.update {
            it.copy(
                screen = Screen.ProjectIssues(nid, title, machineName, sourceTab),
                projectIssues = emptyList(),
                projectIssueKeyword = "",
                projectIssuePage = 0,
                projectIssueHasMore = false,
                projectIssueError = null
            )
        }
        loadProjectIssues(nid, machineName, "", page = 0)
    }

    fun onProjectIssueKeywordChange(keyword: String) {
        _state.update { it.copy(projectIssueKeyword = keyword) }
    }

    fun searchProjectIssues() {
        val screen = _state.value.screen as? Screen.ProjectIssues ?: return
        val keyword = _state.value.projectIssueKeyword.trim()
        _state.update {
            it.copy(
                projectIssues = emptyList(),
                projectIssuePage = 0,
                projectIssueHasMore = false,
                projectIssueError = null
            )
        }
        loadProjectIssues(screen.projectNid, screen.projectMachineName, keyword, page = 0)
    }

    private fun loadProjectIssues(projectNid: String, machineName: String, keyword: String, page: Int) {
        _state.update { it.copy(projectIssueIsLoading = true, projectIssueError = null) }
        viewModelScope.launch {
            try {
                val issueList = IssueRssFetcher.searchInProject(machineName, keyword, page = page)
                _state.update {
                    it.copy(
                        projectIssueIsLoading = false,
                        projectIssues = if (page == 0) issueList else it.projectIssues + issueList,
                        projectIssuePage = page,
                        projectIssueHasMore = issueList.size >= PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Project issues error", e)
                _state.update { it.copy(projectIssueIsLoading = false, projectIssueError = "Network error: ${e.message}") }
            }
        }
    }

    fun loadMoreProjectIssues() {
        val state = _state.value
        if (!state.projectIssueHasMore || state.projectIssueIsLoading) return
        val screen = state.screen as? Screen.ProjectIssues ?: return
        loadProjectIssues(screen.projectNid, screen.projectMachineName, state.projectIssueKeyword.trim(), state.projectIssuePage + 1)
    }

    // --- Star / unstar ---

    fun starProject(project: ProjectNodeApiModel) {
        viewModelScope.launch {
            db.starredProjectDao().upsert(
                StarredProject(
                    nid = project.nid,
                    machineName = project.machineName ?: project.nid,
                    title = project.title,
                    lastChecked = System.currentTimeMillis()
                )
            )
        }
    }

    fun unstarProject(nid: String) {
        viewModelScope.launch { db.starredProjectDao().deleteByNid(nid) }
    }

    fun starIssue(issue: IssueApiModel, projectNid: String = "", projectTitle: String = "") {
        viewModelScope.launch {
            db.starredIssueDao().upsert(
                StarredIssue(
                    nid = issue.nid,
                    projectNid = projectNid,
                    projectTitle = projectTitle,
                    title = issue.title,
                    status = issue.status,
                    priority = issue.priority,
                    changed = issue.changed.toLongOrNull() ?: 0L,
                    url = issue.url,
                    commentCount = issue.commentCount.toIntOrNull() ?: 0
                )
            )
        }
    }

    fun unstarIssue(nid: String) {
        viewModelScope.launch { db.starredIssueDao().deleteByNid(nid) }
    }

    fun isProjectStarred(nid: String): Boolean =
        _state.value.starredProjects.any { it.nid == nid }

    fun isIssueStarred(nid: String): Boolean =
        _state.value.starredIssues.any { it.nid == nid }

    // --- Summarization ---

    fun summarizeIssue(
        nid: String,
        title: String,
        status: String,
        priority: String,
        commentCount: Int,
        cachedSummary: String?,
        summarizedCommentCount: Int
    ) {
        val apiKey = _state.value.geminiApiKey
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(summarySheet = SummarySheet(
                    issueNid = nid,
                    issueTitle = title,
                    loading = false,
                    error = "No Gemini API key set. Go to Settings to add one."
                ))
            }
            return
        }

        viewModelScope.launch {
            // Resolve cached summary from DB so non-starred search-result issues benefit too
            val starredRow = db.starredIssueDao().getIssue(nid)
            val seenRow = db.issueDao().getIssue(nid)
            val effectiveCached: String?
            val effectiveCount: Int
            when {
                starredRow?.cachedSummary != null -> {
                    effectiveCached = starredRow.cachedSummary
                    effectiveCount = starredRow.summarizedCommentCount
                }
                seenRow?.cachedSummary != null -> {
                    effectiveCached = seenRow.cachedSummary
                    effectiveCount = seenRow.summarizedCommentCount
                }
                else -> {
                    effectiveCached = cachedSummary
                    effectiveCount = summarizedCommentCount
                }
            }

            if (effectiveCached != null && commentCount <= effectiveCount) {
                _state.update {
                    it.copy(summarySheet = SummarySheet(
                        issueNid = nid,
                        issueTitle = title,
                        summary = effectiveCached,
                        loading = false
                    ))
                }
                return@launch
            }

            _state.update { it.copy(summarySheet = SummarySheet(issueNid = nid, issueTitle = title)) }
            try {
                val nodeDeferred = async { RetrofitClient.service.getNodeDetail(nid) }
                val commentsDeferred = async { RetrofitClient.service.getComments(nid) }
                val node = nodeDeferred.await()
                val allComments = commentsDeferred.await().list

                val prompt = if (effectiveCached != null && effectiveCount > 0) {
                    val newComments = allComments.drop(effectiveCount)
                        .take(20)
                        .joinToString("\n\n") { it.commentBody?.value?.stripHtml().orEmpty() }
                    buildIncrementalPrompt(title, effectiveCached, newComments)
                } else {
                    val bodyText = node.body?.value?.stripHtml().orEmpty()
                    val commentText = allComments.take(20)
                        .joinToString("\n\n") { it.commentBody?.value?.stripHtml().orEmpty() }
                    buildPrompt(title, status.toStatusLabel(), priority.toPriorityLabel(), bodyText, commentText)
                }

                val summary = GeminiClient.summarize(apiKey, prompt)

                // Persist to whichever DB rows exist; if none, insert a new seen_issue so
                // the summary survives the next time the user taps Summary on this issue
                when {
                    starredRow != null -> db.starredIssueDao().updateSummary(nid, summary, allComments.size)
                    seenRow != null -> db.issueDao().updateSummary(nid, summary, allComments.size)
                    else -> db.issueDao().upsert(
                        SeenIssue(
                            nid = nid,
                            projectNid = "",
                            title = title,
                            status = status,
                            priority = priority,
                            changed = node.changed?.toLongOrNull() ?: 0L,
                            url = "https://www.drupal.org/node/$nid",
                            commentCount = allComments.size,
                            cachedSummary = summary,
                            summarizedCommentCount = allComments.size
                        )
                    )
                }

                _state.update { it.copy(summarySheet = it.summarySheet?.copy(summary = summary, loading = false)) }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Summarize error", e)
                _state.update { it.copy(summarySheet = it.summarySheet?.copy(error = e.message, loading = false)) }
            }
        }
    }

    fun closeSummary() = _state.update { it.copy(summarySheet = null) }

    // --- Notification stream ---

    fun loadNotifications(reset: Boolean = false) {
        val offset = if (reset) 0 else _state.value.notifications.size
        viewModelScope.launch {
            try {
                val page = db.notificationRecordDao().getPage(limit = 50, offset = offset)
                val total = db.notificationRecordDao().count()
                _state.update {
                    it.copy(
                        notifications = if (reset) page else it.notifications + page,
                        notifPage = if (reset) 0 else it.notifPage + 1,
                        notifHasMore = (if (reset) page.size else it.notifications.size + page.size) < total,
                        unreadNotifCount = if (reset) total else it.unreadNotifCount
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load notifications error", e)
            }
        }
    }

    fun loadMoreNotifications() {
        if (!_state.value.notifHasMore) return
        loadNotifications(reset = false)
    }

    fun clearUnreadCount() = _state.update { it.copy(unreadNotifCount = 0) }

    // --- Settings ---

    fun saveApiKey(key: String) {
        viewModelScope.launch { settings.saveApiKey(key.trim()) }
    }

    fun saveNotificationSettings(s: NotificationSettings) {
        viewModelScope.launch { settings.saveNotificationSettings(s) }
    }

    // --- Prompts ---

    private fun String.stripHtml(): String =
        Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT).toString().trim()

    private fun buildPrompt(
        title: String,
        status: String,
        priority: String,
        body: String,
        comments: String
    ): String = buildString {
        appendLine("You are a helpful assistant summarizing a Drupal.org issue for a developer.")
        appendLine()
        appendLine("Issue: $title")
        appendLine("Status: $status | Priority: $priority")
        appendLine()
        if (body.isNotBlank()) {
            appendLine("Description:")
            appendLine(body.take(3000))
            appendLine()
        }
        if (comments.isNotBlank()) {
            appendLine("Discussion:")
            appendLine(comments.take(4000))
            appendLine()
        }
        appendLine("Provide a concise summary with four short sections:")
        appendLine("**Problem** — what the issue is about.")
        appendLine("**Discussion** — key points from the comments.")
        appendLine("**Next steps** — what needs to happen or is blocking progress.")
        appendLine("**How can I help** — specific ways a new contributor could help, both code (writing a patch, reviewing an existing patch, fixing tests) and non-code (manual testing, reproducing the bug, updating documentation, triaging).")
    }

    private fun buildIncrementalPrompt(
        title: String,
        previousSummary: String,
        newComments: String
    ): String = buildString {
        appendLine("You previously summarized this Drupal.org issue titled \"$title\".")
        appendLine()
        appendLine("Previous summary:")
        appendLine(previousSummary)
        appendLine()
        if (newComments.isNotBlank()) {
            appendLine("New comments since the last summary:")
            appendLine(newComments.take(4000))
            appendLine()
        }
        appendLine("Update the summary to reflect these new comments. Keep the same four sections:")
        appendLine("**Problem** — what the issue is about.")
        appendLine("**Discussion** — key points from all comments including the new ones.")
        appendLine("**Next steps** — what needs to happen or is blocking progress.")
        appendLine("**How can I help** — specific ways a new contributor could help, both code (writing a patch, reviewing an existing patch, fixing tests) and non-code (manual testing, reproducing the bug, updating documentation, triaging).")
    }
}
