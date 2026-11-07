package dev.ndeso17.camwastegreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Menyalakan [CameraService] setelah perangkat selesai boot (CWG-12).
 *
 * Tanpa ini kamera hanya hidup bila operator membuka aplikasi. Receiver tidak
 * diekspor: hanya system broadcast yang menyalakannya.
 *
 * Kegagalan memulai service tidak dibiarkan mematikan proses. Alasannya
 * dilaporkan lewat [MJPEGServerInstance.reportCameraError] sehingga muncul
 * pada `cameraStateReason` di `/status`, bukan hilang sebagai exception.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IPCam.BootReceiver"

        /** Alasan pada `cameraStateReason` bila autostart gagal. */
        private const val REASON_AUTOSTART_FAILED = "autostart_failed"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Receiver ini hanya didaftarkan untuk ACTION_BOOT_COMPLETED, tetapi
        // aksinya tetap diperiksa supaya intent lain tidak memulai kamera.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        try {
            CameraService.start(context)
            Log.i(TAG, "Autostart setelah boot: CameraService diminta berjalan.")
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) atau
            // SecurityException bila vendor memblokir autostart.
            Log.e(
                TAG,
                "Autostart gagal: ${e.javaClass.simpleName}: ${e.message}",
                e
            )
            MJPEGServerInstance.reportCameraError(REASON_AUTOSTART_FAILED)
        }
    }
}
