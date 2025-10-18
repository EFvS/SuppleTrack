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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class DoseType { MEDICATION, SUPPLEMENT }
enum class DoseStatus { TAKEN, SKIPPED, MISSED }

data class DoseSchedule(
    val times: List<LocalTime> = listOf(LocalTime.of(8,0)),
    val recurrenceDays: List<Int> = (0..6).toList(), // 0=Montag
    val durationDays: Int? = null // z.B. 7 Tage
)

// RefillThreshold entfernt aus DoseItem
data class DoseItem(
    val id: Int,
    var name: String,
    var dosage: String,
    var type: DoseType,
    var schedule: DoseSchedule,
    var inventory: Int,
    var adherenceLog: MutableList<DoseLog> = mutableListOf()
)

data class DoseLog(
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    val reason: String? = null
)

data class Profile(val name: String)
data class Supplement(val name: String, var taken: Boolean = false)
data class IntakeHistory(val date: LocalDate, val supplement: String, val taken: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            var language by remember { mutableStateOf("Deutsch") }
            SuppleTrackTheme(darkMode) {
                AppRoot(
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it },
                    language = language,
                    onLanguageChange = { language = it }
                )
            }
        }
    }
}

enum class MainScreen(val label: String, val icon: ImageVector) {
    Checklist("Checkliste", Icons.AutoMirrored.Filled.List), // Icons.Filled.Checklist ersetzt durch List
    Calendar("Kalender", Icons.Filled.DateRange),
    //Profiles("Profile", Icons.Filled.Person),
    //Export("Export/Backup", Icons.Filled.Warning),
    //Widget("Widget", Icons.Filled.AddCircle),
    Settings("Einstellungen", Icons.Filled.Settings)
}

@Composable
fun AppRoot(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    // Simulierte Datenhaltung (ersetzbar durch ViewModel/Room)
    var profiles by remember { mutableStateOf(listOf(Profile("Ich"))) }
    var selectedProfile by remember { mutableStateOf(profiles.first()) }
    var supplements by remember { mutableStateOf(
        listOf(
            Supplement("Creatin"),
        )
    )}
    var intakeHistory by remember { mutableStateOf(listOf<IntakeHistory>()) }
    var doseItems by remember { mutableStateOf(
        mutableListOf(
            DoseItem(
                id = 1,
                name = "Creatin",
                dosage = "3 g",
                type = DoseType.SUPPLEMENT,
                schedule = DoseSchedule(
                    times = listOf(LocalTime.of(8,0)),
                    recurrenceDays = (0..6).toList()
                ),
                inventory = 30
            )
        )
    ) }
    var selectedScreen by remember { mutableStateOf(MainScreen.Checklist) }
    var exportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Refill-Benachrichtigung entfernt
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
                MainScreen.Checklist -> DoseChecklistScreen(
                    doseItems = doseItems,
                    onLogIntake = { idx, status, reason ->
                        val item = doseItems[idx]
                        val now = LocalTime.now()
                        item.adherenceLog.add(DoseLog(LocalDate.now(), now, status, reason))
                        if (status == DoseStatus.TAKEN) {
                            item.inventory = (item.inventory - 1).coerceAtLeast(0)
                        }
                        doseItems = doseItems.toMutableList()
                    },
                    onEdit = { idx, updated -> doseItems[idx] = updated; doseItems = doseItems.toMutableList() },
                    onDelete = { idx -> doseItems.removeAt(idx); doseItems = doseItems.toMutableList() },
                    onAdd = { doseItems.add(it); doseItems = doseItems.toMutableList() }
                )
                MainScreen.Calendar -> DoseCalendarScreen(doseItems)
                MainScreen.Settings -> SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    notificationsEnabled = true,
                    onNotificationsChange = {},
                    language = language,
                    onLanguageChange = onLanguageChange
                )
            }
            if (exportDialog) {
                ExportDialog(doseItems = doseItems, onDismiss = { exportDialog = false })
            }
        }
    }
}

