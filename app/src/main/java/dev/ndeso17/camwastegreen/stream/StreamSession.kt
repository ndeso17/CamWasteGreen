package dev.ndeso17.camwastegreen.stream

/**
 * Sesi satu klien stream.
 *
 * Tiga tanggung jawab, semuanya murni Kotlin tanpa `android.*`:
 *
 * 1. **Dedupe**: frame dengan `seq` sama atau lebih kecil dari yang terakhir
 *    dikirim tidak dikirim ulang. Ini menggantikan pola lama yang menulis
 *    frame sama setiap 50 ms.
 * 2. **Batas klien**: slot diambil lewat [tryAcquire] dan dikembalikan lewat
 *    [release].
 * 3. **Batas waktu tulis**: [onWriteStarted] dan [onWriteFinished] memberi
 *    penanda [isSlowClient]. Penutupan pipa yang sebenarnya dilakukan
 *    pemanggil, karena `PipedOutputStream` tidak bisa dibatasi waktunya
 *    sendiri.
 */
class StreamSession(
    private val maxClients: Int,
    private val writeTimeoutMs: Long,
    private val nowMs: () -> Long
) {

    init {
        require(maxClients >= 1) { "maxClients minimal 1, bukan $maxClients" }
        require(writeTimeoutMs > 0) { "writeTimeoutMs harus positif, bukan $writeTimeoutMs" }
    }

    private val lock = Any()

    private var activeClients = 0
    private var lastSentSeq: Long? = null
    private var writeStartedAtMs: Long? = null
    private var slowClient = false

    /** Jumlah klien aktif saat ini. */
    val clients: Int
        get() = synchronized(lock) { activeClients }

    /** Nomor urut frame terakhir yang dikirim, atau `null` bila belum ada. */
    val lastSeqSent: Long?
        get() = synchronized(lock) { lastSentSeq }

    /** `true` bila klien ini ditandai lambat karena melewati batas waktu tulis. */
    val isSlowClient: Boolean
        get() = synchronized(lock) { slowClient }

    /**
     * Ambil satu slot klien.
     *
     * @return `true` bila slot didapat, `false` bila batas klien sudah tercapai
     */
    fun tryAcquire(): Boolean {
        synchronized(lock) {
            if (activeClients >= maxClients) return false
            activeClients++
            return true
        }
    }

    /**
     * Kembalikan slot klien.
     *
     * @return `true` bila ada slot yang dikembalikan, `false` bila tidak ada
     *   slot aktif (misalnya [release] dipanggil dua kali)
     */
    fun release(): Boolean {
        synchronized(lock) {
            if (activeClients <= 0) return false
            activeClients--
            writeStartedAtMs = null
            slowClient = false
            return true
        }
    }

    /**
     * Tentukan apakah paket perlu dikirim ke klien ini.
     *
     * Paket dikirim hanya bila `seq` lebih besar dari yang terakhir dikirim.
     * Pemanggil yang mengirim bertanggung jawab memanggil [onPacketSent]
     * setelah penulisan berhasil.
     */
    fun shouldSend(packet: FramePacket): Boolean {
        synchronized(lock) {
            val last = lastSentSeq
            return last == null || packet.seq > last
        }
    }

    /**
     * Catat bahwa paket sudah berhasil ditulis ke klien.
     *
     * Nomor urut hanya maju; paket yang lebih tua tidak menurunkan penanda.
     */
    fun onPacketSent(packet: FramePacket) {
        synchronized(lock) {
            val last = lastSentSeq
            if (last == null || packet.seq > last) {
                lastSentSeq = packet.seq
            }
        }
    }

    /** Tandai awal penulisan frame ke klien. */
    fun onWriteStarted() {
        synchronized(lock) {
            writeStartedAtMs = nowMs()
        }
    }

    /**
     * Tandai akhir penulisan frame dan periksa apakah melewati batas waktu.
     *
     * @return `true` bila penulisan melewati [writeTimeoutMs] sehingga klien
     *   ditandai lambat
     */
    fun onWriteFinished(): Boolean {
        synchronized(lock) {
            val started = writeStartedAtMs ?: return slowClient
            val elapsed = nowMs() - started
            writeStartedAtMs = null
            if (elapsed > writeTimeoutMs) {
                slowClient = true
            }
            return slowClient
        }
    }
}
