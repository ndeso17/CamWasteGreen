# QC Report — WP-04

**WP:** WP-04 — Hapus kontrol mikrofon dan equalizer dari UI
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `1b7490e`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: PASS_WITH_NOTES

Kriteria terima WP-04 terpenuhi seluruhnya dan dibuktikan ulang dengan perintah
yang QC jalankan sendiri. Tidak ada rework. Empat catatan (note) tercatat di
bagian Temuan; tidak satu pun menahan WP-05.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Build + test | `./gradlew assembleDebug testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 3s`, 45 task up-to-date (dijalankan ulang, bukan salin Log) | sesi QC |
| Lint | `./gradlew lintDebug --console=plain` | `BUILD FAILED`; 1 error `MicManager.kt:54` `MissingPermission`, 112 warning — error lama di berkas yang dihapus pada WP-05, bukan regresi WP-04 | `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| Cakupan commit | `git show --name-only --format="" 1b7490e` | `.work/Plan_...md`, `README.md`, `MainActivity.kt`, `activity_main.xml` | sesi QC |
| Pohon kerja | `git status --short` | bersih | sesi QC |
| Diff hygiene | `git show --check --format="" 1b7490e` | tanpa keluaran | sesi QC |
| Identitas | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>`; trailer `Signed-off-by:` ada | sesi QC |

Lint masih gagal dengan satu error lama pada `MicManager.kt`, berkas yang tepat
dihapus pada WP-05. WP-04 tidak menyentuh berkas itu dan tidak mengklaim lint
bersih.

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC (dijalankan sendiri) |
|---|---|---|
| T-008 | Field `equalizerView` dan `btnMicToggle` dihapus | `git show 1b7490e -- MainActivity.kt` → dua baris `private lateinit var` hilang |
| T-008 | Kedua `findViewById` dihapus | diff memuat `-        equalizerView = findViewById(R.id.equalizerView)` dan `-        btnMicToggle = findViewById(R.id.btnMicToggle)` |
| T-008 | Listener mikrofon dihapus | diff memuat penghapusan blok `btnMicToggle.setOnClickListener { ... }` beserta `MicManager.toggle()` dan `equalizerView.setActive(MicManager.isActive)` |
| T-008 | Pemanggilan `updateUI()` mikrofon dihapus | diff memuat `-        equalizerView.setActive(isStreaming && MicManager.isActive)` |
| T-008 | `btnMicToggle.isEnabled` dan `setImageResource` dihapus | diff memuat penghapusan `btnMicToggle.isEnabled = true` dan blok `btnMicToggle.setImageResource(if (MicManager.isActive) R.drawable.ic_mic else R.drawable.ic_mic_off)` |
| T-008 | `MicManager.stop()` di `onDestroy()` dihapus | diff memuat `-        MicManager.stop()`; `onDestroy()` kini memuat `stopCamera()`, `MJPEGServerInstance.stop()`, `FilterPipeline.shutdown()`, `cameraExecutor.shutdown()` |
| T-009 | `REQUIRED_PERMISSIONS` hanya `CAMERA` | `grep -n "REQUIRED_PERMISSIONS" MainActivity.kt` → `arrayOf(Manifest.permission.CAMERA)`; `RECORD_AUDIO` = 0 kemunculan pada berkas itu |
| T-010 | View tombol mikrofon dihapus dari layout | `activity_main.xml` tidak lagi memuat `btnMicToggle`; `contentDescription="Mic"` juga hilang (baseline `cfa7f24` memuatnya di baris 126) |
| T-010 | `EqualizerView` dihapus dari layout | `grep -n "EqualizerView" activity_main.xml` → tanpa hasil; tag `dev.ndeso17.camwastegreen.EqualizerView` dan komentar "Right equalizer" hilang |
| T-010 | Jumlah `@+id/` berkurang tepat dua | baseline `git show cfa7f24:...activity_main.xml \| grep -c "@+id/"` = 25; sekarang = 23 |
| T-087 | Baris audio README akurat | `sed -n '26p' README.md` → "Endpoint `/mic/toggle`, rute `/audio`, dan tombol mikrofon di UI **sudah dihapus**. `MicManager` dan izin `RECORD_AUDIO` masih ada", kolom "Ditutup oleh" = `WP-05` |
| Kriteria | grep gabungan dua berkas kosong | `grep -rn "MicManager\|EqualizerView\|RECORD_AUDIO" MainActivity.kt activity_main.xml` → `NONE` |
| Kriteria | `assembleDebug` lulus | `BUILD SUCCESSFUL` |
| Kriteria | Bukti ada di Log WP | Log WP-04 memuat keluaran grep, jumlah id, dan ringkasan build |

