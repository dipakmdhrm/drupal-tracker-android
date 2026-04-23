package com.drupaltracker.app.ui.viewmodel

import android.app.Application
import android.text.Html
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drupaltracker.app.data.api.GeminiClient
import com.drupaltracker.app.data.api.RetrofitClient
import com.drupaltracker.app.data.db.AppDatabase
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.WatchedProject
import com.drupaltracker.app.data.model.toPriorityLabel
import com.drupaltracker.app.data.model.toStatusLabel
import com.drupaltracker.app.data.preferences.SettingsRepository
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

data class UiState(
    val projects: List<WatchedProject> = emptyList(),
    val selectedProjectNid: String? = null,
    val issuesForSelected: List<SeenIssue> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResult: WatchedProject? = null,
    val searchError: String? = null,
    val isLoading: Boolean = false,
    val showSettings: Boolean = false,
    val geminiApiKey: String = "",
    val summarySheet: SummarySheet? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settings = SettingsRepository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            db.projectDao().getAllProjects().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
        viewModelScope.launch {
            settings.apiKeyFlow.collect { key ->
                _state.update { it.copy(geminiApiKey = key) }
            }
        }
    }

    // --- Navigation ---

    fun showSettings() = _state.update { it.copy(showSettings = true) }
    fun hideSettings() = _state.update { it.copy(showSettings = false) }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settings.saveApiKey(key.trim())
            _state.update { it.copy(showSettings = false) }
        }
    }

    // --- Projects ---

    fun selectProject(nid: String?) {
        _state.update { it.copy(selectedProjectNid = nid, issuesForSelected = emptyList()) }
        if (nid != null) {
            viewModelScope.launch {
                db.issueDao().getIssuesForProject(nid).collect { issues ->
                    _state.update { it.copy(issuesForSelected = issues) }
                }
            }
        }
    }

    fun searchProject(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null, searchResult = null) }
    }

    fun resolveProject() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) return
        _state.update { it.copy(isLoading = true, searchError = null) }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.service.getProjectByMachineName(query)
                val node = response.list.firstOrNull()
                if (node == null) {
                    _state.update { it.copy(isLoading = false, searchError = "Project '$query' not found") }
                } else {
                    val project = WatchedProject(
                        nid = node.nid,
                        machineName = node.machineName ?: query,
                        title = node.title
                    )
                    _state.update { it.copy(isLoading = false, searchResult = project) }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Search error", e)
                _state.update { it.copy(isLoading = false, searchError = "Network error: ${e.message}") }
            }
        }
    }

    fun addProject(project: WatchedProject) {
        viewModelScope.launch {
            db.projectDao().upsert(project)
            _state.update { it.copy(searchResult = null, searchQuery = "", isSearching = false) }
        }
    }

    fun removeProject(project: WatchedProject) {
        viewModelScope.launch {
            db.projectDao().delete(project)
            db.issueDao().deleteForProject(project.nid)
            if (_state.value.selectedProjectNid == project.nid) {
                _state.update { it.copy(selectedProjectNid = null, issuesForSelected = emptyList()) }
            }
        }
    }

    fun setSearching(searching: Boolean) {
        _state.update { it.copy(isSearching = searching, searchQuery = "", searchResult = null, searchError = null) }
    }

    // --- Summarization ---

    fun summarizeIssue(issue: SeenIssue) {
        val apiKey = _state.value.geminiApiKey
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(summarySheet = SummarySheet(
                    issueNid = issue.nid,
                    issueTitle = issue.title,
                    loading = false,
                    error = "No Gemini API key set. Go to Settings to add one."
                ))
            }
            return
        }

        // If cached and no new comments, show immediately without API call
        if (issue.cachedSummary != null && issue.commentCount <= issue.summarizedCommentCount) {
            _state.update {
                it.copy(summarySheet = SummarySheet(
                    issueNid = issue.nid,
                    issueTitle = issue.title,
                    summary = issue.cachedSummary,
                    loading = false
                ))
            }
            return
        }

        _state.update {
            it.copy(summarySheet = SummarySheet(issueNid = issue.nid, issueTitle = issue.title))
        }
        viewModelScope.launch {
            try {
                val nodeDeferred = async { RetrofitClient.service.getNodeDetail(issue.nid) }
                val commentsDeferred = async { RetrofitClient.service.getComments(issue.nid) }
                val node = nodeDeferred.await()
                val allComments = commentsDeferred.await().list

                val prompt = if (issue.cachedSummary != null && issue.summarizedCommentCount > 0) {
                    // Incremental: old summary + only the new comments
                    val newComments = allComments.drop(issue.summarizedCommentCount)
                        .take(20)
                        .joinToString("\n\n") { it.commentBody?.value?.stripHtml().orEmpty() }
                    buildIncrementalPrompt(issue.title, issue.cachedSummary, newComments)
                } else {
                    // Full summarization
                    val bodyText = node.body?.value?.stripHtml().orEmpty()
                    val commentText = allComments.take(20)
                        .joinToString("\n\n") { it.commentBody?.value?.stripHtml().orEmpty() }
                    buildPrompt(issue.title, issue.status.toStatusLabel(), issue.priority.toPriorityLabel(), bodyText, commentText)
                }

                val summary = GeminiClient.summarize(apiKey, prompt)
                db.issueDao().updateSummary(issue.nid, summary, allComments.size)
                _state.update { it.copy(summarySheet = it.summarySheet?.copy(summary = summary, loading = false)) }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Summarize error", e)
                _state.update { it.copy(summarySheet = it.summarySheet?.copy(error = e.message, loading = false)) }
            }
        }
    }

    fun closeSummary() = _state.update { it.copy(summarySheet = null) }

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
