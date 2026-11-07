# CamWasteGreen

Android app yang menjadikan ponsel sebagai **kamera sensor** untuk sistem
pemilah sampah. Aplikasi menangkap frame dengan CameraX dan menyajikannya di
jaringan lokal sebagai stream MJPEG multipart, beserta status kesehatan kamera
dan stream.

## Batas peran

Aplikasi ini **hanya** kamera sensor. Aplikasi **tidak** menjalankan inferensi,
tidak menjalankan model ONNX atau ML Kit, tidak memilih target, tidak memetakan
bak, dan tidak mengirim perintah ke robot. Deteksi dan orkestrasi berada di
backend; kendali aktuator berada di ESP32-S3.

Audio dan mikrofon tidak dipakai. Jalur sensor tidak memuat filter visual,
deteksi wajah, maupun segmentasi selfie.

## Asal kode dan atribusi

Repo ini adalah fork dari <https://github.com/ndeso17/ipcam>. Kode dasar
diambil dan diubah untuk peran kamera sensor; sebagian fitur demo upstream
(audio, ML Kit, filter GPU, virtual camera driver, jembatan OBS) tidak termasuk
jalur operasi. Atribusi dan hak cipta kode asal tetap milik pemilik repo
tersebut; lisensi repo ini dijelaskan pada [LICENSE](LICENSE).

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
