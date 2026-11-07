# CamWasteGreen

Android app yang menjadikan ponsel sebagai **kamera sensor** untuk sistem
pemilah sampah. Aplikasi menangkap frame dengan CameraX dan menyajikannya di
jaringan lokal sebagai stream MJPEG multipart, beserta status kesehatan kamera
dan stream.

## Batas peran

Aplikasi ini **hanya** kamera sensor. Peran yang dituju: aplikasi tidak
menjalankan inferensi, tidak menjalankan model ONNX atau ML Kit, tidak memilih
target, tidak memetakan bak, dan tidak mengirim perintah ke robot. Deteksi dan
orkestrasi berada di backend; kendali aktuator berada di ESP32-S3. Peran yang
dituju juga mengecualikan audio, mikrofon, filter visual, deteksi wajah, dan
segmentasi selfie dari jalur sensor.

## Keadaan saat ini

Peran di atas adalah target, belum seluruhnya berlaku pada kode sekarang.
Pada commit ini masih ada:

| Sisa jalur upstream | Keadaan | Ditutup oleh |
|---|---|---|
| ML Kit face detection | `FilterPipeline.init` menyalakan `AiProcessor.isFaceDetectionEnabled`, dan `FilterPipeline.process` memanggil `AiProcessor.process` pada setiap frame | WP-13, WP-15, WP-16 |
| Filter visual dan enkode ganda | Jalur sensor memanggil `FilterPipeline.process` yang melakukan `YuvImage.compressToJpeg` lalu `Bitmap.compress` | WP-13, WP-14, WP-15 |
| Audio dan mikrofon | `MicManager`, endpoint `/mic/toggle`, tombol mikrofon di UI, dan izin `RECORD_AUDIO` masih ada | WP-03, WP-04, WP-05 |
| Kamera terikat Activity | `bindToLifecycle` dipanggil dari `MainActivity`; belum ada foreground service | WP-17, WP-18 |
| Status belum terukur | `/status` melaporkan nilai konfigurasi, bukan pengukuran | WP-10, WP-12 |
| Stream tanpa otorisasi | Tidak ada token; `usesCleartextTraffic=true` | WP-25, WP-26, WP-27 |
| Jalur H.264 ganda | `StreamEncoder` dan `TcpStreamServer` masih ada | WP-31 |

Jangan menganggap tabel di atas sebagai daftar fitur. Isinya sisa pekerjaan.

## Asal kode dan atribusi

Repo ini adalah fork dari <https://github.com/ndeso17/ipcam>. Kode dasar
diambil dan diubah untuk peran kamera sensor. Sebagian fitur demo upstream
(audio, ML Kit, filter GPU, virtual camera driver, jembatan OBS) **direncanakan
keluar** dari jalur operasi; lihat bagian "Keadaan saat ini" untuk sisa yang
masih berjalan pada kode sekarang. Atribusi dan hak cipta kode asal tetap milik
pemilik repo tersebut; lisensi repo ini dijelaskan pada [LICENSE](LICENSE).

## Status pengembangan

Perbaikan temuan audit kamera 7 November 2026 sedang dikerjakan menurut
`.work/Plan_PenambalanTemuanKamera_07112026-170214.md`. Selama work package
tersebut belum selesai, sebagian perilaku masih seperti versi upstream:
kamera terikat ke Activity, belum ada foreground service, `/status` masih
melaporkan nilai konfigurasi, dan stream belum memakai token.

Belum ada pengukuran FPS, latensi, atau bandwidth pada perangkat sasaran.
Angka yang muncul di dokumen adalah default konfigurasi, bukan ambang mutu.

## Bangun dan uji

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew installDebug
```

Android SDK dibaca dari `local.properties` (`sdk.dir=...`). Berkas itu berada di
luar repo dan tidak boleh di-commit.

## Dokumen

| Berkas | Isi |
|---|---|
| `.work/PRD.md` | Kebutuhan `CWG-01..16` dan `CWG-N01..N06`, kontrak stream |
| `.work/Architecture.md` | Komponen, kepemilikan state, alur frame |
| `.work/Structure.md` | Struktur berkas dan tanggung jawab |
| `.work/Rules.md` | Batas keras implementasi |
| `AGENTS.md` | Pengetahuan proyek untuk agen dan pengembang |

## Lisensi

MIT. Lihat [LICENSE](LICENSE). Pemegang hak cipta: `2026 ndeso17`.
