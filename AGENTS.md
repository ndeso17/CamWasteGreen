# PROJECT KNOWLEDGE BASE — CamWasteGreen

Updated: 2026-11-08
Package: `dev.ndeso17.camwastegreen`
Branch: `main`

## OVERVIEW

Android app that turns a phone into a **camera sensor** for the waste-sorting
system. CameraX captures frames in a foreground service, the app serves them
over LAN as MJPEG with per-frame metadata, and reports camera/stream health.
The app does **not** run inference, does not pick targets, and does not talk
to robots.

Upstream basis: fork of `https://github.com/ndeso17/ipcam`. Code was reused,
not rewritten. The upstream demo features (audio, ML Kit, GPU filters, virtual
camera driver, OBS bridge, H.264 TCP stream) are **not** part of the operating
path and their code is gone from this repo.

## SOURCE OF TRUTH

| Dokumen | Isi |
|---|---|
| `.work/PRD.md` | Kebutuhan `CWG-01..16`, `CWG-N01..N06`, kontrak stream |
| `.work/Architecture.md` | Komponen, kepemilikan state, alur frame |
| `.work/Structure.md` | Struktur berkas dan tanggung jawab |
| `.work/Rules.md` | Batas keras implementasi |
| `.work/evidence/` | Baseline gerbang dan checklist MANUAL-VERIFY |
| `../AGENTS.md` (root) | Kontrak proses pipeline Planner/Executor/QC |
| `../.work/FINDINGS.md` (root) | Registri blocker `B-01..B-31` |

Blocker yang sudah selesai: `B-21` kontrak stream ditutup D-27 dan D-29
(`/video` + `/status`, 1280x720, 20 FPS, q80, maks 2 klien); `B-28` model
otorisasi sisi kamera ditutup D-27 (token statis `X-Api-Key`); `B-29` lisensi
ditutup D-28 (MIT, atribusi di `README.md`); `B-30` jalur H.264 ditutup D-30
(kedua berkasnya dihapus). Yang masih terbuka: `B-03` geometri pemasangan,
`B-06` ambang FPS/latensi, `B-24` korespondensi encoder–observasi, dan `B-31`
autostart kamera pada Android 14+.

## STACK

- **Language**: Kotlin
- **UI**: View-based (AppCompat + Material)
- **Camera**: CameraX (camera2 interop untuk kunci AE/AWB/AF)
- **Server**: NanoHTTPD, MJPEG multipart over HTTP
- **Removed from sensor path**: ML Kit, OpenGL filters, `AudioRecord`, H.264
- **minSdk 24**, compileSdk 36, targetSdk 34
- **Gradle** 8.13, **AGP** 8.13.0, **Kotlin** 2.0.21

## CURRENT STRUCTURE

```
CamWasteGreen/
├── app/src/main/java/dev/ndeso17/camwastegreen/
│   ├── MainActivity.kt              # UI operator; pilihan interface, start/stop
│   ├── CameraService.kt             # foreground service; kamera + analyzer
│   ├── MJPEGServerInstance.kt       # singleton server, kandidat alamat, status
│   ├── BootReceiver.kt              # menerima BOOT_COMPLETED (lihat B-31)
│   ├── camera/CameraTuning.kt       # kunci AE/AWB/AF setelah warmup
│   └── stream/
│       ├── MjpegHttpServer.kt       # NanoHTTPD: /, /video, /status
│       ├── ApiAuth.kt               # token X-Api-Key + batas alamat
│       ├── CameraConfig.kt          # default resolusi/FPS/quality/port
│       ├── FrameEncoder.kt          # YUV ke JPEG satu kali + rotasi
│       ├── FramePacket.kt           # jpeg + seq + capturedAtMs + dimensi
│       ├── FrameSource.kt           # frame terakhir, penomoran urut
│       ├── MultipartWriter.kt       # header part MJPEG
│       ├── StatusRegistry.kt        # measuredFps, clients, cameraState, tuningState
│       ├── StreamSession.kt         # dedupe seq, batas klien, batas tulis
│       ├── StreamInterfacePrefs.kt  # pilihan interface tersimpan
│       ├── NetworkChoice.kt         # pilihan interface + alamat bind
│       └── Nv21Transform.kt         # rotasi NV21 murni
├── .work/                           # PRD, Architecture, Structure, Rules, Plan, evidence
├── docs/                            # token.md; superpowers/ catatan upstream
└── tools/                           # upstream virtual camera driver (unused)
```

