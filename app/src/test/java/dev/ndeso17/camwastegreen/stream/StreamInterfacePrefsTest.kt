package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test parsing pilihan interface (WP-34).
 *
 * Hanya bagian murni [StreamInterfaceChoice] yang diuji. Pembungkus
 * `SharedPreferences` butuh `Context` dan karena itu tidak diuji di JVM tanpa
 * Robolectric; yang diuji adalah logika yang menentukan nilai tersimpan.
 */
class StreamInterfacePrefsTest {

    @Test
    fun `parseChoice null menghasilkan ALL`() {
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice(null))
    }

    @Test
    fun `parseChoice string kosong dan spasi menghasilkan ALL`() {
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice(""))
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice("   "))
    }

    @Test
    fun `parseChoice nilai tidak dikenal menghasilkan ALL`() {
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice("bogus"))
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice("WIFI_LAN"))
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice("wifi-lan"))
    }

    @Test
    fun `parseChoice mengenali setiap nilai sah`() {
        assertEquals(NetworkChoice.ALL, StreamInterfaceChoice.parseChoice("all"))
        assertEquals(NetworkChoice.WIFI_LAN, StreamInterfaceChoice.parseChoice("wifi_lan"))
        assertEquals(
            NetworkChoice.USB_TETHERING,
            StreamInterfaceChoice.parseChoice("usb_tethering")
        )
    }

    @Test
    fun `round trip serialize lalu parseChoice untuk tiga pilihan`() {
        NetworkChoice.values().forEach { choice ->
            val raw = StreamInterfaceChoice.serialize(choice)
            assertEquals(choice, StreamInterfaceChoice.parseChoice(raw))
        }
    }

    @Test
    fun `serialize memakai nilai penyimpanan bukan nama enum`() {
        assertEquals("all", StreamInterfaceChoice.serialize(NetworkChoice.ALL))
        assertEquals("wifi_lan", StreamInterfaceChoice.serialize(NetworkChoice.WIFI_LAN))
        assertEquals(
            "usb_tethering",
            StreamInterfaceChoice.serialize(NetworkChoice.USB_TETHERING)
        )
    }

    @Test
    fun `nilai tersimpan di sekitar spasi tetap dikenali`() {
        assertEquals(NetworkChoice.WIFI_LAN, StreamInterfaceChoice.parseChoice(" wifi_lan "))
    }
}
