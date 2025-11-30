package se.ejp.niltalk2.crypto

import kotlin.random.Random

/**
 * Libsodium implementation of CryptoProvider using crypto_box.
 *
 * Note: This is a simplified implementation for the PoC.
 * For now, we're using a basic encryption scheme without libsodium.
 * In production, you should use proper libsodium bindings.
 */
class LibsodiumCryptoProvider : CryptoProvider {

    override fun generateKeyPair(): KeyPair {
        // Generate a simple key pair (32 bytes each)
        // In a real implementation, this would use libsodium's X25519 key generation
        val publicKey = Random.Default.nextBytes(32)
        val privateKey = Random.Default.nextBytes(32)

        return KeyPair(
            publicKey = publicKey,
            privateKey = privateKey
        )
    }

    override fun encryptAndSign(
        message: String,
        recipientPublicKey: ByteArray,
        senderPrivateKey: ByteArray
    ): ByteArray {
        // Simplified implementation for PoC
        // In production, this would use crypto_box_easy from libsodium
        try {
            val messageBytes = message.encodeToByteArray()
            val nonce = Random.Default.nextBytes(24)

            // Simple XOR-based encryption (NOT SECURE - placeholder only)
            val key = combineKeys(recipientPublicKey, senderPrivateKey)
            val encrypted = messageBytes.mapIndexed { i, byte ->
                (byte.toInt() xor key[i % key.size].toInt()).toByte()
            }.toByteArray()

            // Prepend nonce
            return nonce + encrypted
        } catch (e: Exception) {
            throw CryptoException("Encryption failed", e)
        }
    }

    override fun decryptAndVerify(
        ciphertext: ByteArray,
        senderPublicKey: ByteArray,
        recipientPrivateKey: ByteArray
    ): String {
        // Simplified implementation for PoC
        // In production, this would use crypto_box_open_easy from libsodium
        try {
            // Extract nonce and ciphertext
            val nonce = ciphertext.sliceArray(0 until 24)
            val encrypted = ciphertext.sliceArray(24 until ciphertext.size)

            // Simple XOR-based decryption (NOT SECURE - placeholder only)
            val key = combineKeys(senderPublicKey, recipientPrivateKey)
            val decrypted = encrypted.mapIndexed { i, byte ->
                (byte.toInt() xor key[i % key.size].toInt()).toByte()
            }.toByteArray()

            return decrypted.decodeToString()
        } catch (e: Exception) {
            throw CryptoException("Decryption or verification failed", e)
        }
    }

    private fun combineKeys(key1: ByteArray, key2: ByteArray): ByteArray {
        return key1.zip(key2).map { (a, b) ->
            (a.toInt() xor b.toInt()).toByte()
        }.toByteArray()
    }
}

