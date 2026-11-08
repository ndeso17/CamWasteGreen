package dev.ndeso17.camwastegreen

import android.content.Context
import android.util.Log
import dev.ndeso17.camwastegreen.stream.ApiAuth
import dev.ndeso17.camwastegreen.stream.CameraConfig
import dev.ndeso17.camwastegreen.stream.FrameSource
import dev.ndeso17.camwastegreen.stream.MjpegHttpServer
import dev.ndeso17.camwastegreen.stream.NetworkCandidate
import dev.ndeso17.camwastegreen.stream.NetworkChoice
import dev.ndeso17.camwastegreen.stream.SessionToken
import dev.ndeso17.camwastegreen.stream.StatusRegistry
import dev.ndeso17.camwastegreen.stream.StreamInterfacePrefs
import dev.ndeso17.camwastegreen.stream.resolveBindAddress
import java.net.Inet4Address
import java.net.NetworkInterface

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

    /**
     * Konfigurasi yang sedang dipakai server.
     *
     * Satu-satunya sumber nilai port, batas klien, dan batas waktu tulis.
     * Default-nya berasal dari [CameraConfig], bukan konstanta lokal, supaya
     * resolusi/FPS/kualitas dan nilai server tidak bisa menyimpang (CWG-08).
     */
    @Volatile
    var config: CameraConfig = CameraConfig()
        private set

    private val frameSource = FrameSource()

    private val statusRegistry = StatusRegistry(nowMs = { System.currentTimeMillis() })

    @Volatile
    private var server: MjpegHttpServer? = null

    /** Token sesi berjalan; diganti setiap start, tidak pernah ditulis ke disk. */
    @Volatile
    private var currentApiKey: String? = null

    /**
     * Kandidat alamat IPv4 dari interface yang aktif sekarang.
     *
     * Satu-satunya tempat enumerasi `NetworkInterface`. Pemanggil (server dan
     * UI) memakai daftar yang sama, sehingga alamat yang ditampilkan tidak
     * dihitung dengan aturan berbeda dari alamat yang benar-benar dipakai
     * bind. Interface loopback dan yang tidak `up` dilewati.
     */
    fun candidates(): List<NetworkCandidate> {
        val result = mutableListOf<NetworkCandidate>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address.isLoopbackAddress || address !is Inet4Address) continue
                    val host = address.hostAddress ?: continue
                    result.add(NetworkCandidate(intf.name, host))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca daftar interface: ${e.message}", e)
        }
        return result
    }

    /**
     * Pilihan interface yang tersimpan di perangkat.
     *
     * Dibaca saat server distart, bukan saat frame diterbitkan, sehingga
     * pilihan hanya berpengaruh pada siklus start berikutnya.
     */
    fun storedChoice(context: Context): NetworkChoice =
        StreamInterfacePrefs(context).getChoice()

    /**
     * Alamat bind untuk [choice] berdasarkan interface yang aktif.
     *
     * @return alamat, atau `null` bila pilihan tidak tersedia. `null` tidak
     *   boleh diganti `0.0.0.0`: itu akan melebarkan bind melebihi pilihan
     *   operator.
     */
    fun bindAddressFor(choice: NetworkChoice): String? =
        resolveBindAddress(choice, candidates())

    /**
     * Mulai server jika belum berjalan.
     *
     * Alamat bind diambil dari pilihan interface tersimpan saat pemanggilan
     * ini, bukan dari nilai yang dibaca ulang per frame.
     *
     * @return `true` bila server berjalan (baru atau sudah), `false` bila gagal
     *   atau bila interface pilihan tidak tersedia
     */
    @Synchronized
    fun start(context: Context, config: CameraConfig = this.config): Boolean {
        this.config = config
        val port = config.port
        server?.let {
            Log.i(TAG, "Server sudah berjalan di port ${it.listeningPort}")
            return true
        }

        // Token dibuat acak setiap kali server dimulai (tidak tersimpan dan tidak
        // ada di APK). Ditampilkan di layar agar operator memasukkannya ke klien.
        val apiKey = config.apiKey.ifEmpty { SessionToken.generate() }
        currentApiKey = apiKey

        val choice = storedChoice(context)
        val bindAddress = bindAddressFor(choice)
        if (bindAddress == null) {
            Log.e(TAG, "Interface ${choice.storageValue} tidak tersedia; server tidak dimulai")
            statusRegistry.setCameraState(StatusRegistry.CameraState.ERROR, "interface_unavailable")
            return false
        }

        return try {
            val created = MjpegHttpServer(
                frameSource = frameSource,
                statusRegistry = statusRegistry,
                bindAddress = bindAddress,
                port = port,
                maxClients = config.maxClients,
                writeTimeoutMs = config.writeTimeoutMs,
                apiKey = apiKey,
                allowedAddresses = ApiAuth.parseAddresses(BuildConfig.ALLOWED_ADDRESSES)
            )
            created.start(SOCKET_READ_TIMEOUT_MS, true)
            server = created
            statusRegistry.setCameraState(StatusRegistry.CameraState.STARTING)
            Log.i(TAG, "Server MJPEG berjalan di $bindAddress:$port")
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

    /** Catat status kunci AE/AWB/AF agar `/status` melaporkannya (WP-24). */
    fun setTuningState(state: String) {
        statusRegistry.setTuningState(state)
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
            currentApiKey = null
            frameSource.clear()
            statusRegistry.resetFrames()
            statusRegistry.setCameraState(StatusRegistry.CameraState.STOPPED)
        }
    }

    /** Apakah server sedang berjalan. */
    fun isRunning(): Boolean = server != null

    /**
     * Token `X-Api-Key` pada sesi server yang sedang berjalan, atau `null` bila
     * server tidak berjalan. Dibuat ulang setiap kali server dimulai.
     */
    fun getApiKey(): String? = if (server != null) currentApiKey else null

    /** Port aktif, atau `null` bila server tidak berjalan. */
    fun getPort(): Int? = server?.listeningPort

    /**
     * Alamat bind yang benar-benar dipakai server, atau `null` bila server
     * tidak berjalan. Nilai `0.0.0.0` berarti bind ke semua interface.
     */
    fun getBindAddress(): String? = server?.bindAddress

    /** Status terakhir untuk ditampilkan Activity. */
    fun statusSnapshot() = statusRegistry.snapshot()

    /**
     * Batas tunggu socket NanoHTTPD. Bukan ambang mutu stream; hanya agar
     * socket klien yang mati tidak menahan thread selamanya.
     */
    private const val SOCKET_READ_TIMEOUT_MS = 5000
}
