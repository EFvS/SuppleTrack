package com.efvs.suppletrack.ui.supplement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.efvs.suppletrack.data.local.SupplementEntity

@Composable
fun SupplementEditScreen(
    profileId: Long,
    initialSupplement: SupplementEntity? = null,
    onSave: () -> Unit,
    viewModel: SupplementEditViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var name by remember { mutableStateOf(initialSupplement?.name ?: "") }
    var dosage by remember { mutableStateOf(initialSupplement?.dosage.orEmpty()) }
    var note by remember { mutableStateOf(initialSupplement?.note.orEmpty()) }
    var icon by remember { mutableStateOf(initialSupplement?.icon ?: "💊") }
    var color by remember { mutableStateOf(initialSupplement?.color ?: 0xFF2196F3) }

    // Reminder interval and PRN/As-needed logic (basic version here, can be extended)
    var intervalType by remember { mutableStateOf(initialSupplement?.intervalType ?: "DAILY") }
    var intervalData by remember { mutableStateOf(initialSupplement?.intervalData ?: "08:00") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (initialSupplement == null) "Add Supplement" else "Edit Supplement") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val entity = SupplementEntity(
                        id = initialSupplement?.id ?: 0,
                        profileId = profileId,
                        name = name,
                        dosage = dosage,
                        note = note,
                        icon = icon,
                        color = color,
                        intervalType = intervalType,
                        intervalData = intervalData,
                        startDate = initialSupplement?.startDate ?: System.currentTimeMillis(),
                        endDate = initialSupplement?.endDate,
                        isActive = true
                    )
                    viewModel.saveSupplement(entity, onSave)
                },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Icon: ")
                Text(icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                // TODO: Show icon picker dialog
                Spacer(Modifier.width(16.dp))
                Text("Color:")
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color(color))
                )
                // TODO: Show color picker dialog
            }
            Spacer(Modifier.height(16.dp))
            Text("Reminder Type")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = intervalType == "DAILY",
                    onClick = { intervalType = "DAILY"; intervalData = "08:00" }
                )
                Text("Daily")
                Spacer(Modifier.width(8.dp))
                RadioButton(
                    selected = intervalType == "PRN",
                    onClick = { intervalType = "PRN"; intervalData = "" }
                )
                Text("As needed")
            }
            // TODO: Allow setting time(s) for reminders, or "as needed"
        }
    }
}