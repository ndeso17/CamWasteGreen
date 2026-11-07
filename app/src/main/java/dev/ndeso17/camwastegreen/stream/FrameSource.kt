package dev.ndeso17.camwastegreen.stream

/**
 * Penyimpan frame terbaru dengan nomor urut monoton.
 *
 * Kelas ini murni Kotlin tanpa `android.*` dan tanpa membaca jam sendiri:
 * waktu tangkap selalu datang dari pemanggil. Dengan begitu urutan dan waktu
 * dapat diuji tanpa menunggu waktu nyata.
 *
 * `publish` disinkronkan karena analyzer dan pemanggil lain dapat menulis dari
 * thread berbeda; nomor urut tidak boleh melompat atau berulang.
 */
class FrameSource {

    private val lock = Any()
    private var nextSeq = 1L

    @Volatile
    private var current: FramePacket? = null

    /** Frame terakhir, atau `null` bila belum ada frame. */
    val latest: FramePacket?
        get() = current

    /** Nomor urut frame terakhir, atau `null` bila belum ada frame. */
    val lastSeq: Long?
        get() = current?.seq

    /** Waktu tangkap frame terakhir, atau `null` bila belum ada frame. */
    val lastFrameAtMs: Long?
        get() = current?.capturedAtMs

    /**
     * Simpan frame baru dan beri nomor urut berikutnya.
     *
     * @param capturedAtMs waktu tangkap dari pemanggil
     */
    @Synchronized
    fun publish(jpeg: ByteArray, width: Int, height: Int, capturedAtMs: Long): FramePacket {
        require(width > 0 && height > 0) { "Dimensi tidak sah: ${width}x$height" }
        val packet = FramePacket(
            seq = nextSeq++,
            capturedAtMs = capturedAtMs,
            width = width,
            height = height,
            jpeg = jpeg
        )
        current = packet
        return packet
    }

    /** Hapus frame terakhir tanpa mengulang nomor urut. */
    @Synchronized
    fun clear() {
        current = null
    }
}
