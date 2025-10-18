package com.efvs.suppletrack.ui.supplement

// ... imports ...
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.efvs.suppletrack.notifications.ReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SupplementEditViewModel @Inject constructor(
    application: Application,
    private val supplementUseCases: SupplementUseCases
) : AndroidViewModel(application) {
    // ...existing code...

    fun saveSupplementWithReminder(
        supplement: SupplementEntity,
        profileId: Long,
        nextIntakeTime: Long?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = if (supplement.id == 0L) {
                supplementUseCases.insertSupplement(supplement)
            } else {
                supplementUseCases.updateSupplement(supplement)
                supplement.id
            }
            // Schedule reminder if needed
            if (nextIntakeTime != null) {
                ReminderWorker.scheduleReminder(
                    getApplication(),
                    id,
                    profileId,
                    nextIntakeTime
                )
            }
            onComplete()
        }
    }
}