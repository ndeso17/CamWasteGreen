package dev.ndeso17.camwastegreen.stream

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Hasil satu kali enkode JPEG, sudah dalam orientasi tegak.
 *
 * [width] dan [height] adalah dimensi **setelah** rotasi, sehingga metadata
 * frame selalu cocok dengan piksel yang benar-benar dikirim.
 */
data class EncodedFrame(
    val jpeg: ByteArray,
    val width: Int,
    val height: Int
)

/**
 * Mengubah `ImageProxy` YUV_420_888 menjadi satu JPEG.
 *
 * Jalur ini menekan enkode ganda: rotasi dilakukan pada bidang NV21 sebelum
 * enkode, bukan pada bitmap sesudah enkode seperti jalur lama. Enkode JPEG
 * hanya terjadi satu kali per frame.
 */
object FrameEncoder {

    private const val TAG = "FrameEncoder"

    /**
     * Enkode satu frame.
     *
     * @param mountRotationDegrees rotasi pemasangan dari [CameraConfig],
     *   ditambahkan ke rotasi sensor. Nilai harus kelipatan 90; `CameraConfig`
     *   sudah menolak yang lain.
     * @return [EncodedFrame] dengan dimensi hasil rotasi, atau `null` bila
     *   buffer tidak dapat dibaca atau enkode gagal. Pemanggil yang mencatat
     *   alasan kegagalan pada status kamera.
     */
    fun encode(image: ImageProxy, quality: Int, mountRotationDegrees: Int = 0): EncodedFrame? {
        return try {
            val sourceWidth = image.width
            val sourceHeight = image.height
            if (sourceWidth <= 0 || sourceHeight <= 0) return null

            val nv21 = toNv21(image, sourceWidth, sourceHeight)
            val degrees = ((image.imageInfo.rotationDegrees + mountRotationDegrees) % 360 + 360) % 360
            val rotated = if (degrees == 0) {
                nv21
            } else {
                Nv21Transform.rotateNv21(nv21, sourceWidth, sourceHeight, degrees)
            }

            val outWidth = if (degrees == 90 || degrees == 270) sourceHeight else sourceWidth
            val outHeight = if (degrees == 90 || degrees == 270) sourceWidth else sourceHeight

            val out = ByteArrayOutputStream()
            val yuvImage = YuvImage(rotated, ImageFormat.NV21, outWidth, outHeight, null)
            val ok = yuvImage.compressToJpeg(Rect(0, 0, outWidth, outHeight), quality, out)
            // Satu-satunya panggilan enkode JPEG pada jalur sensor (CWG-N01).
            if (!ok) {
                Log.w(TAG, "Enkode JPEG menolak frame ${outWidth}x$outHeight")
                return null
            }
            EncodedFrame(out.toByteArray(), outWidth, outHeight)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Frame tidak sah: ${e.message}")
            null
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Status frame tidak sah: ${e.message}")
            null
        } catch (e: RuntimeException) {
            Log.w(TAG, "Enkode frame gagal: ${e.message}")
            null
        }
    }

    /**
     * Susun NV21 dari tiga bidang YUV_420_888.
     *
     * `rowStride` dan `pixelStride` dibaca dari setiap bidang karena jarak
     * baris tidak selalu sama dengan lebar gambar. Urutan chroma NV21 adalah
     * V lalu U, sedangkan bidang `planes[1]` adalah U.
     */
    private fun toNv21(image: ImageProxy, width: Int, height: Int): ByteArray {
        val out = ByteArray(Nv21Transform.nv21Size(width, height))

        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val yBase = yBuffer.position()
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        var dst = 0
        for (row in 0 until height) {
            var src = yBase + row * yRowStride
            for (col in 0 until width) {
                out[dst++] = yBuffer.get(src)
                src += yPixelStride
            }
        }

        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val uBase = uBuffer.position()
        val vBase = vBuffer.position()
        val chromaWidth = Nv21Transform.chromaWidth(width)
        val chromaHeight = Nv21Transform.chromaHeight(height)

        for (row in 0 until chromaHeight) {
            val uRow = uBase + row * uPlane.rowStride
            val vRow = vBase + row * vPlane.rowStride
            for (col in 0 until chromaWidth) {
                out[dst++] = vBuffer.get(vRow + col * vPlane.pixelStride)
                out[dst++] = uBuffer.get(uRow + col * uPlane.pixelStride)
            }
        }

        return out
    }
}
