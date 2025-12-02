package se.ejp.dulvindr.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore wrapper for securely storing and retrieving encryption keys.
 *
 * This class manages a master AES key stored in Android Keystore that is used
 * to wrap (encrypt) other sensitive keys like libsodium private keys.
 *
 * Features:
 * - Uses AES-256-GCM for authenticated encryption
 * - Keys are hardware-backed when available (TEE/Secure Element)
 * - Keys never leave the Keystore (non-extractable)
 * - Provides protection against key extraction and tampering
 */
class AndroidKeystoreWrapper {

    companion object {
        private const val WRAP_KEY_ALIAS = "dulvindr_wrap_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val GCM_IV_LENGTH = 12 // bytes (recommended for GCM)
    }

    /**
     * Ensures the master wrap key exists in Android Keystore.
     * If it doesn't exist, generates a new one.
     *
     * @return The SecretKey that can be used for encryption/decryption operations
     * @throws CryptoException if key generation fails
     */
    fun ensureWrapKeyExists(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }

            // Check if key already exists
            val existingEntry = keyStore.getEntry(WRAP_KEY_ALIAS, null)
            if (existingEntry is KeyStore.SecretKeyEntry) {
                return existingEntry.secretKey
            }

            // Key doesn't exist, generate a new one
            generateWrapKey()
        } catch (e: Exception) {
            throw CryptoException("Failed to ensure wrap key exists: ${e.message}", e)
        }
    }

    /**
     * Generates a new AES-256-GCM key in Android Keystore.
     *
     * Key properties:
     * - 256-bit AES key
     * - GCM block mode for authenticated encryption
     * - No padding (GCM doesn't need padding)
     * - Non-extractable (cannot be exported from Keystore)
     * - Purpose: encrypt and decrypt only
     *
     * @return The newly generated SecretKey
     * @throws CryptoException if key generation fails
     */
    private fun generateWrapKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                WRAP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Prevent key extraction - key cannot leave the Keystore
                .setRandomizedEncryptionRequired(true)
                // Optional: Require user authentication to use the key
                // .setUserAuthenticationRequired(true)
                // .setUserAuthenticationValidityDurationSeconds(30)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            throw CryptoException("Failed to generate wrap key: ${e.message}", e)
        }
    }

    /**
     * Encrypts data using the Keystore-protected AES key.
     *
     * @param plaintext The data to encrypt (e.g., a libsodium private key)
     * @return EncryptedData containing the ciphertext and IV
     * @throws CryptoException if encryption fails
     */
    fun encrypt(plaintext: ByteArray): EncryptedData {
        return try {
            val secretKey = ensureWrapKeyExists()

            val cipher = Cipher.getInstance(AES_GCM_CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // GCM generates a random IV automatically
            val ciphertext = cipher.doFinal(plaintext)

            EncryptedData(
                ciphertext = ciphertext,
                iv = iv
            )
        } catch (e: Exception) {
            throw CryptoException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts data using the Keystore-protected AES key.
     *
     * @param encryptedData The encrypted data with IV
     * @return The decrypted plaintext
     * @throws CryptoException if decryption fails (wrong key, tampered data, etc.)
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        return try {
            val secretKey = ensureWrapKeyExists()

            val cipher = Cipher.getInstance(AES_GCM_CIPHER)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

            cipher.doFinal(encryptedData.ciphertext)
        } catch (e: Exception) {
            throw CryptoException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Checks if the wrap key exists in the Keystore.
     *
     * @return true if the key exists, false otherwise
     */
    fun wrapKeyExists(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
            keyStore.containsAlias(WRAP_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes the wrap key from the Keystore.
     * WARNING: This will make all data encrypted with this key unrecoverable!
     *
     * @return true if the key was deleted, false if it didn't exist
     * @throws CryptoException if deletion fails
     */
    fun deleteWrapKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }

            if (keyStore.containsAlias(WRAP_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAP_KEY_ALIAS)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            throw CryptoException("Failed to delete wrap key: ${e.message}", e)
        }
    }
}

/**
 * Container for encrypted data with its initialization vector.
 * Both the ciphertext and IV are needed for decryption.
 * The IV is public and doesn't need to be kept secret.
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