// Übersicht mit Checkbox für "genommen" und Rückgängig
@Composable
fun DoseChecklistScreen(
    doseItems: List<DoseItem>,
    onLogIntake: (Int, DoseStatus, String?) -> Unit,
    onEdit: (Int, DoseItem) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: (DoseItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editIdx by remember { mutableStateOf<Int?>(null) }
    var deleteIdx by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Tages-Checkliste", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { showAddDialog = true }) { Text("Neues Medikament/Supplement") }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(doseItems.withIndex().toList()) { (idx, item) ->
                val todayLog = item.adherenceLog.findLast { it.date == LocalDate.now() }
                val checked = todayLog?.status == DoseStatus.TAKEN
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    onLogIntake(idx, DoseStatus.TAKEN, null)
                                } else {
                                    // Rückgängig: Entferne heutigen Logeintrag
                                    item.adherenceLog.removeIf { it.date == LocalDate.now() && it.status == DoseStatus.TAKEN }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.name} (${item.dosage})", fontWeight = FontWeight.Bold)
                            Text(if (item.type == DoseType.MEDICATION) "Medikament" else "Supplement")
                            Text("Inventar: ${item.inventory}")
                            Text("Zeitplan: " + item.schedule.times.joinToString { it.format(DateTimeFormatter.ofPattern("HH:mm")) })
                        }
                        IconButton(onClick = { editIdx = idx }) {
                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { deleteIdx = idx }) { Text("Löschen") }
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        DoseEditDialog(onDismiss = { showAddDialog = false }, onSave = { onAdd(it); showAddDialog = false })
    }
    if (editIdx != null) {
        DoseEditDialog(
            doseItem = doseItems[editIdx!!],
            onDismiss = { editIdx = null },
            onSave = { onEdit(editIdx!!, it); editIdx = null }
        )
    }
    if (deleteIdx != null) {
        AlertDialog(
            onDismissRequest = { deleteIdx = null },
            title = { Text("Löschen bestätigen") },
            text = { Text("Wirklich löschen?") },
            confirmButton = {
                Button(onClick = { onDelete(deleteIdx!!); deleteIdx = null }) { Text("Ja") }
            },
            dismissButton = {
                Button(onClick = { deleteIdx = null }) { Text("Nein") }
            }
        )
    }
}

