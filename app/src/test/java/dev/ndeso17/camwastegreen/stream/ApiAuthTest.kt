package dev.ndeso17.camwastegreen.stream

import dev.ndeso17.camwastegreen.stream.ApiAuth.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiAuthTest {

    private val key = "rahasia-uji-123"
    private val lan = "10.42.0.176"

    @Test
    fun tokenBenarDiterima() {
        assertEquals(Result.ALLOWED, ApiAuth.authorize(key, key, lan, emptySet()))
    }

    @Test
    fun tokenSalahDitolakWrongKey() {
        assertEquals(Result.WRONG_KEY, ApiAuth.authorize("salah", key, lan, emptySet()))
    }

    @Test
    fun tokenKosongDanNullDitolakMissingKey() {
        assertEquals(Result.MISSING_KEY, ApiAuth.authorize(null, key, lan, emptySet()))
        assertEquals(Result.MISSING_KEY, ApiAuth.authorize("", key, lan, emptySet()))
    }

    @Test
    fun alamatDiLuarDaftarDitolak() {
        val allowed = setOf("10.42.0.10")
        assertEquals(
            Result.ADDRESS_NOT_ALLOWED,
            ApiAuth.authorize(key, key, lan, allowed)
        )
    }

    @Test
    fun alamatDalamDaftarDiterima() {
        val allowed = setOf(lan, "10.42.0.10")
        assertEquals(Result.ALLOWED, ApiAuth.authorize(key, key, lan, allowed))
    }

    @Test
    fun daftarAlamatKosongTanpaBatasAlamat() {
        assertEquals(Result.ALLOWED, ApiAuth.authorize(key, key, "203.0.113.9", emptySet()))
    }

    @Test
    fun tokenServerKosongMisconfigured() {
        assertEquals(Result.SERVER_MISCONFIGURED, ApiAuth.authorize(key, "", lan, emptySet()))
        assertEquals(Result.SERVER_MISCONFIGURED, ApiAuth.authorize("", "", lan, emptySet()))
    }

    @Test
    fun prefiksTokenDitolakWrongKey() {
        assertEquals(Result.WRONG_KEY, ApiAuth.authorize("rahasia-uji", key, lan, emptySet()))
    }

    @Test
    fun parseAddressesMembuangSpasiDanEntriKosong() {
        assertEquals(
            setOf("10.42.0.10", "10.42.0.11"),
            ApiAuth.parseAddresses(" 10.42.0.10, ,10.42.0.11,")
        )
        assertEquals(emptySet<String>(), ApiAuth.parseAddresses(""))
    }
}
