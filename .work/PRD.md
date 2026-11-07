# PRD — CamWasteGreen (Aplikasi Kamera Pemilah)

**Repo:** `CamWasteGreen/`
**Status:** Draf kerja
**Tanggal:** 7 November 2026
**Dokumen induk:** `PRD Sistem Pemilah Objek.md` (root)
**Kontrak proses:** `AGENTS.md` (root)
**Spesifikasi backend terkait:**
`.work/PRD-backend_orkestrasi_deteksi_dan_kendali_sortir-07112026-150755.md` (root)
**Dasar kode:** fork dari `https://github.com/ndeso17/ipcam` (dipakai ulang,
bukan ditulis ulang)

## 1. Tujuan

Aplikasi Android yang mengubah HP menjadi **kamera sensor** untuk sistem
pemilah objek:

```
HP (CamWasteGreen)  --MJPEG/HTTP-->  Backend (inferensi + orkestrasi)
```

Aplikasi **tidak** menjalankan inferensi model, **tidak** memilih target, dan
**tidak** mengirim perintah ke robot. Aplikasi hanya:

1. menangkap frame kamera,
2. menyajikannya sebagai stream MJPEG bermetadata,
3. melaporkan kesehatan kamera dan stream.

## 2. Batas kewenangan

1. Inferensi, pemilihan target, dan penyusunan perintah bergerak ke backend
   pada arsitektur 7 November 2026 (`.work/DECISIONS.md` D-23). PRD induk
   bagian 11 yang menetapkan inferensi di Android **tidak diubah** oleh repo
   ini; konflik dicatat di `.work/FINDINGS.md` root sebagai `C-ARCH-01`.
2. Repo `Camera/` (lama, `id.pemilah`) tidak lagi menjadi jalur operasi
   (D-24). Kode di sana tidak boleh dipakai sebagai acuan tanpa alasan
   tertulis.
3. Perubahan geometri mesin, kalibrasi, dan pemetaan kelas ke bak adalah milik
   backend, bukan aplikasi ini.
4. Penghentian darurat tetap perangkat keras (NFR-06).

## 3. Ruang lingkup

### 3.1 Termasuk

| Area | Isi |
|---|---|
| Kamera | Capture frame, orientasi tetap, kunci eksposur, resolusi/FPS terkonfigurasi |
| Stream | Server MJPEG multipart dengan header metadata per frame |
| Kesehatan | Umur frame, nomor urut, FPS terukur, jumlah klien, status kamera |
| Operation | Foreground service, kamera tetap hidup saat app di background |
| Keamanan | Batas akses jaringan sesuai keputusan otorisasi |
| Diagnostik | Endpoint status dan log yang bisa dibaca backend |

### 3.2 Tidak termasuk

Inferensi model, pemilihan target, klasifikasi, pemetaan bak, pengiriman
perintah ke robot, audio dan mikrofon, filter visual, deteksi wajah dan
segmentasi selfie, virtual camera driver, dan integrasi OBS.

## 4. Kebutuhan fungsional

Namespace `CWG-` dipakai untuk kebutuhan repo ini dan **disahkan pengguna**
pada 7 November 2026 (D-31). Bentuk ID tidak boleh diubah tanpa keputusan
baru; perubahan bentuk wajib dicatat di `.work/DECISIONS.md` root.

