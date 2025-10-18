package com.efvs.suppletrack.ui.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.efvs.suppletrack.R
import com.efvs.suppletrack.data.local.SuppleTrackDatabase
import java.text.SimpleDateFormat
import java.util.*

class IntakeWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return IntakeChecklistFactory(applicationContext)
    }
}

class IntakeChecklistFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var intakeItems: List<Item> = emptyList()

    data class Item(
        val supplementName: String,
        val intakeTime: Long,
        val taken: Boolean
    )

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // For demo, fetch today's intakes from Room (in production, consider using a repository)
        val db = SuppleTrackDatabase.getInstance(context)
        val profile = db.profileDao().getAllProfiles().blockingFirstOrNull()?.firstOrNull() ?: return
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrow = today + 24 * 60 * 60 * 1000
        val intakes = db.intakeDao().getIntakesForProfileInRange(profile.id, today, tomorrow).blockingFirstOrNull() ?: emptyList()
        val supplements = db.supplementDao().getSupplementsForProfile(profile.id).blockingFirstOrNull() ?: emptyList()
        val supplementMap = supplements.associateBy { it.id }
        intakeItems = intakes.map {
            Item(
                supplementName = supplementMap[it.supplementId]?.name ?: "Unknown",
                intakeTime = it.intakeTime,
                taken = it.taken
            )
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = intakeItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = intakeItems[position]
        val views = RemoteViews(context.packageName, R.layout.widget_intake_checklist_item)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_title, "${item.supplementName} (${timeFormat.format(Date(item.intakeTime))})")
        views.setImageViewResource(
            R.id.widget_item_status,
            if (item.taken) R.drawable.ic_check_circle else R.drawable.ic_circle
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}