package se.ejp.niltalk2.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.ejp.niltalk2.crypto.LibsodiumCryptoProvider

class CryptoTestViewModel : ViewModel() {

    var testResult by mutableStateOf<TestResult?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    fun runCryptoTest() {
        isRunning = true
        testResult = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Initialize libsodium first
                LibsodiumCryptoProvider.initialize()

                val crypto = LibsodiumCryptoProvider()

                // Test 1: Generate keys
                val aliceKeys = crypto.generateKeyPair()
                val bobKeys = crypto.generateKeyPair()

            // Test 2: Encrypt a message
            val originalMessage = "Hello, secure world! 🔒"
            val ciphertext = crypto.encryptAndSign(
                message = originalMessage,
                recipientPublicKey = bobKeys.publicKey,
                senderPrivateKey = aliceKeys.privateKey
            )

            // Test 3: Decrypt the message
            val decryptedMessage = crypto.decryptAndVerify(
                ciphertext = ciphertext,
                senderPublicKey = aliceKeys.publicKey,
                recipientPrivateKey = bobKeys.privateKey
            )

            // Verify result
            val success = originalMessage == decryptedMessage

            testResult = TestResult(
                success = success,
                originalMessage = originalMessage,
                decryptedMessage = decryptedMessage,
                ciphertextSize = ciphertext.size,
                alicePublicKeyFingerprint = aliceKeys.publicKey.take(8)
                    .joinToString("") { byte -> byte.toInt().and(0xFF).toString(16).padStart(2, '0') },
                bobPublicKeyFingerprint = bobKeys.publicKey.take(8)
                    .joinToString("") { byte -> byte.toInt().and(0xFF).toString(16).padStart(2, '0') }
            )
            } catch (e: Exception) {
                testResult = TestResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            } finally {
                isRunning = false
            }
        }
    }
}

data class TestResult(
    val success: Boolean,
    val originalMessage: String = "",
    val decryptedMessage: String = "",
    val ciphertextSize: Int = 0,
    val alicePublicKeyFingerprint: String = "",
    val bobPublicKeyFingerprint: String = "",
    val error: String? = null
)

@Composable
fun CryptoTestScreen(
    modifier: Modifier = Modifier,
    viewModel: CryptoTestViewModel = viewModel { CryptoTestViewModel() }
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Libsodium Crypto Test",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { viewModel.runCryptoTest() },
                enabled = !viewModel.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (viewModel.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running Test...")
                } else {
                    Text("Run Encryption/Decryption Test")
                }
            }

            viewModel.testResult?.let { result ->
                Spacer(Modifier.height(24.dp))

                TestResultCard(result)
            }
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.success)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (result.success) "Test Passed ✓" else "Test Failed ✗",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (result.success)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (result.success) {
                TestDetailRow("Original Message", result.originalMessage)
                TestDetailRow("Decrypted Message", result.decryptedMessage)
                TestDetailRow("Ciphertext Size", "${result.ciphertextSize} bytes")
                TestDetailRow("Alice's Public Key", result.alicePublicKeyFingerprint, true)
                TestDetailRow("Bob's Public Key", result.bobPublicKeyFingerprint, true)

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "✓ crypto_box authenticated encryption working correctly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "Error: ${result.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TestDetailRow(label: String, value: String, monospace: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

