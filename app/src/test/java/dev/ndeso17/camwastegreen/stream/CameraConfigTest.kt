package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test [CameraConfig] (CWG-08, CWG-N06).
 *
 * Angka default diuji terhadap D-29. Bila default diubah tanpa memperbarui
 * keputusan itu, test ini harus gagal — itulah gunanya.
 */
class CameraConfigTest {

    @Test
    fun `default sesuai D-29`() {
        val config = CameraConfig()

        assertEquals(1280, config.width)
        assertEquals(720, config.height)
        assertEquals(20, config.targetFps)
        assertEquals(80, config.jpegQuality)
        assertEquals(2, config.maxClients)
        assertEquals(8085, config.port)
        assertEquals(0, config.mountRotationDegrees)
        assertTrue(config.writeTimeoutMs > 0)
        assertEquals("", config.apiKey)
    }

    @Test
    fun `targetFps di luar 1 sampai 30 ditolak`() {
        assertDitolak("targetFps harus di 1..30") { CameraConfig(targetFps = 0) }
        assertDitolak("targetFps harus di 1..30") { CameraConfig(targetFps = 31) }
        assertDitolak("targetFps harus di 1..30") { CameraConfig(targetFps = -5) }
    }

    @Test
    fun `targetFps batas 1 dan 30 diterima`() {
        assertEquals(1, CameraConfig(targetFps = 1).targetFps)
        assertEquals(30, CameraConfig(targetFps = 30).targetFps)
    }

    @Test
    fun `jpegQuality di luar 30 sampai 100 ditolak`() {
        assertDitolak("jpegQuality harus di 30..100") { CameraConfig(jpegQuality = 29) }
        assertDitolak("jpegQuality harus di 30..100") { CameraConfig(jpegQuality = 101) }
    }

    @Test
    fun `maxClients kurang dari 1 ditolak`() {
        assertDitolak("maxClients minimal 1") { CameraConfig(maxClients = 0) }
        assertDitolak("maxClients minimal 1") { CameraConfig(maxClients = -1) }
    }

    @Test
    fun `mountRotationDegrees bukan kelipatan 90 ditolak`() {
        assertDitolak("kelipatan 90") { CameraConfig(mountRotationDegrees = 45) }
        assertDitolak("kelipatan 90") { CameraConfig(mountRotationDegrees = 91) }
    }

    @Test
    fun `mountRotationDegrees kelipatan 90 diterima`() {
        assertEquals(90, CameraConfig(mountRotationDegrees = 90).mountRotationDegrees)
        assertEquals(180, CameraConfig(mountRotationDegrees = 180).mountRotationDegrees)
        assertEquals(270, CameraConfig(mountRotationDegrees = 270).mountRotationDegrees)
    }

    @Test
    fun `port dan dimensi tidak sah ditolak`() {
        assertDitolak("port harus di 1..65535") { CameraConfig(port = 0) }
        assertDitolak("port harus di 1..65535") { CameraConfig(port = 70000) }
        assertDitolak("width harus positif") { CameraConfig(width = 0) }
        assertDitolak("height harus positif") { CameraConfig(height = -1) }
    }

    @Test
    fun `writeTimeoutMs harus positif`() {
        assertDitolak("writeTimeoutMs harus positif") { CameraConfig(writeTimeoutMs = 0) }
    }

    /** Memastikan pesan menyebut nilai yang salah, bukan pesan generik. */
    @Test
    fun `pesan penolakan menyebut nilai`() {
        val e = assertDitolak("bukan 31") { CameraConfig(targetFps = 31) }
        assertTrue(e.message!!.contains("31"))
    }

    private fun assertDitolak(fragmen: String, blok: () -> Unit): IllegalArgumentException {
        val e = try {
            blok()
            throw AssertionError("Nilai seharusnya ditolak, tetapi diterima")
        } catch (e: IllegalArgumentException) {
            e
        }
        assertTrue(
            "Pesan \"${e.message}\" tidak memuat \"$fragmen\"",
            e.message!!.contains(fragmen)
        )
        return e
    }
}
