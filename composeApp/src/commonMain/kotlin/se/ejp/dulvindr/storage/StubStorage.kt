package se.ejp.dulvindr.storage

import se.ejp.dulvindr.model.IdentityMetadata

/**
 * In-memory stub implementation of SecureStorage.
 * WARNING: This is NOT secure and should only be used for development/testing.
 *
 * Production implementations should use:
 * - Android: EncryptedSharedPreferences or Android Keystore
 * - iOS: Keychain Services
 * - JVM: Java KeyStore or encrypted files
 * - JS: Web Crypto API with IndexedDB
 */
class StubSecureStorage : SecureStorage {
    private val storage = mutableMapOf<String, ByteArray>()

    override suspend fun storePrivateKey(identityId: String, privateKey: ByteArray) {
        // In a real implementation, this would encrypt and store securely
        storage[identityId] = privateKey.copyOf()
        println("StubSecureStorage: Stored private key for identity $identityId")
    }

    override suspend fun getPrivateKey(identityId: String): ByteArray? {
        // In a real implementation, this would decrypt and return
        return storage[identityId]?.copyOf()
    }

    override suspend fun deletePrivateKey(identityId: String) {
        storage.remove(identityId)
        println("StubSecureStorage: Deleted private key for identity $identityId")
    }
}

/**
 * In-memory stub implementation of IdentityStorage.
 * WARNING: This is NOT persistent and data will be lost when the app restarts.
 *
 * Production implementations should use:
 * - Android: Room Database or DataStore
 * - iOS: CoreData or UserDefaults
 * - JVM: SQLite or file-based storage
 * - JS: IndexedDB or LocalStorage
 */
class StubIdentityStorage : IdentityStorage {
    private val identities = mutableMapOf<String, IdentityMetadata>()

    override suspend fun saveIdentity(identity: IdentityMetadata) {
        identities[identity.id] = identity
        println("StubIdentityStorage: Saved identity ${identity.id} (${identity.name})")
    }

    override suspend fun getIdentities(): List<IdentityMetadata> {
        return identities.values.toList()
    }

    override suspend fun deleteIdentity(identityId: String) {
        identities.remove(identityId)
        println("StubIdentityStorage: Deleted identity $identityId")
    }

    override suspend fun getIdentity(identityId: String): IdentityMetadata? {
        return identities[identityId]
    }
}

/**
 * Coordinated storage manager that handles both identity metadata and private keys.
 * Ensures that private keys are stored separately from public metadata.
 */
class IdentityStorageManager(
    private val identityStorage: IdentityStorage,
    private val secureStorage: SecureStorage
) {
    /**
     * Save a complete identity (metadata + private key).
     */
    suspend fun saveIdentity(metadata: IdentityMetadata, privateKey: ByteArray) {
        // Store metadata
        identityStorage.saveIdentity(metadata)

        // Store private key securely
        secureStorage.storePrivateKey(metadata.id, privateKey)
    }

    /**
     * Get identity metadata only (without private key).
     */
    suspend fun getIdentities(): List<IdentityMetadata> {
        return identityStorage.getIdentities()
    }

    /**
     * Get a specific identity's metadata.
     */
    suspend fun getIdentity(identityId: String): IdentityMetadata? {
        return identityStorage.getIdentity(identityId)
    }

    /**
     * Get a private key for an identity.
     */
    suspend fun getPrivateKey(identityId: String): ByteArray? {
        return secureStorage.getPrivateKey(identityId)
    }

    /**
     * Delete an identity completely (metadata + private key).
     */
    suspend fun deleteIdentity(identityId: String) {
        identityStorage.deleteIdentity(identityId)
        secureStorage.deletePrivateKey(identityId)
    }
}

