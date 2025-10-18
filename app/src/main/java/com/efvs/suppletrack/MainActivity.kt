package com.efvs.suppletrack

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

data class Profile(val name: String)
data class Supplement(val name: String, var taken: Boolean = false)
data class IntakeHistory(val date: LocalDate, val supplement: String, val taken: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuppleTrackTheme {
                AppRoot()
            }
        }
    }
}

enum class MainScreen(val label: String, val icon: ImageVector) {
    Checklist("Checkliste", Icons.AutoMirrored.Filled.List), // Icons.Filled.Checklist ersetzt durch List
    Calendar("Kalender", Icons.Filled.DateRange),
    Profiles("Profile", Icons.Filled.Person),
    //Export("Export/Backup", Icons.Filled.Warning),
    //Widget("Widget", Icons.Filled.AddCircle),
    Settings("Einstellungen", Icons.Filled.Settings)
}

@Composable
fun AppRoot() {
    // Simulierte Datenhaltung (ersetzbar durch ViewModel/Room)
    var profiles by remember { mutableStateOf(listOf(Profile("Ich"))) }
    var selectedProfile by remember { mutableStateOf(profiles.first()) }
    var supplements by remember { mutableStateOf(
        listOf(
            Supplement("Creatin"),
        )
    )}
    var intakeHistory by remember { mutableStateOf(listOf<IntakeHistory>()) }
    var selectedScreen by remember { mutableStateOf(MainScreen.Checklist) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                MainScreen.Checklist -> ChecklistScreen(
                    supplements = supplements,
                    onToggle = { idx, checked ->
                        supplements = supplements.mapIndexed { i, s ->
                            if (i == idx) s.copy(taken = checked) else s
                        }
                        intakeHistory = intakeHistory + IntakeHistory(
                            LocalDate.now(),
                            supplements[idx].name,
                            checked
                        )
                    },
                    profile = selectedProfile
                )
                MainScreen.Calendar -> CalendarScreen(intakeHistory)
                MainScreen.Profiles -> ProfilesScreen(
                    profiles = profiles,
                    selected = selectedProfile,
                    onSelect = { selectedProfile = it },
                    onAdd = { name ->
                        if (name.isNotBlank() && profiles.none { it.name == name }) {
                            profiles = profiles + Profile(name)
                        }
                    },
                    onDelete = { profile ->
                        if (profiles.size > 1) {
                            profiles = profiles.filter { it != profile }
                            if (selectedProfile == profile) selectedProfile = profiles.first()
                        }
                    }
                )
                /*
                MainScreen.Export -> ExportScreen {
                    Toast.makeText(context, "Exportiert (Demo)", Toast.LENGTH_SHORT).show()
                }
                MainScreen.Widget -> WidgetScreen()
                 */
                MainScreen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
fun ChecklistScreen(
    supplements: List<Supplement>,
    onToggle: (Int, Boolean) -> Unit,
    profile: Profile
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Tages-Checkliste für ${profile.name}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (supplements.isEmpty()) {
            Text("Keine Supplements/Medikamente hinterlegt.")
        } else {
            LazyColumn {
                items(supplements.withIndex().toList()) { (idx, supp) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = supp.taken,
                            onCheckedChange = { onToggle(idx, it) }
                        )
                        Text(supp.name, fontWeight = if (supp.taken) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(intakeHistory: List<IntakeHistory>) {
    val today = LocalDate.now()
    val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Kalender-Übersicht", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        days.forEach { day ->
            val takenCount = intakeHistory.count { it.date == day && it.taken }
            val total = intakeHistory.count { it.date == day }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(day.toString(), modifier = Modifier.width(110.dp))
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(120.dp)
                        .background(
                            if (total > 0 && takenCount == total) Color(0xFF81C784)
                            else if (takenCount > 0) Color(0xFFFFF176)
                            else Color(0xFFE57373)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text("$takenCount/$total")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Grün: alles genommen, Gelb: teilweise, Rot: nichts")
    }
}

@Composable
fun ProfilesScreen(
    profiles: List<Profile>,
    selected: Profile,
    onSelect: (Profile) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Profile) -> Unit
) {
    var newProfile by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(profiles) { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(profile) }
                        .background(if (profile == selected) Color(0xFFE3F2FD) else Color.Transparent)
                        .padding(8.dp)
                ) {
                    Text(profile.name, fontWeight = if (profile == selected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    if (profiles.size > 1) {
                        IconButton(onClick = { onDelete(profile) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newProfile,
                onValueChange = { newProfile = it },
                label = { Text("Neues Profil") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                onAdd(newProfile.trim())
                newProfile = ""
            }) {
                Text("Hinzufügen")
            }
        }
    }
}

@Composable
fun ExportScreen(onExport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Export & Backup", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExport) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Exportieren (Demo)")
        }
        Spacer(Modifier.height(16.dp))
        Text("Exportiere deine Einnahme-Historie oder sichere deine Daten in Google Drive.")
    }
}

@Composable
fun WidgetScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Widget", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Füge das SuppleTrack-Widget zu deinem Homescreen hinzu, um die Tages-Checkliste schnell zu sehen.")
    }
}

@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("Deutsch") }
    val languages = listOf("Deutsch", "English")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Mode", modifier = Modifier.weight(1f))
            Switch(
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Benachrichtigungen", modifier = Modifier.weight(1f))
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("Sprache")
        DropdownMenuBox(
            selected = language,
            options = languages,
            onSelected = { language = it }
        )
        Spacer(Modifier.height(32.dp))
        Text("Passe Benachrichtigungen, Barrierefreiheit, Sprache und mehr an.")
    }
}

@Composable
fun DropdownMenuBox(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SuppleTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
