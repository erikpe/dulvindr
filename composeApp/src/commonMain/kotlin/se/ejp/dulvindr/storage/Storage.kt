package se.ejp.dulvindr.storage

import se.ejp.dulvindr.model.IdentityMetadata

/**
 * Secure storage interface for private keys.
 * Implementations should use platform-specific secure storage:
 * - Android: Encrypted SharedPreferences or Android Keystore
 * - iOS: Keychain
 * - JVM: OS keystore or encrypted file
 * - JS: Browser secure storage APIs
 */
interface SecureStorage {
    /**
     * Store a private key securely.
     * @param identityId Unique identifier for the identity
     * @param privateKey The private key bytes to store
     */
    suspend fun storePrivateKey(identityId: String, privateKey: ByteArray)

    /**
     * Retrieve a private key.
     * @param identityId Unique identifier for the identity
     * @return The private key bytes, or null if not found
     */
    suspend fun getPrivateKey(identityId: String): ByteArray?

    /**
     * Delete a private key.
     * @param identityId Unique identifier for the identity
     */
    suspend fun deletePrivateKey(identityId: String)
}

/**
 * Storage interface for identity metadata (non-sensitive data).
 * Private keys should be stored separately in SecureStorage.
 */
interface IdentityStorage {
    /**
     * Save identity metadata.
     * Note: This should only store public information (name, public key).
     * Private keys must be stored separately using SecureStorage.
     */
    suspend fun saveIdentity(identity: IdentityMetadata)

    /**
     * Get all stored identity metadata.
     * @return List of identity metadata (without private keys)
     */
    suspend fun getIdentities(): List<IdentityMetadata>

    /**
     * Delete identity metadata.
     * Note: This should be coordinated with SecureStorage to also delete the private key.
     * @param identityId Unique identifier for the identity to delete
     */
    suspend fun deleteIdentity(identityId: String)

    /**
     * Get a specific identity by ID.
     * @param identityId Unique identifier for the identity
     * @return The identity metadata, or null if not found
     */
    suspend fun getIdentity(identityId: String): IdentityMetadata?
}

