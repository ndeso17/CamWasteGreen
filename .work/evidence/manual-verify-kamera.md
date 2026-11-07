# Checklist MANUAL-VERIFY kamera CamWasteGreen

Berkas ini adalah **daftar periksa yang belum dikerjakan**, bukan bukti hasil.
Semua butir berstatus `BELUM DIUJI`.

Alasan: pada sesi 2026-11-08 tidak ada perangkat yang terhubung
(`adb devices` kosong). Sebelumnya pengujian memakai `ZP2222SQLL`
(moto g57 power, Android 16, targetSdk 34), tetapi perangkat itu tidak lagi
terdeteksi.

Aturan pengisian:
- Jangan ubah status menjadi teruji tanpa mengisi kolom **Bukti** dan
  **Tanggal**.
- Angka FPS, latensi, dan toleransi posisi **belum disepakati** (B-06).
  Catat hasil ukur apa adanya; jangan menetapkan ambang keberterimaan sendiri.
- Simpan tangkapan log, `curl`, dan berkas keluaran di `.work/evidence/`.

Kolom status yang dipakai: `BELUM DIUJI`, `TERUJI-SESUAI`, `TERUJI-TIDAK-SESUAI`,
`TIDAK-DAPAT-DIUJI`.

---

## A. Ketahanan service

### MV-01 — Layar mati tidak menghentikan stream

- Status: `BELUM DIUJI`
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

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-11
- Perangkat: ____
- Prosedur: mulai stream, tekan HOME, buka aplikasi lain, tunggu 60 detik,
  lalu akses `/status`.
- Bukti: keluaran `/status` lengkap, `dumpsys activity activities` yang
  menunjukkan `topResumedActivity` bukan CamWasteGreen.
- Tanggal: ____
- Catatan: ____

### MV-03 — Pengingat setelah boot

- Status: `BELUM DIUJI`
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

## B. Batas klien dan klien lambat

### MV-04 — Dua klien bersamaan diterima

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-10
- Perangkat: ____
- Prosedur: buka dua `curl` `/video` bersamaan selama 30 detik, lalu baca
  `/status`.
- Bukti: dua keluaran `curl` (kode HTTP dan byte), `/status` yang menunjukkan
  `clients:2`.
- Tanggal: ____
- Catatan: ____

### MV-05 — Klien ketiga ditolak dengan alasan jelas

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-10
- Perangkat: ____
- Prosedur: dengan dua klien berjalan, buka klien ketiga.
- Bukti: kode HTTP klien ketiga (harus `503`) dan badan balasannya.
- Tanggal: ____
- Catatan: ____

### MV-06 — Klien lambat diputus tanpa memblokir server

- Status: `BELUM DIUJI`
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

- Status: `BELUM DIUJI`
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

- Status: `BELUM DIUJI`
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

- Status: `BELUM DIUJI`
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

- Status: `BELUM DIUJI`
- Kebutuhan: CWG-15
- Perangkat: ____
- Prosedur: dari mesin lain, akses `/status` tanpa header, dengan header
  salah, dan dengan header benar.
- Bukti: tiga keluaran `curl -i` (kode dan badan), dan kode alasan pada log
  aplikasi.
- Tanggal: ____
- Catatan: ____

### MV-13 — Pembatasan alamat klien

- Status: `BELUM DIUJI`
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

| Butir | Status |
|---|---|
| MV-01 .. MV-14 | `BELUM DIUJI` |

Tidak ada butir pada berkas ini yang boleh dipakai sebagai bukti bahwa
kebutuhan terkait telah terpenuhi.
