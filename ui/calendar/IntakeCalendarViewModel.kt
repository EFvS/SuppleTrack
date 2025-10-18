package com.efvs.suppletrack.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efvs.suppletrack.data.local.IntakeEntity
import com.efvs.suppletrack.domain.usecase.IntakeUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class IntakeCalendarViewModel @Inject constructor(
    private val intakeUseCases: IntakeUseCases
) : ViewModel() {

    private val _calendarIntakes = MutableStateFlow<Map<LocalDate, List<IntakeEntity>>>(emptyMap())
    val calendarIntakes: StateFlow<Map<LocalDate, List<IntakeEntity>>> = _calendarIntakes.asStateFlow()

    fun loadIntakesForMonth(profileId: Long, year: Int, month: Int) {
        // Get first and last day of month in millis
        val zone = ZoneId.systemDefault()
        val start = LocalDate.of(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        viewModelScope.launch {
            intakeUseCases.getIntakesForProfileInRange(profileId, start, end).collect { items ->
                val grouped = items.groupBy {
                    LocalDate.ofEpochDay(it.intakeTime / (24 * 60 * 60 * 1000L))
                }
                _calendarIntakes.value = grouped
            }
        }
    }
}