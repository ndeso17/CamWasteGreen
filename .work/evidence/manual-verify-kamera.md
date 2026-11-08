# Checklist MANUAL-VERIFY kamera CamWasteGreen

Berkas ini adalah **daftar periksa hasil pengujian perangkat**, bukan bukti
bahwa kebutuhan sudah terpenuhi untuk semua perangkat.

Per 8 November 2026 sebagian butir sudah diuji pada `ZP2222SQLL`
(moto g57 power, Android 16, targetSdk 34) dengan APK uji bertoken. Butir yang
belum dijalankan tetap `BELUM DIUJI`; butir yang tidak dapat dijalankan pada
perangkat ini ditandai `TIDAK-DAPAT-DIUJI` beserta alasannya. Butir yang baru
sebagian diuji ditandai `BELUM DIUJI` dengan catatan `uji parsial` agar tidak
terbaca sebagai lulus.

Aturan pengisian:
- Jangan ubah status menjadi teruji tanpa mencatat **hasil** dan **tanggal**
  (tanggal ada di baris status, hasil di `Catatan hasil`).
- Angka FPS, latensi, dan toleransi posisi **belum disepakati** (B-06).
  Catat hasil ukur apa adanya; jangan menetapkan ambang keberterimaan sendiri.
- Simpan tangkapan log, `curl`, dan berkas keluaran di `.work/evidence/`.

Kolom status yang dipakai: `BELUM DIUJI`, `TERUJI-SESUAI`, `TERUJI-TIDAK-SESUAI`,
`TIDAK-DAPAT-DIUJI`.

---

## A. Ketahanan service

### MV-01 — Layar mati tidak menghentikan stream

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: setelah `KEYCODE_SLEEP`, `dumpsys power` menunjukkan
  `mWakefulness=Dozing`. Setelah 60 detik, `curl /video` menjawab `HTTP=200`
  dan `bytes=1932032`; `/status` tetap `streaming:true`, `frameSeq` naik
  (768 → 1241). `dumpsys activity services`:
  `ServiceRecord{... dev.ndeso17.camwastegreen/.CameraService}`, 
  `isForeground=true foregroundId=1001 types=0x00000040`
  (`FOREGROUND_SERVICE_TYPE_CAMERA`), notifikasi `channel=camera_stream`
  dengan flag `ONGOING_EVENT|NO_CLEAR`.
- Kebutuhan: CWG-11
- Perangkat: ____
- Prosedur: mulai stream, kunci layar (tombol power), tunggu 60 detik,
  lalu `curl -s -o /dev/null -w "%{http_code}" -H "X-Api-Key: <token>" http://<ip>:8085/video`.
- Bukti yang harus diisi: keluaran `curl` (kode HTTP dan jumlah byte),
  keluaran `dumpsys activity services dev.ndeso17.camwastegreen`, dan logcat
  yang memuat status foreground service.
- Tanggal: ____
- Catatan: ____

### MV-02 — Aplikasi di latar belakang tetap melayani

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: setelah `KEYCODE_HOME`, `topResumedActivity` =
  `com.motorola.launcher3/...CustomizationPanelLauncher`. `/status` tiap 30 detik
  selama ±3 menit tetap `streaming:true`, `frameSeq` naik (244 → 768),
  `measuredFps` 3,2–3,5. Bukti tekstual di sesi; tangkapan layar belum disimpan.
- Kebutuhan: CWG-11
- Perangkat: ____
- Prosedur: mulai stream, tekan HOME, buka aplikasi lain, tunggu 60 detik,
  lalu akses `/status`.
- Bukti: keluaran `/status` lengkap, `dumpsys activity activities` yang
  menunjukkan `topResumedActivity` bukan CamWasteGreen.
- Tanggal: ____
- Catatan: ____

### MV-03 — Pengingat setelah boot

