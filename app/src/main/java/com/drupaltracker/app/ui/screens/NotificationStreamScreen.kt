package com.drupaltracker.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.model.NotificationRecord
import com.drupaltracker.app.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStreamScreen(
    state: UiState,
    onBack: () -> Unit,
    onLoadMore: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
        if (state.notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No notifications yet.\nStart watching starred projects to receive updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(state.notifications, key = { it.id }) { record ->
                    NotificationRecordCard(record = record)
                    HorizontalDivider()
                }
                if (state.notifHasMore) {
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

@Composable
private fun NotificationRecordCard(record: NotificationRecord) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
    val date = dateFormat.format(Date(record.timestamp))

    ListItem(
        headlineContent = {
            Text(record.title, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    record.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4
                )
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.targetUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(record.targetUrl)))
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            if (record.isProject) "Open project" else "Open issue",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    )
}
