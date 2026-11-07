package dev.ndeso17.camwastegreen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Pengingat setelah perangkat selesai boot (CWG-12).
 *
 * Receiver ini **tidak** menyalakan kamera. Android 14 ke atas melarang
 * foreground service tipe `camera` memperoleh akses kamera bila dimulai dari
 * latar belakang; `BOOT_COMPLETED` hanya mengizinkan service *dimulai*, bukan
 * memakai kamera. Percobaan pada perangkat `ZP2222SQLL` berakhir dengan
 * `am_foreground_service_denied` dan proses dibekukan (B-31, D-32).
 *
 * Yang dilakukan receiver: menampilkan notifikasi agar operator membuka
 * aplikasi dan menekan start. Notifikasi tidak dipaksakan; bila izin
 * `POST_NOTIFICATIONS` belum diberikan, kejadian itu hanya dicatat di log.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IPCam.BootReceiver"
        private const val CHANNEL_ID = "boot_reminder"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Receiver hanya didaftarkan untuk ACTION_BOOT_COMPLETED, tetapi aksinya
        // tetap diperiksa supaya intent lain tidak memicu pengingat.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (!canPostNotifications(context)) {
            Log.i(TAG, "Pengingat boot dilewati: izin POST_NOTIFICATIONS belum diberikan.")
            return
        }

        try {
            createChannel(context)
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, buildNotification(context))
            Log.i(TAG, "Pengingat boot ditampilkan; kamera tetap menunggu operator.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Pengingat boot tidak dapat ditampilkan: ${e.message}")
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_boot),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_boot_desc)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(context: Context): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(context.getString(R.string.notif_boot_title))
            .setContentText(context.getString(R.string.notif_boot_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_boot_text))
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
