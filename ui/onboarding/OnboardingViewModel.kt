package com.efvs.suppletrack.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efvs.suppletrack.data.local.ProfileEntity
import com.efvs.suppletrack.domain.usecase.ProfileUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    val profiles: StateFlow<List<ProfileEntity>> = _profiles.asStateFlow()

    init {
        viewModelScope.launch {
            profileUseCases.getAllProfiles().collect { list ->
                _profiles.value = list
            }
        }
    }

    fun addProfile(name: String, icon: String, color: Long) {
        viewModelScope.launch {
            profileUseCases.insertProfile(
                ProfileEntity(
                    name = name,
                    icon = icon,
                    color = color
                )
            )
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            profileUseCases.deleteProfile(profile)
        }
    }
}