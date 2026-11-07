package dev.ndeso17.camwastegreen.camera

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import java.util.concurrent.Executor

/**
 * Kunci eksposur, white balance, dan fokus setelah kamera menghangat (CWG-07).
 *
 * Alasan: nilai otomatis kamera berubah saat ponsel dipasang menghadap objek
 * dengan warna berbeda, sehingga warna kelas yang sama dapat bergeser antar
 * pengambilan. Kunci dipasang setelah sejumlah frame warmup supaya nilai yang
 * dikunci adalah nilai yang sudah menyesuaikan diri dengan pencahayaan ruang,
 * bukan nilai awal yang masih berburu.
 *
 * Kunci tidak dianggap berhasil sebelum perangkat melaporkannya. Setiap
 * kegagalan dikembalikan sebagai status, bukan exception yang mematikan
 * operasi (CWG-13, CWG-16).
 */
object CameraTuning {

    /** Kunci belum diminta. */
    const val STATE_PENDING = "pending"

    /** Perangkat menerima permintaan kunci. */
    const val STATE_LOCKED = "locked"

    /** Perangkat menolak opsi kunci; operasi berlanjut tanpa kunci. */
    const val STATE_UNSUPPORTED = "unsupported"

    /** Permintaan kunci gagal karena sebab lain; operasi berlanjut tanpa kunci. */
    const val STATE_FAILED = "failed"

    /**
     * Pasang kunci AE, AWB, dan AF pada kamera yang sudah berjalan.
     *
     * AF dikunci dengan mematikan motor fokus (`CONTROL_AF_MODE_OFF`), sehingga
     * lensa tetap pada posisi saat kunci dipasang. Ini bukan mode makro atau
     * fokus tetap yang dikalibrasi; nilainya hanya berlaku untuk jarak kerja
     * saat pemasangan.
     *
     * @param onResult dipanggil sekali dengan salah satu `STATE_*`
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun lock(camera: Camera, executor: Executor, onResult: (String) -> Unit) {
        val options = try {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                .build()
        } catch (_: IllegalArgumentException) {
            onResult(STATE_UNSUPPORTED)
            return
        }

        val control = try {
            Camera2CameraControl.from(camera.cameraControl)
        } catch (_: Exception) {
            onResult(STATE_UNSUPPORTED)
            return
        }

        val future = control.addCaptureRequestOptions(options)
        future.addListener({
            try {
                future.get()
                onResult(STATE_LOCKED)
            } catch (_: IllegalArgumentException) {
                // Opsi tidak didukung konfigurasi kamera perangkat ini.
                onResult(STATE_UNSUPPORTED)
            } catch (_: UnsupportedOperationException) {
                onResult(STATE_UNSUPPORTED)
            } catch (_: Exception) {
                onResult(STATE_FAILED)
            }
        }, executor)
    }
}
