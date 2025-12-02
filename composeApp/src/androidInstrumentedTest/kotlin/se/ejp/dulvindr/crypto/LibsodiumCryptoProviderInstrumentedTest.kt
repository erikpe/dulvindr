package se.ejp.dulvindr.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for LibsodiumCryptoProvider.
 *
 * These tests run on an Android device/emulator where libsodium native library is available.
 * This is the same test suite as commonTest/LibsodiumCryptoProviderTest.kt, adapted for
 * Android instrumented testing.
 */
@RunWith(AndroidJUnit4::class)
class LibsodiumCryptoProviderInstrumentedTest {

    private lateinit var crypto: LibsodiumCryptoProvider

    @Before
    fun setup() = runBlocking {
        // Initialize libsodium before each test
        LibsodiumCryptoProvider.initialize()
        crypto = LibsodiumCryptoProvider()
    }

    @Test
    fun testKeyPairGeneration() = runBlocking {
        val keyPair = crypto.generateKeyPair()

        // Verify key sizes (X25519 uses 32-byte keys)
        assertEquals("Public key should be 32 bytes", 32, keyPair.publicKey.size)
        assertEquals("Private key should be 32 bytes", 32, keyPair.privateKey.size)

        // Verify keys are different
        assertFalse(
            "Public and private keys should be different",
            keyPair.publicKey.contentEquals(keyPair.privateKey)
        )
    }

    @Test
    fun testEncryptDecrypt() = runBlocking {
        // Generate key pairs for Alice and Bob
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        val message = "Hello, Bob! This is a secret message."

        // Alice encrypts a message for Bob
        val ciphertext = crypto.encryptAndSign(
            message = message,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        // Verify ciphertext is longer than plaintext (includes nonce + MAC)
        assertTrue(
            "Ciphertext should be longer than plaintext",
            ciphertext.size > message.length
        )

        // Bob decrypts the message from Alice
        val decrypted = crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        // Verify decrypted message matches original
        assertEquals("Decrypted message should match original", message, decrypted)
    }

    @Test
    fun testEncryptionProducesDifferentCiphertexts() = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        val message = "Same message"

        // Encrypt the same message twice
        val ciphertext1 = crypto.encryptAndSign(
            message = message,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        val ciphertext2 = crypto.encryptAndSign(
            message = message,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        // Different nonces should produce different ciphertexts
        assertFalse(
            "Same message encrypted twice should produce different ciphertexts (different nonces)",
            ciphertext1.contentEquals(ciphertext2)
        )

        // But both should decrypt to the same message
        val decrypted1 = crypto.decryptAndVerify(
            ciphertext = ciphertext1,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        val decrypted2 = crypto.decryptAndVerify(
            ciphertext = ciphertext2,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        assertEquals(message, decrypted1)
        assertEquals(message, decrypted2)
    }

    @Test(expected = CryptoException::class)
    fun testDecryptionWithWrongKeyFails(): Unit = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()
        val eveKeyPair = crypto.generateKeyPair() // Attacker

        val message = "Secret message"

        // Alice encrypts for Bob
        val ciphertext = crypto.encryptAndSign(
            message = message,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        // Eve tries to decrypt with her own key - should throw CryptoException
        crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = eveKeyPair.privateKey
        )
    }

    @Test(expected = CryptoException::class)
    fun testTamperedCiphertextFails(): Unit = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        val message = "Original message"

        val ciphertext = crypto.encryptAndSign(
            message = message,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        // Tamper with the ciphertext
        val tampered = ciphertext.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt().xor(0xFF)).toByte()

        // Decryption should fail due to MAC verification
        crypto.decryptAndVerify(
            ciphertext = tampered,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )
    }

    @Test(expected = CryptoException::class)
    fun testShortCiphertextFails(): Unit = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        // Ciphertext too short (less than nonce size)
        val shortCiphertext = ByteArray(10)

        crypto.decryptAndVerify(
            ciphertext = shortCiphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )
    }

    @Test
    fun testUnicodeMessages() = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        val unicodeMessage = "Hello 世界! 🔒 Encrypted message with émojis and àccénts"

        val ciphertext = crypto.encryptAndSign(
            message = unicodeMessage,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        val decrypted = crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        assertEquals("Unicode message should be preserved", unicodeMessage, decrypted)
    }

    @Test
    fun testEmptyMessage() = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        val emptyMessage = ""

        val ciphertext = crypto.encryptAndSign(
            message = emptyMessage,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        val decrypted = crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        assertEquals("Empty message should be preserved", emptyMessage, decrypted)
    }

    @Test
    fun testLongMessage() = runBlocking {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        // Create a long message (10KB)
        val longMessage = "A".repeat(10240)

        val ciphertext = crypto.encryptAndSign(
            message = longMessage,
            recipientPublicKey = bobKeyPair.publicKey,
            senderPrivateKey = aliceKeyPair.privateKey
        )

        val decrypted = crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )

        assertEquals("Long message should be preserved", longMessage, decrypted)
        assertEquals("Message length should match", longMessage.length, decrypted.length)
    }

    @Test
    fun testMultipleKeyPairsAreUnique() = runBlocking {
        val keyPair1 = crypto.generateKeyPair()
        val keyPair2 = crypto.generateKeyPair()
        val keyPair3 = crypto.generateKeyPair()

        // All public keys should be unique
        assertFalse(
            "KeyPair 1 and 2 public keys should be different",
            keyPair1.publicKey.contentEquals(keyPair2.publicKey)
        )
        assertFalse(
            "KeyPair 1 and 3 public keys should be different",
            keyPair1.publicKey.contentEquals(keyPair3.publicKey)
        )
        assertFalse(
            "KeyPair 2 and 3 public keys should be different",
            keyPair2.publicKey.contentEquals(keyPair3.publicKey)
        )

        // All private keys should be unique
        assertFalse(
            "KeyPair 1 and 2 private keys should be different",
            keyPair1.privateKey.contentEquals(keyPair2.privateKey)
        )
        assertFalse(
            "KeyPair 1 and 3 private keys should be different",
            keyPair1.privateKey.contentEquals(keyPair3.privateKey)
        )
        assertFalse(
            "KeyPair 2 and 3 private keys should be different",
            keyPair2.privateKey.contentEquals(keyPair3.privateKey)
        )
    }

}

