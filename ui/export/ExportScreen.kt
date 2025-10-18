package com.efvs.suppletrack.ui.export

// ...other imports...
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload

@Composable
fun ExportScreen(
    profile: ProfileEntity,
    supplements: List<SupplementEntity>,
    intakes: List<IntakeEntity>,
    viewModel: ExportViewModel,
    onBack: () -> Unit
) {
    // ...other state...
    var showSuccess by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    // ...
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Backup") },
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
                .padding(16.dp)
        ) {
            Text("Export your intake data for ${profile.name}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    exporting = true
                    viewModel.exportIntakesAsCsv(
                        profile,
                        supplements,
                        intakes
                    ) { path ->
                        exportResult = path
                        exporting = false
                        showSuccess = true
                        successMsg = "CSV exported!"
                    }
                },
                enabled = !exporting,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export as CSV")
            }
            // ...PDF, Drive, etc...
            if (showSuccess) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSuccess = false }) { Text("OK") }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(successMsg)
                }
            }
        }
    }
}