---

## Anti-ghost — kriteria dibuktikan berdiskriminasi

| Kriteria | Baseline `cfa7f24` | Sekarang `1b7490e` |
|---|---|---|
| Kemunculan `MicManager\|btnMicToggle\|equalizerView` di `MainActivity.kt` | 12 | 0 |
| Jumlah `@+id/` di `activity_main.xml` | 25 | 23 |
| `contentDescription="Mic"` di layout | ada (baris 126) | tidak ada |
| `R.id.btnMicToggle` / `R.id.equalizerView` dirujuk dari mana pun di `app/src` | ya | tidak |

Tanpa uji ini, "grep kosong" tidak membuktikan apa pun.

---

## Pemeriksaan tambahan yang tidak diminta kriteria

| Aspek | Hasil |
|---|---|
| Layout masih XML valid | `python3 -c "ElementTree.parse('activity_main.xml')"` → `XML OK` |
| Sisa rujukan id yang sudah dihapus | `grep -rn "btnMicToggle\|equalizerView\|R.id.btnMic\|R.id.equalizer" app/src` → tanpa hasil; tidak ada `findViewById` menggantung yang menyebabkan `NullPointerException` |
| Impor dan tipe masih terpakai | `ImageButton` masih dipakai (4 kemunculan) untuk `btnMinimize`, `btnSwitchCamera`, `btnOrientationLock`, `btnFilterToggle`; impor tidak jadi mati |
| `EqualizerView` dan `MicManager` | berkas masih ada; `EqualizerView.kt:46` masih memanggil `MicManager.amplitude`, tetapi tidak ada lagi pemanggil kelas EqualizerView dari layout. Keduanya dihapus pada WP-05 |
| Drawable `ic_mic`, `ic_mic_off` | tidak dirujuk dari mana pun di `app/src` lagi; kedua berkas masih ada dan dihapus pada WP-05 |
| Izin `RECORD_AUDIO` | masih ada di `AndroidManifest.xml:15`; itu slated WP-05 (`T-012`) |
| Rahasia | `git show 1b7490e --format="" \| grep -E "ghp_\|github_pat_\|AKIA[0-9A-Z]{16}\|-----BEGIN\|password\s*=\|secret\s*="` → tanpa hasil |
| Karakter rusak | pemindaian non-ASCII pada PLAN, `README.md`, `MainActivity.kt`, `activity_main.xml` → 0 baris bermasalah |

### Kenaikan warning lint dijelaskan, bukan diterima begitu saja

Baseline 111 warning, sekarang 112. Selisih +1, bukan dua. QC memeriksa
komposisinya:

| Sumber | Efek |
|---|---|
| `R.drawable.ic_mic` sekarang unused | +1 warning `UnusedResources` |
| `R.drawable.ic_mic_off` sekarang unused | +1 warning `UnusedResources` |
| `contentDescription="Mic"` hilang bersama view | -1 warning `HardcodedText` |

Netto +1, cocok dengan angka yang terukur.

QC tidak berhenti pada penjelasan. Baseline `cfa7f24` di-build ulang di
pohon kerja yang sama dan `lintDebug` dijalankan dua kali, lalu kedua laporan
warning dinormalisasi (path dipangkas, nomor baris dibuang) lalu dibandingkan:

