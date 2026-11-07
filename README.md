# CamWasteGreen

Android app yang menjadikan ponsel sebagai **kamera sensor** untuk sistem
pemilah sampah. Aplikasi menangkap frame dengan CameraX dan menyajikannya di
jaringan lokal sebagai stream MJPEG multipart, beserta status kesehatan kamera
dan stream.

## Batas peran

Aplikasi ini **hanya** kamera sensor. Aplikasi tidak menjalankan inferensi,
tidak menjalankan model ONNX atau ML Kit, tidak memilih target, tidak memetakan
bak, dan tidak mengirim perintah ke robot. Deteksi dan orkestrasi berada di
backend; kendali aktuator berada di ESP32-S3.

Jalur audio, mikrofon, filter visual, deteksi wajah, segmentasi selfie, dan
stream H.264 sudah dihapus dari kode, bukan hanya dinonaktifkan. Yang tersisa
pada jalur sensor: kamera, satu enkode JPEG, dan server MJPEG.

Kemampuan yang **belum** terbukti: seluruh butir perangkat pada
`.work/evidence/manual-verify-kamera.md` masih `BELUM DIUJI`. Autostart kamera
setelah boot terhalang `B-31` (Android 14+ melarang foreground service tipe
`camera` dimulai dari latar belakang).

## Keadaan pekerjaan

Status per work package tidak dicatat di berkas ini karena akan cepat basi.
Sumber status tunggal:

- `.work/Plan_PenambalanTemuanKamera_07112026-170214.md` — daftar WP, status
  `todo`/`wip`/`qc`/`rework`/`done`, dan Log tiap WP.
- `.work/evidence/baseline-camwastegreen.md` — hasil terakhir perintah gerbang
  beserta commit yang diuji.
- `AGENTS.md` — struktur berkas terkini dan daftar defek yang sudah atau belum
  tertutup.

Jangan menyimpulkan kemampuan aplikasi dari `README.md` ini saja.

## Asal kode dan atribusi

Repo ini adalah fork dari <https://github.com/ndeso17/ipcam>. Kode dasar
diambil dan diubah untuk peran kamera sensor. Sebagian fitur demo upstream
(audio, ML Kit, filter GPU, virtual camera driver, jembatan OBS) ditujukan
keluar dari jalur operasi; status pekerjaan itu ada pada PLAN, bukan di berkas
ini. Atribusi dan hak cipta kode asal tetap milik pemilik repo tersebut;
lisensi repo ini dijelaskan pada [LICENSE](LICENSE).

## Status pengembangan

Perbaikan temuan audit kamera 7 November 2026 dikerjakan sebagai work package
menurut PLAN. `README.md` tidak mengulang daftar WP.

Belum ada pengukuran FPS, latensi, atau bandwidth pada perangkat sasaran.
Angka yang muncul di dokumen (1280x720, 20 FPS, quality 80, maks 2 klien, port
8085) adalah default konfigurasi menurut D-29, bukan ambang mutu. Ambang mutu
menunggu `B-06`.

Akses `/video` dan `/status` memerlukan header `X-Api-Key`. Cara membuat dan
mengisi token ada di [docs/token.md](docs/token.md).

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
| `.work/Plan_PenambalanTemuanKamera_07112026-170214.md` | Daftar work package, status, dan log pengerjaan |
| `AGENTS.md` | Pengetahuan proyek untuk agen dan pengembang |

## Lisensi

MIT. Lihat [LICENSE](LICENSE). Pemegang hak cipta: `2026 ndeso17`.
