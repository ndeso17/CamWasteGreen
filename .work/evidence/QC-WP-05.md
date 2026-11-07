# QC Report — WP-05

**WP:** WP-05 — Hapus `MicManager`, `EqualizerView`, izin audio, dan drawable mikrofon
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `800886e`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: PASS

Kriteria terima WP-05 terpenuhi seluruhnya, dan gerbang mutu yang sebelumnya
selalu gagal kini **hijau penuh**. Tidak ada rework dan tidak ada temuan
blocker. Dua catatan (note) tercatat di bagian Temuan.

Bukti terkuat WP ini: `lintDebug` **BUILD SUCCESSFUL dengan 0 error**, diukur
pada **clean build** (`./gradlew clean` lalu ketiga gerbang dijalankan ulang),
bukan pada build inkremental yang bisa menutupi hasil lama.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Clean build | `./gradlew clean` | selesai tanpa keluaran error | sesi QC |
| Build + test + lint | `./gradlew assembleDebug testDebugUnitTest lintDebug --console=plain` | `BUILD SUCCESSFUL in 1m 18s`, **55 task executed** (bukan up-to-date) | sesi QC |
| Lint error | `grep -c ': Error:' lint-results-debug.txt` | **0** | `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| Lint warning | `grep -c ': Warning:' lint-results-debug.txt` | 108 | idem |
| Unit test | `python3` parse `app/build/test-results/testDebugUnitTest/*.xml` | 1 test, 0 failure, 0 error | idem |
| Cakupan commit | `git show --name-only --format="" 800886e` | 4 berkas dihapus, `AndroidManifest.xml`, PLAN | sesi QC |
| Pohon kerja | `git status --short` | bersih | sesi QC |
| Diff hygiene | `git show --check --format="" 800886e` | tanpa keluaran | sesi QC |
| Identitas | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>`; trailer `Signed-off-by:` ada | sesi QC |

Catatan penting: QC menjalankan `clean` lebih dulu. Tanpa itu, hasil lint bisa
berasal dari cache build sebelumnya dan tidak membuktikan keadaan commit ini.

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC (dijalankan sendiri) |
|---|---|---|
| T-011 | `MicManager.kt` dihapus | `test -f .../MicManager.kt` → `no`; `git show --name-only 800886e` memuat berkas itu sebagai terhapus |
| T-011 | `EqualizerView.kt` dihapus | `test -f .../EqualizerView.kt` → `no` |
| T-011 | `ic_mic.xml` dihapus | `test -f app/src/main/res/drawable/ic_mic.xml` → `no` |
| T-011 | `ic_mic_off.xml` dihapus | `test -f app/src/main/res/drawable/ic_mic_off.xml` → `no` |
| T-012 | `RECORD_AUDIO` dihapus dari manifest sumber | `grep -n "uses-permission" app/src/main/AndroidManifest.xml` → 9 izin: CAMERA, INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_CAMERA, WAKE_LOCK, SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS; tanpa `RECORD_AUDIO` |
| Kriteria | `assembleDebug` dan `testDebugUnitTest` lulus | `BUILD SUCCESSFUL in 1m 18s`, 55 task executed |
| Kriteria | `lintDebug` tanpa `MissingPermission` dan tanpa error sisa | 0 error; baris `MicManager.kt:54` tidak ada lagi karena berkasnya tidak ada |
| Kriteria | `grep -rn "RECORD_AUDIO\|MicManager\|EqualizerView" app/src` kosong | tanpa hasil |
| Kriteria | Jumlah warning dicatat | 108 (sebelumnya 112) |
| Bukti | Keluaran lint dan grep pada Log WP | ada, termasuk tabel diff warning |

---

## Anti-ghost — kriteria dibuktikan berdiskriminasi

| Kriteria | Baseline `70c7410` | Sekarang `800886e` |
|---|---|---|
| Berkas `MicManager.kt` | ada | tidak ada |
| Berkas `EqualizerView.kt` | ada | tidak ada |
| Drawable `ic_mic*.xml` | 2 berkas | 0 berkas |
| `RECORD_AUDIO` di manifest sumber | 1 baris | 0 |
| `RECORD_AUDIO` di **merged manifest hasil build** | 1 | 0 |
| Error lint | 1 (`MicManager.kt:54`) | 0 |
| Warning lint | 112 | 108 |

Baris merged manifest diperiksa karena manifest sumber bisa saja masih
menghasilkan izin lewat dependensi library. Hasilnya nol, jadi izin mikrofon
benar-benar tidak ada pada APK yang dibangun, bukan hanya pada berkas sumber.

---

## Pemeriksaan tambahan yang tidak diminta kriteria

| Aspek | Hasil |
|---|---|
| Merged manifest hasil build | `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` memuat 9 izin, tanpa `RECORD_AUDIO` (`grep -c` = 0) |
| Sisa jalur audio di kode | `grep -rn "AudioRecord\|MediaRecorder\|audio/aac\|AudioFormat" app/src` → tanpa hasil |
| Rujukan lintas berkas | `grep -rn "MicManager\|EqualizerView\|ic_mic\|RECORD_AUDIO" --include=*.kt --include=*.xml --include=*.kts` di luar `app/build` dan `.work` → tanpa hasil |
| `tools/` (skrip driver upstream) | masih memuat kata "audio"/"mic" pada skrip virtual-camera/OBS. Itu skrip upstream di luar `app/src`, tidak dibangun ke APK, dan tidak termasuk lingkup WP ini |
| APK terbangun | `app/build/outputs/apk/debug/IPCam_07November2026_1811.apk`, 120.066.362 byte (debug, membawa ML Kit dan CameraX; penurunan ukuran bukan kriteria WP-05 dan tidak diklaim) |
| Diff warning WP-04 → WP-05 | empat baris hilang, tidak ada yang baru: 2× `UseKtx` (`EqualizerView.kt`), 2× `UnusedResources` (`ic_mic.xml`, `ic_mic_off.xml`) |
| Diff warning clean build vs inkremental | `diff` dua daftar ternormalisasi → `IDENTICAL`, sehingga angka 108 bukan artefak cache |
| Rahasia | `git show 800886e --format="" \| grep -E "ghp_\|github_pat_\|AKIA[0-9A-Z]{16}\|-----BEGIN\|password\s*=\|secret\s*="` → tanpa hasil |
| Berkas lokal | `git ls-files \| grep -E "local.properties\|\.jks\|\.keystore"` → tanpa hasil |

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-24 | note | `AGENTS.md:44,67` masih menyebut `MjpegHttpServer.kt # NanoHTTPD: /video, /status, /mic/toggle` dan mendaftarkan `/mic/toggle` sebagai defek P0-2. `.work/Structure.md` masih mencantumkan `MicManager.kt` dan `EqualizerView.kt` pada pohon berkas. Semua sudah dijadwalkan pada `T-077`/`T-078` WP-29; WP-05 tidak boleh menyentuhnya karena di luar daftar `File:`. | `AGENTS.md:44,67`; `.work/Structure.md:26,29,51,59` |
| QC-25 | note | `lintDebug` kini hijau, tetapi 108 warning masih tersisa. Tidak satu pun blocker; WP-28 mencatat baseline baru. WP ini tidak mengklaim lint bebas warning. | `lint-results-debug.txt` |

Tidak ada temuan severity blocker. Tidak ada klaim Executor yang tidak
terbukti.

---

## Keamanan

| Aspek | Sebelum WP-05 | Sesudah WP-05 |
|---|---|---|
| Izin `RECORD_AUDIO` di APK | ada | tidak ada (diverifikasi pada merged manifest) |
| Kelas yang dapat merekam audio | `MicManager` ada | tidak ada |
| Endpoint HTTP yang menyalakan mikrofon | sudah hilang sejak WP-03 | tetap tidak ada |
| Permukaan serangan izin | 10 izin | 9 izin |
| Klaim keamanan | — | P0-2 pada sisi aplikasi **tertutup**: tidak ada izin, kelas, tombol, atau endpoint mikrofon. Catatan: aplikasi tetap memakai HTTP cleartext tanpa token (P0-3, WP-25..27) |

P0-2 tidak boleh dinaikkan menjadi klaim keamanan menyeluruh: stream masih
tanpa autentikasi dan masih cleartext. Itu pekerjaan WP-25 sampai WP-27.

---

## Rework

Tidak ada.

---

## Catatan push

Guard push dijalankan di `800886e`: root repo benar, pohon kerja bersih,
tidak ada berkas lokal/rahasia ter-track, author `ndeso17`, `diff --check`
bersih, tidak ada pola rahasia pada diff. Verdict PASS → commit artefak QC
lalu push `main`.
