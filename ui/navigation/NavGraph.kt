package com.efvs.suppletrack.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.efvs.suppletrack.data.local.ProfileEntity
import com.efvs.suppletrack.data.local.SupplementEntity
import com.efvs.suppletrack.data.local.IntakeEntity
import com.efvs.suppletrack.ui.calendar.IntakeCalendarScreen
import com.efvs.suppletrack.ui.calendar.IntakeCalendarViewModel
import com.efvs.suppletrack.ui.export.ExportScreen
import com.efvs.suppletrack.ui.export.ExportViewModel
import com.efvs.suppletrack.ui.intake.IntakeChecklistScreen
import com.efvs.suppletrack.ui.intake.IntakeChecklistViewModel
import com.efvs.suppletrack.ui.main.MainScreen
import com.efvs.suppletrack.ui.main.MainViewModel
import com.efvs.suppletrack.ui.onboarding.OnboardingScreen
import com.efvs.suppletrack.ui.settings.SettingsScreen
import com.efvs.suppletrack.ui.settings.SettingsViewModel
import com.efvs.suppletrack.ui.supplement.SupplementEditScreen
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    var selectedProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var supplementToEdit by remember { mutableStateOf<SupplementEntity?>(null) }
    val mainViewModel: MainViewModel = hiltViewModel()
    val intakeChecklistViewModel: IntakeChecklistViewModel = hiltViewModel()
    val calendarViewModel: IntakeCalendarViewModel = hiltViewModel()
    val exportViewModel: ExportViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var showCalendar by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var calendarSelectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showExport by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                onContinue = { profile: ProfileEntity ->
                    selectedProfile = profile
                    mainViewModel.setActiveProfile(profile)
                }
            )
            if (selectedProfile != null) {
                LaunchedEffect(Unit) {
                    navController.navigate("main")
                }
            }
        }
        composable("main") {
            selectedProfile?.let { profile ->
                MainScreen(
                    profile = profile,
                    viewModel = mainViewModel,
                    onAddSupplement = {
                        supplementToEdit = null
                        navController.navigate("supplementEdit")
                    },
                    onOpenSupplement = { supp ->
                        supplementToEdit = supp
                        navController.navigate("supplementEdit")
                    },
                    onGoToCalendar = {
                        showCalendar = true
                        calendarMonth = YearMonth.now()
                        calendarSelectedDate = LocalDate.now()
                        calendarViewModel.loadIntakesForMonth(profile.id, calendarMonth.year, calendarMonth.monthValue)
                        navController.navigate("calendar")
                    },
                    onGoToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
        }
        composable("supplementEdit") {
            selectedProfile?.let { profile ->
                SupplementEditScreen(
                    profileId = profile.id,
                    initialSupplement = supplementToEdit,
                    onSave = {
                        navController.popBackStack()
                    }
                )
            }
        }
        composable("intakeChecklist") {
            selectedProfile?.let { profile ->
                LaunchedEffect(profile.id) {
                    intakeChecklistViewModel.loadSupplements(profile.id)
                    intakeChecklistViewModel.loadIntakesForToday(profile.id)
                }
                val supplements by intakeChecklistViewModel.supplements.collectAsState()
                val intakes by intakeChecklistViewModel.intakes.collectAsState()
                IntakeChecklistScreen(
                    profile = profile,
                    supplements = supplements,
                    intakes = intakes,
                    onToggleIntake = {
                        intakeChecklistViewModel.toggleIntake(it)
                    },
                    onAddPRNIntake = {
                        intakeChecklistViewModel.addPRNIntake(profile.id, it.id)
                    }
                )
            }
        }
        composable("calendar") {
            selectedProfile?.let { profile ->
                val calendarIntakes by calendarViewModel.calendarIntakes.collectAsState()
                IntakeCalendarScreen(
                    profile = profile,
                    calendarIntakes = calendarIntakes,
                    selectedDate = calendarSelectedDate,
                    onDateSelected = { calendarSelectedDate = it },
                    onMonthChanged = { newMonth ->
                        calendarMonth = calendarMonth.withMonth(newMonth)
                        calendarViewModel.loadIntakesForMonth(profile.id, calendarMonth.year, calendarMonth.monthValue)
                    }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("export") {
            selectedProfile?.let { profile ->
                // For simplicity, reload supplements and intakes here
                val supplements by mainViewModel.supplements.collectAsState()
                val intakes by intakeChecklistViewModel.intakes.collectAsState()
                ExportScreen(
                    profile = profile,
                    supplements = supplements,
                    intakes = intakes,
                    viewModel = exportViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}