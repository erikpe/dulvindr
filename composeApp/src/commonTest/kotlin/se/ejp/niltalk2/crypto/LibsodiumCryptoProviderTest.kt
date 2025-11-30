package se.ejp.niltalk2.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LibsodiumCryptoProviderTest {
    
    private val crypto = LibsodiumCryptoProvider()
    
    @Test
    fun testKeyPairGeneration() {
        val keyPair = crypto.generateKeyPair()
        
        // Verify key sizes (X25519 uses 32-byte keys)
        assertEquals(32, keyPair.publicKey.size, "Public key should be 32 bytes")
        assertEquals(32, keyPair.privateKey.size, "Private key should be 32 bytes")
        
        // Verify keys are different
        assertNotEquals(
            keyPair.publicKey.contentHashCode(),
            keyPair.privateKey.contentHashCode(),
            "Public and private keys should be different"
        )
    }
    
    @Test
    fun testEncryptDecrypt() {
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
        assertTrue(ciphertext.size > message.length, "Ciphertext should be longer than plaintext")

        // Bob decrypts the message from Alice
        val decrypted = crypto.decryptAndVerify(
            ciphertext = ciphertext,
            senderPublicKey = aliceKeyPair.publicKey,
            recipientPrivateKey = bobKeyPair.privateKey
        )
        
        // Verify decrypted message matches original
        assertEquals(message, decrypted, "Decrypted message should match original")
    }
    
    @Test
    fun testEncryptionProducesDifferentCiphertexts() {
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
        assertNotEquals(
            ciphertext1.contentHashCode(),
            ciphertext2.contentHashCode(),
            "Same message encrypted twice should produce different ciphertexts (different nonces)"
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
    
    @Test
    fun testDecryptionWithWrongKeyFails() {
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
        
        // Eve tries to decrypt with her own key - should fail
        assertFailsWith<CryptoException> {
            crypto.decryptAndVerify(
                ciphertext = ciphertext,
                senderPublicKey = aliceKeyPair.publicKey,
                recipientPrivateKey = eveKeyPair.privateKey
            )
        }
    }
    
    @Test
    fun testTamperedCiphertextFails() {
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
        assertFailsWith<CryptoException> {
            crypto.decryptAndVerify(
                ciphertext = tampered,
                senderPublicKey = aliceKeyPair.publicKey,
                recipientPrivateKey = bobKeyPair.privateKey
            )
        }
    }
    
    @Test
    fun testShortCiphertextFails() {
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()
        
        // Ciphertext too short (less than nonce size)
        val shortCiphertext = ByteArray(10)
        
        assertFailsWith<CryptoException> {
            crypto.decryptAndVerify(
                ciphertext = shortCiphertext,
                senderPublicKey = aliceKeyPair.publicKey,
                recipientPrivateKey = bobKeyPair.privateKey
            )
        }
    }
    
    @Test
    fun testUnicodeMessages() {
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
        
        assertEquals(unicodeMessage, decrypted, "Unicode message should be preserved")
    }
}

