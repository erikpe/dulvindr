package se.ejp.dulvindr.crypto

/**
 * Cryptographic operations provider using libsodium's crypto_box
 * for authenticated encryption suitable for stateless messaging.
 */
interface CryptoProvider {
    /**
     * Generate a new X25519 key pair for crypto_box operations.
     */
    fun generateKeyPair(): KeyPair

    /**
     * Encrypt and authenticate a message using crypto_box.
     * Combines encryption with recipient's public key and signing with sender's private key.
     *
     * @param message The plaintext message to encrypt
     * @param recipientPublicKey The recipient's public key (32 bytes)
     * @param senderPrivateKey The sender's private key (32 bytes)
     * @return Encrypted and authenticated ciphertext
     */
    fun encryptAndSign(
        message: String,
        recipientPublicKey: ByteArray,
        senderPrivateKey: ByteArray
    ): ByteArray

    /**
     * Decrypt and verify a message using crypto_box.
     * Decrypts with recipient's private key and verifies sender's signature.
     *
     * @param ciphertext The encrypted message
     * @param senderPublicKey The sender's public key (32 bytes) for verification
     * @param recipientPrivateKey The recipient's private key (32 bytes)
     * @return Decrypted plaintext message
     * @throws CryptoException if decryption or verification fails
     */
    fun decryptAndVerify(
        ciphertext: ByteArray,
        senderPublicKey: ByteArray,
        recipientPrivateKey: ByteArray
    ): String
}

/**
 * Represents a libsodium X25519 key pair for crypto_box operations.
 */
data class KeyPair(
    val publicKey: ByteArray,  // 32 bytes
    val privateKey: ByteArray  // 32 bytes
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KeyPair

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        return result
    }
}

class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

