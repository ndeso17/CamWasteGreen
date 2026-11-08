package dev.ndeso17.camwastegreen

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Admin perangkat dengan satu kebijakan: `force-lock` (D-33, CWG-19).
 *
 * Receiver ini tidak meminta izin apa pun sendiri dan tidak melakukan aksi
 * atas broadcast. Aktivasi hanya terjadi bila operator menyetujui layar
 * konfirmasi sistem yang dibuka dari `MainActivity`. Pencabutan dapat
 * dilakukan kapan saja lewat Settings.
 */
class ScreenLockAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Admin perangkat aktif; kebijakan force-lock tersedia.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Admin perangkat dicabut; tombol matikan layar tidak lagi mengunci.")
    }

    companion object {
        private const val TAG = "IPCam.ScreenLockAdmin"
    }
}
