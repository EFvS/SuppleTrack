package com.efvs.suppletrack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.app.TimePickerDialog
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
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

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

        // Notification Channel erstellen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "suppletrack_reminder",
                "SuppleTrack Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

enum class MainScreen(val label: String, val icon: ImageVector) {
    Checklist("Checkliste", Icons.AutoMirrored.Filled.List),
    Calendar("Kalender", Icons.Filled.DateRange),
    Manage("Verwalten", Icons.Filled.Edit), // NEU: Reiter für Hinzufügen/Bearbeiten/Löschen
    Settings("Einstellungen", Icons.Filled.Settings)
}

@Composable
fun AppRoot(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
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
                )
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
                        icon = { Icon(screen.icon, contentDescription = tr(screen.label.lowercase(), language)) },
                        label = { Text(tr(screen.label.lowercase(), language)) },
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
            if (exportDialog) {
                ExportDialog(doseItems = doseItems, onDismiss = { exportDialog = false })
            }
        }
    }

    // Reminder planen, wenn DoseItems geändert werden
    LaunchedEffect(doseItems) {
        // scheduleSupplementReminders(context, doseItems) // Kommentar entfernt, da WorkManager entfernt wurde
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
    var times by remember { mutableStateOf(doseItem?.schedule?.times ?: listOf(LocalTime.of(8,0))) }
    var recurrenceDays by remember { mutableStateOf(doseItem?.schedule?.recurrenceDays ?: (0..6).toList()) }

    val context = LocalContext.current

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
                times.forEachIndexed { idx, t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.format(DateTimeFormatter.ofPattern("HH:mm")), modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            times = times.toMutableList().apply { removeAt(idx) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove time")
                        }
                    }
                }
                Button(
                    onClick = {
                        val now = LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour: Int, minute: Int ->
                                val newTime = LocalTime.of(hour, minute)
                                if (!times.contains(newTime)) times = times + newTime
                            },
                            now.hour,
                            now.minute,
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Add time")
                    Spacer(Modifier.width(8.dp))
                    Text(tr("add_time", language))
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("repeat", language) + ":")
                Row {
                    val daysOfWeek = listOf(
                        tr("mo", language), tr("tu", language), tr("we", language),
                        tr("th", language), tr("fr", language), tr("sa", language), tr("su", language)
                    )
                    (0..6).forEach { dayIdx ->
                        val selected = recurrenceDays.contains(dayIdx)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                recurrenceDays = if (selected) recurrenceDays - dayIdx else recurrenceDays + dayIdx
                            },
                            label = { Text(daysOfWeek[dayIdx]) },
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = recurrenceDays.size == 7,
                        onCheckedChange = { checked ->
                            recurrenceDays = if (checked) (0..6).toList() else listOf()
                        }
                    )
                    Text(tr("repeat_daily", language))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = doseItem?.id ?: (0..100000).random()
                val schedule = DoseSchedule(times = times.sorted(), recurrenceDays = recurrenceDays.sorted())
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
        // Intervall-Slider (Woche/Monat)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            SegmentedButton(
                options = listOf("Woche", "Monat"),
                selected = viewMode,
                onSelected = { viewMode = it }
            )
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
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Zurück")
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
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Vor")
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
            Row {
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

// Ersetze die fehlerhafte SegmentedButton-Funktion durch diese eigene Implementierung:
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
                Text(option, color = if (option == selected) Color.White else Color.Black)
            }
        }
    }
}

// Reminder-Worker für Push Notification (WorkManager entfernt, nur als Platzhalter belassen)
class SupplementReminderWorker(
    ctx: Context,
    params: Any // WorkerParameters entfernt
) /*: Worker(ctx, params)*/ {
    // Dummy-Implementierung, da WorkManager entfernt
    // fun doWork(): Result { ... }
}

// BroadcastReceiver zum Anzeigen der Notification und Markieren als genommen
class SupplementTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supplementId = intent.getIntExtra("supplementId", -1)
        val supplementName = intent.getStringExtra("supplementName") ?: ""
        // Notification mit "Taken"-Aktion
        val takenIntent = Intent(context, SupplementMarkTakenReceiver::class.java).apply {
            putExtra("supplementId", supplementId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            supplementId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = androidx.core.app.NotificationCompat.Builder(context, "suppletrack_reminder")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Supplement Reminder")
            .setContentText("Take: $supplementName")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Taken",
                takenPendingIntent
            )
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(supplementId, builder.build())
    }
}

// Receiver zum Markieren als genommen (hier nur Notification entfernen, Logik muss ergänzt werden)
class SupplementMarkTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supplementId = intent.getIntExtra("supplementId", -1)
        NotificationManagerCompat.from(context).cancel(supplementId)
        // TODO: DoseItem als genommen markieren (z.B. über Repository/Room/SharedPreferences)
    }
}

// Push Notification Reminder ohne WorkManager, mit Timer (Demo)
fun scheduleSupplementReminders(context: Context, doseItems: List<DoseItem>) {
    doseItems.forEach { item ->
        item.schedule.times.forEach { time ->
            val now = LocalTime.now()
            val today = LocalDate.now()
            val targetDateTime = today.atTime(time)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            val intent = Intent(context, SupplementTakenReceiver::class.java).apply {
                putExtra("supplementId", item.id)
                putExtra("supplementName", item.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // AlarmManager für Demo (in echten Apps: JobScheduler/ForegroundService für Zuverlässigkeit)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setExact(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