- Status: `BELUM DIUJI` — bukti sebelumnya dicabut, lihat catatan koreksi di bawah
- Kebutuhan: CWG-12 (bentuk baru menurut D-32)
- Perangkat: ____
- Prosedur: berikan izin notifikasi, reboot perangkat, tunggu 90 detik tanpa
  membuka aplikasi, lalu periksa baki notifikasi untuk
  "Kamera belum menyala" dari CamWasteGreen. Setelah itu akses `/status` dari
  mesin lain **sebelum** menekan mulai.
- Bukti: tangkapan layar notifikasi, logcat yang memuat
  `IPCam.BootReceiver`, dan keluaran `/status` yang menunjukkan kamera belum
  berjalan (atau tidak ada proses aplikasi).
- Catatan wajib: **`CWG-12` sudah berubah bentuk (D-32, penutup B-31).** Yang
  diuji bukan lagi autostart, melainkan munculnya notifikasi pengingat setelah
  boot. Android 14+ menolak akses kamera untuk foreground service tipe
  `camera` yang dimulai dari latar belakang
  (`am_foreground_service_denied`), jadi tidak ada lagi klaim autostart.
  Butir ini hanya dapat dinyatakan sesuai bila notifikasi benar-benar muncul
  dan kamera tetap belum berjalan sampai operator menekan mulai.
- Tanggal: ____
- Catatan: ____

#### Koreksi bukti MV-03, 8 November 2026

Bukti MV-03 yang sebelumnya ditulis (dengan status `TERUJI-SESUAI`) **dicabut**.
Alasannya, berdasarkan pembacaan ulang logcat perangkat:

- `am_proc_start` untuk `dev.ndeso17.camwastegreen` pada 8 Nov 2026 mencatat
  `10:18:13.418 ... for next-top-activity {...MainActivity}`. Proses aplikasi
  lahir karena **operator membuka aplikasi**, bukan karena broadcast.
- Notifikasi `id=1002 channel=boot_reminder` yang tercatat memiliki
  `when=1794107894112` = **10:18:14**, yaitu satu detik setelah pembukaan
  aplikasi di atas dan empat menit setelah `btime`. Waktu ini tidak
  membedakan antara pemicu boot dan pembukaan aplikasi, sehingga tidak bisa
  dijadikan bukti perilaku reboot.
- `BroadcastQueue: BOOT_COMPLETED_BROADCAST_COMPLETION_LATENCY_REPORTED`
  mencatat `10:18:14.118 ... receiversSize:1` dan
  `10:18:30.067 ... receiversSize:394`, dengan
  `ActivityManager: Finished processing BOOT_COMPLETED for u0` pada
  `10:18:30`. Boot perangkat (`btime 1794107662` = 10:14:22) jauh lebih awal.
- Baris log aplikasi `IPCam.BootReceiver` **tidak ada** di buffer logcat saat
  diperiksa. Bukti lama bertumpu pada `dumpsys notification` dan tangkapan
  layar baki, bukan pada log aplikasi.
- Catatan tambahan: notifikasi `id=1002` juga pernah muncul sekitar dua detik
  setelah `adb install`, karena sistem memicu `BOOT_COMPLETED` ke paket yang
  baru dipasang/diganti. Karena itu kemunculan notifikasi saja tidak
  membuktikan perilaku reboot.
- Selain itu, paket dalam keadaan `stopped=true` (setelah `am force-stop`)
  tidak menerima `BOOT_COMPLETED`. Untuk uji reboot yang sah, paket harus
  `stopped=false`: buka aplikasi sekali setelah pemasangan, tekan HOME,
  baru reboot.

Kesimpulan: **`MV-03` kembali `BELUM DIUJI`.** Uji ulang yang sah butuh
reboot dengan paket `stopped=false`, logcat yang dibersihkan sebelum reboot
(`adb logcat -c`), dan bukti berupa baris log
`IPCam.BootReceiver: Pengingat boot ditampilkan` beserta `when` notifikasi
yang jatuh **setelah** `btime` dan **sebelum** `am_proc_start` aplikasi
untuk pembukaan manual.

