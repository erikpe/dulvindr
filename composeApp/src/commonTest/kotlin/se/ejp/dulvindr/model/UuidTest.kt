package se.ejp.dulvindr.model

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests for UUID generation using Kotlin stdlib
 */
class UuidTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testKotlinStdlibUuidGeneration() {
        // Generate a UUID using Kotlin stdlib
        val uuid = Uuid.random()

        // Convert to string
        val uuidString = uuid.toString()

        assertNotNull(uuidString)

        // UUID format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        // Should be 36 characters (32 hex + 4 hyphens)
        assertTrue(uuidString.length == 36, "UUID string should be 36 characters long")

        // Should contain hyphens at specific positions
        assertTrue(uuidString[8] == '-', "UUID should have hyphen at position 8")
        assertTrue(uuidString[13] == '-', "UUID should have hyphen at position 13")
        assertTrue(uuidString[18] == '-', "UUID should have hyphen at position 18")
        assertTrue(uuidString[23] == '-', "UUID should have hyphen at position 23")

        println("Generated UUID: $uuidString")
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testMultipleUuidsAreUnique() {
        // Generate multiple UUIDs
        val uuid1 = Uuid.random().toString()
        val uuid2 = Uuid.random().toString()
        val uuid3 = Uuid.random().toString()

        // They should all be different
        assertTrue(uuid1 != uuid2, "UUIDs should be unique")
        assertTrue(uuid2 != uuid3, "UUIDs should be unique")
        assertTrue(uuid1 != uuid3, "UUIDs should be unique")

        println("UUID 1: $uuid1")
        println("UUID 2: $uuid2")
        println("UUID 3: $uuid3")
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testIdentityMetadataWithStdlibUuid() {
        // Create an identity with stdlib UUID
        val identity = Identity(
            name = "Test User",
            publicKey = ByteArray(32) { it.toByte() },
            privateKey = ByteArray(32) { (it + 100).toByte() }
        )

        // Convert to metadata (uses Uuid.random() internally)
        val metadata = identity.toMetadata()

        assertNotNull(metadata.id)
        assertTrue(metadata.id.length == 36, "Identity ID should be a valid UUID string")
        assertTrue(metadata.name == "Test User")

        println("Identity ID (UUID): ${metadata.id}")
        println("Identity name: ${metadata.name}")
    }
}

