package dev.ndeso17.camwastegreen.stream

import java.security.SecureRandom

/**
 * Pembuat token `X-Api-Key` untuk satu sesi server (D-34).
 *
 * Token dibuat ulang setiap kali server dimulai, disimpan hanya di memori, dan
 * ditampilkan di layar operator. Token tidak berasal dari APK dan tidak ditulis
 * ke disk, sehingga APK yang beredar tidak membawa kunci yang bisa diekstrak.
 *
 * Murni JVM supaya bisa diuji tanpa perangkat.
 */
object SessionToken {

    /** Panjang token dalam byte; hasil hex dua kali lipatnya. */
    const val BYTE_LENGTH = 32

    private val HEX = "0123456789abcdef".toCharArray()

    /**
     * @param random sumber acak; dapat diganti di test
     * @return token hex sepanjang [BYTE_LENGTH] * 2 karakter, huruf kecil
     */
    fun generate(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(BYTE_LENGTH)
        random.nextBytes(bytes)
        val chars = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            chars[index * 2] = HEX[value ushr 4]
            chars[index * 2 + 1] = HEX[value and 0x0F]
        }
        return String(chars)
    }
}
