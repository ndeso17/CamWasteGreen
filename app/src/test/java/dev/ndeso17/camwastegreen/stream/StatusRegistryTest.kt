package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StatusRegistryTest {

    private var clock = 1_000_000L

    private fun registry(windowMs: Long = 1000L) = StatusRegistry(nowMs = { clock }, windowMs = windowMs)

    private fun packet(seq: Long, at: Long, w: Int = 1280, h: Int = 720) =
        FramePacket(seq, at, w, h, byteArrayOf(1, 2, 3))

    @Test
    fun fpsTerukurDari11FrameJarak100ms() {
        val registry = registry()
        // 11 frame pada jarak 100 ms menempati rentang 1000 ms.
        repeat(11) { i ->
            clock = 1_000_000L + i * 100L
            registry.onFrame(packet(i + 1L, clock))
        }
        clock = 1_000_000L + 1000L
        val fps = registry.snapshot().measuredFps
        assertNotNull(fps)
        // 10 selang selama 1000 ms -> 10 fps.
        assertEquals(10.0, fps!!, 0.001)
    }

    @Test
    fun fpsNullSaatBelumAdaFrame() {
        val registry = registry()
        clock += 5000
        val snapshot = registry.snapshot()
        assertNull(snapshot.measuredFps)
        assertNull(snapshot.frameSeq)
        assertNull(snapshot.lastFrameAtMs)
        assertNull(snapshot.width)
        assertNull(snapshot.height)
    }

    @Test
    fun fpsNolSetelahJendelaKosong() {
        val registry = registry()
        clock = 2_000_000L
        registry.onFrame(packet(1L, clock))
        clock += 1500L // lewat jendela 1000 ms
        assertEquals(0.0, registry.snapshot().measuredFps!!, 0.0)
    }

    @Test
    fun frameSeqDanLastFrameAtMsMengikutiPaketTerakhir() {
        val registry = registry()
        clock = 3_000_000L
        registry.onFrame(packet(1L, clock, 640, 480))
        clock += 100
        registry.onFrame(packet(2L, clock, 1280, 720))

        val snapshot = registry.snapshot()
        assertEquals(2L, snapshot.frameSeq)
        assertEquals(clock, snapshot.lastFrameAtMs)
        assertEquals(1280, snapshot.width)
        assertEquals(720, snapshot.height)
    }

    @Test
    fun cameraStateBerubahSaatKegagalanDicatat() {
        val registry = registry()
        assertEquals(StatusRegistry.CameraState.STARTING, registry.snapshot().cameraState)
        assertNull(registry.snapshot().cameraStateReason)

        registry.setCameraState(StatusRegistry.CameraState.STREAMING)
        assertEquals("streaming", registry.snapshot().cameraState)
        assertNull(registry.snapshot().cameraStateReason)

        registry.setCameraState(StatusRegistry.CameraState.ERROR, "camera_disconnected")
        val failed = registry.snapshot()
        assertEquals("error", failed.cameraState)
        assertEquals("camera_disconnected", failed.cameraStateReason)
    }

    @Test
    fun clientsMengikutiNilaiYangDiset() {
        val registry = registry()
        assertEquals(0, registry.snapshot().clients)
        registry.setClients(2)
        assertEquals(2, registry.snapshot().clients)
    }

    @Test
    fun jendelaBergulirMembuangFrameLama() {
        val registry = registry(windowMs = 500L)
        clock = 4_000_000L
        // Empat frame dalam 300 ms, lalu berhenti.
        repeat(4) { i ->
            clock = 4_000_000L + i * 100L
            registry.onFrame(packet(i + 1L, clock))
        }
        // Nilai FPS dengan 3 selang selama 300 ms -> 10 fps.
        assertEquals(10.0, registry.snapshot().measuredFps!!, 0.001)

        // Lewat jendela: seluruh frame lama terbuang, FPS menjadi 0.
        clock = 4_000_000L + 2000L
        assertEquals(0.0, registry.snapshot().measuredFps!!, 0.0)
    }

    @Test
    fun resetFramesMenghapusFpsTetapiMempertahankanCameraState() {
        val registry = registry()
        clock = 5_000_000L
        registry.onFrame(packet(1L, clock))
        registry.setCameraState(StatusRegistry.CameraState.ERROR, "encoder_failed")

        registry.resetFrames()

        val snapshot = registry.snapshot()
        assertNull(snapshot.measuredFps)
        assertNull(snapshot.frameSeq)
        assertEquals("error", snapshot.cameraState)
        assertEquals("encoder_failed", snapshot.cameraStateReason)
    }

    @Test
    fun tuningStateAwalNullDanTidakMengubahCameraState() {
        val registry = registry()
        clock = 6_000_000L
        registry.onFrame(packet(1L, clock))
        registry.setCameraState(StatusRegistry.CameraState.STREAMING)

        assertNull(registry.snapshot().tuningState)

        registry.setTuningState("unsupported")

        val snapshot = registry.snapshot()
        assertEquals("unsupported", snapshot.tuningState)
        // Kunci yang tidak didukung bukan kegagalan stream (CWG-16).
        assertEquals(StatusRegistry.CameraState.STREAMING, snapshot.cameraState)
        assertNull(snapshot.cameraStateReason)
    }
}
