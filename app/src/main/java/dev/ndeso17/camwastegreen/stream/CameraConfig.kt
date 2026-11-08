package dev.ndeso17.camwastegreen.stream

/**
 * Konfigurasi kamera dan server stream (CWG-08).
 *
 * Nilai default di sini adalah **default konfigurasi** sesuai D-29, bukan
 * ambang mutu. FPS, latensi, dan toleransi numerik yang sah menunggu B-06 dan
 * tidak boleh diturunkan dari angka default ini.
 *
 * Semua nilai divalidasi saat objek dibuat. Nilai tidak sah langsung
 * melempar [IllegalArgumentException] dengan pesan yang menyebut nilainya.
 */
data class CameraConfig(
    val width: Int = 1280,
    val height: Int = 720,
    val targetFps: Int = 20,
    val jpegQuality: Int = 80,
    val maxClients: Int = 2,
    val port: Int = 8085,
    val mountRotationDegrees: Int = 0,
    val writeTimeoutMs: Long = 2000L,
    /**
     * Token `X-Api-Key` (D-34). Default kosong berarti server membuat token
     * acak baru setiap kali start; nilai eksplisit hanya dipakai sebagai
     * pengganti di test atau bila operator memaksa token tetap.
     */
    val apiKey: String = "",
) {

    init {
        require(width > 0) { "width harus positif, bukan $width" }
        require(height > 0) { "height harus positif, bukan $height" }
        require(targetFps in MIN_FPS..MAX_FPS) {
            "targetFps harus di $MIN_FPS..$MAX_FPS, bukan $targetFps"
        }
        require(jpegQuality in MIN_QUALITY..MAX_QUALITY) {
            "jpegQuality harus di $MIN_QUALITY..$MAX_QUALITY, bukan $jpegQuality"
        }
        require(maxClients >= 1) { "maxClients minimal 1, bukan $maxClients" }
        require(port in 1..65535) { "port harus di 1..65535, bukan $port" }
        require(mountRotationDegrees % 90 == 0) {
            "mountRotationDegrees harus kelipatan 90, bukan $mountRotationDegrees"
        }
        require(writeTimeoutMs > 0) { "writeTimeoutMs harus positif, bukan $writeTimeoutMs" }
    }

    companion object {
        const val MIN_FPS = 1
        const val MAX_FPS = 30
        const val MIN_QUALITY = 30
        const val MAX_QUALITY = 100
    }
}
