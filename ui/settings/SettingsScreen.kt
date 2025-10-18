package com.efvs.suppletrack.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            SettingRow(
                icon = Icons.Default.DarkMode,
                label = "Dark Mode",
                control = {
                    Switch(checked = settings.darkMode, onCheckedChange = { viewModel.toggleDarkMode() })
                }
            )
            Spacer(Modifier.height(12.dp))
            SettingRow(
                icon = Icons.Default.Language,
                label = "Language",
                control = {
                    DropdownMenuButton(
                        value = settings.language,
                        options = listOf("en", "de", "fr", "es"),
                        onValueChange = { viewModel.setLanguage(it) }
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            SettingRow(
                icon = Icons.Default.Security,
                label = "PIN/Biometric Lock",
                control = {
                    Switch(checked = settings.pinEnabled, onCheckedChange = { viewModel.togglePin() })
                }
            )
            Spacer(Modifier.height(12.dp))
            SettingRow(
                icon = Icons.Default.TextFields,
                label = "Text Size",
                control = {
                    Slider(
                        value = settings.textSize,
                        onValueChange = { viewModel.setTextSize(it) },
                        valueRange = 0.8f..1.5f,
                        steps = 4,
                        modifier = Modifier.width(120.dp)
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            SettingRow(
                icon = Icons.Default.Visibility,
                label = "Color Blind Mode",
                control = {
                    Switch(checked = settings.colorBlindMode, onCheckedChange = { viewModel.toggleColorBlindMode() })
                }
            )
            Spacer(Modifier.height(12.dp))
            SettingRow(
                icon = Icons.Default.NotificationsActive,
                label = "Notifications",
                control = {
                    Switch(checked = settings.notificationsEnabled, onCheckedChange = { viewModel.toggleNotifications() })
                }
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { showAbout = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Info, contentDescription = "About", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("About SuppleTrack")
            }
        }
        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) { Text("OK") }
                },
                title = { Text("About SuppleTrack") },
                text = {
                    Column {
                        Text("SuppleTrack v1.0\nDeveloped by EFvS")
                        Text("For support, visit github.com/EFvS/SuppleTrack or email support@example.com.")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    control: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f))
        control()
    }
}