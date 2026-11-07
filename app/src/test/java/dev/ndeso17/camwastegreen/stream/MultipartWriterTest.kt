package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultipartWriterTest {

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())
    private val packet = FramePacket(
        seq = 42L,
        capturedAtMs = 1_700_000_000_123L,
        width = 1280,
        height = 720,
        jpeg = jpeg
    )
    private val boundary = "cwg-boundary"

    private fun headerText(): String =
        String(MultipartWriter.partHeader(packet, boundary), Charsets.US_ASCII)

    @Test
    fun enamHeaderWajibAda() {
        val text = headerText()
        assertTrue("Content-Type hilang", text.contains("Content-Type: image/jpeg\r\n"))
        assertTrue("Content-Length hilang", text.contains("Content-Length: ${jpeg.size}\r\n"))
        assertTrue("X-Frame-Seq hilang", text.contains("X-Frame-Seq: 42\r\n"))
        assertTrue(
            "X-Frame-Captured-At-Ms hilang",
            text.contains("X-Frame-Captured-At-Ms: 1700000000123\r\n")
        )
        assertTrue("X-Frame-Width hilang", text.contains("X-Frame-Width: 1280\r\n"))
        assertTrue("X-Frame-Height hilang", text.contains("X-Frame-Height: 720\r\n"))
    }

    @Test
    fun headerDiawaliBoundaryDanDiakhiriCrlfGanda() {
        val text = headerText()
        assertTrue(text.startsWith("--$boundary\r\n"))
        assertTrue(text.endsWith("\r\n\r\n"))
        val lines = text.split("\r\n")
        assertEquals("baris terakhir harus kosong", "", lines.last())
    }

    @Test
    fun contentLengthSamaDenganUkuranJpeg() {
        val text = headerText()
        val value = Regex("Content-Length: (\\d+)").find(text)?.groupValues?.get(1)
        assertEquals(jpeg.size.toString(), value)
    }

    @Test
    fun boundaryDipakaiApaAdanya() {
        val lain = "batas.Lain_123"
        val text = String(MultipartWriter.partHeader(packet, lain), Charsets.US_ASCII)
        assertTrue(text.startsWith("--$lain\r\n"))
    }

    @Test
    fun partMenggabungkanHeaderJpegDanCrlfPenutup() {
        val part = MultipartWriter.part(packet, boundary)
        val header = MultipartWriter.partHeader(packet, boundary)
        assertEquals(header.size + jpeg.size + 2, part.size)
        assertArrayEquals(header, part.copyOfRange(0, header.size))
        assertArrayEquals(jpeg, part.copyOfRange(header.size, header.size + jpeg.size))
        assertEquals('\r'.code.toByte(), part[part.size - 2])
        assertEquals('\n'.code.toByte(), part[part.size - 1])
    }

    @Test
    fun seqBerubahMengubahHeaderTanpaMenyentuhJpeg() {
        val lain = packet.copy(seq = 43L, width = 640, height = 480)
        val a = String(MultipartWriter.partHeader(packet, boundary), Charsets.US_ASCII)
        val b = String(MultipartWriter.partHeader(lain, boundary), Charsets.US_ASCII)
        assertTrue(a.contains("X-Frame-Seq: 42\r\n"))
        assertTrue(b.contains("X-Frame-Seq: 43\r\n"))
        assertTrue(b.contains("X-Frame-Width: 640\r\n"))
        assertTrue(b.contains("X-Frame-Height: 480\r\n"))
        assertArrayEquals(MultipartWriter.part(packet, boundary).copyOfRange(a.length, a.length + jpeg.size), jpeg)
    }

    @Test
    fun waktuTangkapNolTetapDitulis() {
        val nol = packet.copy(capturedAtMs = 0L)
        val text = String(MultipartWriter.partHeader(nol, boundary), Charsets.US_ASCII)
        assertTrue(text.contains("X-Frame-Captured-At-Ms: 0\r\n"))
    }
}
