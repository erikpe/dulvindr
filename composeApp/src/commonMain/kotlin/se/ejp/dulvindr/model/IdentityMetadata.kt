package se.ejp.dulvindr.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class IdentityMetadata(
    val id: String,
    val name: String,
    val publicKey: ByteArray,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IdentityMetadata

        if (id != other.id) return false
        if (name != other.name) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + createdAt.hashCode()
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

    /**
     * Encode identity data (name and public key) for QR code.
     * Format: name|hexadecimal-encoded-public-key
     */
    fun encodeForQRCode(): String {
        val publicKeyHex = publicKey.joinToString("") { byte ->
            byte.toInt().and(0xFF).toString(16).padStart(2, '0')
        }
        return "$name|$publicKeyHex"
    }
}

// Extension to convert Identity to metadata
@OptIn(ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)
fun Identity.toMetadata(id: String = Uuid.random().toString()): IdentityMetadata {
    return IdentityMetadata(
        id = id,
        name = name,
        publicKey = publicKey,
        createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
    )
}

// Extension to convert IdentityMetadata back to Identity (requires private key from secure storage)
fun IdentityMetadata.toIdentity(privateKey: ByteArray): Identity {
    return Identity(
        name = name,
        publicKey = publicKey,
        privateKey = privateKey
    )
}


