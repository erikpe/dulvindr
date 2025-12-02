package se.ejp.dulvindr.crypto

/**
 * ANDROID KEY STORAGE ARCHITECTURE - USAGE EXAMPLES
 *
 * This file documents how to use the secure key storage system on Android.
 * It demonstrates the complete flow from key generation to encryption/decryption.
 *
 * ============================================================================
 * ARCHITECTURE OVERVIEW
 * ============================================================================
 *
 * 1. LibsodiumCryptoProvider - Generates X25519 keypairs for crypto_box operations
 * 2. AndroidKeystoreWrapper - Manages AES-256-GCM master key in Android Keystore
 * 3. SecureKeyManager - Combines both to securely wrap and store keypairs
 * 4. DataStore/File - Persists the encrypted keypair (not implemented yet - Part 4)
 *
 * ============================================================================
 * SECURITY PROPERTIES
 * ============================================================================
 *
 * - Private keys are NEVER stored in plaintext
 * - Master AES key is hardware-backed (when available) and non-extractable
 * - Uses AES-256-GCM for authenticated encryption (prevents tampering)
 * - Forward-compatible versioning system for future migrations
 * - No dependency on deprecated EncryptedSharedPreferences
 *
 * ============================================================================
 * EXAMPLE 1: FIRST-TIME KEY GENERATION AND STORAGE
 * ============================================================================
 */

suspend fun exampleGenerateAndStoreNewKeys() {
    // Initialize libsodium (must be done before any crypto operations)
    LibsodiumCryptoProvider.initialize()

    // Create the key manager
    val cryptoProvider = LibsodiumCryptoProvider()
    val secureKeyManager = SecureKeyManager(cryptoProvider)

    // Generate a new keypair and wrap the private key
    val serializedKeyPair = secureKeyManager.generateAndWrapKeyPair()

    // TODO (Part 4): Store in DataStore
    // Example with Proto DataStore:
    // dataStore.updateData { currentPrefs ->
    //     currentPrefs.toBuilder()
    //         .setPublicKey(serializedKeyPair.publicKeyBase64)
    //         .setEncryptedPrivateKey(serializedKeyPair.encryptedPrivateKeyBase64)
    //         .setIv(serializedKeyPair.ivBase64)
    //         .setVersion(serializedKeyPair.version)
    //         .build()
    // }

    // For now, you could use a simple JSON file or SharedPreferences
    // (Just don't use EncryptedSharedPreferences - it's deprecated!)

    println("Public Key: ${serializedKeyPair.publicKeyBase64}")
    println("Encrypted Private Key: ${serializedKeyPair.encryptedPrivateKeyBase64}")
    println("IV: ${serializedKeyPair.ivBase64}")
    println("Version: ${serializedKeyPair.version}")
}

/**
 * ============================================================================
 * EXAMPLE 2: LOADING AND USING STORED KEYS
 * ============================================================================
 */

suspend fun exampleLoadAndUseKeys(serializedKeyPair: SerializedKeyPair) {
    // Initialize libsodium
    LibsodiumCryptoProvider.initialize()

    // Create managers
    val cryptoProvider = LibsodiumCryptoProvider()
    val secureKeyManager = SecureKeyManager(cryptoProvider)

    // Unwrap the keypair (decrypts the private key using Keystore)
    val myKeyPair = secureKeyManager.unwrapKeyPair(serializedKeyPair)

    // Now you can use the keypair for encryption/decryption
    val recipientPublicKey = byteArrayOf(/* recipient's public key */)

    val encrypted = cryptoProvider.encryptAndSign(
        message = "Hello, secure world!",
        recipientPublicKey = recipientPublicKey,
        senderPrivateKey = myKeyPair.privateKey
    )

    println("Encrypted message: ${encrypted.joinToString()}")
}

/**
 * ============================================================================
 * EXAMPLE 3: COMPLETE END-TO-END ENCRYPTED MESSAGING
 * ============================================================================
 */

