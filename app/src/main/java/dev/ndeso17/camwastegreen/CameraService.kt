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
import android.os.SystemClock
import android.util.Log
import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dev.ndeso17.camwastegreen.camera.CameraTuning
import dev.ndeso17.camwastegreen.stream.CameraConfig
import dev.ndeso17.camwastegreen.stream.FrameEncoder
import dev.ndeso17.camwastegreen.stream.StatusRegistry
import dev.ndeso17.camwastegreen.stream.StatusSnapshot
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service pemilik jalur kamera dan server MJPEG (CWG-11).
 *
 * Kamera diikat ke [Lifecycle] milik service ini, bukan ke Activity. Akibatnya
 * stream bertahan saat layar mati atau Activity ditutup, dan Activity hanya
 * menjadi pemulai service serta penampil status.
 *
 * Lifecycle service selalu [Lifecycle.State.RESUMED] selama service berjalan,
 * karena siklus hidup service tidak mengikuti visibilitas layar.
 */
class CameraService : Service(), LifecycleOwner {

    companion object {
        private const val TAG = "IPCam.CameraService"

        /** Mulai service dan kamera. Wajib dipanggil dari Activity atau receiver. */
        const val ACTION_START = "dev.ndeso17.camwastegreen.action.START_CAMERA"

        /** Hentikan kamera, server, dan service. */
        const val ACTION_STOP = "dev.ndeso17.camwastegreen.action.STOP_CAMERA"

        /** Tukar kamera depan/belakang. */
        const val ACTION_SWITCH_CAMERA = "dev.ndeso17.camwastegreen.action.SWITCH_CAMERA"

        /** Ubah FPS target. */
        const val ACTION_SET_FPS = "dev.ndeso17.camwastegreen.action.SET_FPS"

        /** Ubah kualitas JPEG. */
        const val ACTION_SET_QUALITY = "dev.ndeso17.camwastegreen.action.SET_QUALITY"

        /** Extra untuk [ACTION_SET_FPS] dan [ACTION_SET_QUALITY]. */
        const val EXTRA_VALUE = "extra_value"

        /** Extra FPS dan kualitas awal pada [ACTION_START]. */
        private const val EXTRA_FPS = "extra_fps"
        private const val EXTRA_QUALITY = "extra_quality"

        /**
         * Default konfigurasi (D-29), bukan ambang mutu. Semua nilai kamera dan
         * server dibaca dari sini supaya tidak ada angka yang tersebar di
         * dalam fungsi (CWG-08).
         */
        private val DEFAULT_CONFIG = CameraConfig()

        private const val CHANNEL_ID = "camera_stream"
        private const val CHANNEL_NAME = "Kamera dan stream"
        private const val NOTIFICATION_ID = 1001
        private const val REFRESH_INTERVAL_MS = 5000L

        /** Alasan yang muncul pada `cameraStateReason` bila foreground gagal. */
        private const val REASON_FOREGROUND_FAILED = "foreground_start_failed"

        /**
         * Jumlah frame sebelum kunci AE/AWB/AF dipasang. Nilai ini adalah
         * jumlah frame warmup, bukan ambang mutu: ia hanya memberi waktu
         * kontrol otomatis menyesuaikan diri sebelum dibekukan.
         */
        private const val WARMUP_FRAMES = 30

        /**
         * Mulai [CameraService] sebagai foreground service.
         *
         * Pemanggil dari luar Activity (mis. `BootReceiver` pada WP-19) harus
         * sudah memegang izin kamera; bila tidak, service melaporkan
         * kegagalannya sebagai status, bukan mematikan proses.
         */
        fun start(
            context: Context,
            fps: Int = DEFAULT_CONFIG.targetFps,
            quality: Int = DEFAULT_CONFIG.jpegQuality
        ) {
            val intent = Intent(context, CameraService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_FPS, fps)
                .putExtra(EXTRA_QUALITY, quality)
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Minta service berhenti.
         *
         * Dipanggil hanya saat aplikasi terlihat: pada Android 8+
         * `startService` dari latar belakang dilarang.
         */
        fun stop(context: Context) {
            send(context, Intent(context, CameraService::class.java).setAction(ACTION_STOP))
        }

        /** Tukar kamera depan/belakang tanpa menghentikan stream. */
        fun switchCamera(context: Context) {
            send(context, Intent(context, CameraService::class.java).setAction(ACTION_SWITCH_CAMERA))
        }

        /** Ubah FPS target tanpa menghentikan stream. */
        fun setFps(context: Context, fps: Int) {
            send(
                context,
                Intent(context, CameraService::class.java)
                    .setAction(ACTION_SET_FPS)
                    .putExtra(EXTRA_VALUE, fps)
            )
        }

        /** Ubah kualitas JPEG tanpa menghentikan stream. */
        fun setQuality(context: Context, quality: Int) {
            send(
                context,
                Intent(context, CameraService::class.java)
                    .setAction(ACTION_SET_QUALITY)
                    .putExtra(EXTRA_VALUE, quality)
            )
        }

        private fun send(context: Context, intent: Intent) {
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                // Dilarang memulai service dari latar belakang (Android 8+).
                Log.w(TAG, "Perintah service tidak dapat dikirim: ${e.message}")
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val handler = Handler(Looper.getMainLooper())

    private val refreshLoop = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var targetFps = DEFAULT_CONFIG.targetFps

    @Volatile
    private var jpegQuality = DEFAULT_CONFIG.jpegQuality

    /**
     * Konfigurasi yang dipakai service ini. Disusun ulang lewat [copy] setiap
     * kali FPS atau kualitas diubah, sehingga validasi `CameraConfig` tetap
     * berlaku pada nilai runtime.
     */
    private var config = DEFAULT_CONFIG

    @Volatile
    private var useFrontCamera = false

    private var lastFrameTimestamp = 0L

    /** Kamera yang sedang terikat; `null` bila belum/tidak terikat. */
    private var boundCamera: Camera? = null

    /** Jumlah frame yang sudah terbit sejak kamera terikat (penghitung warmup). */
    private var framesSinceBind = 0

    /** Kunci AE/AWB/AF hanya diminta sekali per ikatan kamera. */
    private var tuningRequested = false

    // H.264 streaming — dipindahkan apa adanya dari MainActivity; WP-31
    // menghapus jalur ini karena tidak pernah menerima frame.
    private var streamEncoder: StreamEncoder? = null
    private var tcpStreamServer: TcpStreamServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        cameraExecutor = Executors.newSingleThreadExecutor()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCamera()
                MJPEGServerInstance.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SWITCH_CAMERA -> {
                if (MJPEGServerInstance.isRunning()) {
                    useFrontCamera = !useFrontCamera
                    Log.i(TAG, "Beralih ke kamera: ${if (useFrontCamera) "depan" else "belakang"}")
                    bindCameraUseCases()
                }
                return START_STICKY
            }

            ACTION_SET_FPS -> {
                val value = intent.getIntExtra(EXTRA_VALUE, targetFps)
                targetFps = value.coerceIn(CameraConfig.MIN_FPS, CameraConfig.MAX_FPS)
                config = config.copy(targetFps = targetFps)
                Log.i(TAG, "FPS target: $targetFps")
                return START_STICKY
            }

            ACTION_SET_QUALITY -> {
                val value = intent.getIntExtra(EXTRA_VALUE, jpegQuality)
                jpegQuality = value.coerceIn(CameraConfig.MIN_QUALITY, CameraConfig.MAX_QUALITY)
                config = config.copy(jpegQuality = jpegQuality)
                Log.i(TAG, "Kualitas JPEG: $jpegQuality")
                return START_STICKY
            }
        }

        intent?.let {
            if (it.hasExtra(EXTRA_FPS)) {
                targetFps = it.getIntExtra(EXTRA_FPS, targetFps)
                    .coerceIn(CameraConfig.MIN_FPS, CameraConfig.MAX_FPS)
            }
            if (it.hasExtra(EXTRA_QUALITY)) {
                jpegQuality = it.getIntExtra(EXTRA_QUALITY, jpegQuality)
                    .coerceIn(CameraConfig.MIN_QUALITY, CameraConfig.MAX_QUALITY)
            }
            config = config.copy(targetFps = targetFps, jpegQuality = jpegQuality)
        }

        enterForeground()

        if (!MJPEGServerInstance.isRunning()) {
            startCamera()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshLoop)
        stopCamera()
        cameraExecutor.shutdown()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
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

    private fun startCamera() {
        if (!hasCameraPermission()) {
            Log.e(TAG, "Izin kamera belum diberikan; kamera tidak dimulai.")
            MJPEGServerInstance.reportCameraError("camera_permission_missing")
            return
        }

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memulai kamera: ${e.message}", e)
                MJPEGServerInstance.reportCameraError("camera_provider_failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        // Resolusi diminta lewat ResolutionSelector (API yang tidak usang).
        // Rasio 16:9 dipaksa lebih dulu agar kamera tidak memilih bingkai
        // persegi; bila perangkat tidak punya 16:9, strategi jatuh ke rasio
        // terdekat yang tersedia.
        val resolutionStrategy = ResolutionStrategy(
            Size(config.width, config.height),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(resolutionStrategy)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(resolutionSelector)
            .build()

        // H.264 stream encoder
        val encoder = StreamEncoder()
        streamEncoder = encoder
        try {
            encoder.start()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulai encoder H.264", e)
            streamEncoder = null
        }

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            analyze(imageProxy)
        }

        val cameraSelector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        try {
            provider.unbindAll()
            boundCamera = provider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            framesSinceBind = 0
            tuningRequested = false
            MJPEGServerInstance.setTuningState(CameraTuning.STATE_PENDING)

            if (!MJPEGServerInstance.isRunning()) {
                if (MJPEGServerInstance.start(this, config)) {
                    Log.i(TAG, "Server MJPEG aktif di port ${MJPEGServerInstance.getPort()}")
                }
            }

            if (streamEncoder != null) {
                tcpStreamServer = TcpStreamServer(streamEncoder!!)
                tcpStreamServer?.start()
                Log.i(TAG, "Server TCP H.264 aktif di port 8086")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengikat kamera: ${e.message}", e)
            MJPEGServerInstance.reportCameraError("camera_bind_failed")
        }
    }

    /**
     * Minta kunci AE/AWB/AF setelah frame warmup terbit.
     *
     * Dipanggil dari thread analyzer; penghitungnya hanya disentuh dari thread
     * yang sama, sehingga tidak perlu sinkronisasi tambahan.
     */
    private fun maybeLockTuning() {
        framesSinceBind++
        if (tuningRequested || framesSinceBind < WARMUP_FRAMES) return
        val camera = boundCamera ?: return
        tuningRequested = true
        CameraTuning.lock(camera, ContextCompat.getMainExecutor(this)) { state ->
            Log.i(TAG, "Status kunci AE/AWB/AF: $state")
            MJPEGServerInstance.setTuningState(state)
        }
    }

    /**
     * Enkode satu frame lalu terbitkan.
     *
     * Waktu tangkap diambil paling awal, sebelum konversi apa pun, supaya usia
     * frame tidak lebih muda daripada kenyataan.
     */
    private fun analyze(imageProxy: ImageProxy) {
        if (imageProxy.format != ImageFormat.YUV_420_888 || imageProxy.planes.size < 3) {
            imageProxy.close()
            return
        }

        val capturedAtMs = System.currentTimeMillis()
        val currentTime = SystemClock.elapsedRealtime()
        val frameInterval = 1000 / targetFps

        if (currentTime - lastFrameTimestamp < frameInterval) {
            imageProxy.close()
            return
        }
        lastFrameTimestamp = currentTime

        try {
            val encoded = FrameEncoder.encode(imageProxy, jpegQuality, config.mountRotationDegrees)
            if (encoded == null) {
                // Kegagalan enkode dicatat sebagai alasan pada /status
                MJPEGServerInstance.reportCameraError("encode_failed")
            } else {
                MJPEGServerInstance.publishFrame(
                    encoded.jpeg, encoded.width, encoded.height, capturedAtMs
                )
                maybeLockTuning()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pada analyzer: ${e.message}", e)
            MJPEGServerInstance.reportCameraError("analyzer_failed")
        } finally {
            imageProxy.close()
        }
    }

    private fun stopCamera() {
        tcpStreamServer?.stop()
        tcpStreamServer = null
        streamEncoder?.release()
        streamEncoder = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        Log.i(TAG, "Kamera dihentikan.")
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

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
