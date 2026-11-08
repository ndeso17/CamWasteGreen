package dev.ndeso17.camwastegreen

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.ndeso17.camwastegreen.stream.NetworkChoice
import dev.ndeso17.camwastegreen.stream.StreamInterfacePrefs
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
    private lateinit var tvApiKey: TextView
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
    private lateinit var groupInterface: RadioGroup
    private lateinit var btnApplyInterface: Button
    private lateinit var tvInterfaceHint: TextView
    private lateinit var previewView: PreviewView
    private lateinit var previewOverlay: LinearLayout
    private lateinit var tvPreviewHint: TextView
    private lateinit var btnTogglePreview: ImageButton
    private lateinit var btnScreenOff: ImageButton

    /** Penyimpanan pilihan interface; dibaca saat start server (WP-35). */
    private lateinit var interfacePrefs: StreamInterfacePrefs

    /**
     * Binder service yang sedang terikat. `null` berarti service belum berjalan
     * atau Activity sedang berhenti; pratinjau tidak bisa dipasang tanpa ini.
     */
    private var serviceBinder: CameraService.PreviewBinder? = null

    /** Keadaan pratinjau yang dipilih operator; awal nyala (D-33). */
    private var previewEnabled = true

    /** `true` bila permintaan bind service sudah terdaftar. */
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as? CameraService.PreviewBinder
            applyPreviewState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            applyPreviewState()
        }
    }

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

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        interfacePrefs = StreamInterfacePrefs(this)

        initViews()
        setupBottomSheet()
        setupButtonListeners()
        showStoredChoice()

        if (allPermissionsGranted()) {
            updateUI()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(statusLoop)
        // Ikat tanpa BIND_AUTO_CREATE: Activity tidak menyalakan service sendiri.
        serviceBound =
            bindService(Intent(this, CameraService::class.java), serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(statusLoop)
        // Lepas permukaan dulu supaya surface tidak ditahan saat Activity tidak terlihat.
        serviceBinder?.setPreviewProvider(null)
        serviceBinder = null
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
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
        tvApiKey = findViewById(R.id.tvApiKey)
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
        groupInterface = findViewById(R.id.groupInterface)
        btnApplyInterface = findViewById(R.id.btnApplyInterface)
        tvInterfaceHint = findViewById(R.id.tvInterfaceHint)
        previewView = findViewById(R.id.previewView)
        previewOverlay = findViewById(R.id.previewOverlay)
        tvPreviewHint = findViewById(R.id.tvPreviewHint)
        btnTogglePreview = findViewById(R.id.btnTogglePreview)
        btnScreenOff = findViewById(R.id.btnScreenOff)
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

        btnTogglePreview.setOnClickListener {
            previewEnabled = !previewEnabled
            applyPreviewState()
            updateUI()
        }

        btnScreenOff.setOnClickListener { requestScreenOff() }

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

        btnApplyInterface.setOnClickListener {
            // Penjaga kedua: tombol sudah dinonaktifkan saat streaming, tetapi
            // status dapat berubah di antara pembaruan UI dan ketukan.
            if (MJPEGServerInstance.isRunning()) {
                Toast.makeText(this, R.string.label_interface_locked, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            interfacePrefs.setChoice(selectedChoice())
            Toast.makeText(this, R.string.toast_interface_saved, Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    /** Pilihan yang sedang ditandai di radio group. */
    private fun selectedChoice(): NetworkChoice = when (groupInterface.checkedRadioButtonId) {
        R.id.rbInterfaceWifiLan -> NetworkChoice.WIFI_LAN
        R.id.rbInterfaceUsbTethering -> NetworkChoice.USB_TETHERING
        else -> NetworkChoice.ALL
    }

    /**
     * Tampilkan pilihan tersimpan. Dipanggil sekali saat Activity dibuat;
     * pembaruan berkala tidak menimpa pilihan operator yang belum diterapkan.
     */
    private fun showStoredChoice() {
        groupInterface.check(
            when (interfacePrefs.getChoice()) {
                NetworkChoice.WIFI_LAN -> R.id.rbInterfaceWifiLan
                NetworkChoice.USB_TETHERING -> R.id.rbInterfaceUsbTethering
                NetworkChoice.ALL -> R.id.rbInterfaceAll
            }
        )
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

        val port = MJPEGServerInstance.getPort() ?: MJPEGServerInstance.config.port
        val bindAddress = MJPEGServerInstance.getBindAddress()

        tvIpInfo.text = when {
            running && bindAddress == null ->
                getString(R.string.label_stream_offline)
            // `0.0.0.0` bukan alamat yang dapat diketik klien; tampilkan
            // alamat interface yang benar-benar ada.
            running && bindAddress == NetworkChoice.ANY_ADDRESS ->
                getString(R.string.label_stream_url_all, getDeviceIpAddress(), port)
            running ->
                getString(R.string.label_stream_url, bindAddress, port)
            else ->
                getString(R.string.label_stream_offline)
        }

        tvCameraState.text = if (running) {
            val reason = snapshot.cameraStateReason?.let { " ($it)" } ?: ""
            getString(R.string.label_camera_state, snapshot.cameraState + reason, snapshot.clients)
        } else {
            getString(R.string.label_server_stopped)
        }

        tvApiKey.text = MJPEGServerInstance.getApiKey()?.let {
            getString(R.string.label_api_key, it)
        } ?: getString(R.string.label_api_key_none)

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
        btnScreenOff.isEnabled = running

        // Pilihan interface hanya berlaku pada start server berikutnya, jadi
        // kontrolnya dikunci selama stream berjalan (T-100).
        groupInterface.isEnabled = !running
        btnApplyInterface.isEnabled = !running
        tvInterfaceHint.text = if (running) {
            getString(R.string.label_interface_locked)
        } else if (MJPEGServerInstance.bindAddressFor(interfacePrefs.getChoice()) == null) {
            getString(R.string.label_interface_unavailable)
        } else {
            getString(R.string.label_interface_ready)
        }

        btnTogglePreview.setImageResource(
            if (previewEnabled) R.drawable.ic_preview_on else R.drawable.ic_preview_off
        )
        tvPreviewHint.text = when {
            !running -> getString(R.string.label_stream_not_running)
            !previewEnabled -> getString(R.string.label_preview_off)
            else -> ""
        }
        val showPreview = running && previewEnabled && serviceBinder != null
        previewView.visibility = if (showPreview) View.VISIBLE else View.GONE
        previewOverlay.visibility = if (showPreview) View.GONE else View.VISIBLE
    }

    /**
     * Pasang atau lepas permukaan pratinjau pada service. Pratinjau mati
     * tidak menghentikan kamera maupun stream (CWG-17).
     */
    private fun applyPreviewState() {
        val binder = serviceBinder ?: return
        binder.setPreviewProvider(if (previewEnabled) previewView.surfaceProvider else null)
    }

    /** Komponen admin perangkat yang dipakai tombol matikan layar (D-33). */
    private val adminComponent: ComponentName
        get() = ComponentName(this, ScreenLockAdminReceiver::class.java)

    /**
     * Kunci layar lewat admin perangkat.
     *
     * Tiga keadaan diperiksa berurutan dan tidak dicampur: admin belum aktif
     * (minta aktivasi), layar belum punya kunci (jelaskan, jangan klaim
     * berhasil), dan siap (panggil `lockNow`). Tidak ada aktivasi diam-diam
     * saat aplikasi dibuka (CWG-19).
     */
    private fun requestScreenOff() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (dpm == null || !dpm.isAdminActive(adminComponent)) {
            Log.i(TAG, "Admin belum aktif; membuka layar aktivasi, tidak mengunci.")
            try {
                startActivity(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        .putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            getString(R.string.device_admin_explanation)
                        )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Layar aktivasi admin tidak dapat dibuka: ${e.message}", e)
                Toast.makeText(this, R.string.toast_lock_failed, Toast.LENGTH_LONG).show()
            }
            return
        }

        val keyguard = getSystemService(android.app.KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isDeviceSecure) {
            // Tanpa PIN/pattern, `lockNow` tidak menghasilkan layar terkunci.
            Toast.makeText(this, R.string.toast_set_screen_lock, Toast.LENGTH_LONG).show()
            return
        }

        try {
            Log.i(TAG, "lockNow dipanggil: admin aktif, kunci layar tersedia.")
            dpm.lockNow()
            Toast.makeText(this, R.string.toast_screen_locked, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "lockNow ditolak: ${e.message}", e)
            Toast.makeText(this, R.string.toast_lock_failed, Toast.LENGTH_LONG).show()
        }
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
