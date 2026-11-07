package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test [NetworkChoice] dan fungsi murninya (WP-33).
 *
 * Semua kasus berjalan di JVM tanpa perangkat: tidak ada `android.*` dan tidak
 * ada enumerasi interface nyata.
 */
class NetworkChoiceTest {

    private val wlan = NetworkCandidate("wlan0", "192.168.1.10")
    private val usb = NetworkCandidate("usb0", "192.168.42.129")
    private val rndis = NetworkCandidate("rndis0", "192.168.42.200")
    private val eth = NetworkCandidate("eth0", "10.0.0.5")
    private val seluler = NetworkCandidate("rmnet_data0", "10.20.30.40")

    @Test
    fun `classify mengenali interface USB tethering`() {
        assertEquals(NetworkChoice.USB_TETHERING, NetworkChoice.classify("usb0"))
        assertEquals(NetworkChoice.USB_TETHERING, NetworkChoice.classify("rndis0"))
        assertEquals(NetworkChoice.USB_TETHERING, NetworkChoice.classify("eth0"))
    }

    @Test
    fun `classify mengenali awalan wlan`() {
        assertEquals(NetworkChoice.WIFI_LAN, NetworkChoice.classify("wlan0"))
        assertEquals(NetworkChoice.WIFI_LAN, NetworkChoice.classify("wlan1"))
    }

    @Test
    fun `classify menolak nama yang tidak dikenal`() {
        assertNull(NetworkChoice.classify("lo"))
        assertNull(NetworkChoice.classify("rmnet_data0"))
        assertNull(NetworkChoice.classify("tun0"))
        assertNull(NetworkChoice.classify(""))
        // Bukan awalan wlan yang sah: hanya "wlan" di awal yang diterima.
        assertNull(NetworkChoice.classify("swlan0"))
    }

    @Test
    fun `selectCandidates menyaring sesuai pilihan`() {
        val semua = listOf(wlan, usb, rndis, eth, seluler)

        assertEquals(
            listOf(wlan),
            selectCandidates(NetworkChoice.WIFI_LAN, semua)
        )
        assertEquals(
            listOf(usb, rndis, eth),
            selectCandidates(NetworkChoice.USB_TETHERING, semua)
        )
        assertEquals(semua, selectCandidates(NetworkChoice.ALL, semua))
    }

    @Test
    fun `resolveBindAddress ALL selalu 0_0_0_0`() {
        assertEquals(
            NetworkChoice.ANY_ADDRESS,
            resolveBindAddress(NetworkChoice.ALL, listOf(wlan, usb))
        )
        // Tetap 0.0.0.0 walau daftar kandidat kosong: ALL tidak memilih
        // interface, jadi tidak butuh kandidat.
        assertEquals(
            NetworkChoice.ANY_ADDRESS,
            resolveBindAddress(NetworkChoice.ALL, emptyList())
        )
    }

    @Test
    fun `resolveBindAddress mengembalikan alamat pertama yang cocok`() {
        val semua = listOf(seluler, wlan, usb, rndis)

        assertEquals(wlan.address, resolveBindAddress(NetworkChoice.WIFI_LAN, semua))
        assertEquals(usb.address, resolveBindAddress(NetworkChoice.USB_TETHERING, semua))
    }

    @Test
    fun `USB_TETHERING tanpa kandidat USB mengembalikan null`() {
        val hanyaWifi = listOf(wlan, seluler)

        assertNull(resolveBindAddress(NetworkChoice.USB_TETHERING, hanyaWifi))
        assertNull(resolveBindAddress(NetworkChoice.WIFI_LAN, listOf(usb)))
        assertNull(resolveBindAddress(NetworkChoice.USB_TETHERING, emptyList()))
    }

    @Test
    fun `setiap pilihan punya label dan nilai penyimpanan berbeda`() {
        val values = NetworkChoice.values().toList()

        assertEquals(3, values.size)
        assertEquals(values.size, values.map { it.label }.toSet().size)
        assertEquals(values.size, values.map { it.storageValue }.toSet().size)
        assertTrue(values.all { it.label.isNotBlank() && it.storageValue.isNotBlank() })
    }
}
