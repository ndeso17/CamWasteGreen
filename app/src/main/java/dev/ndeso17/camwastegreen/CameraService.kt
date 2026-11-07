package dev.ndeso17.camwastegreen

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.ndeso17.camwastegreen.stream.StatusRegistry
import dev.ndeso17.camwastegreen.stream.StatusSnapshot

/**
 * Foreground service yang menjaga jalur kamera tetap hidup saat Activity
 * berhenti atau layar mati (CWG-11).
 *
 * WP-17 hanya mendirikan wadahnya: service, kanal notifikasi, dan status
 * foreground. Pengikatan kamera dan pengelolaan server tetap berada di
 * `MainActivity` sampai WP-18 memindahkannya ke sini. Karena itu service ini
 * belum mengubah perilaku stream.
 *
 * Notifikasi menampilkan keadaan kamera dan jumlah klien yang dilaporkan
 * `StatusRegistry`. Nilai itu berasal dari status terukur, bukan dari
 * konfigurasi.
 */
class CameraService : Service() {

    companion object {
        private const val TAG = "IPCam.CameraService"

        /** Mulai service ini. Wajib dipanggil dari Activity atau receiver. */
        const val ACTION_START = "dev.ndeso17.camwastegreen.action.START_CAMERA"

        /** Hentikan service dan notifikasi foreground-nya. */
        const val ACTION_STOP = "dev.ndeso17.camwastegreen.action.STOP_CAMERA"

        private const val CHANNEL_ID = "camera_stream"
        private const val CHANNEL_NAME = "Kamera dan stream"
        private const val NOTIFICATION_ID = 1001
        private const val REFRESH_INTERVAL_MS = 5000L

        /** Alasan yang muncul pada `cameraStateReason` bila foreground gagal. */
        private const val REASON_FOREGROUND_FAILED = "foreground_start_failed"

        /**
         * Mulai [CameraService] sebagai foreground service.
         *
         * Pemanggil dari luar Activity (mis. `BootReceiver` pada WP-19) harus
         * sudah memegang izin kamera; bila tidak, service melaporkan
         * kegagalannya sebagai status, bukan mematikan proses.
         */
        fun start(context: Context) {
            val intent = Intent(context, CameraService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Minta service berhenti. Tidak melakukan apa pun bila belum berjalan. */
        fun stop(context: Context) {
            val intent = Intent(context, CameraService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val refreshLoop = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        enterForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshLoop)
        super.onDestroy()
    }

    /**
     * Masuk ke keadaan foreground dengan tipe `camera`.
     *
     * Kegagalan di sini dapat terjadi pada Android 12+ bila service dimulai
     * dari latar belakang, atau bila izin kamera belum diberikan. Kegagalan
     * dilaporkan sebagai `cameraState` gagal lalu service berhenti sendiri,
     * bukan dibiarkan melempar exception ke proses (T-045, CWG-13).
     */
    private fun enterForeground() {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                } else {
                    0
                }
            )
            handler.removeCallbacks(refreshLoop)
            handler.postDelayed(refreshLoop, REFRESH_INTERVAL_MS)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Gagal masuk foreground: ${e.javaClass.simpleName}: ${e.message}",
                e
            )
            MJPEGServerInstance.reportCameraError(REASON_FOREGROUND_FAILED)
            stopSelf()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Status kamera dan jumlah klien stream"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(describe(MJPEGServerInstance.statusSnapshot()))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Perbarui isi notifikasi tanpa menyentuh status foreground.
     *
     * Pada Android 13+ notifikasi tanpa izin `POST_NOTIFICATIONS` tidak
     * ditampilkan; service tetap berjalan dan status tetap terbaca dari
     * `/status`, jadi ketiadaan izin tidak diperlakukan sebagai kegagalan.
     */
    private fun updateNotification() {
        if (!canPostNotifications()) return
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifikasi tidak dapat diperbarui: ${e.message}")
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Ringkasan keadaan untuk isi notifikasi. `cameraState` ditulis apa adanya
     * supaya nilainya sama dengan yang dilaporkan `/status`.
     */
    private fun describe(snapshot: StatusSnapshot): String {
        val clients = if (snapshot.clients == 1) "1 klien" else "${snapshot.clients} klien"
        val reason = snapshot.cameraStateReason?.let { " ($it)" } ?: ""
        val label = when (snapshot.cameraState) {
            StatusRegistry.CameraState.STREAMING -> "kamera mengalir"
            StatusRegistry.CameraState.STARTING -> "kamera menyiapkan"
            StatusRegistry.CameraState.STOPPED -> "kamera berhenti"
            StatusRegistry.CameraState.ERROR -> "kamera bermasalah"
            else -> snapshot.cameraState
        }
        return "Status ${snapshot.cameraState}: $label$reason, $clients"
    }
}