suspend fun exampleEndToEndEncryption() {
    // Initialize libsodium
    LibsodiumCryptoProvider.initialize()

    val cryptoProvider = LibsodiumCryptoProvider()
    val secureKeyManager = SecureKeyManager(cryptoProvider)

    // === Alice's side ===
    val aliceSerializedKeys = secureKeyManager.generateAndWrapKeyPair()
    val aliceKeyPair = secureKeyManager.unwrapKeyPair(aliceSerializedKeys)

    // === Bob's side ===
    val bobSerializedKeys = secureKeyManager.generateAndWrapKeyPair()
    val bobKeyPair = secureKeyManager.unwrapKeyPair(bobSerializedKeys)

    // === Alice sends a message to Bob ===
    val messageFromAlice = "Hi Bob! This is a secret message."

    val encryptedMessage = cryptoProvider.encryptAndSign(
        message = messageFromAlice,
        recipientPublicKey = bobKeyPair.publicKey,  // Bob's public key
        senderPrivateKey = aliceKeyPair.privateKey  // Alice's private key
    )

    // === Bob receives and decrypts Alice's message ===
    val decryptedMessage = cryptoProvider.decryptAndVerify(
        ciphertext = encryptedMessage,
        senderPublicKey = aliceKeyPair.publicKey,   // Alice's public key (for verification)
        recipientPrivateKey = bobKeyPair.privateKey // Bob's private key
    )

    println("Original: $messageFromAlice")
    println("Decrypted: $decryptedMessage")
    println("Match: ${messageFromAlice == decryptedMessage}")
}

/**
 * ============================================================================
 * EXAMPLE 4: KEY ROTATION / RE-WRAPPING
 * ============================================================================
 */

suspend fun exampleKeyRotation(oldSerializedKeys: SerializedKeyPair) {
    LibsodiumCryptoProvider.initialize()

    val cryptoProvider = LibsodiumCryptoProvider()

    // If the Keystore master key is compromised or you need to rotate:

    // 1. Unwrap with old Keystore key
    val oldKeystoreWrapper = AndroidKeystoreWrapper()
    val oldKeyManager = SecureKeyManager(cryptoProvider, oldKeystoreWrapper)
    val keyPair = oldKeyManager.unwrapKeyPair(oldSerializedKeys)

    // 2. Delete old Keystore key
    oldKeystoreWrapper.deleteWrapKey()

    // 3. Re-wrap with new Keystore key
    val newKeystoreWrapper = AndroidKeystoreWrapper()
    val newKeyManager = SecureKeyManager(cryptoProvider, newKeystoreWrapper)
    val newSerializedKeys = newKeyManager.wrapExistingKeyPair(keyPair)

    // 4. Store the new wrapped keys
    // ... save newSerializedKeys to storage ...

    println("Keys successfully rotated!")
}

/**
 * ============================================================================
 * EXAMPLE 5: ERROR HANDLING
 * ============================================================================
 */

suspend fun exampleErrorHandling() {
    try {
        LibsodiumCryptoProvider.initialize()

        val cryptoProvider = LibsodiumCryptoProvider()
        val secureKeyManager = SecureKeyManager(cryptoProvider)

        // Attempt to decrypt with wrong key
        val keyPair1 = cryptoProvider.generateKeyPair()
        val keyPair2 = cryptoProvider.generateKeyPair()

        val encrypted = cryptoProvider.encryptAndSign(
            message = "Secret",
            recipientPublicKey = keyPair1.publicKey,
            senderPrivateKey = keyPair1.privateKey
        )

        // This will throw CryptoException because we're using wrong keys
        val decrypted = cryptoProvider.decryptAndVerify(
            ciphertext = encrypted,
            senderPublicKey = keyPair2.publicKey,  // Wrong!
            recipientPrivateKey = keyPair2.privateKey  // Wrong!
        )

    } catch (e: CryptoException) {
        println("Crypto operation failed: ${e.message}")
        // Handle appropriately:
        // - Log the error
        // - Show user-friendly message
        // - Trigger key regeneration if needed
        // - Report to crash analytics
    }
}

/**
 * ============================================================================
 * INTEGRATION NOTES
 * ============================================================================
 *
 * INITIALIZATION:
 * - Call LibsodiumCryptoProvider.initialize() once at app startup
 * - This is a suspend function, call from a coroutine or suspend context
 * - Safe to call multiple times (idempotent)
 *
 * ACTIVITY/COMPOSABLE USAGE:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val keyManager = remember {
 *         SecureKeyManager(LibsodiumCryptoProvider())
 *     }
 *
 *     LaunchedEffect(Unit) {
 *         val keys = keyManager.generateAndWrapKeyPair()
 *         // Save to DataStore
 *     }
 * }
 * ```
 *
 * DEPENDENCY INJECTION (Recommended):
 * - Create a singleton CryptoProvider
 * - Create a singleton SecureKeyManager
 * - Inject where needed using Koin, Hilt, or manual DI
 *
 * TESTING:
 * - AndroidKeystoreWrapper requires a real Android device/emulator
 * - For unit tests, create a mock implementation
 * - For instrumented tests, use AndroidKeystoreWrapper directly
 *
 * NEXT STEPS (Parts 3-5):
 * Part 3: Implement DataStore for persistent storage
 * Part 4: Add key export/import for backup
 * Part 5: Add biometric authentication (optional)
 */

