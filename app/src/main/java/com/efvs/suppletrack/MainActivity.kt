package com.efvs.suppletrack

import android.app.*
import android.content.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

// Compose Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit

enum class DoseType { MEDICATION, SUPPLEMENT }
enum class DoseStatus { TAKEN, SKIPPED, MISSED }

data class DoseSchedule(
    val times: List<LocalTime> = listOf(LocalTime.of(8,0)),
    val recurrenceDays: List<Int> = (0..6).toList(),
    val durationDays: Int? = null
)

data class DoseItem(
    val id: Int,
    var name: String,
    var dosage: String,
    var type: DoseType,
    var schedule: DoseSchedule,
    var adherenceLog: MutableList<DoseLog> = mutableListOf()
)

data class DoseLog(
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus,
    val reason: String? = null
)

class MainActivity : ComponentActivity() {
    private lateinit var doseTakenBroadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Standardmäßig Englisch
            var language by remember { mutableStateOf(AppLanguage.EN) }
            var darkMode by remember { mutableStateOf(true) }
            SuppleTrackTheme(darkMode) {
                AppRoot(
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it },
                    language = language,
                    onLanguageChange = { language = it }
                )
            }
        }

        // Notification Channel für Reminder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "suppletrack_reminder",
                "SuppleTrack Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Receiver für "DoseTaken" registrieren
        doseTakenBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val doseId = intent.getIntExtra("doseId", -1)
                val doseTime = intent.getStringExtra("doseTime")
                // Sende einen lokalen Broadcast, damit Compose die Einnahme als "genommen" markieren kann
                val uiIntent = Intent("com.efvs.suppletrack.DOSE_TAKEN_UI").apply {
                    putExtra("doseId", doseId)
                    putExtra("doseTime", doseTime)
                }
                sendBroadcast(uiIntent)
            }
        }
        // Fix: Setze das Flag für nicht exportierten Receiver
        registerReceiver(
            doseTakenBroadcastReceiver,
            IntentFilter("com.efvs.suppletrack.DOSE_TAKEN"),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(doseTakenBroadcastReceiver)
    }
}

enum class MainScreen(val labelKey: String, val icon: ImageVector) {
    Checklist("checklist", Icons.AutoMirrored.Filled.List),
    Calendar("calendar", Icons.Filled.Star),
    Manage("manage", Icons.Filled.Edit),
    Settings("settings", Icons.Filled.Settings)
}

@Composable
fun AppRoot(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    // Simulierte Datenhaltung (ersetzbar durch ViewModel/Room)
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
                )
            )
        )
    ) }
    var selectedScreen by remember { mutableStateOf(MainScreen.Checklist) }
    val context = LocalContext.current

    // Reminder: Nur einmal pro Änderung planen
    LaunchedEffect(doseItems.size) {
        scheduleMissedDoseNotifications(context, doseItems)
    }

    // Broadcast-Receiver für UI-Update
    val doseTakenUiReceiver = rememberUpdatedState(newValue = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val doseId = intent.getIntExtra("doseId", -1)
            val doseTime = intent.getStringExtra("doseTime")
            val today = LocalDate.now()
            val idx = doseItems.indexOfFirst { it.id == doseId }
            if (idx != -1) {
                val item = doseItems[idx]
                val time = doseTime?.let { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }
                if (time != null && item.adherenceLog.none { it.date == today && it.time == time && it.status == DoseStatus.TAKEN }) {
                    item.adherenceLog.add(DoseLog(today, time, DoseStatus.TAKEN, null))
                    doseItems = doseItems.toMutableList()
                }
            }
        }
    })

    DisposableEffect(Unit) {
        val filter = IntentFilter("com.efvs.suppletrack.DOSE_TAKEN_UI")
        // Fix: Setze das Flag für nicht exportierten Receiver
        context.registerReceiver(doseTakenUiReceiver.value, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(doseTakenUiReceiver.value)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = tr(screen.labelKey, language)) },
                        label = { Text(tr(screen.labelKey, language)) },
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
                        doseItems = doseItems.toMutableList()
                    },
                    language = language
                )
                MainScreen.Manage -> DoseManageScreen(
                    doseItems = doseItems,
                    onEdit = { idx, updated -> doseItems[idx] = updated; doseItems = doseItems.toMutableList() },
                    onDelete = { idx -> doseItems.removeAt(idx); doseItems = doseItems.toMutableList() },
                    onAdd = { doseItems.add(it); doseItems = doseItems.toMutableList() },
                    language = language
                )
                MainScreen.Calendar -> DoseCalendarScreen(doseItems, language)
                MainScreen.Settings -> SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    notificationsEnabled = true,
                    onNotificationsChange = {},
                    language = language,
                    onLanguageChange = onLanguageChange
                )
            }
        }
    }
}