// Dialog für Hinzufügen/Bearbeiten (RefillThreshold entfernt)
@Composable
fun DoseEditDialog(
    doseItem: DoseItem? = null,
    onDismiss: () -> Unit,
    onSave: (DoseItem) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue(doseItem?.name ?: "")) }
    var dosage by remember { mutableStateOf(TextFieldValue(doseItem?.dosage ?: "")) }
    var type by remember { mutableStateOf(doseItem?.type ?: DoseType.SUPPLEMENT) }
    var inventory by remember { mutableStateOf(doseItem?.inventory?.toString() ?: "30") }
    var time by remember { mutableStateOf(doseItem?.schedule?.times?.firstOrNull()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "08:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doseItem == null) "Neues Item" else "Bearbeiten") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosierung") })
                Row {
                    Text("Typ:")
                    Spacer(Modifier.width(8.dp))
                    DropdownMenuBox(
                        selected = if (type == DoseType.MEDICATION) "Medikament" else "Supplement",
                        options = listOf("Medikament", "Supplement"),
                        onSelected = { type = if (it == "Medikament") DoseType.MEDICATION else DoseType.SUPPLEMENT }
                    )
                }
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Uhrzeit (HH:mm)") })
                OutlinedTextField(value = inventory, onValueChange = { inventory = it }, label = { Text("Inventar") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = doseItem?.id ?: (0..100000).random()
                val schedule = DoseSchedule(times = listOf(LocalTime.parse(time)))
                onSave(DoseItem(
                    id = id,
                    name = name.text,
                    dosage = dosage.text,
                    type = type,
                    schedule = schedule,
                    inventory = inventory.toIntOrNull() ?: 0
                ))
            }) { Text("Speichern") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

// Kalender mit Adherence-Score und Detail
@Composable
fun DoseCalendarScreen(doseItems: List<DoseItem>) {
    var viewMode by remember { mutableStateOf("Monat") } // "Woche" oder "Monat"
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var currentWeekStart by remember { mutableStateOf(today.with(java.time.DayOfWeek.MONDAY)) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Kalender-Übersicht", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ansicht:")
            Spacer(Modifier.width(8.dp))
            SegmentedButton(
                options = listOf("Woche", "Monat"),
                selected = viewMode,
                onSelected = { viewMode = it }
            )
            Spacer(Modifier.weight(1f))
            if (viewMode == "Monat") {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Vorheriger Monat")
                }
                Text(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Nächster Monat")
                }
            } else {
                IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Vorherige Woche")
                }
                val weekEnd = currentWeekStart.plusDays(6)
                Text("${currentWeekStart.format(DateTimeFormatter.ofPattern("dd.MM."))} - ${weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
                IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Nächste Woche")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (viewMode == "Monat") {
            MonthCalendar(
                monthStart = currentMonth,
                doseItems = doseItems,
                onDayClick = { selectedDay = it }
            )
        } else {
            WeekCalendar(
                weekStart = currentWeekStart,
                doseItems = doseItems,
                onDayClick = { selectedDay = it }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Grün: alles genommen, Gelb: teilweise, Rot: nichts, Grau: keine Einträge")
        Spacer(Modifier.height(16.dp))
        val totalTaken = doseItems.flatMap { it.adherenceLog }.count { it.status == DoseStatus.TAKEN }
        val totalScheduled = doseItems.flatMap { it.adherenceLog }.size
        val adherence = if (totalScheduled > 0) (totalTaken * 100 / totalScheduled) else 0
        Text("Adherence: $adherence%")
        if (selectedDay != null) {
            val logs = doseItems.flatMap { it.adherenceLog.filter { log -> log.date == selectedDay } }
            AlertDialog(
                onDismissRequest = { selectedDay = null },
                title = { Text("Details für ${selectedDay!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}") },
                text = {
                    Column {
                        logs.forEach {
                            Text("${it.time.format(DateTimeFormatter.ofPattern("HH:mm"))}: ${it.status} ${it.reason ?: ""}")
                        }
                        if (logs.isEmpty()) Text("Keine Einträge.")
                    }
                },
                confirmButton = { Button(onClick = { selectedDay = null }) { Text("Schließen") } }
            )
        }
    }
}

// Segmented Button für Wochen-/Monatswahl
@Composable
fun SegmentedButton(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row {
        options.forEach { option ->
            val selectedColor = if (option == selected) MaterialTheme.colorScheme.primary else Color.LightGray
            Button(
                onClick = { onSelected(option) },
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(option)
            }
        }
    }
}

// Monatsansicht: 6x7 Grid
@Composable
fun MonthCalendar(
    monthStart: LocalDate,
    doseItems: List<DoseItem>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfWeek = java.time.DayOfWeek.MONDAY
    val daysInMonth = monthStart.lengthOfMonth()
    val firstDay = monthStart
    val firstWeekDay = firstDay.dayOfWeek.value % 7 // Montag=1
    val gridDays = mutableListOf<LocalDate?>()
    for (i in 1..firstWeekDay) gridDays.add(null)
    for (i in 0 until daysInMonth) gridDays.add(firstDay.plusDays(i.toLong()))
    while (gridDays.size % 7 != 0) gridDays.add(null)
    Column {
        Row {
            listOf("Mo","Di","Mi","Do","Fr","Sa","So").forEach {
                Text(it, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
        }
        for (week in gridDays.chunked(7)) {
            Row {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .background(
                                day?.let {
                                    val logs = doseItems.flatMap { it.adherenceLog.filter { log -> log.date == day } }
                                    val taken = logs.count { it.status == DoseStatus.TAKEN }
                                    val total = logs.size
                                    when {
                                        total == 0 -> Color.LightGray
                                        taken == total -> Color(0xFF81C784)
                                        taken > 0 -> Color(0xFFFFF176)
                                        else -> Color(0xFFE57373)
                                    }
                                } ?: Color.Transparent
                            )
                            .clickable(enabled = day != null) { day?.let { onDayClick(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) Text(day.dayOfMonth.toString())
                    }
                }
            }
        }
    }
}

// Wochenansicht: 1x7
@Composable
fun WeekCalendar(
    weekStart: LocalDate,
    doseItems: List<DoseItem>,
    onDayClick: (LocalDate) -> Unit
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    Row {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.8f)
                    .padding(2.dp)
                    .background(
                        run {
                            val logs = doseItems.flatMap { it.adherenceLog.filter { log -> log.date == day } }
                            val taken = logs.count { it.status == DoseStatus.TAKEN }
                            val total = logs.size
                            when {
                                total == 0 -> Color.LightGray
                                taken == total -> Color(0xFF81C784)
                                taken > 0 -> Color(0xFFFFF176)
                                else -> Color(0xFFE57373)
                            }
                        }
                    )
                    .clickable { onDayClick(day) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(day.dayOfWeek.name.take(2), fontWeight = FontWeight.Bold)
                Text(day.dayOfMonth.toString())
            }
        }
    }
}

// Exportfunktion (Demo)
@Composable
fun ExportDialog(doseItems: List<DoseItem>, onDismiss: () -> Unit) {
    val csv = doseItems.flatMap { item ->
        item.adherenceLog.map {
            "${item.name},${item.dosage},${item.type},${it.date},${it.time},${it.status},${it.reason ?: ""}"
        }
    }.joinToString("\n")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export (CSV)") },
        text = { Text(csv.ifBlank { "Keine Daten." }) },
        confirmButton = { Button(onClick = onDismiss) { Text("Schließen") } }
    )
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
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf("Deutsch", "English")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            if (language == "English") "Settings" else "Einstellungen",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == "English") "Dark Mode" else "Dark Mode", modifier = Modifier.weight(1f))
            Switch(
                checked = darkMode,
                onCheckedChange = onDarkModeChange
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == "English") "Notifications" else "Benachrichtigungen", modifier = Modifier.weight(1f))
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(if (language == "English") "Language" else "Sprache")
        DropdownMenuBox(
            selected = language,
            options = languages,
            onSelected = onLanguageChange
        )
        Spacer(Modifier.height(32.dp))
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
fun SuppleTrackTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
