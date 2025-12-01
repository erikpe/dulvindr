package se.ejp.dulvindr.storage

import kotlinx.coroutines.test.runTest
import se.ejp.dulvindr.model.IdentityMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the stub storage implementations.
 * These tests verify that the storage interfaces work correctly.
 */
class StorageTest {

    @Test
    fun testSecureStorage() = runTest {
        val storage = StubSecureStorage()
        val identityId = "test-identity-123"
        val privateKey = ByteArray(32) { it.toByte() }

        // Store private key
        storage.storePrivateKey(identityId, privateKey)

        // Retrieve private key
        val retrieved = storage.getPrivateKey(identityId)
        assertNotNull(retrieved)
        assertEquals(32, retrieved.size)
        assertTrue(privateKey.contentEquals(retrieved))

        // Delete private key
        storage.deletePrivateKey(identityId)
        val afterDelete = storage.getPrivateKey(identityId)
        assertNull(afterDelete)
    }

    @Test
    fun testIdentityStorage() = runTest {
        val storage = StubIdentityStorage()
        val metadata = IdentityMetadata(
            id = "test-id-456",
            name = "Test User",
            publicKey = ByteArray(32) { it.toByte() },
            createdAt = 12345678L
        )

        // Save identity
        storage.saveIdentity(metadata)

        // Retrieve all identities
        val identities = storage.getIdentities()
        assertEquals(1, identities.size)
        assertEquals("Test User", identities[0].name)

        // Retrieve specific identity
        val retrieved = storage.getIdentity("test-id-456")
        assertNotNull(retrieved)
        assertEquals("Test User", retrieved.name)

        // Delete identity
        storage.deleteIdentity("test-id-456")
        val afterDelete = storage.getIdentities()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testIdentityStorageManager() = runTest {
        val identityStorage = StubIdentityStorage()
        val secureStorage = StubSecureStorage()
        val manager = IdentityStorageManager(identityStorage, secureStorage)

        val metadata = IdentityMetadata(
            id = "test-id-789",
            name = "Manager Test User",
            publicKey = ByteArray(32) { it.toByte() },
            createdAt = 98765432L
        )
        val privateKey = ByteArray(32) { (it + 100).toByte() }

        // Save complete identity
        manager.saveIdentity(metadata, privateKey)

        // Retrieve metadata
        val identities = manager.getIdentities()
        assertEquals(1, identities.size)
        assertEquals("Manager Test User", identities[0].name)

        // Retrieve private key
        val retrievedPrivateKey = manager.getPrivateKey("test-id-789")
        assertNotNull(retrievedPrivateKey)
        assertTrue(privateKey.contentEquals(retrievedPrivateKey))

        // Delete complete identity
        manager.deleteIdentity("test-id-789")
        val afterDelete = manager.getIdentities()
        assertEquals(0, afterDelete.size)

        val privateKeyAfterDelete = manager.getPrivateKey("test-id-789")
        assertNull(privateKeyAfterDelete)
    }
}