// Übersicht mit Checkbox für "genommen" und Rückgängig
@Composable
fun DoseChecklistScreen(
    doseItems: List<DoseItem>,
    onLogIntake: (Int, DoseStatus, String?) -> Unit,
    language: AppLanguage
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(tr("checklist", language), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(doseItems.withIndex().toList()) { (idx, item) ->
                val todayLog = item.adherenceLog.findLast { it.date == LocalDate.now() }
                val checked = todayLog?.status == DoseStatus.TAKEN
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            // Toggle beim Klick auf das Card-Item
                            if (checked) {
                                item.adherenceLog.removeIf { it.date == LocalDate.now() && it.status == DoseStatus.TAKEN }
                            } else {
                                onLogIntake(idx, DoseStatus.TAKEN, null)
                            }
                        }
                ) {
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
                                    item.adherenceLog.removeIf { it.date == LocalDate.now() && it.status == DoseStatus.TAKEN }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.name} (${item.dosage})", fontWeight = FontWeight.Bold)
                            Text(tr(if (item.type == DoseType.MEDICATION) "medication" else "supplement", language))
                            Text(tr("schedule", language) + ": " + item.schedule.times.joinToString { it.format(DateTimeFormatter.ofPattern("HH:mm")) })
                        }
                    }
                }
            }
        }
    }
}

// NEU: Verwalten-Screen für Hinzufügen/Bearbeiten/Löschen
@Composable
fun DoseManageScreen(
    doseItems: List<DoseItem>,
    onEdit: (Int, DoseItem) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: (DoseItem) -> Unit,
    language: AppLanguage
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editIdx by remember { mutableStateOf<Int?>(null) }
    var deleteIdx by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(tr("manage", language), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        // Großer, breiter Button mit Plus-Icon von links nach rechts
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = tr("add", language))
            Spacer(Modifier.width(12.dp))
            Text(tr("add", language), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(doseItems.withIndex().toList()) { (idx, item) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.name} (${item.dosage})", fontWeight = FontWeight.Bold)
                            Text(tr(if (item.type == DoseType.MEDICATION) "medication" else "supplement", language))
                            Text(tr("schedule", language) + ": " + item.schedule.times.joinToString { it.format(DateTimeFormatter.ofPattern("HH:mm")) })
                        }
                        IconButton(onClick = { editIdx = idx }) {
                            Icon(Icons.Default.Edit, contentDescription = tr("edit", language))
                        }
                        Spacer(Modifier.width(8.dp))
                        // Löschen als Trashcan-Icon
                        IconButton(onClick = { deleteIdx = idx }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("delete", language))
                        }
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        DoseEditDialog(onDismiss = { showAddDialog = false }, onSave = { onAdd(it); showAddDialog = false }, language = language)
    }
    if (editIdx != null) {
        DoseEditDialog(
            doseItem = doseItems[editIdx!!],
            onDismiss = { editIdx = null },
            onSave = { onEdit(editIdx!!, it); editIdx = null },
            language = language
        )
    }
    if (deleteIdx != null) {
        AlertDialog(
            onDismissRequest = { deleteIdx = null },
            title = { Text(tr("delete_confirm", language)) },
            confirmButton = {
                Button(onClick = { onDelete(deleteIdx!!); deleteIdx = null }) { Text(tr("yes", language)) }
            },
            dismissButton = {
                Button(onClick = { deleteIdx = null }) { Text(tr("no", language)) }
            }
        )
    }
}

