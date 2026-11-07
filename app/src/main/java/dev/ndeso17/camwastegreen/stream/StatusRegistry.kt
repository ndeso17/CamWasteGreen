package dev.ndeso17.camwastegreen.stream

/**
 * Status kamera dan stream yang diukur, bukan dari nilai konfigurasi.
 *
 * Nilai yang belum diketahui bernilai `null`, bukan `0`. `null` berarti belum
 * terukur; `0` berarti terukur nol. Klien dapat membedakan keduanya.
 */
data class StatusSnapshot(
    val frameSeq: Long?,
    val lastFrameAtMs: Long?,
    val measuredFps: Double?,
    val width: Int?,
    val height: Int?,
    val clients: Int,
    val cameraState: String,
    val cameraStateReason: String?
)

/**
 * Registry status yang dipakai server untuk menjawab `/status`.
 *
 * FPS dihitung dari jumlah frame yang masuk dalam jendela bergulir
 * (default 1000 ms), bukan dari nilai FPS konfigurasi. Waktu datang dari
 * sumber yang disuntikkan (`nowMs`) supaya hasil dapat diuji tanpa menunggu
 * waktu nyata.
 *
 * Kelas ini murni Kotlin tanpa `android.*`.
 */
class StatusRegistry(
    private val nowMs: () -> Long,
    private val windowMs: Long = 1000L
) {

    /** Keadaan kamera yang dapat muncul pada `cameraState`. */
    object CameraState {
        const val STARTING = "starting"
        const val STREAMING = "streaming"
        const val ERROR = "error"
        const val STOPPED = "stopped"
    }

    private val lock = Any()
    private val frameTimes = ArrayDeque<Long>()

    private var lastPacket: FramePacket? = null
    private var cameraState: String = CameraState.STARTING
    private var cameraStateReason: String? = null
    private var clientCount: Int = 0

    /**
     * Catat frame yang baru diterima.
     *
     * Waktu tangkap diambil dari paket, bukan dari jam internal, sehingga
     * pengukuran FPS memakai waktu tangkap frame yang sama dengan metadata
     * yang dikirim ke klien.
     */
    fun onFrame(packet: FramePacket) {
        synchronized(lock) {
            lastPacket = packet
            val now = nowMs()
            frameTimes.addLast(now)
            val cutoff = now - windowMs
            while (frameTimes.isNotEmpty() && frameTimes.first() < cutoff) {
                frameTimes.removeFirst()
            }
        }
    }

    /** Catat keadaan kamera beserta alasan singkat bila ada. */
    fun setCameraState(state: String, reason: String? = null) {
        synchronized(lock) {
            cameraState = state
            cameraStateReason = reason
        }
    }

    /** Set jumlah klien stream yang sedang terhubung. */
    fun setClients(count: Int) {
        synchronized(lock) {
            clientCount = count
        }
    }

    /**
     * Ambil status saat ini.
     *
     * `measuredFps` bernilai `null` bila belum ada frame, `0.0` bila frame
     * pernah masuk tetapi jendela terakhir kosong (stream berhenti).
     */
    fun snapshot(): StatusSnapshot {
        synchronized(lock) {
            val packet = lastPacket
            val now = nowMs()
            val cutoff = now - windowMs
            while (frameTimes.isNotEmpty() && frameTimes.first() < cutoff) {
                frameTimes.removeFirst()
            }

            val measuredFps = when {
                packet == null -> null
                frameTimes.size < 2 -> 0.0
                else -> {
                    val span = frameTimes.last() - frameTimes.first()
                    if (span <= 0L) 0.0
                    else (frameTimes.size - 1) * 1000.0 / span
                }
            }

            return StatusSnapshot(
                frameSeq = packet?.seq,
                lastFrameAtMs = packet?.capturedAtMs,
                measuredFps = measuredFps,
                width = packet?.width,
                height = packet?.height,
                clients = clientCount,
                cameraState = cameraState,
                cameraStateReason = cameraStateReason
            )
        }
    }

    /** Hapus riwayat frame tanpa mengubah `cameraState`. */
    fun resetFrames() {
        synchronized(lock) {
            frameTimes.clear()
            lastPacket = null
        }
    }
}
