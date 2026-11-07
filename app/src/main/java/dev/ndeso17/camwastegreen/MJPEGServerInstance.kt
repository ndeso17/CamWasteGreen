package dev.ndeso17.camwastegreen

import android.content.Context
import android.util.Log
import dev.ndeso17.camwastegreen.stream.FrameSource
import dev.ndeso17.camwastegreen.stream.MjpegHttpServer
import dev.ndeso17.camwastegreen.stream.StatusRegistry

/**
 * Singleton pemilik server MJPEG, [FrameSource], dan [StatusRegistry].
 *
 * Tujuan: server tidak dibuat ulang saat orientasi berubah (Activity restart),
 * dan status tetap terbaca walau Activity sedang dibuat ulang.
 *
 * [publishFrame] adalah satu-satunya jalan masuk frame. Pemanggil memberi waktu
 * tangkap sendiri (dari `imageProxy`), bukan dari jam di dalam server.
 */
object MJPEGServerInstance {

    private const val TAG = "MJPEGServerInstance"

    /** Default konfigurasi, bukan ambang mutu (D-29). */
    private const val DEFAULT_PORT = 8085
    private const val DEFAULT_MAX_CLIENTS = 2
    private const val DEFAULT_WRITE_TIMEOUT_MS = 5000L

    private val frameSource = FrameSource()

    private val statusRegistry = StatusRegistry(nowMs = { System.currentTimeMillis() })

    @Volatile
    private var server: MjpegHttpServer? = null

    /**
     * Mulai server jika belum berjalan.
     *
     * @return `true` bila server berjalan (baru atau sudah), `false` bila gagal
     */
    @Synchronized
    fun start(context: Context, port: Int = DEFAULT_PORT): Boolean {
        server?.let {
            Log.i(TAG, "Server sudah berjalan di port ${it.listeningPort}")
            return true
        }

        return try {
            val created = MjpegHttpServer(
                frameSource = frameSource,
                statusRegistry = statusRegistry,
                port = port,
                maxClients = DEFAULT_MAX_CLIENTS,
                writeTimeoutMs = DEFAULT_WRITE_TIMEOUT_MS
            )
            created.start(SOCKET_READ_TIMEOUT_MS, true)
            server = created
            statusRegistry.setCameraState(StatusRegistry.CameraState.STARTING)
            Log.i(TAG, "Server MJPEG berjalan di port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulai server: ${e.message}", e)
            statusRegistry.setCameraState(StatusRegistry.CameraState.ERROR, "server_start_failed")
            false
        }
    }

    /**
     * Terbitkan satu frame baru dan bangunkan klien yang menunggu.
     *
     * @param capturedAtMs waktu tangkap dari pemanggil (bukan waktu sekarang
     *   di dalam fungsi ini)
     */
    fun publishFrame(jpeg: ByteArray, width: Int, height: Int, capturedAtMs: Long) {
        val packet = try {
            frameSource.publish(jpeg, width, height, capturedAtMs)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Frame ditolak: ${e.message}")
            statusRegistry.setCameraState(StatusRegistry.CameraState.ERROR, "invalid_frame_dimensions")
            return
        }
        statusRegistry.onFrame(packet)
        statusRegistry.setCameraState(StatusRegistry.CameraState.STREAMING)
        server?.notifyFrameAvailable()
    }

    /** Catat kegagalan kamera agar `/status` melaporkan alasannya. */
    fun reportCameraError(reason: String) {
        statusRegistry.setCameraState(StatusRegistry.CameraState.ERROR, reason)
    }

    /** Hentikan server dan bersihkan status. */
    @Synchronized
    fun stop() {
        try {
            server?.shutdown()
            server?.stop()
            Log.i(TAG, "Server MJPEG dihentikan")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghentikan server: ${e.message}", e)
        } finally {
            server = null
            frameSource.clear()
            statusRegistry.resetFrames()
            statusRegistry.setCameraState(StatusRegistry.CameraState.STOPPED)
        }
    }

    /** Apakah server sedang berjalan. */
    fun isRunning(): Boolean = server != null

    /** Port aktif, atau `null` bila server tidak berjalan. */
    fun getPort(): Int? = server?.listeningPort

    /** Status terakhir untuk ditampilkan Activity. */
    fun statusSnapshot() = statusRegistry.snapshot()

    /**
     * Batas tunggu socket NanoHTTPD. Bukan ambang mutu stream; hanya agar
     * socket klien yang mati tidak menahan thread selamanya.
     */
    private const val SOCKET_READ_TIMEOUT_MS = 5000
}
