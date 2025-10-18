// ...existing code...
private fun showIntakeNotification(name: String, dosage: String, icon: String, intakeId: Long) {
    val markTakenIntent = Intent(context, MarkTakenReceiver::class.java).apply {
        putExtra("intake_id", intakeId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, intakeId.toInt(), markTakenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, "suppletrack_reminders")
        .setSmallIcon(R.drawable.ic_pill)
        .setContentTitle("Time to take $name")
        .setContentText(if (dosage.isNotBlank()) "Dosage: $dosage" else "Tap to mark as taken")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(R.drawable.ic_check, "Mark as Taken", pendingIntent)
    NotificationManagerCompat.from(context).notify(intakeId.toInt(), builder.build())
}