Catatan pengguna (8 Nov 2026): proses `CameraService` pada 10:18:24
dinyalakan **manual** oleh operator melalui tombol aplikasi, bukan oleh
`BootReceiver`. `MainActivity` memanggil `CameraService.start` hanya dari
tekan tombol tersebut; `logcat` mencatat
`10:18:24.079 Background started FGS: Allowed [... callingUid: 10458;
uidState: TOP ...]`. Jadi tidak ada autostart kamera, konsisten dengan D-32.

## B. Batas klien dan klien lambat

### MV-04 — Dua klien bersamaan diterima

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: dua `curl /video` dengan `X-Api-Key` benar berjalan bersamaan;
  `/status` menunjukkan `clients:2`.
- Kebutuhan: CWG-10
- Perangkat: ____
- Prosedur: buka dua `curl` `/video` bersamaan selama 30 detik, lalu baca
  `/status`.
- Bukti: dua keluaran `curl` (kode HTTP dan byte), `/status` yang menunjukkan
  `clients:2`.
- Tanggal: ____
- Catatan: ____

### MV-05 — Klien ketiga ditolak dengan alasan jelas

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: klien ketiga menerima `HTTP=503` dengan badan
  `Batas 2 klien tercapai`.
- Kebutuhan: CWG-10
- Perangkat: ____
- Prosedur: dengan dua klien berjalan, buka klien ketiga.
- Bukti: kode HTTP klien ketiga (harus `503`) dan badan balasannya.
- Tanggal: ____
- Catatan: ____

### MV-06 — Klien lambat diputus tanpa memblokir server

- Status: `BELUM DIUJI` (uji parsial 8 Nov 2026: server tetap melayani; pemutusan oleh server belum terbukti)
- Catatan hasil: satu klien `curl --limit-rate 2k` membaca `/video` selama
  70 detik (270336 byte terkirim) sementara `/status` dipanggil tiap 10
  detik. Semua balasan `/status` `HTTP=200` dengan waktu tanggap 15–25 ms,
  jadi klien lambat tidak memblokir server. Klien lambat terputus oleh
  `curl --max-time`; pemutusan oleh sisi server belum dibedakan dari pemutusan
  karena `curl` menutup koneksi — `clients` kembali `0` sesudah koneksi
  ditutup. Perilaku "klien lambat diputus oleh server" sendiri belum
  dibuktikan secara terpisah.
- Kebutuhan: CWG-09
- Perangkat: ____
- Prosedur: hubungkan klien yang membaca sangat lambat (mis.
  `curl --limit-rate 2k`), biarkan 60 detik, sementara klien lain mengakses
  `/status` berulang.
- Bukti: waktu tanggap `/status` selama uji, log pemutusan klien lambat,
  jumlah `clients` setelah pemutusan.
- Tanggal: ____
- Catatan: ____

## C. Pengukuran

### MV-07 — FPS terukur pada perangkat

- Status: `BELUM DIUJI` (uji parsial 8 Nov 2026: 6 sampel dalam ±3 menit, bukan 5 menit penuh)
- Catatan hasil parsial: `measuredFps` dari `/status` tiap 30 detik pada
  720x1280, `targetFps` 20: 3,22 → 3,49 → 3,50 → 3,46 → 3,48 (09:41–09:43,
  layar menyala, aplikasi di latar belakang). Setelah layar tidur, satu sampel
  3,40. `frameSeq` naik konsisten ±105 per 30 detik. Suhu perangkat tidak
  dicatat. Angka ini hasil satu perangkat dan **bukan** ambang kelulusan
  (B-06 tetap terbuka).
- Kebutuhan: CWG-08, CWG-N05
- Perangkat: ____
- Prosedur: jalankan stream 5 menit pada resolusi dan FPS konfigurasi default,
  catat `measuredFps` dari `/status` setiap 30 detik.
- Bukti: tabel waktu vs `measuredFps`, resolusi nyata dari `/status`,
  suhu perangkat bila tersedia.