```text
$ diff baseline-warnings.txt wp04-warnings.txt
32a33,34
> app/src/main/res/drawable/ic_mic_off.xml: ... unused [UnusedResources]
> app/src/main/res/drawable/ic_mic.xml: ... unused [UnusedResources]
42d43
< app/src/main/res/layout/activity_main.xml: Hardcoded string "Mic" ... [HardcodedText]
```

Hanya tiga baris itu yang berubah. Tidak ada warning lain yang muncul atau
hilang. Baseline terukur ulang: 1 error + 111 warning; WP-04: 1 error + 112
warning. Setelah perbandingan, `app/src` dipulihkan ke `1b7490e` dan
`assembleDebug` + `testDebugUnitTest` dijalankan ulang (`BUILD SUCCESSFUL`)
serta `lintDebug` diulang (1 error + 112 warning), sehingga keadaan disk
setelah QC sama seperti keadaan commit yang diperiksa.

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-18 | note | `lintDebug` masih gagal 1 error di `MicManager.kt:54` dan kini 112 warning. Error itu ditutup WP-05; warning `UnusedResources` untuk `ic_mic*` juga tertutup WP-05. WP-04 tidak mengklaim lint bersih. | `lint-results-debug.txt` |
| QC-19 | note | `EqualizerView.kt` kini tidak dirujuk view mana pun di layout, tetapi masih ada dan masih memanggil `MicManager.amplitude`. Kode mati sementara; dihapus WP-05. Tidak ada perilaku salah pada runtime, hanya sisa. | `EqualizerView.kt:46`; `grep -rn "EqualizerView" app/src` hanya berkas itu sendiri |
| QC-20 | note | `README.md:26` akan kembali tidak akurat pada WP-05, ketika `MicManager` dan izin `RECORD_AUDIO` ikut hilang. Pembaruan akhir sudah dijadwalkan pada `T-085` WP-29; tabel README akan salah selama beberapa WP di antaranya. Ini konsekuensi bentuk dokumentasi yang dipilih, bukan pelanggaran baru. | `README.md:26`; PLAN `T-085` |
| QC-21 | info | `AGENTS.md:44` masih menulis `MjpegHttpServer.kt # NanoHTTPD: /video, /status, /mic/toggle` dan `AGENTS.md:67` masih mendaftarkan `/mic/toggle` sebagai defek P0-2. Keduanya sudah tercakup `T-077` WP-29 (temuan QC-14 WP-03). | `AGENTS.md:44,67` |

Tidak ada temuan severity blocker. Tidak ada klaim Executor yang tidak
terbukti.

---

## Keamanan

| Aspek | Sebelum WP-04 | Sesudah WP-04 |
|---|---|---|
| Izin `RECORD_AUDIO` diminta saat aplikasi mulai | ya (`REQUIRED_PERMISSIONS` memuatnya) | tidak |
| Cara mikrofon diaktifkan dari UI | tombol `btnMicToggle` | tidak ada; `MicManager.toggle()` tidak lagi dipanggil dari mana pun di `app/src` |
| Berkas rekam audio | `MicManager` masih hidup | `MicManager` masih ada sebagai berkas, tapi tanpa pemanggil; dihapus WP-05 |

P0-2 belum tuntas: izin `RECORD_AUDIO` masih ada di manifest dan kelas
`MicManager` masih ada sampai WP-05. QC tidak mengklaim mikrofon sudah tidak
dapat diaktifkan.

---

## Rework

Tidak ada.

---

## Catatan push

Guard push dijalankan di `1b7490e`: root repo benar, pohon kerja bersih,
tidak ada berkas lokal/rahasia ter-track, author `ndeso17`, `diff --check`
bersih, tidak ada pola rahasia pada diff. Verdict PASS_WITH_NOTES → commit
artefak QC lalu push `main`.
