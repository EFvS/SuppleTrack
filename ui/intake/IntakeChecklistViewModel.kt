package com.efvs.suppletrack.ui.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efvs.suppletrack.data.local.IntakeEntity
import com.efvs.suppletrack.data.local.SupplementEntity
import com.efvs.suppletrack.domain.usecase.IntakeUseCases
import com.efvs.suppletrack.domain.usecase.SupplementUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

@HiltViewModel
class IntakeChecklistViewModel @Inject constructor(
    private val supplementUseCases: SupplementUseCases,
    private val intakeUseCases: IntakeUseCases
) : ViewModel() {

    private val _supplements = MutableStateFlow<List<SupplementEntity>>(emptyList())
    val supplements: StateFlow<List<SupplementEntity>> = _supplements.asStateFlow()

    private val _intakes = MutableStateFlow<List<IntakeEntity>>(emptyList())
    val intakes: StateFlow<List<IntakeEntity>> = _intakes.asStateFlow()

    fun loadSupplements(profileId: Long) {
        viewModelScope.launch {
            supplementUseCases.getSupplementsForProfile(profileId).collect { list ->
                _supplements.value = list
            }
        }
    }

    fun loadIntakesForToday(profileId: Long) {
        // Get start and end of today in millis
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        viewModelScope.launch {
            intakeUseCases.getIntakesForProfileInRange(profileId, startOfDay, endOfDay).collect { items ->
                _intakes.value = items
            }
        }
    }

    fun toggleIntake(intake: IntakeEntity) {
        viewModelScope.launch {
            val updated = intake.copy(
                taken = !intake.taken,
                takenAt = if (!intake.taken) System.currentTimeMillis() else null
            )
            intakeUseCases.updateIntake(updated)
        }
    }

    fun addPRNIntake(profileId: Long, supplementId: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prnIntake = IntakeEntity(
                supplementId = supplementId,
                profileId = profileId,
                intakeTime = now,
                taken = true,
                takenAt = now
            )
            intakeUseCases.insertIntake(prnIntake)
        }
    }
}