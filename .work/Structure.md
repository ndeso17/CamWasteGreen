# Structure — CamWasteGreen

**Repo:** `CamWasteGreen/`
**Status:** Draf kerja
**Dokumen terkait:** `PRD.md`, `Architecture.md`, `Rules.md`

## 1. Peta direktori

```
CamWasteGreen/
├── .work/                       # artefak kerja repo ini
│   ├── PRD.md                   # kebutuhan CWG-01..16, CWG-N01..N06
│   ├── Architecture.md          # komponen, kepemilikan state, alur frame
│   ├── Structure.md             # dokumen ini
│   └── Rules.md                 # batas keras implementasi
├── AGENTS.md                    # pengetahuan basis kode repo ini
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/dev/ndeso17/camwastegreen/
│       │   │   ├── MainActivity.kt        # UI operator, pilihan interface
│       │   │   ├── CameraService.kt       # foreground service + analyzer
│       │   │   ├── MJPEGServerInstance.kt # singleton server + status
│       │   │   ├── BootReceiver.kt        # BOOT_COMPLETED
│       │   │   ├── camera/
│       │   │   │   └── CameraTuning.kt    # kunci AE/AWB/AF
│       │   │   └── stream/
│       │   │       ├── MjpegHttpServer.kt
│       │   │       ├── ApiAuth.kt
│       │   │       ├── CameraConfig.kt
│       │   │       ├── FrameEncoder.kt
│       │   │       ├── FramePacket.kt
│       │   │       ├── FrameSource.kt
│       │   │       ├── MultipartWriter.kt
│       │   │       ├── NetworkChoice.kt
│       │   │       ├── Nv21Transform.kt
│       │   │       ├── StatusRegistry.kt
│       │   │       ├── StreamInterfacePrefs.kt
│       │   │       └── StreamSession.kt
│       │   └── res/
│       ├── test/java/dev/ndeso17/camwastegreen/
│       └── androidTest/java/dev/ndeso17/camwastegreen/
├── .work/
│   ├── PRD.md
│   ├── Architecture.md
│   ├── Structure.md
│   ├── Rules.md
│   ├── Plan_PenambalanTemuanKamera_07112026-170214.md
│   └── evidence/                # baseline gerbang, checklist MANUAL-VERIFY
├── docs/                        # token.md; superpowers/ catatan upstream
├── tools/                       # skrip virtual camera driver / OBS bridge (tidak dipakai)
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

## 2. Tanggung jawab berkas

| Berkas | Tanggung jawab |
|---|---|
| `MainActivity.kt` | UI operator: izin, start/stop, pilihan interface jaringan, tampilan alamat |
| `CameraService.kt` | Foreground service tipe `camera`; mengikat CameraX, menjalankan analyzer, memicu kunci tuning, notifikasi status |
| `MJPEGServerInstance.kt` | Singleton server, kandidat alamat dari `NetworkInterface`, penyedia token, penerusan frame ke `FrameSource` |
| `BootReceiver.kt` | Menerima `BOOT_COMPLETED` dan meminta service start; hasilnya bergantung B-31 |
| `camera/CameraTuning.kt` | Memasang `CONTROL_AE_LOCK`, `CONTROL_AWB_LOCK`, `CONTROL_AF_MODE_OFF` lewat Camera2Interop |
| `stream/MjpegHttpServer.kt` | NanoHTTPD: `/` (halaman diagnosa), `/video`, `/status`; otorisasi, dedupe, balasan galat |
| `stream/ApiAuth.kt` | Verifikasi `X-Api-Key` waktu-tetap dan daftar alamat yang diizinkan |
| `stream/CameraConfig.kt` | Default resolusi, FPS, kualitas JPEG, batas klien, port; validasi nilai |
| `stream/FrameEncoder.kt` | Satu enkode YUV ke JPEG per frame, termasuk rotasi |
| `stream/FramePacket.kt` | Data frame: `jpeg`, `seq`, `capturedAtMs`, `width`, `height` |
| `stream/FrameSource.kt` | Menyimpan satu frame terakhir dan memberi nomor urut |
| `stream/MultipartWriter.kt` | Menyusun header part MJPEG |
| `stream/StatusRegistry.kt` | Status terukur: `measuredFps`, `clients`, `cameraState`, `tuningState` |
| `stream/StreamSession.kt` | Dedupe per klien, batas jumlah klien, batas waktu tulis |
| `stream/StreamInterfacePrefs.kt` | Menyimpan dan membaca pilihan interface |
| `stream/NetworkChoice.kt` | Daftar pilihan interface dan alamat bind hasilnya |
| `stream/Nv21Transform.kt` | Rotasi NV21 murni, tanpa Android |
| `tools/*` | Virtual camera driver dan OBS bridge upstream; tidak dipakai di jalur operasi |

### Berkas yang sudah dihapus

Jalur demo upstream (audio, filter visual, ML Kit, enkode H.264 ganda, buffer
frame lama, dan equalizer) sudah dihapus dari repo. Rincian per berkas ada di
Log WP-03, WP-05, WP-08, WP-15, dan WP-31 pada PLAN; tidak didaftar ulang di
sini agar dokumen ini tidak memuat nama berkas yang sudah tidak ada.

## 3. Komponen target (hasil akhir)

| Komponen | Lokasi | Rujukan |
|---|---|---|
| `CameraService` | `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt` | Architecture bagian 5 |
| `BootReceiver` | `app/src/main/java/dev/ndeso17/camwastegreen/BootReceiver.kt` | CWG-12 |
| `CameraTuning` | `app/src/main/java/dev/ndeso17/camwastegreen/camera/CameraTuning.kt` | CWG-07 |
| `FrameSource` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameSource.kt` | Architecture bagian 3 |
| `FrameEncoder` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt` | Architecture bagian 4 |
| `MjpegHttpServer` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt` | Architecture bagian 2 |
| `StatusRegistry` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/StatusRegistry.kt` | Architecture bagian 3 |
| `ApiAuth` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/ApiAuth.kt` | CWG-15 |
| `StreamSession` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamSession.kt` | CWG-09, CWG-10 |
| `NetworkChoice` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/NetworkChoice.kt` | CWG-10 |
| `StreamInterfacePrefs` | `app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamInterfacePrefs.kt` | CWG-10 |

Catatan: tidak ada kelas `CameraController` terpisah. Pengaturan kamera berada
di `CameraService`, dan kunci eksposur/warna/fokus berada di `CameraTuning`.
Dokumen ini sebelumnya menyebut `CameraController` sebagai rencana; rencana itu
tidak dipakai agar tidak ada lapisan tambahan tanpa pemilik state yang jelas.

Pemindahan dan pemisahan paket adalah bagian dari work package refactor.
Struktur akhir tidak boleh mencampur tanggung jawab.

## 4. Konvensi

1. Satu kelas satu tanggung jawab. Server HTTP tidak boleh menyentuh kamera
   secara langsung.
2. State yang diakses lintas komponen hanya lewat pemilik tunggalnya
   (Architecture bagian 3). Hindari `object` global yang ditulis dari banyak
   tempat. `MJPEGServerInstance` adalah singleton yang tersisa; ia hanya
   meneruskan ke `FrameSource` dan `StatusRegistry`, tidak menyimpan state
   sendiri.
3. Nilai yang diukur dan nilai yang dikonfigurasi harus punya nama berbeda dan
   jenis berbeda. Contoh: `measuredFps`, bukan `fps`.
4. Konstanta numerik yang memengaruhi kualitas sinyal (resolusi, FPS, quality
   JPEG, batas klien) berada di satu berkas konfigurasi yang dapat diubah tanpa
   menyentuh logika.
5. Tidak ada nomor urut frame, timestamp, atau counter yang dibuat di dalam
   lapisan HTTP.

## 5. Bukti dan artefak uji

| Jenis | Lokasi |
|---|---|
| Unit test | `app/src/test/java/dev/ndeso17/camwastegreen/` |
| Uji instrumen | `app/src/androidTest/java/dev/ndeso17/camwastegreen/` |
| Bukti manual | `.work/evidence/` (`baseline-camwastegreen.md`, `manual-verify-kamera.md`) |
| Laporan lint | `app/build/reports/lint-results-debug.txt` |

Uji yang wajib ada setelah refactor:

1. Pembentukan part multipart beserta header `X-Frame-*`.
2. Aturan dedupe berdasarkan `seq`.
3. Perhitungan FPS terukur dari penghitung frame.
4. Penolakan klien ketika batas klien terlampaui.
