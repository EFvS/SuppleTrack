package com.efvs.suppletrack.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.efvs.suppletrack.data.local.ProfileEntity

@Composable
fun OnboardingScreen(
    onContinue: (ProfileEntity) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pageCount = 4
    var page by remember { mutableStateOf(0) }
    val profiles by viewModel.profiles.collectAsState()
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("💊") }
    var color by remember { mutableStateOf(0xFF2196F3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated progress dots
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            repeat(pageCount) { idx ->
                Box(
                    Modifier
                        .size(if (idx == page) 14.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (idx == page) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                )
                if (idx < pageCount - 1) Spacer(Modifier.width(6.dp))
            }
        }
        when (page) {
            0 -> {
                // Lottie welcome animation (put onboarding_welcome.json in assets/)
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("onboarding_welcome.json"))
                LottieAnimation(
                    composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(180.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Welcome to SuppleTrack!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("Track your daily supplements and medications for yourself or family.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
            }
            1 -> {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("reminder_bell.json"))
                LottieAnimation(
                    composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Smart Reminders", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Get notified when it's time to take your supplements or medications. Mark them as taken directly from the notification.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            }
            2 -> {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("backup_cloud.json"))
                LottieAnimation(
                    composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Backup & Export", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Export your data as CSV or PDF, and backup securely to Google Drive.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            }
            3 -> {
                Text("Set Up Your Profile", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Icon: ")
                    Text(icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Color:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addProfile(name.trim(), icon, color)
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Profile")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Existing Profiles:", style = MaterialTheme.typography.titleMedium)
                Column {
                    profiles.forEach { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onContinue(profile) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(profile.icon, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (profiles.isNotEmpty()) {
                    Button(
                        onClick = { onContinue(profiles.first()) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue") }
                }
            }
        }
    }
}