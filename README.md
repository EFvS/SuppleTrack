# SuppleTrack

**SuppleTrack** is a modern, privacy-focused Android app to help you (and your family) track supplement and medication intake.  
It supports reminders, intake history, daily checklists, calendar, multi-profile, export/backup, and a home screen widget—all with a minimal, accessible UI.

---

## Features

- **Medikament/Supplement-Tracker:** Verwalte alle Einnahmen als DoseItem (Name, Dosierung, Typ, Zeitplan, Inventar, Adherence-Log).
- **Inventar & Refill:** Pillenzähler, automatische Abzüge, Warnung bei niedrigem Bestand.
- **Flexible Zeitpläne:** Tägliche, mehrtägige, Wochentage, Intervall, temporäre Einnahme.
- **Erinnerungen:** Push-Benachrichtigungen mit Aktionen (Genommen, Überspringen, Snooze).
- **Adherence-Log:** Status (Genommen, Übersprungen, Verpasst) mit Zeitstempel und Grund.
- **Kalender-Übersicht:** Visualisierung der Einnahmehistorie, Detailansicht pro Tag.
- **Adherence-Score:** Prozentuale Anzeige der Einnahme-Quote pro Woche/Monat.
- **Export:** CSV/JSON-Export der Adherence-Daten für beliebigen Zeitraum.
- **Verwalten-Reiter:** Hinzufügen, Bearbeiten und Löschen von Medikamenten/Supplements erfolgt in einem eigenen Tab.
- **Multiple profiles:** Track intake for yourself and family.
- **Flexible reminders:** Actionable notifications; cross off from lock screen.
- **As-needed tracking:** Log intakes even when not scheduled.
- **Daily checklist:** See today's supplements/meds, cross off as you go.
- **Calendar overview:** Visualize adherence, tap any day for details.
- **Accessibility:** Text scaling, colorblind mode, dark/light themes.
- **Export & backup:** Export intake history as CSV/PDF; backup/restore via Google Drive.
- **Widget:** Home screen widget for today's checklist.
- **Offline-first:** No login required; optional PIN/biometric lock.
- **Localization:** English (default), others easily added.

---

## Getting Started

1. **Clone or download the repo.**
2. Open in [Android Studio](https://developer.android.com/studio).
3. Sync Gradle and run on an emulator or device.
   - Falls Fehler bei der Gradle-Synchronisation auftreten (z.B. Dependency-Fehler), prüfe die `build.gradle`-Dateien auf korrekte Syntax und Versionsangaben.
4. On first launch, follow the onboarding flow to set up your first profile.

---

## Screenshots

<!-- Add screenshots here when available -->

---

## Architecture

- **Kotlin + Jetpack Compose (Material3)**
- **Room** for local storage
- **DataStore** for settings
- **WorkManager** for reminders
- **Hilt** for DI
- **MVVM + Use Cases**
- **Google Drive API** for optional backup/export

---

## Build Customization

- For Google Drive backup, set up your own OAuth credentials ([docs](https://developers.google.com/drive)).
- For more languages, extend string resources in `/res/values-xx/strings.xml`.

---

## Contributing

Pull requests are welcome! Please follow best practices and include tests where possible.

---

## License

MIT

---

### Contact

Created by [EFvS](https://github.com/EFvS).  
For questions, feedback, or feature requests, open an issue or PR on GitHub.