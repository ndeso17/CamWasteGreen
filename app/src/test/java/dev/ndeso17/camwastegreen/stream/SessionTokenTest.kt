package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class SessionTokenTest {

    @Test
    fun tokenHasSixtyFourLowercaseHexChars() {
        val token = SessionToken.generate()
        assertEquals(SessionToken.BYTE_LENGTH * 2, token.length)
        assertTrue(token.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun fixedRandomProducesExpectedHex() {
        // Sumber acak palsu: semua byte 0xAB.
        val fake = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                bytes.fill(0xAB.toByte())
            }
        }
        val token = SessionToken.generate(fake)
        assertEquals("ab".repeat(SessionToken.BYTE_LENGTH), token)
    }

    @Test
    fun twoSessionsGetDifferentTokens() {
        assertNotEquals(SessionToken.generate(), SessionToken.generate())
    }
}
