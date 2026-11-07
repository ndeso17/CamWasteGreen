package dev.ndeso17.camwastegreen.stream

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONObject
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * Server MJPEG berbasis [FrameSource] dan [StatusRegistry].
 *
 * Perbedaan penting dari server lama:
 *
 * - `/video` hanya menulis frame yang **baru**. Klien tidak menerima frame
 *   yang sama berulang-ulang, dan tidak ada jeda waktu tetap sebagai penentu
 *   laju. Penungguan memakai `Condition` yang dibangunkan
 *   [notifyFrameAvailable].
 * - `/status` menjawab dari [StatusRegistry.snapshot], jadi `measuredFps`
 *   berasal dari penghitung frame dan nilai yang belum terukur dikirim
 *   sebagai `null`.
 * - Jumlah klien dibatasi [maxClients]; klien yang melebihi batas menerima
 *   `503`.
 * - Klien yang tulisannya melewati [writeTimeoutMs] diputus.
 *
 * Kelas ini memakai NanoHTTPD hanya sebagai lapisan HTTP. Logika urutan frame,
 * batas klien, dan waktu tulis berada di kelas murni yang diuji terpisah
 * ([MultipartWriter], [StreamSession]).
 */
class MjpegHttpServer(
    private val frameSource: FrameSource,
    private val statusRegistry: StatusRegistry,
    port: Int,
    private val maxClients: Int,
    private val writeTimeoutMs: Long
) : NanoHTTPD(port) {

    /** Slot klien bersama; satu instance agar batas berlaku lintas koneksi. */
    private val clientSlots = StreamSession(
        maxClients = maxClients,
        writeTimeoutMs = writeTimeoutMs,
        nowMs = { System.currentTimeMillis() }
    )

    private val executor = Executors.newCachedThreadPool()

    private val frameLock = ReentrantLock()

    private val frameChanged = frameLock.newCondition()

    private val streams = mutableListOf<PipedOutputStream>()

    @Volatile
    private var running = true

    /** Bangunkan penulis yang sedang menunggu frame baru. */
    fun notifyFrameAvailable() {
        frameLock.lock()
        try {
            frameChanged.signalAll()
        } finally {
            frameLock.unlock()
        }
    }

    override fun serve(session: IHTTPSession?): Response {
        return when (session?.uri ?: "/") {
            "/" -> newFixedLengthResponse(Response.Status.OK, "text/html", diagnosticPage())
            "/video" -> handleVideo()
            "/status" -> handleStatus()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
        }
    }

    private fun handleVideo(): Response {
        if (!clientSlots.tryAcquire()) {
            return newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                "text/plain",
                "Batas $maxClients klien tercapai"
            )
        }
        statusRegistry.setClients(clientSlots.clients)

        val pipeIn = PipedInputStream(PIPE_BUFFER_BYTES)
        val pipeOut = PipedOutputStream(pipeIn)
        synchronized(streams) { streams.add(pipeOut) }

        // Sesi per klien: dedupe dan penanda lambat milik klien ini sendiri.
        val session = StreamSession(
            maxClients = 1,
            writeTimeoutMs = writeTimeoutMs,
            nowMs = { System.currentTimeMillis() }
        )

        executor.execute {
            try {
                var lastSent: Long? = null
                while (running && !Thread.currentThread().isInterrupted) {
                    val packet = awaitFrameAfter(lastSent)
                    if (packet == null) {
                        if (!running) break
                        continue
                    }
                    if (!session.shouldSend(packet)) continue
                    if (session.isSlowClient) break

                    session.onWriteStarted()
                    pipeOut.write(MultipartWriter.partHeader(packet, BOUNDARY))
                    pipeOut.write(packet.jpeg)
                    pipeOut.write(CRLF)
                    pipeOut.flush()
                    session.onPacketSent(packet)
                    lastSent = packet.seq
                    if (session.onWriteFinished()) break
                }
            } catch (_: Exception) {
                // Klien memutus koneksi atau pipa ditutup saat server berhenti.
            } finally {
                synchronized(streams) { streams.remove(pipeOut) }
                try { pipeOut.close() } catch (_: Exception) {}
                clientSlots.release()
                statusRegistry.setClients(clientSlots.clients)
            }
        }

        return newChunkedResponse(
            Response.Status.OK,
            "multipart/x-mixed-replace; boundary=$BOUNDARY",
            pipeIn
        )
    }

    /**
     * Tunggu sampai ada frame dengan `seq` lebih besar dari [afterSeq].
     *
     * @return frame baru, atau `null` bila waktu tunggu habis atau server
     *   berhenti; pemanggil memeriksa [running] untuk membedakannya
     */
    private fun awaitFrameAfter(afterSeq: Long?): FramePacket? {
        frameLock.lock()
        try {
            while (running) {
                val latest = frameSource.latest
                if (latest != null && (afterSeq == null || latest.seq > afterSeq)) {
                    return latest
                }
                if (!frameChanged.await(WAIT_SLICE_MS, TimeUnit.MILLISECONDS)) {
                    return null
                }
            }
            return null
        } finally {
            frameLock.unlock()
        }
    }

    private fun handleStatus(): Response {
        val snapshot = statusRegistry.snapshot()
        val json = JSONObject().apply {
            put("streaming", snapshot.cameraState == StatusRegistry.CameraState.STREAMING)
            put("cameraState", snapshot.cameraState)
            put("cameraStateReason", snapshot.cameraStateReason ?: JSONObject.NULL)
            put("frameSeq", snapshot.frameSeq ?: JSONObject.NULL)
            put("lastFrameAtMs", snapshot.lastFrameAtMs ?: JSONObject.NULL)
            put("measuredFps", snapshot.measuredFps ?: JSONObject.NULL)
            put("width", snapshot.width ?: JSONObject.NULL)
            put("height", snapshot.height ?: JSONObject.NULL)
            put("clients", snapshot.clients)
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun diagnosticPage(): String = """
        <html>
          <head><title>CamWasteGreen</title></head>
          <body style="margin:0;background:#000;color:#fff;font-family:sans-serif;">
            <h3 style="text-align:center;">CamWasteGreen</h3>
            <p style="text-align:center;">
              <a style="color:#8cf;" href="/status">/status</a>
              <img src="/video" style="width:100%;" alt="stream" />
            </p>
          </body>
        </html>
    """.trimIndent()

    /**
     * Tutup semua pipa klien lalu hentikan penulis. Penutupan pipa membangunkan
     * penulisan yang sedang menunggu.
     */
    fun shutdown() {
        running = false
        notifyFrameAvailable()
        val open: List<PipedOutputStream>
        synchronized(streams) {
            open = streams.toList()
            streams.clear()
        }
        open.forEach { try { it.close() } catch (_: Exception) {} }
        executor.shutdownNow()
    }

    companion object {
        /** Batas part; hanya ASCII agar aman pada klien sederhana. */
        const val BOUNDARY = "camwastegreen"

        private val CRLF = "\r\n".toByteArray(Charsets.US_ASCII)

        private const val PIPE_BUFFER_BYTES = 65536

        /**
         * Pengaman agar penulis tidak menggantung selamanya ketika kamera
         * berhenti tanpa [shutdown]. Bukan pengatur laju frame.
         */
        private const val WAIT_SLICE_MS = 1000L
    }
}
