// ...imports...
import com.efvs.suppletrack.ui.security.LockScreen
import com.efvs.suppletrack.ui.settings.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()
            var unlocked by remember { mutableStateOf(!settings.pinEnabled) }
            if (!unlocked && settings.pinEnabled) {
                LockScreen(
                    onUnlock = { unlocked = true },
                    onFail = { finish() }
                )
            } else {
                SuppleTrackRoot()
            }
        }
    }
}