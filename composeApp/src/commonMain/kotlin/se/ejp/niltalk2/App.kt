package se.ejp.niltalk2

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import se.ejp.niltalk2.ui.screen.CreateIdentityScreen
import se.ejp.niltalk2.ui.screen.CryptoTestScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Create Identity") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Test Crypto") }
                )
            }

            when (selectedTab) {
                0 -> CreateIdentityScreen()
                1 -> CryptoTestScreen()
            }
        }
    }
}