### 4.1 Kamera dan stream

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| CWG-01 | Aplikasi menangkap frame dan menyajikannya sebagai stream MJPEG multipart | Klien di LAN menerima frame JPEG valid tanpa interupsi |
| CWG-02 | Setiap part MJPEG membawa nomor urut dan waktu pengambilan | Header `X-Frame-Seq` dan `X-Frame-Captured-At-Ms` terbaca dan bernilai naik |
| CWG-03 | Server hanya mengirim frame baru, bukan mengulang frame yang sama | Dua part berturut-turut punya `X-Frame-Seq` berbeda |
| CWG-04 | `/status` melaporkan FPS terukur, bukan nilai konfigurasi | Nilai FPS berasal dari penghitung frame, bukan dari variabel UI |
| CWG-05 | Server melaporkan lebar, tinggi, dan jumlah klien aktif | Field tersebut terbaca di `/status` |
| CWG-06 | Orientasi kamera tetap, tidak mengikuti sensor | Rotasi frame keluar tetap sesuai pemasangan di conveyor |
| CWG-07 | Eksposur dan white balance dikunci setelah warmup | Nilai otomatis tidak berubah selama operasi |
| CWG-08 | Resolusi dan FPS dapat dikonfigurasi, bukan hardcode | Perubahan konfigurasi tercermin di `/status` |
| CWG-09 | Klien yang lambat tidak memblokir thread server secara permanen | Penulisan frame punya batas waktu; klien lambat diputus, server tetap melayani |
| CWG-10 | Jumlah klien dibatasi | Melebihi batas menghasilkan penolakan yang jelas |

### 4.2 Operation dan ketahanan

| ID | Kebutuhan | Kriteria terima |
|---|---|---|
| CWG-11 | Kamera berjalan di foreground service bertipe `camera` | Layar mati dan app di background membuat stream tetap hidup |
| CWG-12 | Aplikasi mengingatkan operator setelah perangkat dinyalakan bahwa kamera perlu dinyalakan manual (D-32; autostart tidak mungkin pada Android 14+, lihat B-31) | Setelah boot, notifikasi pengingat muncul bila izin `POST_NOTIFICATIONS` diberikan. **Tidak** disyaratkan service aktif tanpa sentuhan operator |
| CWG-13 | Kegagalan kamera dilaporkan sebagai status, bukan exception yang menelan proses | `/status` menunjukkan `cameraState` dan alasan kegagalan |
| CWG-14 | Aplikasi tidak meminta izin mikrofon | `RECORD_AUDIO` tidak ada di manifest |
| CWG-15 | Server hanya menerima koneksi sesuai keputusan akses jaringan | Permintaan dari sumber yang tidak diizinkan ditolak |
| CWG-16 | Kegagalan streaming tidak boleh mengubah status frame menjadi sehat | Status degraded disertai alasannya |

### 4.3 Nonfungsional

| ID | Kebutuhan |
|---|---|
| CWG-N01 | Beban CPU dan baterai cukup untuk operasi penuh; tidak ada dua kali enkode JPEG per frame |
| CWG-N02 | Tidak ada inferensi, filter wajah, atau pemrosesan AI pada jalur sensor |
| CWG-N03 | Tidak ada rahasia di repo; kredensial berada pada berkas di luar repo |
| CWG-N04 | Waktu pada header frame adalah waktu pengambilan perangkat dalam epoch ms |
| CWG-N05 | Tidak ada angka FPS, latensi, atau bandwidth yang dikarang; nilai berasal dari pengukuran |
| CWG-N06 | Build, unit test, dan lint wajib lulus sebelum perubahan dianggap selesai |

## 5. Kontrak stream

| Aspek | Nilai |
|---|---|
| Transport | HTTP pada LAN |
| Jalur stream | `/video` (multipart/x-mixed-replace) |
| Jalur status | `/status` (JSON) |
| Batas multipart | `frame` |
| Header per part | `Content-Type`, `Content-Length`, `X-Frame-Seq`, `X-Frame-Captured-At-Ms`, `X-Frame-Width`, `X-Frame-Height` |
| Header permintaan | `X-Api-Key` wajib pada `/video` dan `/status` |
| Port | 8085 |
| Resolusi default | 1280x720 |
| FPS default | 20 |
| Quality JPEG default | 80 |
| Klien aktif maksimum | 2 |

Header `X-Frame-Seq` dan `X-Frame-Captured-At-Ms` adalah bentuk kontrak untuk
**B-24** (penandaan waktu frame) yang belum ditutup sebelum app ini dibangun.