// Dialog für Hinzufügen/Bearbeiten (RefillThreshold entfernt)
@Composable
fun DoseEditDialog(
    doseItem: DoseItem? = null,
    onDismiss: () -> Unit,
    onSave: (DoseItem) -> Unit,
    language: AppLanguage
) {
    var name by remember { mutableStateOf(TextFieldValue(doseItem?.name ?: "")) }
    var dosage by remember { mutableStateOf(TextFieldValue(doseItem?.dosage ?: "")) }
    var type by remember { mutableStateOf(doseItem?.type ?: DoseType.SUPPLEMENT) }
    var time by remember { mutableStateOf(doseItem?.schedule?.times?.firstOrNull() ?: LocalTime.of(8,0)) }
    var recurrenceDays by remember { mutableStateOf(doseItem?.schedule?.recurrenceDays ?: (0..6).toList()) }

    val context = LocalContext.current

    val isDaily = recurrenceDays.size == 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doseItem == null) tr("add_item", language) else tr("edit_item", language)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("name", language)) })
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text(tr("dose", language)) })
                Row {
                    Text(tr("type", language) + ":")
                    Spacer(Modifier.width(8.dp))
                    DropdownMenuBox(
                        selected = tr(if (type == DoseType.MEDICATION) "medication" else "supplement", language),
                        options = listOf(tr("medication", language), tr("supplement", language)),
                        onSelected = { type = if (it == tr("medication", language)) DoseType.MEDICATION else DoseType.SUPPLEMENT }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("schedule", language) + ":")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour: Int, minute: Int ->
                                    time = LocalTime.of(hour, minute)
                                },
                                time.hour,
                                time.minute,
                                android.text.format.DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit time")
                        Spacer(Modifier.width(8.dp))
                        Text(tr("edit", language))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("repeat", language) + ":")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDaily,
                        onCheckedChange = { checked ->
                            recurrenceDays = if (checked) (0..6).toList() else listOf()
                        }
                    )
                    Text(tr("repeat_daily", language))
                }
                // Nur anzeigen, wenn NICHT täglich
                if (!isDaily) {
                    Spacer(Modifier.height(8.dp))
                    Column {
                        val daysOfWeek = listOf(
                            tr("mo", language), tr("tu", language), tr("we", language),
                            tr("th", language), tr("fr", language), tr("sa", language), tr("su", language)
                        )
                        (0..6).forEach { dayIdx ->
                            val selected = recurrenceDays.contains(dayIdx)
                            Row {
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        recurrenceDays = if (selected) recurrenceDays - dayIdx else recurrenceDays + dayIdx
                                    },
                                    label = { Text(daysOfWeek[dayIdx]) },
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = doseItem?.id ?: (0..100000).random()
                val schedule = DoseSchedule(times = listOf(time), recurrenceDays = recurrenceDays.sorted())
                onSave(DoseItem(
                    id = id,
                    name = name.text,
                    dosage = dosage.text,
                    type = type,
                    schedule = schedule
                ))
            }) { Text(tr("confirm", language)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(tr("cancel", language)) } }
    )
}

