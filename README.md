# SuppleTrack

SuppleTrack is a privacy-focused Android app to track supplement and medication intake.  
It supports reminders (Android standard notifications with action button), intake history, daily checklists, calendar, multi-profile, export/backup, and a home screen widget.

---

## Features

- Supplement/Medication tracking (DoseItem: Name, Dosage, Type, Schedule, Adherence Log)
- Flexible schedules: daily, weekly, intervals, temporary
- Reminders: Push notifications for missed doses (Android standard, no WorkManager), mark as taken directly from notification
- **Requires permission:** `android.permission.SCHEDULE_EXACT_ALARM` in AndroidManifest.xml for exact reminders
- **Requires receiver registration:**  
  ```xml
  <receiver android:name=".MissedDoseReceiver" android:exported="false"/>
  <receiver android:name=".DoseTakenReceiver" android:exported="false"/>
  ```
- Adherence log: Status (Taken, Skipped, Missed) with timestamp and reason
- Calendar overview: Visualize intake history, details per day
- Adherence score: Percentage per week/month
- Export: CSV/JSON export for any period
- Manage tab: Add, edit, delete supplements/medications in a dedicated tab
- Multiple profiles
- Daily checklist
- Accessibility: Text scaling, colorblind mode, dark/light themes
- Export & backup: CSV/PDF export, Google Drive backup/restore
- Widget: Home screen checklist
- Offline-first, no login required
- Localization: English (default), German, Spanish, French

---

## Getting Started

1. Clone or download the repo.
2. Open in [Android Studio](https://developer.android.com/studio).
3. **Add to your AndroidManifest.xml:**  
   ```xml
   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
   <receiver android:name=".MissedDoseReceiver" android:exported="false"/>
   <receiver android:name=".DoseTakenReceiver" android:exported="false"/>
   ```
4. Sync Gradle and run on an emulator or device.
5. On first launch, follow onboarding to set up your first profile.

---

## Architecture

- Kotlin + Jetpack Compose (Material3)
- Room for local storage
- DataStore for settings
- AlarmManager for reminders (Android standard)
- MVVM + Use Cases
- Google Drive API for backup/export

---

## Contributing

Pull requests welcome! Please follow best practices and include tests.

---

## License

MIT

---

### Contact

Created by [EFvS](https://github.com/EFvS).  
For questions, feedback, or feature requests, open an issue or PR on GitHub.