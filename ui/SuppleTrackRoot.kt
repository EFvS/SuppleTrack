package com.efvs.suppletrack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.efvs.suppletrack.ui.navigation.NavGraph
import com.efvs.suppletrack.ui.theme.SuppleTrackTheme

@Composable
fun SuppleTrackRoot() {
    SuppleTrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavGraph()
        }
    }
}