// Einfacher Kalender mit Adherence-Score und Detail
@Composable
fun DoseCalendarScreen(doseItems: List<DoseItem>, language: AppLanguage) {
    var viewMode by remember { mutableStateOf("Monat") }
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var currentWeekStart by remember { mutableStateOf(today.with(java.time.DayOfWeek.MONDAY)) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(tr("calendar", language), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        // Ersetze SegmentedButton durch zwei OutlinedButtons
        Row {
            OutlinedButton(
                onClick = { viewMode = "Woche" },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (viewMode == "Woche") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Woche")
            }
            OutlinedButton(
                onClick = { viewMode = "Monat" },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (viewMode == "Monat") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Monat")
            }
        }
        // Navigation (Zurück, Text, Weiter)
        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    if (viewMode == "Monat") currentMonth = currentMonth.minusMonths(1)
                    else currentWeekStart = currentWeekStart.minusWeeks(1)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Zurück")
                }
                Spacer(Modifier.width(8.dp))
                if (viewMode == "Monat") {
                    Text(
                        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                } else {
                    val weekEnd = currentWeekStart.plusDays(6)
                    Text(
                        "${currentWeekStart.format(DateTimeFormatter.ofPattern("dd.MM."))} - ${weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (viewMode == "Monat") currentMonth = currentMonth.plusMonths(1)
                    else currentWeekStart = currentWeekStart.plusWeeks(1)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Vor")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Kalender
        if (viewMode == "Monat") {
            MonthCalendar(
                monthStart = currentMonth,
                doseItems = doseItems,
                today = today,
                onDayClick = { selectedDay = it }
            )
        } else {
            WeekCalendar(
                weekStart = currentWeekStart,
                doseItems = doseItems,
                today = today,
                onDayClick = { selectedDay = it }
            )
        }
        Spacer(Modifier.height(16.dp))
        // "Auf heute springen" Button
        Button(
            onClick = {
                if (viewMode == "Monat") currentMonth = today.withDayOfMonth(1)
                else currentWeekStart = today.with(java.time.DayOfWeek.MONDAY)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Star, contentDescription = "Heute")
            Spacer(Modifier.width(8.dp))
            Text(tr("today", language))
        }
        val totalTaken = doseItems.flatMap { it.adherenceLog }.count { it.status == DoseStatus.TAKEN }
        val totalScheduled = doseItems.flatMap { it.adherenceLog }.size
        val adherence = if (totalScheduled > 0) (totalTaken * 100 / totalScheduled) else 0
        Text(tr("adherence", language) + ": $adherence%", style = MaterialTheme.typography.titleMedium)
        if (selectedDay != null) {
            val logs = doseItems.flatMap { it.adherenceLog.filter { log -> log.date == selectedDay } }
            AlertDialog(
                onDismissRequest = { selectedDay = null },
                title = { Text("${tr("details_for", language)} ${selectedDay!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}") },
                text = {
                    Column {
                        logs.forEach {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (it.status) {
                                        DoseStatus.TAKEN -> Icons.Default.CheckCircle
                                        DoseStatus.SKIPPED -> Icons.Default.Info
                                        DoseStatus.MISSED -> Icons.Default.Warning
                                    },
                                    contentDescription = null,
                                    tint = when (it.status) {
                                        DoseStatus.TAKEN -> Color(0xFF81C784)
                                        DoseStatus.SKIPPED -> Color(0xFFFFF176)
                                        DoseStatus.MISSED -> Color(0xFFE57373)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${it.time.format(DateTimeFormatter.ofPattern("HH:mm"))}: ${tr(it.status.name.lowercase(), language)} ${it.reason ?: ""}")
                            }
                        }
                        if (logs.isEmpty()) Text(tr("no_entries", language))
                    }
                },
                confirmButton = { Button(onClick = { selectedDay = null }) { Text(tr("cancel", language)) } }
            )
        }
    }
}

// Monatsansicht mit optimierten Farben für Dark Mode und grünem "Heute" bei vollständiger Einnahme
@Composable
fun MonthCalendar(
    monthStart: LocalDate,
    doseItems: List<DoseItem>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val daysInMonth = monthStart.lengthOfMonth()
    val firstDay = monthStart
    val firstWeekDay = firstDay.dayOfWeek.value % 7 // Montag=1
    val gridDays = mutableListOf<LocalDate?>()
    for (i in 1..firstWeekDay) gridDays.add(null)
    for (i in 0 until daysInMonth) gridDays.add(firstDay.plusDays(i.toLong()))
    while (gridDays.size % 7 != 0) gridDays.add(null)
    val darkTakenColor = Color(0xFF43A047) // kräftiges Grün für Dark Mode
    val darkPartialColor = Color(0xFFFFC107) // kräftiges Gelb
    val darkMissedColor = Color(0xFFD32F2F) // kräftiges Rot
    val darkEmptyColor = Color(0xFF424242) // dunkles Grau

    Column {
        Row {
            listOf("Mo","Di","Mi","Do","Fr","Sa","So").forEach {
                Text(it, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
        }
        for (week in gridDays.chunked(7)) {
            Row(
                modifier = Modifier.padding(vertical = 0.dp) // Abstand zwischen Reihen entfernt
            ) {
                week.forEach { day ->
                    val isToday = day == today
                    val logs = day?.let { doseItems.flatMap { it.adherenceLog.filter { log -> log.date == day } } } ?: emptyList()
                    val taken = logs.count { it.status == DoseStatus.TAKEN }
                    val total = logs.size
                    val allTakenToday = isToday && total > 0 && taken == total
                    val color = when {
                        total == 0 -> darkEmptyColor
                        taken == total -> if (isToday) darkTakenColor else darkTakenColor
                        taken > 0 -> darkPartialColor
                        else -> darkMissedColor
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(
                                when {
                                    allTakenToday -> darkTakenColor // heute alles genommen: grün
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else -> color
                                }
                            )
                            .clickable(enabled = day != null) { day?.let { onDayClick(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.dayOfMonth.toString(), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                if (isToday) Icon(Icons.Default.Star, contentDescription = "Heute", tint = MaterialTheme.colorScheme.primary)
                                if (total > 0) {
                                    Icon(
                                        when {
                                            taken == total -> Icons.Default.CheckCircle
                                            taken > 0 -> Icons.Default.Info
                                            else -> Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            taken == total -> darkTakenColor
                                            taken > 0 -> darkPartialColor
                                            else -> darkMissedColor
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Wochenansicht mit optimierten Farben für Dark Mode und grünem "Heute" bei vollständiger Einnahme
@Composable
fun WeekCalendar(
    weekStart: LocalDate,
    doseItems: List<DoseItem>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val darkTakenColor = Color(0xFF43A047)
    val darkPartialColor = Color(0xFFFFC107)
    val darkMissedColor = Color(0xFFD32F2F)
    val darkEmptyColor = Color(0xFF424242)
    Row {
        days.forEach { day ->
            val isToday = day == today
            val logs = doseItems.flatMap { it.adherenceLog.filter { log -> log.date == day } }
            val taken = logs.count { it.status == DoseStatus.TAKEN }
            val total = logs.size
            val allTakenToday = isToday && total > 0 && taken == total
            val color = when {
                total == 0 -> darkEmptyColor
                taken == total -> if (isToday) darkTakenColor else darkTakenColor
                taken > 0 -> darkPartialColor
                else -> darkMissedColor
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.8f)
                    .padding(4.dp)
                    .background(
                        when {
                            allTakenToday -> darkTakenColor
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> color
                        }
                    )
                    .clickable { onDayClick(day) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(day.dayOfWeek.name.take(2), fontWeight = FontWeight.Bold)
                Text(day.dayOfMonth.toString(), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                if (isToday) Icon(Icons.Default.Star, contentDescription = "Heute", tint = MaterialTheme.colorScheme.primary)
                if (total > 0) {
                    Icon(
                        when {
                            taken == total -> Icons.Default.CheckCircle
                            taken > 0 -> Icons.Default.Info
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            taken == total -> darkTakenColor
                            taken > 0 -> darkPartialColor
                            else -> darkMissedColor
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val languages = listOf(
        AppLanguage.EN to "English",
        AppLanguage.DE to "Deutsch",
        AppLanguage.ES to "Español",
        AppLanguage.FR to "Français"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(tr("settings", language), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("darkmode", language), modifier = Modifier.weight(1f))
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
            Text(tr("notifications", language), modifier = Modifier.weight(1f))
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(tr("language", language))
        DropdownMenuBox(
            selected = languages.first { it.first == language }.second,
            options = languages.map { it.second },
            onSelected = { selected ->
                val lang = languages.firstOrNull { it.second == selected }?.first ?: AppLanguage.EN
                onLanguageChange(lang)
            }
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
    // Optimierte Dark-Theme-Farben für besseren Kontrast
    val darkColors = darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF0D47A1),
        secondary = Color(0xFF80CBC4),
        onSecondary = Color(0xFF004D40),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF232323),
        onSurface = Color(0xFFE0E0E0),
        error = Color(0xFFCF6679),
        onError = Color(0xFF000000)
    )
    val colors = if (darkTheme) darkColors else lightColorScheme()
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

// Sprachschlüssel für Übersetzungen
enum class AppLanguage(val code: String) {
    EN("en"), DE("de"), ES("es"), FR("fr")
}

// Zentrale Übersetzungsfunktion
fun tr(key: String, lang: AppLanguage): String {
    return when (lang) {
        AppLanguage.EN -> when (key) {
            "settings" -> "Settings"
            "checklist" -> "Checklist"
            "calendar" -> "Calendar"
            "manage" -> "Manage"
            "add" -> "Add"
            "edit" -> "Edit"
            "delete" -> "Delete"
            "taken" -> "Taken"
            "skipped" -> "Skipped"
            "missed" -> "Missed"
            "medication" -> "Medication"
            "supplement" -> "Supplement"
            "dose" -> "Dose"
            "schedule" -> "Schedule"
            "today" -> "Today"
            "language" -> "Language"
            "darkmode" -> "Dark Mode"
            "notifications" -> "Notifications"
            "confirm" -> "Confirm"
            "cancel" -> "Cancel"
            "add_item" -> "Add Item"
            "edit_item" -> "Edit Item"
            "delete_confirm" -> "Really delete?"
            "yes" -> "Yes"
            "no" -> "No"
            "export" -> "Export"
            "profile" -> "Profile"
            "new_profile" -> "New Profile"
            "adherence" -> "Adherence"
            "details_for" -> "Details for"
            "add_time" -> "Add time"
            "repeat" -> "Repeat"
            "repeat_daily" -> "Repeat daily"
            "mo" -> "Mo"
            "tu" -> "Tu"
            "we" -> "We"
            "th" -> "Th"
            "fr" -> "Fr"
            "sa" -> "Sa"
            "su" -> "Su"
            else -> key
        }
        AppLanguage.DE -> when (key) {
            "settings" -> "Einstellungen"
            "checklist" -> "Checkliste"
            "calendar" -> "Kalender"
            "manage" -> "Verwalten"
            "add" -> "Hinzufügen"
            "edit" -> "Bearbeiten"
            "delete" -> "Löschen"
            "taken" -> "Genommen"
            "skipped" -> "Übersprungen"
            "missed" -> "Verpasst"
            "medication" -> "Medikament"
            "supplement" -> "Supplement"
            "dose" -> "Dosierung"
            "schedule" -> "Zeitplan"
            "today" -> "Heute"
            "language" -> "Sprache"
            "darkmode" -> "Dark Mode"
            "notifications" -> "Benachrichtigungen"
            "confirm" -> "Bestätigen"
            "cancel" -> "Abbrechen"
            "add_item" -> "Neues Item"
            "edit_item" -> "Bearbeiten"
            "delete_confirm" -> "Wirklich löschen?"
            "yes" -> "Ja"
            "no" -> "Nein"
            "export" -> "Export"
            "profile" -> "Profil"
            "new_profile" -> "Neues Profil"
            "adherence" -> "Adherence"
            "details_for" -> "Details für"
            "add_time" -> "Zeit hinzufügen"
            "repeat" -> "Wiederholen"
            "repeat_daily" -> "Täglich"
            "mo" -> "Mo"
            "tu" -> "Di"
            "we" -> "Mi"
            "th" -> "Do"
            "fr" -> "Fr"
            "sa" -> "Sa"
            "su" -> "So"
            else -> key
        }
        AppLanguage.ES -> when (key) {
            "settings" -> "Configuración"
            "checklist" -> "Lista"
            "calendar" -> "Calendario"
            "manage" -> "Gestionar"
            "add" -> "Añadir"
            "edit" -> "Editar"
            "delete" -> "Eliminar"
            "taken" -> "Tomado"
            "skipped" -> "Saltado"
            "missed" -> "Perdido"
            "medication" -> "Medicamento"
            "supplement" -> "Suplemento"
            "dose" -> "Dosis"
            "schedule" -> "Horario"
            "today" -> "Hoy"
            "language" -> "Idioma"
            "darkmode" -> "Modo oscuro"
            "notifications" -> "Notificaciones"
            "confirm" -> "Confirmar"
            "cancel" -> "Cancelar"
            "add_item" -> "Añadir elemento"
            "edit_item" -> "Editar elemento"
            "delete_confirm" -> "¿Eliminar realmente?"
            "yes" -> "Sí"
            "no" -> "No"
            "export" -> "Exportar"
            "profile" -> "Perfil"
            "new_profile" -> "Nuevo perfil"
            "adherence" -> "Adherencia"
            "details_for" -> "Detalles para"
            "add_time" -> "Añadir hora"
            "repeat" -> "Repetir"
            "repeat_daily" -> "Diario"
            "mo" -> "Lu"
            "tu" -> "Ma"
            "we" -> "Mi"
            "th" -> "Ju"
            "fr" -> "Vi"
            "sa" -> "Sa"
            "su" -> "Do"
            else -> key
        }
        AppLanguage.FR -> when (key) {
            "settings" -> "Paramètres"
            "checklist" -> "Liste"
            "calendar" -> "Calendrier"
            "manage" -> "Gérer"
            "add" -> "Ajouter"
            "edit" -> "Modifier"
            "delete" -> "Supprimer"
            "taken" -> "Pris"
            "skipped" -> "Sauté"
            "missed" -> "Manqué"
            "medication" -> "Médicament"
            "supplement" -> "Supplément"
            "dose" -> "Dose"
            "schedule" -> "Horaire"
            "today" -> "Aujourd'hui"
            "language" -> "Langue"
            "darkmode" -> "Mode sombre"
            "notifications" -> "Notifications"
            "confirm" -> "Confirmer"
            "cancel" -> "Annuler"
            "add_item" -> "Ajouter un élément"
            "edit_item" -> "Modifier l'élément"
            "delete_confirm" -> "Vraiment supprimer?"
            "yes" -> "Oui"
            "no" -> "Non"
            "export" -> "Exporter"
            "profile" -> "Profil"
            "new_profile" -> "Nouveau profil"
            "adherence" -> "Adhérence"
            "details_for" -> "Détails pour"
            "add_time" -> "Ajouter heure"
            "repeat" -> "Répéter"
            "repeat_daily" -> "Quotidien"
            "mo" -> "Lu"
            "tu" -> "Ma"
            "we" -> "Me"
            "th" -> "Je"
            "fr" -> "Ve"
            "sa" -> "Sa"
            "su" -> "Di"
            else -> key
        }
    }
}

// Reminder für verpasste Einnahmen (nur EIN Timer pro DoseItem, Android Standard)
fun scheduleMissedDoseNotifications(context: Context, doseItems: List<DoseItem>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    doseItems.forEach { item ->
        // Nur die erste Zeit im Schedule verwenden
        val time = item.schedule.times.firstOrNull() ?: return@forEach
        val today = LocalDate.now()
        val alreadyTaken = item.adherenceLog.any { it.date == today && it.time == time && it.status == DoseStatus.TAKEN }
        if (!alreadyTaken) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, today.year)
                set(Calendar.MONTH, today.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, today.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) return@forEach
            val intent = Intent(context, MissedDoseReceiver::class.java).apply {
                putExtra("doseId", item.id)
                putExtra("doseName", item.name)
                putExtra("doseTime", time.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id * 1000 + time.hour * 100 + time.minute,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Die Permission android.permission.SCHEDULE_EXACT_ALARM fehlt im Manifest!
                // <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
                e.printStackTrace()
            }
        }
    }
}

class MissedDoseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getIntExtra("doseId", -1)
        val doseName = intent.getStringExtra("doseName") ?: ""
        val doseTime = intent.getStringExtra("doseTime") ?: ""
        val takenIntent = Intent(context, DoseTakenReceiver::class.java).apply {
            putExtra("doseId", doseId)
            putExtra("doseTime", doseTime)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            doseId * 100000,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, "suppletrack_reminder")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Missed Dose Reminder")
            .setContentText("Did you take $doseName at $doseTime?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(android.R.drawable.checkbox_on_background, "Taken", takenPendingIntent)
        NotificationManagerCompat.from(context).notify(doseId, builder.build())
    }
}

class DoseTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getIntExtra("doseId", -1)
        val doseTime = intent.getStringExtra("doseTime")
        NotificationManagerCompat.from(context).cancel(doseId)
        // Markiere die Einnahme als "genommen" in der UI via Broadcast
        val uiIntent = Intent("com.efvs.suppletrack.DOSE_TAKEN_UI").apply {
            putExtra("doseId", doseId)
            putExtra("doseTime", doseTime)
        }
        context.sendBroadcast(uiIntent)
    }
}
