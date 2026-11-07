package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FrameSourceTest {

    private fun jpeg(tag: Int) = byteArrayOf(tag.toByte(), tag.toByte(), tag.toByte())

    @Test
    fun seqNaikSatuSetiapPublish() {
        val source = FrameSource()
        assertEquals(1L, source.publish(jpeg(1), 64, 48, 1000L).seq)
        assertEquals(2L, source.publish(jpeg(2), 64, 48, 1010L).seq)
        assertEquals(3L, source.publish(jpeg(3), 64, 48, 1020L).seq)
    }

    @Test
    fun latestMenunjukPaketTerakhir() {
        val source = FrameSource()
        source.publish(jpeg(1), 64, 48, 1000L)
        val second = source.publish(jpeg(2), 32, 24, 1010L)

        assertEquals(second, source.latest)
        assertEquals(2L, source.latest?.seq)
        assertEquals(32, source.latest?.width)
        assertEquals(24, source.latest?.height)
    }

    @Test
    fun lastFrameAtMsMengikutiCapturedAtMs() {
        val source = FrameSource()
        source.publish(jpeg(1), 64, 48, 5_000L)
        assertEquals(5_000L, source.lastFrameAtMs)

        source.publish(jpeg(2), 64, 48, 7_250L)
        assertEquals(7_250L, source.lastFrameAtMs)
    }

    @Test
    fun belumAdaFrameBerartiNull() {
        val source = FrameSource()
        assertNull(source.latest)
        assertNull(source.lastSeq)
        assertNull(source.lastFrameAtMs)
    }

    @Test
    fun publishDariDuaThreadTidakMenghasilkanSeqDuplikat() {
        val source = FrameSource()
        val perThread = 2000
        val threads = 8
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)
        val collected = java.util.Collections.synchronizedList(mutableListOf<Long>())

        repeat(threads) { t ->
            pool.submit {
                start.await()
                repeat(perThread) { i ->
                    val packet = source.publish(jpeg(t), 64, 48, (t * perThread + i).toLong())
                    collected.add(packet.seq)
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue("thread tidak selesai", pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(threads * perThread, collected.size)
        assertEquals(threads * perThread, collected.toSet().size)
        assertEquals((threads * perThread).toLong(), source.lastSeq)
        assertEquals((1L..(threads * perThread).toLong()).toSet(), collected.toSet())
    }

    @Test
    fun seqTidakBerulangSaatPublishBerulangCepat() {
        val source = FrameSource()
        val seen = mutableSetOf<Long>()
        repeat(1000) { i ->
            val packet = source.publish(jpeg(i), 8, 8, i.toLong())
            assertTrue("seq berulang: ${packet.seq}", seen.add(packet.seq))
        }
        assertEquals(1000, seen.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun dimensiTidakSahDitolak() {
        FrameSource().publish(jpeg(1), 0, 48, 1L)
    }

    @Test
    fun clearTidakMengulangNomorUrut() {
        val source = FrameSource()
        source.publish(jpeg(1), 64, 48, 1000L)
        source.clear()
        assertNull(source.latest)
        assertEquals(2L, source.publish(jpeg(2), 64, 48, 1010L).seq)
    }
}
