package dev.ndeso17.camwastegreen.stream

/**
 * Satu frame JPEG beserta metadata yang dikirim ke klien.
 *
 * [seq] naik monoton dan dipakai klien untuk membuang frame duplikat.
 * [capturedAtMs] adalah waktu tangkap menurut pemanggil, bukan waktu kirim,
 * sehingga klien dapat menghitung usia pengamatan.
 */
data class FramePacket(
    val seq: Long,
    val capturedAtMs: Long,
    val width: Int,
    val height: Int,
    val jpeg: ByteArray
)
