package se.ejp.dulvindr.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for AndroidKeystoreWrapper.
 * These tests run on an Android device or emulator and test actual Keystore operations.
 */
@RunWith(AndroidJUnit4::class)
class AndroidKeystoreWrapperTest {

    private lateinit var keystoreWrapper: AndroidKeystoreWrapper

    @Before
    fun setup() {
        keystoreWrapper = AndroidKeystoreWrapper()
        // Clean up any existing keys from previous test runs
        try {
            keystoreWrapper.deleteWrapKey()
        } catch (e: Exception) {
            // Ignore if key doesn't exist
        }
    }

    @After
    fun tearDown() {
        // Clean up after tests
        try {
            keystoreWrapper.deleteWrapKey()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun testWrapKeyGeneration() {
        // Verify key doesn't exist initially
        assertFalse("Key should not exist initially", keystoreWrapper.wrapKeyExists())

        // Generate key
        val key = keystoreWrapper.ensureWrapKeyExists()
        assertNotNull("Generated key should not be null", key)

        // Verify key now exists
        assertTrue("Key should exist after generation", keystoreWrapper.wrapKeyExists())
    }

    @Test
    fun testEnsureWrapKeyExistsIsIdempotent() {
        // Generate key first time
        val key1 = keystoreWrapper.ensureWrapKeyExists()
        assertNotNull(key1)

        // Generate key second time (should return existing key)
        val key2 = keystoreWrapper.ensureWrapKeyExists()
        assertNotNull(key2)

        // Both calls should succeed and key should still exist
        assertTrue("Key should still exist", keystoreWrapper.wrapKeyExists())
    }

    @Test
    fun testBasicEncryptDecrypt() {
        keystoreWrapper.ensureWrapKeyExists()

        // Test data
        val plaintext = "This is a secret message".toByteArray()

        // Encrypt
        val encrypted = keystoreWrapper.encrypt(plaintext)
        assertNotNull("Encrypted data should not be null", encrypted)
        assertNotNull("Ciphertext should not be null", encrypted.ciphertext)
        assertNotNull("IV should not be null", encrypted.iv)
        assertEquals("IV size should be 12 bytes (GCM standard)", 12, encrypted.iv.size)

        // Verify ciphertext is different from plaintext
        assertFalse(
            "Ciphertext should be different from plaintext",
            encrypted.ciphertext.contentEquals(plaintext)
        )

        // Decrypt
        val decrypted = keystoreWrapper.decrypt(encrypted)
        assertArrayEquals("Decrypted data should match original plaintext", plaintext, decrypted)
    }

    @Test
    fun testEncryptDecryptLibsodiumKeySize() {
        keystoreWrapper.ensureWrapKeyExists()

        // Test with 32-byte data (typical libsodium private key size)
        val plaintext = ByteArray(32) { it.toByte() }

        val encrypted = keystoreWrapper.encrypt(plaintext)
        val decrypted = keystoreWrapper.decrypt(encrypted)

        assertArrayEquals("32-byte key should encrypt/decrypt correctly", plaintext, decrypted)
    }

    @Test
    fun testEncryptProducesUniqueIVs() {
        keystoreWrapper.ensureWrapKeyExists()

        val plaintext = "Secret".toByteArray()

        // Encrypt same data twice
        val encrypted1 = keystoreWrapper.encrypt(plaintext)
        val encrypted2 = keystoreWrapper.encrypt(plaintext)

        // IVs should be different (randomized)
        assertFalse(
            "IVs should be unique for each encryption",
            encrypted1.iv.contentEquals(encrypted2.iv)
        )

        // Ciphertexts should be different (due to different IVs)
        assertFalse(
            "Ciphertexts should be different due to unique IVs",
            encrypted1.ciphertext.contentEquals(encrypted2.ciphertext)
        )

        // But both should decrypt to same plaintext
        val decrypted1 = keystoreWrapper.decrypt(encrypted1)
        val decrypted2 = keystoreWrapper.decrypt(encrypted2)

        assertArrayEquals("First decryption should match plaintext", plaintext, decrypted1)
        assertArrayEquals("Second decryption should match plaintext", plaintext, decrypted2)
    }

    @Test
    fun testEncryptDecryptEmptyData() {
        keystoreWrapper.ensureWrapKeyExists()

        // Test with empty array
        val plaintext = ByteArray(0)

        val encrypted = keystoreWrapper.encrypt(plaintext)
        val decrypted = keystoreWrapper.decrypt(encrypted)

        assertEquals("Empty data should decrypt to empty array", 0, decrypted.size)
    }

    @Test
    fun testEncryptDecryptLargeData() {
        keystoreWrapper.ensureWrapKeyExists()

        // Test with 1KB of data
        val plaintext = ByteArray(1024) { (it % 256).toByte() }

        val encrypted = keystoreWrapper.encrypt(plaintext)
        val decrypted = keystoreWrapper.decrypt(encrypted)

        assertArrayEquals("Large data should encrypt/decrypt correctly", plaintext, decrypted)
    }

    @Test(expected = CryptoException::class)
    fun testDecryptWithWrongIV() {
        keystoreWrapper.ensureWrapKeyExists()

        val plaintext = "Secret".toByteArray()
        val encrypted = keystoreWrapper.encrypt(plaintext)

        // Tamper with IV
        val wrongEncrypted = EncryptedData(
            ciphertext = encrypted.ciphertext,
            iv = ByteArray(12) { 0xFF.toByte() }
        )

        // Should throw CryptoException
        keystoreWrapper.decrypt(wrongEncrypted)
    }

    @Test(expected = CryptoException::class)
    fun testDecryptWithTamperedCiphertext() {
        keystoreWrapper.ensureWrapKeyExists()

        val plaintext = "Secret".toByteArray()
        val encrypted = keystoreWrapper.encrypt(plaintext)

        // Tamper with ciphertext
        val tamperedCiphertext = encrypted.ciphertext.clone()
        if (tamperedCiphertext.isNotEmpty()) {
            tamperedCiphertext[0] = (tamperedCiphertext[0] + 1).toByte()
        }

        val wrongEncrypted = EncryptedData(
            ciphertext = tamperedCiphertext,
            iv = encrypted.iv
        )

        // Should throw CryptoException due to GCM authentication failure
        keystoreWrapper.decrypt(wrongEncrypted)
    }

    @Test
    fun testKeyDeletion() {
        // Generate key
        keystoreWrapper.ensureWrapKeyExists()
        assertTrue("Key should exist", keystoreWrapper.wrapKeyExists())

        // Delete key
        val deleted = keystoreWrapper.deleteWrapKey()
        assertTrue("Delete should return true", deleted)

        // Verify key no longer exists
        assertFalse("Key should not exist after deletion", keystoreWrapper.wrapKeyExists())
    }

    @Test
    fun testDeleteNonExistentKey() {
        // Ensure no key exists
        assertFalse("Key should not exist", keystoreWrapper.wrapKeyExists())

        // Try to delete non-existent key
        val deleted = keystoreWrapper.deleteWrapKey()
        assertFalse("Delete should return false for non-existent key", deleted)
    }

    @Test
    fun testKeyPersistenceAcrossInstances() {
        // Create first instance and generate key
        val wrapper1 = AndroidKeystoreWrapper()
        wrapper1.ensureWrapKeyExists()

        val plaintext = "Test data".toByteArray()
        val encrypted = wrapper1.encrypt(plaintext)

        // Create second instance and decrypt with same Keystore key
        val wrapper2 = AndroidKeystoreWrapper()
        val decrypted = wrapper2.decrypt(encrypted)

        assertArrayEquals(
            "Second instance should decrypt data encrypted by first instance",
            plaintext,
            decrypted
        )
    }

    @Test
    fun testEncryptDecryptWithVariousSizes() {
        keystoreWrapper.ensureWrapKeyExists()

        val testSizes = listOf(1, 16, 32, 64, 128, 256, 512)

        for (size in testSizes) {
            val plaintext = ByteArray(size) { (it % 256).toByte() }
            val encrypted = keystoreWrapper.encrypt(plaintext)
            val decrypted = keystoreWrapper.decrypt(encrypted)

            assertArrayEquals(
                "Data of size $size should encrypt/decrypt correctly",
                plaintext,
                decrypted
            )
        }
    }
}

