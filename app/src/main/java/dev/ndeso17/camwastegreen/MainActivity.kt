package dev.ndeso17.camwastegreen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.net.NetworkInterface

/**
 * UI operator: memulai [CameraService] dan menampilkan statusnya.
 *
 * Activity ini tidak lagi memegang kamera maupun server. Menghentikan Activity
 * tidak menghentikan stream (T-049); hanya [CameraService] yang melakukannya.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvFpsInfo: TextView
    private lateinit var tvQualityInfo: TextView
    private lateinit var tvIpInfo: TextView
    private lateinit var tvFpsValue: TextView
    private lateinit var tvQualityValue: TextView
    private lateinit var btnRecordBg: View
    private lateinit var btnRecordInner: View
    private lateinit var btnIncreaseFps: Button
    private lateinit var btnDecreaseFps: Button
    private lateinit var btnIncreaseQuality: Button
    private lateinit var btnDecreaseQuality: Button
    private lateinit var btnMinimize: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var tvUsbStatus: TextView
    private lateinit var tvCameraState: TextView
    private lateinit var controlPanel: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

    private var currentFps = 20
    private var currentQuality = 80

    private val handler = Handler(Looper.getMainLooper())

    /** Pembaruan status dari registry milik service, bukan dari nilai konfigurasi. */
    private val statusLoop = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, STATUS_INTERVAL_MS)
        }
    }

    companion object {
        private const val TAG = "IPCam.MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val STATUS_INTERVAL_MS = 1000L

        /** Sama dengan default `CameraConfig` (D-29); dipakai hanya untuk teks. */
        private const val DEFAULT_PORT = 8085

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBottomSheet()
        setupButtonListeners()

        if (allPermissionsGranted()) {
            updateUI()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(statusLoop)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(statusLoop)
    }

    /**
     * Activity tidak menghentikan kamera atau server (T-049). Penghentian hanya
     * dilakukan lewat [CameraService.ACTION_STOP].
     */
    override fun onDestroy() {
        handler.removeCallbacks(statusLoop)
        super.onDestroy()
    }

    private fun initViews() {
        tvFpsInfo = findViewById(R.id.tvFpsInfo)
        tvQualityInfo = findViewById(R.id.tvQualityInfo)
        tvIpInfo = findViewById(R.id.tvIpInfo)
        tvFpsValue = findViewById(R.id.tvFpsValue)
        tvQualityValue = findViewById(R.id.tvQualityValue)
        btnRecordBg = findViewById(R.id.btnRecordBg)
        btnRecordInner = findViewById(R.id.btnRecordInner)
        btnIncreaseFps = findViewById(R.id.btnIncreaseFps)
        btnDecreaseFps = findViewById(R.id.btnDecreaseFps)
        btnIncreaseQuality = findViewById(R.id.btnIncreaseQuality)
        btnDecreaseQuality = findViewById(R.id.btnDecreaseQuality)
        btnMinimize = findViewById(R.id.btnTogglePanel)
        controlPanel = findViewById(R.id.controlPanel)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        tvUsbStatus = findViewById(R.id.tvUsbStatus)
        tvCameraState = findViewById(R.id.tvCameraState)
    }

    private fun setupBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(controlPanel).apply {
            isHideable = false
            isFitToContents = true
            peekHeight = resources.getDimensionPixelSize(R.dimen.sheet_peek_height)
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    btnMinimize.setImageResource(
                        if (newState == BottomSheetBehavior.STATE_EXPANDED)
                            android.R.drawable.arrow_down_float
                        else
                            android.R.drawable.arrow_up_float
                    )
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            })
        }
    }

    private fun setupButtonListeners() {
        btnRecordBg.setOnClickListener {
            if (MJPEGServerInstance.isRunning()) {
                CameraService.stop(this)
            } else {
                CameraService.start(this, currentFps, currentQuality)
            }
            // Status sebenarnya menyusul dari registry; UI tidak mengarang.
            handler.postDelayed({ updateUI() }, 300)
            updateUI()
        }

        btnMinimize.setOnClickListener {
            sheetBehavior.state =
                if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    BottomSheetBehavior.STATE_COLLAPSED
                else
                    BottomSheetBehavior.STATE_EXPANDED
        }

        btnSwitchCamera.setOnClickListener {
            if (MJPEGServerInstance.isRunning()) {
                CameraService.switchCamera(this)
            } else {
                Toast.makeText(this, R.string.toast_start_stream_first, Toast.LENGTH_SHORT).show()
            }
        }

        btnIncreaseFps.setOnClickListener {
            currentFps = (currentFps + 5).coerceAtMost(30)
            CameraService.setFps(this, currentFps)
            updateUI()
        }
        btnDecreaseFps.setOnClickListener {
            currentFps = (currentFps - 5).coerceAtLeast(5)
            CameraService.setFps(this, currentFps)
            updateUI()
        }
        btnIncreaseQuality.setOnClickListener {
            currentQuality = (currentQuality + 10).coerceAtMost(100)
            CameraService.setQuality(this, currentQuality)
            updateUI()
        }
        btnDecreaseQuality.setOnClickListener {
            currentQuality = (currentQuality - 10).coerceAtLeast(30)
            CameraService.setQuality(this, currentQuality)
            updateUI()
        }
    }

    /**
     * Gambar ulang UI dari dua sumber yang berbeda dan tidak dicampur:
     * nilai target (FPS/quality yang dipilih operator) dan status terukur
     * (`measuredFps`, `cameraState`, `clients` dari registry service).
     */
    private fun updateUI() {
        val snapshot = MJPEGServerInstance.statusSnapshot()
        val running = MJPEGServerInstance.isRunning()

        tvFpsInfo.text = if (snapshot.measuredFps == null) {
            getString(R.string.label_fps_measured_none)
        } else {
            getString(R.string.label_fps_measured, snapshot.measuredFps)
        }

        val qualityLabel = getQualityLabel(currentQuality)
        tvQualityInfo.text = getString(R.string.label_quality_level, qualityLabel)
        tvFpsValue.text = currentFps.toString()
        tvQualityValue.text = qualityLabel

        tvIpInfo.text = if (running) {
            getString(
                R.string.label_stream_url,
                getDeviceIpAddress(),
                MJPEGServerInstance.getPort() ?: DEFAULT_PORT
            )
        } else {
            getString(R.string.label_stream_offline)
        }

        tvCameraState.text = if (running) {
            val reason = snapshot.cameraStateReason?.let { " ($it)" } ?: ""
            getString(R.string.label_camera_state, snapshot.cameraState + reason, snapshot.clients)
        } else {
            getString(R.string.label_server_stopped)
        }

        tvUsbStatus.text = getUsbTetheringIp()

        btnRecordBg.setBackgroundResource(
            if (running) R.drawable.bg_btn_record_on else R.drawable.bg_btn_record_off
        )
        btnRecordInner.visibility = if (running) View.GONE else View.VISIBLE

        btnIncreaseFps.isEnabled = running
        btnDecreaseFps.isEnabled = running
        btnIncreaseQuality.isEnabled = running
        btnDecreaseQuality.isEnabled = running
        btnSwitchCamera.isEnabled = running

    }

    /** Label tingkat kualitas. Ini nilai target, bukan pengukuran. */
    private fun getQualityLabel(quality: Int): String = when {
        quality >= 80 -> getString(R.string.quality_high)
        quality >= 50 -> getString(R.string.quality_medium)
        else -> getString(R.string.quality_low)
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_PERMISSIONS) return

        val cameraIndex = REQUIRED_PERMISSIONS.indexOf(Manifest.permission.CAMERA)
        val cameraGranted =
            grantResults.getOrNull(cameraIndex) == PackageManager.PERMISSION_GRANTED

        if (cameraGranted) {
            // Izin notifikasi opsional: tanpa izin itu service tetap berjalan.
            updateUI()
        } else {
            Log.e(TAG, "Izin kamera tidak diberikan.")
            Toast.makeText(this, R.string.toast_camera_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Alamat interface USB tethering bila ada. Hanya melaporkan apa yang
     * terlihat; "USB: --" berarti tidak ada interface USB yang aktif.
     */
    private fun getUsbTetheringIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val name = intf.name.lowercase()
                if (intf.isUp && (name == "usb0" || name == "rndis0" || name == "eth0")) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val address = enumIpAddr.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return getString(R.string.label_usb_present, address.hostAddress ?: "--")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendapatkan IP USB", e)
        }
        return getString(R.string.label_usb_absent)
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isUp && !intf.isLoopback) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) {
                            return inetAddress.hostAddress ?: "0.0.0.0"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendapatkan IP Address", e)
        }
        return "0.0.0.0"
    }
}