Nilai default di atas ditetapkan D-29 (7 November 2026) sebagai **default
konfigurasi**, bukan ambang kelulusan mutu. Ambang FPS, latensi, dan bandwidth
tetap menunggu B-06 dan pengukuran pada perangkat sasaran.

Bentuk otorisasi ditetapkan D-27 (7 November 2026): token statis pada header
`X-Api-Key`, dibaca dari berkas di luar repo, dibandingkan dengan waktu tetap,
dengan pembatasan alamat sebagai lapis kedua opsional. Penolakan membalas `401`
dan dicatat pada log aplikasi. Kelemahan yang diketahui: HTTP cleartext membuat
token dapat dibaca bila jaringan disadap; TLS di luar lingkup v1.

## 6. Skenario penerimaan

| Skenario | Hasil yang diperiksa |
|---|---|
| Aplikasi dibuka, kamera menyala | `/status` melaporkan `cameraState` valid dan frame mengalir |
| Layar mati | Stream tetap hidup karena foreground service |
| Perangkat dinyalakan ulang | Service aktif otomatis |
| Klien kedua menyambung | Kedua klien menerima frame |
| Klien memutus koneksi | Server tetap melayani klien lain |
| Klien lambat | Klien diputus; server tidak macet |
| Izin mikrofon | Tidak ada permintaan izin mikrofon |
| Akses dari sumber tidak diizinkan | Ditolak dengan alasan |
| Dua frame berturut-turut | `X-Frame-Seq` berbeda; tidak ada frame duplikat |

## 7. Traceability

| Kebutuhan | Sumber |
|---|---|
| CWG-01 sampai CWG-05 | PRD backend v2 BO-01 sampai BO-03, bagian 5.1 |
| CWG-02, CWG-04 | PRD backend v2 BO-02, BO-N04, BO-N05 |
| CWG-06 sampai CWG-08 | PRD induk bagian 4 (kamera dipasang tetap), bagian 5 (profil) |
| CWG-09, CWG-10 | PRD backend v2 BO-01, BO-03 |
| CWG-11 sampai CWG-13 | PRD induk bagian 2, bagian 13 (gangguan ditampilkan jelas) |
| CWG-14 | PRD backend v2 bagian 3.2 (audio tidak termasuk) |
| CWG-15 | PRD backend v2 BO-N06, BO-32 |
| CWG-N05 | PRD induk bagian 13 butir terakhir; `AGENTS.md` bagian 9 |
| CWG-N06 | `AGENTS.md` bagian 8 |

## 8. Keputusan terbuka

| ID | Masalah | Menahan | Status |
|---|---|---|---|
| B-21 | Kontrak stream: URL, path, resolusi, FPS, autentikasi, ketahanan | CWG-01, CWG-08, CWG-15 | selesai (D-27, D-29, 7 Nov 2026) |
| B-24 | Penandaan waktu frame dan korespondensi encoder dengan observasi | CWG-02 | terbuka; bentuk header ditetapkan di dokumen ini |
| B-28 | Model otorisasi API | CWG-15 | selesai untuk sisi kamera (D-27, 7 Nov 2026) |
| B-29 | Bentuk lisensi dan atribusi fork | Packaging repo | selesai (D-28, 7 Nov 2026: MIT) |
| B-30 | Status jalur H.264 `StreamEncoder` dan `TcpStreamServer` | kebersihan jalur sensor | selesai (D-30, 7 Nov 2026: dihapus) |
| B-03 | Geometri mesin: posisi dan tinggi kamera | CWG-06, CWG-07 | terbuka |
| B-06 | Ambang FPS dan latensi yang disepakati | CWG-N05 | terbuka |
| — | Alias ID `CWG-` | traceability | disahkan (D-31, 7 Nov 2026) |

## 9. Di luar lingkup

Inferensi, klasifikasi, pemilihan target, pemetaan bak, perintah robot,
statistik, dashboard, audio, filter visual, virtual camera driver, dan
integrasi OBS.
