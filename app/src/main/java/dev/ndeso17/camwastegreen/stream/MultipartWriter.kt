package dev.ndeso17.camwastegreen.stream

/**
 * Penulis part multipart untuk stream MJPEG.
 *
 * Format part mengikuti `multipart/x-mixed-replace`: setiap part diawali
 * `--boundary`, lalu blok header HTTP yang diakhiri CRLF ganda, lalu data
 * JPEG, lalu CRLF penutup.
 *
 * Header part memuat metadata frame sebagai header `X-Frame-*`. Header
 * tersebut bersifat aditif sehingga klien lama yang hanya membaca
 * `Content-Type` dan `Content-Length` tetap dapat membaca stream.
 *
 * Kelas ini murni Kotlin tanpa `android.*` supaya bisa diuji di JVM.
 */
object MultipartWriter {

    private const val CRLF = "\r\n"

    /**
     * Header part untuk satu frame.
     *
     * @param boundary batas part, dipakai apa adanya tanpa perubahan
     */
    fun partHeader(packet: FramePacket, boundary: String): ByteArray {
        val header = buildString {
            append("--").append(boundary).append(CRLF)
            append("Content-Type: image/jpeg").append(CRLF)
            append("Content-Length: ").append(packet.jpeg.size).append(CRLF)
            append("X-Frame-Seq: ").append(packet.seq).append(CRLF)
            append("X-Frame-Captured-At-Ms: ").append(packet.capturedAtMs).append(CRLF)
            append("X-Frame-Width: ").append(packet.width).append(CRLF)
            append("X-Frame-Height: ").append(packet.height).append(CRLF)
            append(CRLF)
        }
        return header.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Part lengkap: header + data JPEG + CRLF penutup.
     *
     * Data JPEG disalin satu kali. `part` tidak menggandakan byte JPEG
     * sehingga jalur sensor tidak menambah alokasi per frame.
     */
    fun part(packet: FramePacket, boundary: String): ByteArray {
        val header = partHeader(packet, boundary)
        val body = packet.jpeg
        val out = ByteArray(header.size + body.size + 2)
        System.arraycopy(header, 0, out, 0, header.size)
        System.arraycopy(body, 0, out, header.size, body.size)
        out[header.size + body.size] = '\r'.code.toByte()
        out[header.size + body.size + 1] = '\n'.code.toByte()
        return out
    }
}
