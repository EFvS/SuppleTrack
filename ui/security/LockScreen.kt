package com.efvs.suppletrack.ui.security

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onFail: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (activity != null) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS
            ) {
                val executor = activity.mainExecutor
                val prompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onUnlock()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            error = errString.toString()
                            onFail(errString.toString())
                        }
                        override fun onAuthenticationFailed() {
                            error = "Authentication failed"
                        }
                    })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock SuppleTrack")
                    .setSubtitle("Authenticate to continue")
                    .setNegativeButtonText("Cancel")
                    .build()
                prompt.authenticate(promptInfo)
            } else {
                error = "Biometric not available"
                onFail(error!!)
            }
        }
    }

    Surface {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Authentication Required", style = MaterialTheme.typography.titleLarge)
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}