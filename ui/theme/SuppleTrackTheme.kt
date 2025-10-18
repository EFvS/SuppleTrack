import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SuppleTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorBlindMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }
    val adjustedScheme = if (colorBlindMode) {
        colorScheme.copy(
            primary = Color(0xFF0066AA), // More distinguishable for colorblind users
            secondary = Color(0xFFB25C00)
        )
    } else colorScheme
    MaterialTheme(colorScheme = adjustedScheme, content = content)
}