- Catatan: QC-47 mencatat FPS terukur sekitar 1,6–1,8 pada uji sebelumnya.
  Angka itu hasil satu perangkat dan belum disepakati sebagai ambang (B-06).
- Tanggal: ____
- Catatan: ____

### MV-08 — Bandwidth terukur

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-N05
- Perangkat: ____
- Prosedur: ukur byte yang diterima klien selama 60 detik pada kualitas
  konfigurasi default, ulangi pada kualitas minimum dan maksimum.
- Bukti: byte/detik per tingkat kualitas, cara pengukuran.
- Tanggal: ____
- Catatan: ____

## D. Kualitas gambar

### MV-09 — Kunci AE/AWB/AF berperilaku setelah warmup

- Status: `BELUM DIUJI` (uji parsial 8 Nov 2026: `tuningState` terbaca `locked`; perbandingan bingkai belum)
- Catatan hasil: `/status` melaporkan `tuningState:"locked"` secara
  konsisten sejak stream berjalan lebih dari 30 frame pada semua pembacaan
  (09:40 sampai 09:52). Tidak ada pembacaan `unsupported` maupun `failed`.
  Perbandingan visual warna antar bingkai belum dilakukan, jadi yang
  terbukti adalah state kunci, bukan kualitas warnanya.
- Kebutuhan: CWG-07, CWG-13
- Perangkat: ____
- Prosedur: arahkan kamera ke objek warna berbeda bergantian; setelah 30
  frame warmup, bandingkan nilai eksposur/white balance antar frame.
- Bukti: nilai `tuningState` dari `/status` (`locked`/`unsupported`/`failed`),
  dua tangkapan frame dengan selang 10 detik, dan pengamatan apakah warna
  bergeser.
- Catatan: `unsupported` adalah keterbatasan perangkat, bukan kegagalan stream.
- Tanggal: ____
- Catatan: ____

### MV-10 — Orientasi frame sesuai pemasangan

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-06
- Perangkat: ____
- Prosedur: pasang ponsel pada dudukan sebenarnya, ambil satu frame, periksa
  apakah atas frame sesuai atas objek.
- Bukti: berkas JPEG hasil `/video` dan foto dudukan.
- Catatan: pengujian rotasi paksa tidak dapat dilakukan pada perangkat uji
  sebelumnya (`mDisplayRotation` selalu `ROTATION_0`). `mountRotationDegrees`
  belum pernah diuji dengan nilai bukan nol.
- Tanggal: ____
- Catatan: ____

### MV-11 — Resolusi nyata setelah `ResolutionSelector`

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: `/status` `width:720`, `height:1280`; JPEG `/video` juga
  720x1280. QC-46 (1440x1440) tidak terulang pada uji ini.
- Kebutuhan: CWG-08
- Perangkat: ____
- Prosedur: baca `width`/`height` dari `/status` setelah stream berjalan.
- Bukti: keluaran `/status`.
- Catatan: QC-46 mencatat 1440x1440 pada uji sebelum WP-23. WP-23 mengganti
  pemilihan resolusi; hasilnya pada perangkat belum diketahui.
- Tanggal: ____
- Catatan: ____

## E. Akses jaringan dan token

### MV-12 — Penolakan tanpa token dan token salah

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK uji)
- Catatan hasil: tanpa header → `401` `MISSING_KEY`; token salah → `401`
  `WRONG_KEY`; token benar → `200` dan JSON `/status`. Uji dari `127.0.0.1` via
  `adb forward`, belum dari mesin lain di LAN.
- Kebutuhan: CWG-15
- Perangkat: ____
- Prosedur: dari mesin lain, akses `/status` tanpa header, dengan header
  salah, dan dengan header benar.
- Bukti: tiga keluaran `curl -i` (kode dan badan), dan kode alasan pada log
  aplikasi.
- Tanggal: ____
- Catatan: ____

### MV-13 — Pembatasan alamat klien

