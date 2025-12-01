package se.ejp.dulvindr.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.ejp.dulvindr.crypto.CryptoException
import se.ejp.dulvindr.crypto.CryptoProvider
import se.ejp.dulvindr.crypto.LibsodiumCryptoProvider
import se.ejp.dulvindr.model.Identity
import se.ejp.dulvindr.model.IdentityMetadata
import se.ejp.dulvindr.storage.IdentityStorageManager
import se.ejp.dulvindr.storage.StubIdentityStorage
import se.ejp.dulvindr.storage.StubSecureStorage
import se.ejp.dulvindr.ui.component.QrCodeImage

class CreateIdentityViewModel(
    private val cryptoProvider: CryptoProvider = LibsodiumCryptoProvider(),
    private val storageManager: IdentityStorageManager = IdentityStorageManager(
        identityStorage = StubIdentityStorage(),
        secureStorage = StubSecureStorage()
    )
) : ViewModel() {

    var name by mutableStateOf("")
        private set

    var identityMetadata by mutableStateOf<IdentityMetadata?>(null)
        private set

    var isCreating by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onNameChange(newName: String) {
        name = newName
        errorMessage = null
    }

    fun createIdentity() {
        if (name.isBlank()) return

        isCreating = true
        errorMessage = null

        // Run crypto operations in a background thread to avoid blocking UI
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Initialize libsodium first
                LibsodiumCryptoProvider.initialize()

                // Generate key pair
                val keyPair = cryptoProvider.generateKeyPair()
                val identity = Identity(
                    name = name.trim(),
                    keyPair = keyPair
                )

                // Convert to metadata and store
                val metadata = identity.toMetadata()

                // Store identity metadata and private key separately
                storageManager.saveIdentity(metadata, identity.keyPair.privateKey)

                // Update UI with metadata only (no private key in memory)
                identityMetadata = metadata
            } catch (e: CryptoException) {
                errorMessage = "Crypto error: ${e.message}"
            } catch (e: Exception) {
                errorMessage = "Failed to create identity: ${e.message}"
            } finally {
                isCreating = false
            }
        }
    }

    fun reset() {
        name = ""
        identityMetadata = null
        errorMessage = null
    }
}

@Composable
fun CreateIdentityScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateIdentityViewModel = viewModel { CreateIdentityViewModel() }
) {
    val identityMetadata = viewModel.identityMetadata

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (identityMetadata == null) {
            CreateIdentityForm(
                name = viewModel.name,
                onNameChange = viewModel::onNameChange,
                onCreateClick = viewModel::createIdentity,
                isCreating = viewModel.isCreating,
                errorMessage = viewModel.errorMessage,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            IdentityCreatedView(
                identityMetadata = identityMetadata,
                onCreateAnother = viewModel::reset,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun CreateIdentityForm(
    name: String,
    onNameChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    isCreating: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Your Identity",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            placeholder = { Text("Enter your name") },
            singleLine = true,
            enabled = !isCreating,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = onCreateClick,
            enabled = name.isNotBlank() && !isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Generate Keys & Create Identity")
            }
        }

        // Show error message if any
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Text(
            text = "A new cryptographic key pair will be generated for secure messaging",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun IdentityCreatedView(
    identityMetadata: IdentityMetadata,
    onCreateAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Identity Created!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // QR Code Card
        Card(
            modifier = Modifier
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan to Exchange Contact",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // QR Code
                val qrCodeData = identityMetadata.encodeForQRCode()

                QrCodeImage(
                    data = qrCodeData,
                    size = 250.dp,
                    modifier = Modifier.padding(8.dp)
                )

                Text(
                    text = "Share this QR code to exchange identities",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Identity ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = identityMetadata.id,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = identityMetadata.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Public Key Fingerprint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = identityMetadata.getPublicKeyFingerprint(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Full Public Key (hex)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SelectionContainer {
                    Text(
                        text = identityMetadata.publicKey.joinToString("") { byte ->
                            byte.toInt().and(0xFF).toString(16).padStart(2, '0')
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp),
                        lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5
                    )
                }

                Text(
                    text = "Key Length: ${identityMetadata.publicKey.size} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Your identity is securely stored. Private key is protected in secure storage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedButton(
            onClick = onCreateAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Create Another Identity")
        }
    }
}

