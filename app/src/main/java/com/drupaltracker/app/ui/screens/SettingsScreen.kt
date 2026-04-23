package com.drupaltracker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.drupaltracker.app.data.preferences.NotificationSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    notificationSettings: NotificationSettings,
    onSaveApiKey: (String) -> Unit,
    onSaveNotificationSettings: (NotificationSettings) -> Unit,
    onBack: () -> Unit
) {
    var key by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gemini API Key section
            Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
            Text(
                "Get a free key at aistudio.google.com — 1,500 requests/day at no cost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                }
            )
            Button(
                onClick = { onSaveApiKey(key) },
                enabled = key.isNotBlank()
            ) {
                Text("Save")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Notifications section
            Text("Notifications", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable notifications", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = notificationSettings.enabled,
                    onCheckedChange = { checked ->
                        onSaveNotificationSettings(notificationSettings.copy(enabled = checked))
                    }
                )
            }

            AnimatedVisibility(visible = notificationSettings.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Starred projects", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = notificationSettings.notifyStarredProjects,
                            onCheckedChange = { checked ->
                                onSaveNotificationSettings(notificationSettings.copy(notifyStarredProjects = checked))
                            }
                        )
                    }

                    AnimatedVisibility(visible = notificationSettings.notifyStarredProjects) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Notification type",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = notificationSettings.projectNotifType == "EVERY_UPDATE",
                                    onClick = {
                                        onSaveNotificationSettings(notificationSettings.copy(projectNotifType = "EVERY_UPDATE"))
                                    },
                                    label = { Text("Every update") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = notificationSettings.projectNotifType == "DIGEST",
                                    onClick = {
                                        onSaveNotificationSettings(notificationSettings.copy(projectNotifType = "DIGEST"))
                                    },
                                    label = { Text("Digest") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            AnimatedVisibility(visible = notificationSettings.projectNotifType == "DIGEST") {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Digest interval",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("HOURLY" to "Hourly", "DAILY" to "Daily", "WEEKLY" to "Weekly").forEach { (value, label) ->
                                            FilterChip(
                                                selected = notificationSettings.digestInterval == value,
                                                onClick = {
                                                    onSaveNotificationSettings(notificationSettings.copy(digestInterval = value))
                                                },
                                                label = { Text(label) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Starred issues (digest)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Grouped notifications for issue updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationSettings.notifyStarredIssues,
                            onCheckedChange = { checked ->
                                onSaveNotificationSettings(notificationSettings.copy(notifyStarredIssues = checked))
                            }
                        )
                    }
                }
            }
        }
    }
}
