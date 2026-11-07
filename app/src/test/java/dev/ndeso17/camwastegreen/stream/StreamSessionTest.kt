package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamSessionTest {

    private var clock = 10_000L

    private fun session(maxClients: Int = 2, timeoutMs: Long = 500L) =
        StreamSession(maxClients = maxClients, writeTimeoutMs = timeoutMs, nowMs = { clock })

    private fun packet(seq: Long) = FramePacket(seq, clock, 64, 48, byteArrayOf(1))

    @Test
    fun paketDenganSeqSamaTidakDikirimDuaKali() {
        val session = session()
        val first = packet(1L)
        assertTrue(session.shouldSend(first))
        session.onPacketSent(first)

        assertFalse("seq sama tidak boleh dikirim ulang", session.shouldSend(first))
        assertEquals(1L, session.lastSeqSent)
    }

    @Test
    fun paketDenganSeqLebihKecilJugaDitolak() {
        val session = session()
        session.onPacketSent(packet(5L))
        assertFalse(session.shouldSend(packet(4L)))
        assertFalse(session.shouldSend(packet(5L)))
        assertTrue(session.shouldSend(packet(6L)))
    }

    @Test
    fun seqTidakTurunSaatPaketLebihTuaDikirim() {
        val session = session()
        session.onPacketSent(packet(5L))
        session.onPacketSent(packet(3L))
        assertEquals(5L, session.lastSeqSent)
    }

    @Test
    fun klienKetigaDitolakSaatBatasDua() {
        val session = session(maxClients = 2)
        assertTrue(session.tryAcquire())
        assertTrue(session.tryAcquire())
        assertEquals(2, session.clients)

        assertFalse("klien ketiga harus ditolak", session.tryAcquire())
        assertEquals(2, session.clients)
    }

    @Test
    fun releaseMengembalikanSlot() {
        val session = session(maxClients = 2)
        session.tryAcquire()
        session.tryAcquire()
        assertFalse(session.tryAcquire())

        assertTrue(session.release())
        assertEquals(1, session.clients)
        assertTrue("slot harus tersedia lagi", session.tryAcquire())
        assertEquals(2, session.clients)
    }

    @Test
    fun releaseTanpaSlotAktifMengembalikanFalse() {
        val session = session()
        assertFalse(session.release())
        assertEquals(0, session.clients)
    }

    @Test
    fun penandaKlienLambatAktifSetelahBatasWaktu() {
        val session = session(timeoutMs = 500L)
        session.tryAcquire()
        assertFalse(session.isSlowClient)

        session.onWriteStarted()
        clock += 200
        assertFalse("di bawah batas waktu", session.onWriteFinished())
        assertFalse(session.isSlowClient)

        session.onWriteStarted()
        clock += 800
        assertTrue("melewati batas waktu", session.onWriteFinished())
        assertTrue(session.isSlowClient)
    }

    @Test
    fun penulisanTepatPadaBatasTidakDitandaiLambat() {
        val session = session(timeoutMs = 500L)
        session.onWriteStarted()
        clock += 500
        assertFalse(session.onWriteFinished())
        assertFalse(session.isSlowClient)
    }

    @Test
    fun releaseMembersihkanPenandaLambat() {
        val session = session(timeoutMs = 100L)
        session.tryAcquire()
        session.onWriteStarted()
        clock += 500
        session.onWriteFinished()
        assertTrue(session.isSlowClient)

        session.release()
        assertFalse(session.isSlowClient)
    }

    @Test
    fun awalSesiBelumMengirimApaPun() {
        val session = session()
        assertNull(session.lastSeqSent)
        assertTrue("paket pertama selalu dikirim", session.shouldSend(packet(1L)))
        assertEquals(0, session.clients)
    }

    @Test(expected = IllegalArgumentException::class)
    fun batasKlienTidakSahDitolak() {
        StreamSession(maxClients = 0, writeTimeoutMs = 100L) { clock }
    }

    @Test(expected = IllegalArgumentException::class)
    fun batasWaktuTidakSahDitolak() {
        StreamSession(maxClients = 1, writeTimeoutMs = 0L) { clock }
    }
}
