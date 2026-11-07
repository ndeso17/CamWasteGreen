# Architecture — CamWasteGreen

**Repo:** `CamWasteGreen/`
**Status:** Draf kerja
**Dokumen terkait:** `PRD.md`, `Structure.md`, `Rules.md`,
`../.work/PRD-backend_orkestrasi_deteksi_dan_kendali_sortir-07112026-150755.md`

## 1. Peran aplikasi

Aplikasi adalah kamera sensor. Aplikasi menghasilkan gambar dan status; aplikasi
tidak mengambil keputusan penargetan. Seluruh keputusan pemilihan target dan
penyusunan perintah berada di backend.

```
        ┌──────────────────────────────┐
HP      │  CamWasteGreen               │
        │  CameraService (foreground)  │
        │    CameraX ImageAnalysis     │
        │    FrameSource (seq + time)  │
        │  MjpegHttpServer             │
        └──────┬───────────────────────┘
               │ MJPEG/HTTP (LAN)
               │ header: seq, capturedAt, w, h
        ┌──────▼───────────────────────┐
PC      │  Backend Orchestrator        │
        │  VisionService (Python)      │
        │  MqttGateway ke ESP32-S3     │
        └──────────────────────────────┘
```

## 2. Komponen

| Komponen | Tanggung jawab | Tidak boleh |
|---|---|---|
| `CameraService` | Foreground service; mengikat CameraX ke lifecycle service; mengelola start dan stop | Menjalankan inferensi |
| `CameraController` | Membuka kamera, mengatur resolusi, FPS, orientasi, dan kunci eksposur | Menentukan target |
| `FrameSource` | Menyimpan frame terakhir beserta nomor urut, waktu pengambilan, dan dimensi | Menyimpan lebih dari satu frame |
| `FrameEncoder` | Mengubah frame YUV menjadi JPEG satu kali | Melakukan enkode ganda atau filter wajah |
| `MjpegHttpServer` | Menyajikan `/video` dan `/status`; membatasi klien; memberi batas waktu tulis | Menerima perintah robot atau audio |
| `StatusRegistry` | Menyimpan status terukur: FPS, umur frame, jumlah klien, state kamera | Melaporkan nilai konfigurasi sebagai hasil pengukuran |

## 3. Kepemilikan state

| Data | Pemilik tunggal | Alasan |
|---|---|---|
| Frame terakhir | `FrameSource` | Satu penulis (kamera) dan satu pembaca (server) |
| Nomor urut frame | `FrameSource` | Harus monoton dan tidak dibuat ulang per koneksi |
| Waktu pengambilan | `FrameSource` | Sumber tunggal untuk header waktu |
| FPS terukur | `StatusRegistry` | Dihitung dari jumlah frame dalam jendela waktu nyata |
| State kamera | `StatusRegistry` | Ditulis `CameraController`, dibaca server |
| Konfigurasi kamera | `CameraController` | Resolusi, FPS, orientasi, eksposur |

Tidak ada dua komponen menulis state yang sama. `MjpegHttpServer` tidak
menyimpan frame; server hanya membaca `FrameSource`.

## 4. Alur frame

```
CameraX ImageAnalysis (STRATEGY_KEEP_ONLY_LATEST)
  ke rotasi sesuai orientasi tetap
  ke eksposur terkunci
  ke FrameSource.publish(frame, capturedAtMs)  -> seq bertambah
  ke FrameEncoder (YUV ke JPEG, satu kali)
  ke FrameSource.latest (jpeg, seq, capturedAtMs)
  ke MjpegHttpServer menulis part multipart dengan header
```

Aturan:

1. `STRATEGY_KEEP_ONLY_LATEST` wajib. Frame yang menumpuk membuat usia target
   menyesatkan.
2. Nomor urut dan waktu diambil saat frame ditangkap, bukan saat frame dikirim.
   Penundaan antrean tidak boleh mengubah usia pengamatan.
3. Server tidak boleh mengirim frame yang sama dua kali. Part hanya ditulis bila
   `seq` lebih besar daripada `seq` terakhir yang dikirim ke klien tersebut.
4. Enkoder dijalankan satu kali per frame. Enkode ganda (NV21 ke JPEG, decode,
   lalu JPEG lagi) adalah sisa jalur filter dan harus dihapus.

## 5. Operation service

| Aspek | Nilai |
|---|---|
| Tipe | Foreground service, tipe `camera` |
| Notifikasi | Nama perangkat dan jumlah klien aktif |
| Kamera | Di-bind ke `CameraService`, bukan ke Activity |
| Start | Otomatis saat boot dan saat operator membuka aplikasi |
| Stop | Hanya lewat perintah eksplisit; tidak berhenti saat Activity selesai |
| Izin | `CAMERA`, `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` |

Tidak ada izin `RECORD_AUDIO`. Mikrofon tidak ada di jalur sensor.

## 6. Batas keamanan

| Aspek | Nilai |
|---|---|
| Jaringan | Server hanya mendengarkan pada antarmuka LAN; tidak ada tunneling ke internet |
| Akses | Token statis pada header `X-Api-Key` untuk `/video` dan `/status` (D-27). Pembatasan alamat IP sebagai lapis kedua opsional. Penolakan dibalas `401` dan dicatat |
| Rahasia | Token, kunci, dan konfigurasi berada pada berkas di luar repo (`local.properties`), diteruskan ke `BuildConfig` saat build |
| Perintah robot | Tidak ada endpoint yang meneruskan perintah ke ESP32-S3 |

Kelemahan yang diketahui: HTTP cleartext pada jaringan ruang kontrol membuat
token dapat dibaca bila jaringan disadap. Mitigasi sementara adalah pembatasan
jaringan dan rotasi token; TLS di luar lingkup v1.

Konfigurasi default (D-29): 1280x720, 20 FPS, quality JPEG 80, klien aktif
maksimum 2, port 8085. Angka itu default konfigurasi, bukan ambang mutu.

## 7. Kegagalan dan pemulihan

| Kegagalan | Perilaku |
|---|---|
| Kamera gagal dibuka | `cameraState` bernilai `failed` beserta alasan; service tetap hidup agar dapat mencoba lagi; tidak ada status palsu |
| Stream terputus | Klien menutup koneksi; `lastFrameAtMs` tidak diperbarui sehingga umur frame menunjukkan keadaan sebenarnya |
| Klien lambat | Batas waktu tulis; klien diputus; server tetap melayani klien lain |
| Service dibunuh sistem | Service dimulai ulang sesuai aturan sistem; `restartedAtMs` dicatat |
| Memori habis | Frame tidak dialokasikan tanpa batas; buffer dipakai ulang |

Tidak ada angka FPS, latensi, atau bandwidth yang dibuat-buat. Bila nilai tidak
diketahui, field diisi `null` atau tidak dikirim, bukan `0`.

## 8. Batas dengan repo lain

| Repo | Batas |
|---|---|
| `Backend/` | Satu-satunya konsumen stream dan status |
| `Controller/` | Tidak ada komunikasi langsung |
| `Camera/` | Tidak dipakai lagi sebagai jalur operasi (D-24) |

## 9. Yang belum diputuskan

Penandaan waktu dan korespondensi encoder dengan observasi (B-24), geometri
pemasangan kamera (B-03), serta ambang FPS dan latensi (B-06).

Bentuk kontrak stream dan model otorisasi sudah ditetapkan pada D-27 dan
D-29. Jalur H.264 ditetapkan dihapus pada D-30, sehingga MJPEG `/video` adalah
satu-satunya jalur keluar frame. Namespace alias `CWG-` disahkan pada D-31.
