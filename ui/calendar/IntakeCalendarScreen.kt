package com.efvs.suppletrack.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.efvs.suppletrack.data.local.IntakeEntity
import com.efvs.suppletrack.data.local.ProfileEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun IntakeCalendarScreen(
    profile: ProfileEntity,
    calendarIntakes: Map<LocalDate, List<IntakeEntity>>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (Int) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // TODO: Add month navigation buttons (prev/next)
        }
        Spacer(Modifier.height(16.dp))

        CalendarMonthGrid(
            yearMonth = currentMonth,
            calendarIntakes = calendarIntakes,
            selectedDate = selectedDate,
            onDateSelected = { date -> onDateSelected(date) }
        )

        Spacer(Modifier.height(24.dp))

        // Details for selected date
        val intakesForSelected = calendarIntakes[selectedDate].orEmpty()
        Text("Details for ${selectedDate.toString()}", style = MaterialTheme.typography.titleMedium)
        if (intakesForSelected.isEmpty()) {
            Text("No intakes logged.", color = MaterialTheme.colorScheme.secondary)
        } else {
            intakesForSelected.forEach { intake ->
                Text("- SupplementId: ${intake.supplementId} " +
                        if (intake.taken) "✓" else "✗")
                // TODO: Lookup supplement name, add details
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    yearMonth: YearMonth,
    calendarIntakes: Map<LocalDate, List<IntakeEntity>>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstWeekDay = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
    val daysInMonth = yearMonth.lengthOfMonth()
    val calendarDays = (1..daysInMonth).map { day -> yearMonth.atDay(day) }
    val weeks = mutableListOf<List<LocalDate?>>()
    var week = mutableListOf<LocalDate?>()
    // Fill initial empty days
    repeat(firstWeekDay) { week.add(null) }
    for (date in calendarDays) {
        week.add(date)
        if (week.size == 7) {
            weeks.add(week)
            week = mutableListOf()
        }
    }
    if (week.isNotEmpty()) {
        while (week.size < 7) week.add(null)
        weeks.add(week)
    }

    Column {
        // Days of week header
        Row {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) { Text(it, fontWeight = FontWeight.Bold) }
            }
        }
        weeks.forEach { weekDays ->
            Row {
                weekDays.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable(enabled = date != null) { date?.let { onDateSelected(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            // Color code: green for all taken, red for missed, yellow partial
                            val intakes = calendarIntakes[date].orEmpty()
                            val (bg, fg) = when {
                                intakes.isEmpty() -> Color.Transparent to Color.Unspecified
                                intakes.all { it.taken } -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
                                intakes.none { it.taken } -> Color(0xFFFFCDD2) to Color(0xFFC62828)
                                else -> Color(0xFFFFF9C4) to Color(0xFFF9A825)
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (date == selectedDate) Color(0xFF90CAF9) else bg,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = fg,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}