- Status: `BELUM DIUJI` (uji parsial 8 Nov 2026: sisi tolak terbukti, sisi terima belum)
- Catatan hasil: APK uji dengan `allowedAddresses=10.99.99.99`; akses dari `127.0.0.1` → `401` `ADDRESS_NOT_ALLOWED`. Sisi terima belum diuji.
- Kebutuhan: CWG-15, CWG-N06
- Perangkat: ____
- Prosedur: isi `camwastegreen.allowedAddresses` dengan satu alamat, lalu
  akses dari alamat itu dan dari alamat lain.
- Bukti: keluaran `curl` dari kedua alamat, kode alasan pada log.
- Tanggal: ____
- Catatan: ____

### MV-14 — Pilihan interface jalur streaming

- Status: `BELUM DIUJI` (sebagian bukti sudah ada di Log WP-35, tetapi
  verifikasi ulang pada perangkat belum dilakukan pada sesi ini)
- Kebutuhan: CWG-10, CWG-15
- Perangkat: `ZP2222SQLL` (tidak terdeteksi pada sesi ini)
- Prosedur: pilih tiap opsi interface, mulai stream, periksa alamat listener
  (`/proc/net/tcp`, `/proc/net/tcp6`) dan akses dari mesin lain.
- Bukti yang sudah ada: log WP-35 mencatat `0.0.0.0:8085` untuk "semua",
  `10.42.0.83:8085` untuk Wi-Fi/LAN, dan penolakan start untuk USB tethering
  tanpa interface.
- Bukti yang masih harus diisi: USB tethering nyata (`usb0` dengan IPv4),
  dan tangkapan layar yang menunjukkan tombol terapkan nonaktif saat
  streaming.
- Tanggal: ____
- Catatan: ____

---

## Ringkasan

| Butir | Status | Catatan |
|---|---|---|
| MV-01 | `TERUJI-SESUAI` | layar tidur, `/video` 200, service foreground `camera` |
| MV-02 | `TERUJI-SESUAI` | latar belakang, `/status` tetap `streaming:true` |
| MV-03 | `BELUM DIUJI` | bukti sebelumnya dicabut: notifikasi 10:18:14 bersamaan pembukaan app, bukan boot |
| MV-04 | `TERUJI-SESUAI` | `clients:2` |
| MV-05 | `TERUJI-SESUAI` | klien ketiga `503` "Batas 2 klien tercapai" |
| MV-06 | `BELUM DIUJI` | parsial: server tidak terblokir; pemutusan oleh server belum |
| MV-07 | `BELUM DIUJI` | parsial: 3,2–3,5 FPS, ±3 menit, bukan 5 menit |
| MV-08 | `BELUM DIUJI` | belum diukur |
| MV-09 | `BELUM DIUJI` | parsial: `tuningState=locked`; perbandingan bingkai belum |
| MV-10 | `BELUM DIUJI` | butuh dudukan sebenarnya |
| MV-11 | `TERUJI-SESUAI` | 720x1280 dari `/status` dan JPEG |
| MV-12 | `TERUJI-SESUAI` | `MISSING_KEY`/`WRONG_KEY` → 401, token benar → 200 |
| MV-13 | `BELUM DIUJI` | parsial: `ADDRESS_NOT_ALLOWED`; sisi terima belum |
| MV-14 | `BELUM DIUJI` | USB tethering nyata dan tangkapan tombol terkunci belum |

Butir `TERUJI-SESUAI` hanya berlaku untuk perangkat `ZP2222SQLL` pada
8 November 2026 dengan APK uji. Butir itu **tidak** menetapkan ambang mutu:
`B-06` (FPS/latensi/usia frame) tetap terbuka, `B-03` (geometri pemasangan)
tetap terbuka, dan MV-10 belum dapat dijalankan tanpa dudukan sebenarnya.
Butir yang belum diuji tidak boleh dipakai sebagai bukti pemenuhan kebutuhan.
