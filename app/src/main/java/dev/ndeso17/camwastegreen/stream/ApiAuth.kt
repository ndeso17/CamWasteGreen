package dev.ndeso17.camwastegreen.stream

import java.security.MessageDigest

/**
 * Otorisasi permintaan HTTP untuk `/video` dan `/status` (D-27).
 *
 * Murni JVM: tidak memakai kelas platform Android, sehingga bisa diuji tanpa
 * perangkat. Token dibandingkan dengan [MessageDigest.isEqual] agar waktu
 * respons tidak membocorkan panjang atau awalan token.
 */
object ApiAuth {

    enum class Result {
        /** Permintaan diterima. */
        ALLOWED,

        /** Header `X-Api-Key` tidak ada atau kosong. */
        MISSING_KEY,

        /** Token ada tetapi tidak sama dengan token server. */
        WRONG_KEY,

        /** Alamat klien tidak termasuk daftar yang diizinkan. */
        ADDRESS_NOT_ALLOWED,

        /** Token server kosong; server menolak melayani agar tidak terbuka tanpa kunci. */
        SERVER_MISCONFIGURED
    }

    /**
     * @param providedKey nilai header `X-Api-Key`, `null` bila tidak ada
     * @param expectedKey token yang dikonfigurasi di server
     * @param remoteAddress alamat IP klien
     * @param allowedAddresses daftar alamat yang diizinkan; kosong berarti tanpa batas alamat
     */
    /** Pecah daftar alamat dipisah koma; spasi dibuang, entri kosong diabaikan. */
    fun parseAddresses(raw: String): Set<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun authorize(
        providedKey: String?,
        expectedKey: String,
        remoteAddress: String,
        allowedAddresses: Set<String>
    ): Result {
        if (expectedKey.isEmpty()) return Result.SERVER_MISCONFIGURED

        if (allowedAddresses.isNotEmpty() && remoteAddress !in allowedAddresses) {
            return Result.ADDRESS_NOT_ALLOWED
        }

        if (providedKey.isNullOrEmpty()) return Result.MISSING_KEY

        val provided = providedKey.toByteArray(Charsets.UTF_8)
        val expected = expectedKey.toByteArray(Charsets.UTF_8)
        return if (MessageDigest.isEqual(provided, expected)) {
            Result.ALLOWED
        } else {
            Result.WRONG_KEY
        }
    }
}
