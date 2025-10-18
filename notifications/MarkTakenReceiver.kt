package com.efvs.suppletrack.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.efvs.suppletrack.data.local.SuppleTrackDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MarkTakenReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intakeId = intent.getLongExtra("intake_id", -1L)
        if (intakeId == -1L) return
        GlobalScope.launch {
            val db = SuppleTrackDatabase.getInstance(context)
            val intakeDao = db.intakeDao()
            val intake = intakeDao.getIntakeById(intakeId)
            if (intake != null && !intake.taken) {
                intakeDao.updateIntake(intake.copy(taken = true, takenAt = System.currentTimeMillis()))
            }
        }
    }
}