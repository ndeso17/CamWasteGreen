# QC Report — WP-02

**WP:** WP-02 — Lisensi MIT dan atribusi fork
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `bb8ca06`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: FAIL

Kriteria terima WP-02 terpenuhi 4/4 secara harfiah, tetapi README memuat tiga
pernyataan bentuk sekarang (present tense) yang **bertentangan dengan kode pada
commit yang sama**. Kontrak `AGENTS.md` bagian 7 melarang menyamarkan kemampuan
yang belum tersedia sebagai implementasi lengkap, sehingga klaim itu tidak boleh
diloloskan meski kriterianya berbunyi "menyatakan aplikasi tidak menjalankan
inferensi".

Rework konkret ada di bagian Rework. Kode tidak perlu diubah; hanya README.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Build | `./gradlew assembleDebug --console=plain` | `BUILD SUCCESSFUL in 5s` | sesi QC |
| Cakupan commit | `git show --name-only --format="" bb8ca06` | `LICENSE`, `README.md`, `.work/Plan_...md` | sesi QC |
| Pohon kerja | `git status --short` | bersih setelah commit | sesi QC |
| Berkas lokal | `git ls-files \| grep -c local.properties` | `0` | sesi QC |
| Pola rahasia | `git diff 89b163d bb8ca06 \| grep -E "ghp_\|github_pat_\|AKIA[0-9A-Z]{16}\|-----BEGIN"` | tanpa hasil | sesi QC |

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC |
|---|---|---|
| T-004 | `LICENSE` berisi teks MIT, pemegang hak `2026 ndeso17` | `grep -c "MIT License" LICENSE` = 1; `grep -c 'THE SOFTWARE IS PROVIDED "AS IS"' LICENSE` = 1; baris 3 = `Copyright (c) 2026 ndeso17`; seluruh klausa izin/penafian MIT lengkap |
| T-005 | `README.md` memuat tujuan, batas peran, asal kode, atribusi, perintah build, rujukan `.work/` | `grep -c "ndeso17/ipcam" README.md` = 1; bagian "Bangun dan uji" memuat empat perintah Gradle; tabel "Dokumen" merujuk `.work/PRD.md`, `.work/Architecture.md`, `.work/Structure.md`, `.work/Rules.md`, `AGENTS.md` |
| Kriteria | `LICENSE` ada dan memuat frasa wajib | ✅ |
| Kriteria | `README.md` menyebut `ndeso17/ipcam` | ✅ |
| Kriteria | `git status --short` tanpa `local.properties`/rahasia | ✅ |
| Kriteria | `assembleDebug` tetap lulus | ✅ `BUILD SUCCESSFUL in 5s` |
| Kriteria | Kutipan dua baris pertama tiap berkas pada Log | ✅ Log Executor memuat `grep -c` untuk keduanya |

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-06 | **blocker (dokumentasi)** | `README.md` menyatakan "Aplikasi **tidak** menjalankan inferensi, tidak menjalankan model ONNX atau ML Kit". Pada commit `bb8ca06`, ML Kit face detection berjalan pada **setiap frame**: `FilterPipeline.init` menetapkan `AiProcessor.isFaceDetectionEnabled = true`, dan `FilterPipeline.process` memanggil `AiProcessor.process(bitmap, rotationDegrees)` tanpa syarat; `AiProcessor` membuat `FaceDetector()` pada inisialisasi. Klaim README salah untuk keadaan sekarang. | `app/src/main/java/dev/ndeso17/camwastegreen/pipeline/FilterPipeline.kt:44,66`; `ai/AiProcessor.kt:11,16,18` |
| QC-07 | **blocker (dokumentasi)** | `README.md` menyatakan "Jalur sensor tidak memuat filter visual, deteksi wajah, maupun segmentasi selfie". Jalur sensor justru memanggil `FilterPipeline.process` untuk setiap frame, dan pipeline itu melakukan dua kali enkode JPEG (`compressToJpeg` + `Bitmap.compress`) serta menerapkan filter. | `pipeline/FilterPipeline.kt:62,91`; `MainActivity.kt` pemanggilan `FilterPipeline.process(nv21, ...)` di analyzer |
| QC-08 | **blocker (dokumentasi)** | `README.md` menyatakan "Audio dan mikrofon tidak dipakai". Pada commit yang sama `MicManager` aktif, `/mic/toggle` ada di server, tombol mikrofon ada di UI, dan `RECORD_AUDIO` masih diminta sebagai izin wajib. | `MjpegHttpServer.kt:74`; `MainActivity.kt` `REQUIRED_PERMISSIONS`; `AndroidManifest.xml` `RECORD_AUDIO` |
| QC-09 | note | Bagian "Status pengembangan" sudah benar menyatakan pekerjaan audit belum selesai dan angka di dokumen adalah default konfigurasi, bukan ambang mutu. Ini mitigasi yang baik, tetapi tidak menetralkan tiga klaim bentuk sekarang di atas karena ketiganya ditulis sebagai keadaan, bukan target. | `README.md:12-31` |
| QC-10 | info | `LICENSE` memakai teks MIT kanonik tanpa perubahan selain baris hak cipta. Tidak ada klausa tambahan yang mempersempit hak pengguna. | `LICENSE` |