## WHAT IS ALREADY CLOSED

Audit 7 November 2026 menemukan sepuluh defek. Status penutupannya:

| # | Defek | Status |
|---|---|---|
| 1 | Kamera terikat lifecycle Activity, tanpa foreground service | Ditutup: `CameraService` sebagai foreground service `camera` |
| 2 | Endpoint mikrofon tanpa autentikasi | Ditutup: endpoint dan seluruh jalur audio dihapus |
| 3 | Server tanpa autentikasi, bind semua antarmuka | Ditutup: `X-Api-Key` wajib pada `/video` dan `/status`, alamat bind mengikuti pilihan interface |
| 4 | Frame tanpa nomor urut dan waktu tangkap | Ditutup pada sisi app: `FramePacket` membawa `seq` dan `capturedAtMs`. Korespondensi dengan observasi backend tetap `B-24` |
| 5 | Frame yang sama dikirim berulang | Ditutup: dedupe berdasarkan `seq` di `StreamSession` |
| 6 | `/status` melaporkan FPS konfigurasi | Ditutup: `measuredFps` dihitung dari jendela frame nyata, `null` bila belum ada frame |
| 7 | Dua enkode JPEG per frame | Ditutup: satu `FrameEncoder.encode` per frame |
| 8 | Tanpa kunci AE/AWB/AF | Ditutup di kode: `CameraTuning` memasang kunci setelah 30 frame warmup; hasil pada perangkat belum diuji |
| 9 | Orientasi `fullSensor` | Ditutup: orientasi activity dikunci; rotasi frame memakai `mountRotationDegrees` |
| 10 | Tulisan blocking tanpa batas klien | Ditutup: `StreamSession` membatasi klien dan memberi batas waktu tulis |

"Ditutup di kode" berarti implementasi ada dan unit test lulus. Perilaku pada
perangkat nyata tetap daftar periksa di `.work/evidence/manual-verify-kamera.md`
dan seluruhnya masih `BELUM DIUJI`.

## WHERE TO LOOK

| Task | Location |
|---|---|
| Camera setup, analyzer, warmup | `CameraService.kt` |
| Kunci eksposur/white balance/fokus | `camera/CameraTuning.kt` |
| MJPEG streaming dan status JSON | `stream/MjpegHttpServer.kt` |
| Token dan batas alamat | `stream/ApiAuth.kt`, `docs/token.md` |
| Frame storage dan penomoran urut | `stream/FrameSource.kt`, `stream/FramePacket.kt` |
| Enkode JPEG dan rotasi | `stream/FrameEncoder.kt`, `stream/Nv21Transform.kt` |
| Pilihan interface jaringan | `MainActivity.kt`, `stream/NetworkChoice.kt`, `stream/StreamInterfacePrefs.kt` |
| Manifest permissions | `app/src/main/AndroidManifest.xml` |

## COMMANDS

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew installDebug
```

Gerbang wajib per work package kode: ketiga perintah pertama, exit code dibaca
langsung tanpa pipe.

Baseline `380fd35` (2026-11-08): build lulus, 81 unit test lulus, lint 0 error
dan 61 warning. Rincian ada di `.work/evidence/baseline-camwastegreen.md`.
Baseline lama (setelah rename paket) mencatat 1 error dan 111 warning; error
itu hilang bersama jalur audio.

## OPEN DECISIONS

`B-03` geometri pemasangan kamera, `B-06` ambang FPS/latensi/usia frame,
`B-24` korespondensi penandaan waktu dengan observasi, `B-31` autostart kamera
setelah boot pada Android 14+ (foreground service tipe `camera` tidak boleh
dimulai dari latar belakang). Namespace alias `CWG-` disahkan pada D-31.
