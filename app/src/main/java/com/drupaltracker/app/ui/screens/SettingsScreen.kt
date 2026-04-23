package com.drupaltracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentApiKey: String,
    onSave: (String) -> Unit,
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
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                onClick = { onSave(key) },
                enabled = key.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
