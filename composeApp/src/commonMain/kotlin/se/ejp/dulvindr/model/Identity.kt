package se.ejp.dulvindr.model

/**
 * Represents a user's identity consisting of a name and cryptographic key pair.
 * The key pair is used for stateless authenticated encryption (crypto_box).
 */
data class Identity(
    val name: String,
    val publicKey: ByteArray,  // 32 bytes - X25519 public key
    val privateKey: ByteArray  // 32 bytes - X25519 private key
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Identity

        if (name != other.name) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        return result
    }

    /**
     * Get a short fingerprint of the public key for display purposes.
     */
    fun getPublicKeyFingerprint(): String {
        return publicKey.take(8).joinToString("") { byte ->
            byte.toInt().and(0xFF).toString(16).padStart(2, '0')
        }
    }
}

