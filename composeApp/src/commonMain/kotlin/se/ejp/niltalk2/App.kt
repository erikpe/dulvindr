package se.ejp.niltalk2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import se.ejp.niltalk2.ui.screen.CreateIdentityScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        CreateIdentityScreen()
    }
}