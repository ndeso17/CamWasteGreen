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

- Status: `TERUJI-SESUAI` (8 Nov 2026, reboot bersih, perangkat `ZP2222SQLL`,
  APK `1118` tanpa token uji)
- Catatan hasil (uji sah, 11:19–11:24):
  - `btime 1794111560` = 11:19:20; paket `stopped=false` sebelum reboot
    (dipasang lalu dibuka sekali, tanpa `am force-stop`).
  - `am_proc_start 11:21:53.149 [0,26293,10458,dev.ndeso17.camwastegreen,
    broadcast,{dev.ndeso17.camwastegreen/dev.ndeso17.camwastegreen.BootReceiver}]`
    dan `Start proc 26293 ... for broadcast {...BootReceiver}`.
  - Notifikasi `id=1002 channel=boot_reminder category=reminder
    flags=AUTO_CANCEL` dengan `when=1794111716125` = **11:21:56**, tiga detik
    setelah proses lahir karena broadcast.
  - Tangkapan baki notifikasi (OCR): "Kamera belum menyala - 2 m" dan
    "Android tidak mengizinkan kamera dinyalakan otomatis s...";
    berkas `.work/evidence/mv03-reboot-clean-20261108.png`.
  - Tidak ada proses aplikasi (`ps -A` kosong untuk paket ini), tidak ada
    service (`dumpsys activity services` → 0), tidak ada klien kamera
    (`dumpsys media.camera` → 0 baris menyebut paket), dan tidak ada peluncuran
    Activity (`am_activity_launch_time`/`am_create_activity` untuk paket → 0).
  - Jadi: pengingat tampil, kamera **tidak** menyala, aplikasi tidak terbuka
    sendiri. Sesuai D-32.
- Kebutuhan: CWG-12 (bentuk baru menurut D-32)
- Perangkat: `ZP2222SQLL` (moto g57 power, Android 16, targetSdk 34)
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

Kesimpulan awal: **`MV-03` kembali `BELUM DIUJI`.** Uji ulang pada 11:19–11:24
(8 Nov 2026) dengan reboot bersih berhasil, dan butir ini kembali
`TERUJI-SESUAI`. Bukti rinci ada di "Catatan hasil (uji sah, 11:19–11:24)" di
atas.

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
- Uji lanjutan 8 Nov 2026 (APK 1050, LAN `10.42.0.83:8085`, klien
  `curl --limit-rate 1k`): klien cepat tetap menerima `HTTP=200` (88274 byte)
  saat klien lambat aktif; `/status` tanggap 0,2–3,0 detik (selama
  klien lambat sejak 5 detik). Pada satu percobaan `clients` turun ke 0 pada
  ±17 detik sementara `curl` masih menerima data sampai batas 170 detik
  (250072 byte, `exit=28`). Ini tidak sama dengan pemutusan oleh server:
  tidak ada log pemutusan di kode, dan koneksi tidak selesai. Anomali ini
  belum dijelaskan dan tetap menjadi temuan terbuka.
- Catatan hasil awal: satu klien `curl --limit-rate 2k` membaca `/video` selama
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

- Status: `TERUJI-SESUAI` (8 Nov 2026, APK 1050, sampel 5 menit penuh; **bukan** bukti FPS memenuhi target)
- **Batas klaim:** yang sesuai adalah prosedur pengukuran dan kejujuran angka
  (CWG-N05). FPS terukur 3,47–3,58 sementara `targetFps` konfigurasi 20;
  selisih itu **belum** diterima sebagai wajar maupun sebagai kegagalan karena
  ambang belum disepakati (B-06). QC-47 tetap terbuka.
- Sampel 5 menit (11 sampel, 30 detik, 720x1280, target 20, kualitas High):
  11:08:37 `3,49` · 11:09:07 `3,51` · 11:09:37 `3,52` · 11:10:07 `3,58` ·
  11:10:38 `3,58` · 11:11:08 `3,58` · 11:11:39 `3,53` · 11:12:09 `3,53` ·
  11:12:40 `3,53` · 11:13:10 `3,57` · 11:13:40 `3,47`. `frameSeq` naik ±107 per 30 detik.
- Suhu (`dumpsys thermalservice`, bagian "Current temperatures from HAL",
  sesudah sampel): CPU 55,7–62,1 °C, GPU 50,6–51,6 °C, `skin` 45,7 °C
  (`mStatus=3`), `Thermal Status: 3`. Suhu sebelum sampel (`Cached`, bukan
  pembacaan langsung) tidak dapat dijadikan pembanding.
- Catatan: `Thermal Status: 3` tercatat pada perangkat selama uji. FPS 3,5
  dicapai dalam kondisi itu; belum diuji apakah status termal lebih rendah
  menghasilkan FPS berbeda.
- Catatan lama: uji parsial sebelumnya 6 sampel dalam ±3 menit.
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

- Status: `BELUM DIUJI` (uji parsial 8 Nov 2026, APK 1050, LAN `10.42.0.83:8085`)
- Kebutuhan: CWG-N05
- Perangkat: `ZP2222SQLL`
- Hasil parsial (`curl` 62 detik, `curl -w %{speed_download}`, 720x1280):
  - High (q80, default): 11816434 byte, ±190585 B/s, `measuredFps` 3,63
  - Medium (q50): 3988733 byte, ±64333 B/s (`exit=28` karena `-m 62`, disengaja)
  - Low (q30): 5692747 byte, ±91817 B/s, `exit=28` karena `-m 62`, disengaja
- Anomali: Low lebih besar dari Medium. Tidak dijelaskan oleh data yang ada
  (kemungkinan konten adegan berubah antar-uji). Uji ulang berurutan dengan
  adegan tetap diperlukan sebelum angka ini dipakai.
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

