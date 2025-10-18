// snippet for intake checkbox
IconButton(
    onClick = { onToggleIntake(intake) },
    modifier = Modifier
        .semantics { contentDescription = if (checked) "Taken" else "Not taken" }
        .testTag("intake_toggle_${supp.id}_${intake.id}")
        .minimumTouchTargetSize()
) {
    Icon(
        imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null // handled by semantics above
    )
}