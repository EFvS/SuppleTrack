package com.efvs.suppletrack.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efvs.suppletrack.data.local.ProfileEntity
import com.efvs.suppletrack.data.local.SupplementEntity
import com.efvs.suppletrack.domain.usecase.ProfileUseCases
import com.efvs.suppletrack.domain.usecase.SupplementUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileUseCases: ProfileUseCases,
    private val supplementUseCases: SupplementUseCases
) : ViewModel() {

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile.asStateFlow()

    private val _supplements = MutableStateFlow<List<SupplementEntity>>(emptyList())
    val supplements: StateFlow<List<SupplementEntity>> = _supplements.asStateFlow()

    fun setActiveProfile(profile: ProfileEntity) {
        _activeProfile.value = profile
        loadSupplements(profile.id)
    }

    private fun loadSupplements(profileId: Long) {
        viewModelScope.launch {
            supplementUseCases.getSupplementsForProfile(profileId).collect { list ->
                _supplements.value = list
            }
        }
    }

    fun addSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            supplementUseCases.insertSupplement(supplement)
        }
    }

    fun deleteSupplement(supplement: SupplementEntity) {
        viewModelScope.launch {
            supplementUseCases.deleteSupplement(supplement)
        }
    }
}