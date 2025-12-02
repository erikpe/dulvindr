package se.ejp.dulvindr.crypto

import android.util.Base64

/**
 * Manages secure storage and retrieval of libsodium keypairs on Android.
 *
 * This class combines:
 * 1. Libsodium keypair generation (from LibsodiumCryptoProvider)
 * 2. Android Keystore wrapping (from AndroidKeystoreWrapper)
 * 3. Serialization/deserialization for persistent storage
 *
 * Usage flow:
 * 1. Generate a new keypair: generateAndWrapKeyPair()
 * 2. Store the SerializedKeyPair in DataStore/file
 * 3. Later, retrieve from storage and unwrap: unwrapKeyPair(serializedKeyPair)
 */
class SecureKeyManager(
    private val cryptoProvider: CryptoProvider,
    private val keystoreWrapper: AndroidKeystoreWrapper = AndroidKeystoreWrapper()
) {

    /**
     * Generates a new libsodium keypair and wraps (encrypts) the private key
     * using Android Keystore.
     *
     * The public key is stored in plaintext (it's meant to be public).
     * The private key is encrypted with AES-GCM using a Keystore-protected key.
     *
     * @return SerializedKeyPair ready for persistent storage
     * @throws CryptoException if generation or wrapping fails
     */
    fun generateAndWrapKeyPair(): SerializedKeyPair {
        try {
            // Step 1: Generate libsodium keypair
            val keyPair = cryptoProvider.generateKeyPair()

            // Step 2: Wrap (encrypt) the private key using Keystore
            val encryptedPrivateKey = keystoreWrapper.encrypt(keyPair.privateKey)

            // Step 3: Serialize to Base64 for storage
            return SerializedKeyPair(
                publicKeyBase64 = Base64.encodeToString(
                    keyPair.publicKey,
                    Base64.NO_WRAP
                ),
                encryptedPrivateKeyBase64 = Base64.encodeToString(
                    encryptedPrivateKey.ciphertext,
                    Base64.NO_WRAP
                ),
                ivBase64 = Base64.encodeToString(
                    encryptedPrivateKey.iv,
                    Base64.NO_WRAP
                ),
                version = CURRENT_VERSION
            )
        } catch (e: Exception) {
            throw CryptoException("Failed to generate and wrap keypair: ${e.message}", e)
        }
    }

    /**
     * Unwraps (decrypts) a stored keypair to use for cryptographic operations.
     *
     * @param serializedKeyPair The encrypted keypair from storage
     * @return KeyPair ready for use with CryptoProvider operations
     * @throws CryptoException if unwrapping or deserialization fails
     */
    fun unwrapKeyPair(serializedKeyPair: SerializedKeyPair): KeyPair {
        try {
            // Verify version compatibility
            if (serializedKeyPair.version != CURRENT_VERSION) {
                throw CryptoException(
                    "Unsupported key version: ${serializedKeyPair.version}. " +
                    "Expected: $CURRENT_VERSION"
                )
            }

            // Deserialize from Base64
            val publicKey = Base64.decode(serializedKeyPair.publicKeyBase64, Base64.NO_WRAP)
            val encryptedPrivateKey = Base64.decode(
                serializedKeyPair.encryptedPrivateKeyBase64,
                Base64.NO_WRAP
            )
            val iv = Base64.decode(serializedKeyPair.ivBase64, Base64.NO_WRAP)

            // Unwrap (decrypt) the private key using Keystore
            val privateKey = keystoreWrapper.decrypt(
                EncryptedData(
                    ciphertext = encryptedPrivateKey,
                    iv = iv
                )
            )

            return KeyPair(
                publicKey = publicKey,
                privateKey = privateKey
            )
        } catch (e: Exception) {
            throw CryptoException("Failed to unwrap keypair: ${e.message}", e)
        }
    }

    /**
     * Wraps an existing keypair (useful for migration or import scenarios).
     *
     * @param keyPair The keypair to wrap
     * @return SerializedKeyPair ready for persistent storage
     * @throws CryptoException if wrapping fails
     */
    fun wrapExistingKeyPair(keyPair: KeyPair): SerializedKeyPair {
        try {
            val encryptedPrivateKey = keystoreWrapper.encrypt(keyPair.privateKey)

            return SerializedKeyPair(
                publicKeyBase64 = Base64.encodeToString(
                    keyPair.publicKey,
                    Base64.NO_WRAP
                ),
                encryptedPrivateKeyBase64 = Base64.encodeToString(
                    encryptedPrivateKey.ciphertext,
                    Base64.NO_WRAP
                ),
                ivBase64 = Base64.encodeToString(
                    encryptedPrivateKey.iv,
                    Base64.NO_WRAP
                ),
                version = CURRENT_VERSION
            )
        } catch (e: Exception) {
            throw CryptoException("Failed to wrap existing keypair: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Version identifier for the serialization format.
         * Increment this when making breaking changes to the storage format.
         */
        private const val CURRENT_VERSION = 1
    }
}

/**
 * Serialized representation of a wrapped keypair suitable for persistent storage.
 *
 * This can be stored in:
 * - Proto DataStore (recommended)
 * - Preferences DataStore
 * - A JSON file
 * - Any other persistent storage mechanism
 *
 * All fields are Base64-encoded strings for easy serialization.
 */
data class SerializedKeyPair(
    /**
     * The public key in Base64 format.
     * This is not encrypted as it's meant to be shared.
     */
    val publicKeyBase64: String,

    /**
     * The private key encrypted with AES-GCM, in Base64 format.
     * Can only be decrypted using the Android Keystore.
     */
    val encryptedPrivateKeyBase64: String,

    /**
     * The initialization vector (IV) for AES-GCM decryption, in Base64 format.
     * This is public and needed for decryption.
     */
    val ivBase64: String,

    /**
     * Version of the serialization format.
     * Allows for future format migrations.
     */
    val version: Int
)

