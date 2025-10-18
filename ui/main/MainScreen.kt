package com.efvs.suppletrack.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.efvs.suppletrack.data.local.ProfileEntity
import com.efvs.suppletrack.data.local.SupplementEntity

@Composable
fun MainScreen(
    profile: ProfileEntity,
    viewModel: MainViewModel,
    onAddSupplement: () -> Unit,
    onOpenSupplement: (SupplementEntity) -> Unit,
    onGoToCalendar: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val supplements by viewModel.supplements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${profile.icon}  ${profile.name}") },
                actions = {
                    IconButton(onClick = onGoToCalendar) {
                        Icon(Icons.Default.Add, contentDescription = "Calendar")
                    }
                    IconButton(onClick = onGoToSettings) {
                        Icon(Icons.Default.Add, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSupplement) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplement")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Supplements & Medications", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (supplements.isEmpty()) {
                Text(
                    "No supplements yet. Tap + to add.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LazyColumn {
                items(supplements.size) { i ->
                    val supp = supplements[i]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onOpenSupplement(supp) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(supp.icon, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(supp.name, style = MaterialTheme.typography.titleMedium)
                                supp.dosage?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteSupplement(supp) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete supplement")
                            }
                        }
                    }
                }
            }
        }
    }
}