---

## Keamanan dan hygiene

| Aspek | Perintah | Hasil |
|---|---|---|
| Rahasia pada berkas baru | `grep -nEi "(ghp_\|github_pat_\|AKIA[0-9A-Z]{16}\|-----BEGIN\|password\s*=\|secret\s*=)" LICENSE README.md` | tanpa hasil |
| Berkas lokal tidak ter-track | `git ls-files \| grep -E "local.properties\|\.jks\|\.keystore"` | tanpa hasil |
| Identitas commit | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>` |
| Trailer kontrak | `git log -1 --format="%B" \| tail -3` | `Signed-off-by: ndeso17 <ndeso17@users.noreply.github.com>` |
| Karakter rusak | pemindaian non-ASCII pada `LICENSE` dan `README.md` | 0 baris bermasalah |
| Klaim mutu tanpa ukur | pencarian `FPS`, `latency`, `bandwidth`, angka performa pada `README.md` | hanya pernyataan bahwa angka adalah default konfigurasi, bukan ambang; tidak ada klaim lulus |

---

## Rework

- [ ] **REWORK-R1** — Perbaiki `README.md` agar tidak menyatakan keadaan yang
  belum benar. Ubah klaim pada bagian "Batas peran" menjadi pernyataan target
  yang eksplisit, dan tambahkan satu paragraf "Keadaan saat ini" yang menyebut:
  ML Kit face detection masih berjalan pada setiap frame melalui
  `FilterPipeline`/`AiProcessor`; jalur sensor masih memakai filter dan dua kali
  enkode JPEG; `MicManager`, `/mic/toggle`, dan izin `RECORD_AUDIO` masih ada.
  Sebutkan bahwa ketiganya dihapus pada WP-03 sampai WP-16.
  Cara uji: `grep -n "Keadaan saat ini" README.md` menemukan bagian itu, dan
  tidak ada lagi kalimat bentuk sekarang yang menyatakan ML Kit/audio/filter
  sudah tidak dipakai tanpa penanda target. Bukti tambahan: kutipan bagian baru
  pada Log WP.
- [ ] **REWORK-R2** — Setelah REWORK-R1, pastikan kriteria terima WP-02 tetap
  terpenuhi: `grep -c "ndeso17/ipcam" README.md` = 1, `grep -c "MIT License"
  LICENSE` = 1, `./gradlew assembleDebug` lulus. Catat keluaran pada Log WP.

Rework hanya menyentuh `README.md`; `LICENSE` tidak berubah dan tidak perlu
dibuat ulang.

---

## Catatan push

Tidak ada push. Verdict FAIL, sehingga commit artefak QC dilakukan tetapi
`main` tidak di-push sampai WP-02 `done`.

---

# QC putaran 2 — rework WP-02

**QC date:** 7 November 2026
**Commit yang diperiksa:** working tree setelah `b4db94a` (rework belum di-commit)
**Verdict putaran 2: PASS_WITH_NOTES**

## Rework dibuktikan

| Rework | Perintah QC | Hasil |
|---|---|---|
| REWORK-R1 | `grep -n "^## Keadaan saat ini" README.md` | baris 17 — bagian ada sebagai heading sendiri |
| REWORK-R1 | `sed -n '10,15p' README.md` | klaim kini berbentuk target: "Peran yang dituju: aplikasi tidak menjalankan inferensi ... Peran yang dituju juga mengecualikan audio, mikrofon, filter visual, deteksi wajah, dan segmentasi selfie" |
| REWORK-R1 | `grep -nE "^(Aplikasi \*\*tidak\*\*\|Audio dan mikrofon tidak dipakai\|Jalur sensor tidak memuat)" README.md` | tanpa hasil — tiga klaim bentuk sekarang yang dilaporkan QC-06..QC-08 sudah hilang |
| REWORK-R1 | `grep -n "direncanakan" README.md` | baris 38 — bagian atribusi memakai "**direncanakan keluar** dari jalur operasi" dengan rujukan ke "Keadaan saat ini" |
| REWORK-R2 | `grep -c "ndeso17/ipcam" README.md` | 1 |
| REWORK-R2 | `grep -c "MIT License" LICENSE` | 1 |
| REWORK-R2 | `grep -c 'THE SOFTWARE IS PROVIDED "AS IS"' LICENSE` | 1 |
| REWORK-R2 | `sed -n '3p' LICENSE` | `Copyright (c) 2026 ndeso17` |
| REWORK-R2 | `./gradlew assembleDebug --console=plain` | `BUILD SUCCESSFUL` |

## Tabel "Keadaan saat ini" diperiksa terhadap kode, bukan diterima

Setiap baris tabel QC cocokkan dengan isi berkas pada commit ini:

| Baris README | Klaim | Bukti kode | Cocok |
|---|---|---|---|
| ML Kit face detection | masih berjalan per frame | `pipeline/FilterPipeline.kt:44` `AiProcessor.isFaceDetectionEnabled = true`; `:66` `AiProcessor.process(...)`; `ai/AiProcessor.kt:11` `FaceDetector()` | ya |
| Filter visual dan enkode ganda | `compressToJpeg` lalu `Bitmap.compress` | `pipeline/FilterPipeline.kt:62` dan `:91` | ya |
| Audio dan mikrofon | `MicManager`, `/mic/toggle`, `RECORD_AUDIO` | `MicManager.kt:13` `object MicManager`; `MjpegHttpServer.kt:74`; `AndroidManifest.xml:15`; `MainActivity.kt:81` | ya |
| Kamera terikat Activity | `bindToLifecycle` di `MainActivity` | `MainActivity.kt:443` | ya |
| Status belum terukur | `/status` melaporkan konfigurasi | `MjpegHttpServer.kt:63-72` → `put("fps", currentFps)`, bukan pengukuran | ya |
| Stream tanpa otorisasi | tanpa token, cleartext | `grep -c "X-Api-Key" MjpegHttpServer.kt` = 0; `AndroidManifest.xml:30` `usesCleartextTraffic="true"` | ya |
| Jalur H.264 ganda | kedua berkas masih ada | `ls StreamEncoder.kt TcpStreamServer.kt` → ada | ya |

Rujukan WP pada kolom "Ditutup oleh" cocok dengan isi PLAN: audio WP-03..05,
status WP-10/12, filter+AI WP-13..16, service WP-17/18, token WP-25..27,
H.264 WP-31.

## Temuan putaran 2

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-11 | note | Tabel "Keadaan saat ini" adalah potret commit ini. Bila WP-03..WP-31 selesai, tabel itu harus diperbarui; kalau tidak, README berubah menjadi klaim yang salah pada arah sebaliknya (menyatakan sisa masalah yang sudah hilang). WP-29 T-077 belum menyebut `README.md`; perlu ditambah saat re-plan. | `README.md:17-32`; PLAN WP-29 `File:` |
| QC-12 | note | Bagian "Status pengembangan" (baris 43-51) mengulang tiga butir yang juga ada di tabel "Keadaan saat ini" (kamera terikat Activity, `/status` belum terukur, stream tanpa token). Duplikasi ini tidak salah, tetapi menambah beban pemeliharaan: perubahan harus dilakukan di dua tempat. | `README.md:43-51` vs `README.md:27-29` |
| QC-10 | info | `LICENSE` masih teks MIT kanonik, tidak berubah pada rework. | `LICENSE` |

Tidak ada temuan blocker. QC-06, QC-07, dan QC-08 dinyatakan **tertutup**.

## Keamanan dan hygiene putaran 2

| Aspek | Hasil |
|---|---|
| Pola rahasia pada `README.md` | tanpa hasil |
| Berkas lokal ter-track | tidak ada |
| Karakter non-ASCII rusak pada `README.md` dan PLAN | 0 baris |
| Klaim angka FPS/latensi/bandwidth | hanya pernyataan bahwa angka di dokumen adalah default konfigurasi, bukan ambang mutu; tidak ada angka performa baru |

## Catatan push

Guard push dijalankan setelah rework: root repo benar, tree bersih setelah
commit, tidak ada berkas lokal/rahasia, author `ndeso17`, `diff --check` bersih.
Verdict PASS_WITH_NOTES → commit artefak QC lalu push `main`.
