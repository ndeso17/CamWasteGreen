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
│       │   │   ├── MainActivity.kt
│       │   │   ├── MjpegHttpServer.kt
│       │   │   ├── MJPEGServerInstance.kt
│       │   │   ├── FrameBuffer.kt
│       │   │   ├── MicManager.kt
│       │   │   ├── StreamEncoder.kt
│       │   │   ├── TcpStreamServer.kt
│       │   │   ├── EqualizerView.kt
│       │   │   ├── ai/                  # deteksi wajah, segmentasi selfie
│       │   │   ├── gpuimage/            # shader filter OpenGL
│       │   │   └── pipeline/            # FilterPipeline (filter + AI)
│       │   └── res/
│       ├── test/java/dev/ndeso17/camwastegreen/
│       └── androidTest/java/dev/ndeso17/camwastegreen/
├── docs/                        # spesifikasi desain H.264 bawaan upstream
├── tools/                       # skrip virtual camera driver / OBS bridge
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

## 2. Tanggung jawab berkas

| Berkas | Tanggung jawab | Nasib dalam target |
|---|---|---|
| `MainActivity.kt` | UI operator dan siklus hidup Activity | Diperkecil; kamera pindah ke service |
| `MjpegHttpServer.kt` | Server NanoHTTPD untuk `/video` dan `/status` | Dipertahankan dan diperbaiki: header, dedupe, batas klien, autentikasi |
| `MJPEGServerInstance.kt` | Pembungkus singleton server | Dipertahankan |
| `FrameBuffer.kt` | Penyimpan frame terakhir | Diperluas: seq, capturedAtMs, width, height |
| `MicManager.kt` | Perekam audio | Dihapus |
| `ai/AiProcessor.kt` | Pembungkus ML Kit | Dihapus |
| `ai/FaceDetector.kt` | Deteksi wajah ML Kit | Dihapus |
| `ai/SelfieSegmenter.kt` | Segmentasi selfie ML Kit | Dihapus |
| `gpuimage/*` | Shader OpenGL untuk filter | Dihapus dari jalur sensor |
| `pipeline/FilterPipeline.kt` | Filter, AI, dan enkode ganda | Dihapus atau dipangkas menjadi encoder tunggal |
| `StreamEncoder.kt` | Enkoder H.264 | Tidak dipakai di v1; tetap ada sampai ada keputusan |
| `TcpStreamServer.kt` | Server TCP untuk NAL H.264 | Tidak dipakai di v1 |
| `EqualizerView.kt` | UI equalizer | Tidak relevan untuk sensor |
| `tools/*` | Virtual camera driver dan OBS bridge | Tidak dipakai di jalur operasi |

Berkas bertanda dihapus adalah sisa jalur demo ipcam yang tidak relevan untuk
kamera sensor. Penghapusan harus lewat work package tersendiri agar build
tetap hijau.

## 3. Komponen target (hasil akhir)

| Komponen | Lokasi baru | Rujukan |
|---|---|---|
| `CameraService` | `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt` | Architecture bagian 5 |
| `CameraController` | `.../camera/CameraController.kt` | Architecture bagian 2 |
| `FrameSource` | `.../stream/FrameSource.kt` | Architecture bagian 3 |
| `FrameEncoder` | `.../stream/FrameEncoder.kt` | Architecture bagian 4 |
| `MjpegHttpServer` | `.../stream/MjpegHttpServer.kt` | Architecture bagian 2 |
| `StatusRegistry` | `.../stream/StatusRegistry.kt` | Architecture bagian 3 |
| `BootReceiver` | `.../BootReceiver.kt` | CWG-12 |

Pemindahan dan pemisahan paket adalah bagian dari work package refactor.
Struktur akhir tidak boleh mencampur tanggung jawab.

## 4. Konvensi

1. Satu kelas satu tanggung jawab. Server HTTP tidak boleh menyentuh kamera
   secara langsung.
2. State yang diakses lintas komponen hanya lewat pemilik tunggalnya
   (Architecture bagian 3). Hindari `object` global yang ditulis dari banyak
   tempat; `FrameBuffer` adalah pengecualian sementara yang wajib dibongkar.
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
| Bukti manual | `.work/evidence/` (belum ada) |
| Laporan lint | `app/build/reports/lint-results-debug.txt` |

Uji yang wajib ada setelah refactor:

1. Pembentukan part multipart beserta header `X-Frame-*`.
2. Aturan dedupe berdasarkan `seq`.
3. Perhitungan FPS terukur dari penghitung frame.
4. Penolakan klien ketika batas klien terlampaui.
