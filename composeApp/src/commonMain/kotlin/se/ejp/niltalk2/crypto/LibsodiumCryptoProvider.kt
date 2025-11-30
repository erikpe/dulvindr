package se.ejp.niltalk2.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

/**
 * Libsodium implementation of CryptoProvider using crypto_box.
 *
 * Uses libsodium's authenticated encryption (crypto_box_easy/crypto_box_open_easy):
 * - Combines X25519 Diffie-Hellman key exchange
 * - XSalsa20 stream cipher for encryption
 * - Poly1305 MAC for authentication
 *
 * This provides both confidentiality and authentication in a single operation.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class LibsodiumCryptoProvider : CryptoProvider {

    companion object {
        private val isInitialized = atomic(false)
        private val initMutex = Mutex()

        /**
         * Initialize libsodium asynchronously.
         * This must be called before any crypto operations.
         * Uses a mutex to ensure only one initialization happens.
         */
        suspend fun initialize() {
            // Quick check without lock
            if (isInitialized.value) {
                return
            }

            // Use mutex to ensure only one coroutine performs initialization
            initMutex.withLock {
                // Double-check after acquiring lock
                if (isInitialized.value) {
                    return
                }

                // Suspend until callback is invoked
                suspendCoroutine { continuation ->
                    LibsodiumInitializer.initializeWithCallback {
                        isInitialized.value = true
                        continuation.resume(Unit)
                    }
                }
            }
        }

        /**
         * Check if libsodium is ready to use.
         */
        fun isReady(): Boolean = isInitialized.value
    }

    override fun generateKeyPair(): KeyPair {
        if (!isReady()) {
            throw CryptoException("Libsodium not initialized. Call LibsodiumCryptoProvider.initialize() first.")
        }

        // Generate X25519 key pair for crypto_box operations
        val sodiumKeyPair = Box.keypair()

        return KeyPair(
            publicKey = sodiumKeyPair.publicKey.asByteArray(),
            privateKey = sodiumKeyPair.secretKey.asByteArray()
        )
    }

    override fun encryptAndSign(
        message: String,
        recipientPublicKey: ByteArray,
        senderPrivateKey: ByteArray
    ): ByteArray {
        if (!isReady()) {
            throw CryptoException("Libsodium not initialized. Call LibsodiumCryptoProvider.initialize() first.")
        }

        try {
            val messageBytes = message.encodeToUByteArray()

            // Generate a random nonce (24 bytes for crypto_box)
            val nonce = Random.Default.nextBytes(24).asUByteArray()

            // Perform authenticated encryption using crypto_box_easy
            // This combines the sender's private key and recipient's public key
            // to create a shared secret, then encrypts and authenticates the message
            val ciphertext = Box.easy(
                message = messageBytes,
                nonce = nonce,
                recipientsPublicKey = recipientPublicKey.asUByteArray(),
                sendersSecretKey = senderPrivateKey.asUByteArray()
            )

            // Prepend nonce to ciphertext for transport
            // The nonce is public and needed for decryption
            return (nonce + ciphertext).asByteArray()
        } catch (e: Exception) {
            throw CryptoException("Encryption failed: ${e.message}", e)
        }
    }

    override fun decryptAndVerify(
        ciphertext: ByteArray,
        senderPublicKey: ByteArray,
        recipientPrivateKey: ByteArray
    ): String {
        if (!isReady()) {
            throw CryptoException("Libsodium not initialized. Call LibsodiumCryptoProvider.initialize() first.")
        }

        try {
            val data = ciphertext.asUByteArray()

            // Extract nonce (first 24 bytes) and actual ciphertext
            // crypto_box uses 24-byte nonces
            val nonceSize = 24
            if (data.size < nonceSize) {
                throw CryptoException("Invalid ciphertext: too short")
            }

            val nonce = data.sliceArray(0 until nonceSize)
            val actualCiphertext = data.sliceArray(nonceSize until data.size)

            // Decrypt and verify using crypto_box_open_easy
            // This recreates the shared secret from recipient's private key and sender's public key,
            // then decrypts and verifies the MAC
            val decrypted = Box.openEasy(
                ciphertext = actualCiphertext,
                nonce = nonce,
                sendersPublicKey = senderPublicKey.asUByteArray(),
                recipientsSecretKey = recipientPrivateKey.asUByteArray()
            )

            return decrypted.asByteArray().decodeToString()
        } catch (e: Exception) {
            throw CryptoException("Decryption or verification failed: ${e.message}", e)
        }
    }
}