- Status: `TERUJI-SESUAI` (8 Nov 2026, perangkat `ZP2222SQLL`, APK 1036)
- Catatan hasil: sisi tolak (APK 0947, `allowedAddresses=10.99.99.99`): akses
  dari `127.0.0.1` → `401` `ADDRESS_NOT_ALLOWED`. Sisi terima (APK 1036,
  `allowedAddresses=127.0.0.1`): `/status` dari `127.0.0.1` dengan token benar
  → `200` (JSON memuat `streaming:true`, `720x1280`), tanpa token → `401`,
  token salah → `401`. `/video` dengan token benar → `200`.
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
- Bukti 8 Nov 2026: `.work/evidence/mv14-interface-locked-20261108.png`
  (panel dibuka saat streaming aktif) memuat teks OCR "Hentikan stream untuk
  mengganti interface"; bagian interface tampil tanpa tombol aktif yang bisa
  diubah. `/proc/net/tcp6` memuat listener `:1F95` (8085) di `::` untuk
  pilihan "semua" (state `0A` LISTEN).
- Bukti yang masih harus diisi: USB tethering nyata (`usb0` dengan IPv4).
  Pilihan USB tethering tanpa interface sudah terbukti ditolak (WP-35), tetapi
  jalur tethering yang berjalan belum.
- Tanggal: ____
- Catatan: ____

---

## Ringkasan

| Butir | Status | Catatan |
|---|---|---|
| MV-01 | `TERUJI-SESUAI` | layar tidur, `/video` 200, service foreground `camera` |
| MV-02 | `TERUJI-SESUAI` | latar belakang, `/status` tetap `streaming:true` |
| MV-03 | `TERUJI-SESUAI` | reboot bersih 11:19 (`stopped=false`): broadcast BootReceiver 11:21:53, notifikasi 11:21:56, tanpa proses/kamera/Activity (`mv03-reboot-clean-20261108.png`); bukti 10:14/10:18 dicabut |
| MV-04 | `TERUJI-SESUAI` | `clients:2` |
| MV-05 | `TERUJI-SESUAI` | klien ketiga `503` "Batas 2 klien tercapai" |
| MV-06 | `BELUM DIUJI` | parsial: server tidak terblokir; pemutusan oleh server belum terbukti (lihat catatan MV-06) |
| MV-07 | `TERUJI-SESUAI` | 5 menit: 3,47–3,58 FPS pada target 20; `Thermal Status: 3`; selisih vs target belum dinilai (B-06, QC-47) |
| MV-08 | `BELUM DIUJI` | parsial: High ~190 kB/s, Medium ~64 kB/s, Low ~92 kB/s; Low>Medium tidak dijelaskan |
| MV-09 | `BELUM DIUJI` | parsial: `tuningState=locked`; perbandingan bingkai belum |
| MV-10 | `BELUM DIUJI` | butuh dudukan sebenarnya |
| MV-11 | `TERUJI-SESUAI` | 720x1280 dari `/status` dan JPEG |
| MV-12 | `TERUJI-SESUAI` | `MISSING_KEY`/`WRONG_KEY` → 401, token benar → 200 |
| MV-13 | `TERUJI-SESUAI` | 8 Nov 2026, APK 1036 dengan `allowedAddresses=127.0.0.1`: `/status` dari `127.0.0.1` dengan token benar → `200`, tanpa token → `401`; `ADDRESS_NOT_ALLOWED` dari APK 0947 tetap berlaku |
| MV-14 | `BELUM DIUJI` | parsial: tombol terkunci saat streaming (tangkapan `mv14-interface-locked-20261108.png`); listener `::` untuk "semua"; USB tethering nyata belum |
| MV-15 | `TERUJI-SESUAI` | 8 Nov 2026, APK bertoken 1230 di `ZP2222SQLL`: pratinjau nyala (rata-rata piksel 69,4) → mati (10,7, teks "Pratinjau dimatikan. Stream tetap berjalan.") → nyala (69,9); `/status` `streaming:true`, `frameSeq` 44 → 751 → 793; `measuredFps` dicatat, bukan ambang |
| MV-16 | `TERUJI-SESUAI` | 8 Nov 2026, `ZP2222SQLL`, APK 1246. Tap `btnScreenOff` (`813 181`) saat layar `Awake`: log `IPCam.MainActivity: lockNow dipanggil: admin aktif, kunci layar tersedia.` (12:48:10.205), lalu `mWakefulness=Dozing`, `mAwake=false`, `mScreenOnFully=false`, `isKeyguardShowing=true`; `/status` `streaming:true` (`frameSeq` 97) dan `/video` `200` (`mv16b-frame.jpg`). Uji 12:41 sebelumnya (APK 1230) menunjukkan hasil layar sama tanpa baris log; uji itu tidak dipakai sebagai bukti utama. Tap `830 179` sempat tidak mengenai tombol dan `862 182` jatuh di celah antar-chip; keduanya dibuang |
| MV-17 | `TERUJI-SESUAI` | 8 Nov 2026: admin sudah aktif (`Enabled Device Admins` memuat `ScreenLockAdminReceiver` dengan `force-lock`); tidak ada permintaan admin saat aplikasi dibuka. Alur aktivasi dari nol belum diuji karena admin sudah ada sebelumnya |

Butir `TERUJI-SESUAI` hanya berlaku untuk perangkat `ZP2222SQLL` pada
8 November 2026 dengan APK uji. Butir itu **tidak** menetapkan ambang mutu:
`B-06` (FPS/latensi/usia frame) tetap terbuka, `B-03` (geometri pemasangan)
tetap terbuka, dan MV-10 belum dapat dijalankan tanpa dudukan sebenarnya.
Butir yang belum diuji tidak boleh dipakai sebagai bukti pemenuhan kebutuhan.
