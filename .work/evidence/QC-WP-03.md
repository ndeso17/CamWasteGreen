# QC Report — WP-03

**WP:** WP-03 — Hapus endpoint `/audio` dan `/mic/toggle`
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `12cdbf1`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: FAIL

Kriteria terima WP-03 terpenuhi 2/2 dan kode bersih: rute benar-benar hilang,
tidak ada rujukan `MicManager`, `assembleDebug` lulus. Kegagalan berasal dari
dokumentasi yang baru menjadi tidak benar karena WP ini: `README.md` masih
menyatakan endpoint `/mic/toggle` ada, padahal commit `12cdbf1` menghapusnya.
Itu persoalan yang sama dengan QC-06..QC-08 pada WP-02, sehingga standar yang
sama diterapkan.

Rework hanya satu baris tabel pada `README.md`.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Build | `./gradlew assembleDebug --console=plain` | `BUILD SUCCESSFUL in 6s` (dijalankan ulang, bukan salin Log) | sesi QC |
| Lint | `./gradlew lintDebug --console=plain` | `BUILD FAILED`; 1 error `MicManager.kt:54` `MissingPermission`, 111 warning — identik baseline, bukan regresi | `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| Cakupan commit | `git show --name-only --format="" 12cdbf1` | `MjpegHttpServer.kt`, `.work/Plan_...md` | sesi QC |
| Pohon kerja | `git status --short` | bersih | sesi QC |
| Diff hygiene | `git show --check --format="" 12cdbf1` | tanpa keluaran | sesi QC |

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC |
|---|---|---|
| T-006 | Rute `/audio` dan `/mic/toggle` dihapus | `grep -n '"/' MjpegHttpServer.kt` → hanya `/`, `/video`, `/status`. Tidak ada `audio/aac`, tidak ada `audioInputStream` |
| T-006 | `audioInputStream()` dihapus | `git diff 1a76cf4 12cdbf1` memuat `-38` baris termasuk seluruh badan fungsi; `grep -c audioInputStream` = 0 |
| T-006 | Field `mic` dihapus dari `/status` | `sed -n '54,62p'` → `streaming`, `fps`, `quality`, `clients`; tidak ada `mic` |
| T-007 | Tidak ada rujukan `MicManager` | `grep -n "MicManager\|audio\|mic\|Mic" MjpegHttpServer.kt` → `NONE` |
| T-007 | Impor `MicManager` dihapus | `git diff` memuat `-import dev.ndeso17.camwastegreen.MicManager` |
| Kriteria | `grep -n "audio\|mic"` tidak menemukan rute/rujukan | `NONE` |
| Kriteria | `./gradlew assembleDebug` lulus | `BUILD SUCCESSFUL in 6s` |

## Anti-ghost

| Kriteria | Sebelum (`1a76cf4`) | Sesudah (`12cdbf1`) |
|---|---|---|
| `audioInputStream` atau `mic/toggle` pada `MjpegHttpServer.kt` | 3 kemunculan | 0 |

Kriteria grep terbukti berdiskriminasi; nilainya berubah karena pekerjaan WP,
bukan karena kriteria yang selalu benar.

## Pemeriksaan tambahan yang tidak diminta kriteria

| Aspek | Hasil |
|---|---|
| Impor tidak terpakai | `InputStream` masih dipakai sebagai tipe balik `mjpegInputStream`; `Executors`, `JSONObject`, `IHTTPSession` masih dipakai. Tidak ada impor mati |
| `Method.POST` tersisa | `grep -n "Method\."` → tidak ada; referensi ke kelas nested `NanoHTTPD.Method` hilang bersama rute |
| Rute sah masih utuh | `/` (halaman), `/video` (multipart), `/status` (JSON) tetap ada dan tidak berubah perilaku |
| Rute tak dikenal | `else` → `404 Not Found` tetap ada, sehingga `/audio` dan `/mic/toggle` kini jatuh ke 404 |
| Rahasia | `git diff` tanpa pola token/kunci |
| Karakter rusak | pemindaian non-ASCII pada berkas berubah → 0 baris bermasalah |

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-13 | **blocker (dokumentasi)** | `README.md:26` masih menyatakan "`MicManager`, endpoint `/mic/toggle`, tombol mikrofon di UI, dan izin `RECORD_AUDIO` masih ada". Endpoint `/mic/toggle` sudah dihapus pada commit ini, jadi baris itu salah sekarang. Sebagian lain baris itu benar (`MicManager` dan `RECORD_AUDIO` masih ada sampai WP-05). | `README.md:26` vs `grep -rn "mic/toggle" app/src` → 0 hasil |
| QC-14 | note | `AGENTS.md:44` masih menulis `MjpegHttpServer.kt # NanoHTTPD: /video, /status, /mic/toggle` dan `AGENTS.md:67` masih mendaftarkan `/mic/toggle` sebagai defek P0-2. Keduanya menjadi tidak akurat setelah WP ini. `AGENTS.md` sudah berada pada `File:` WP-29 (`T-077`), sehingga tidak perlu rework di sini, tetapi daftar itu wajib diperbarui di WP-29. | `AGENTS.md:44,67` |
| QC-15 | note | `lintDebug` masih gagal karena 1 error di `MicManager.kt:54`. Itu berkas yang dihapus pada WP-05 dan bukan akibat WP ini; 111 warning juga tidak berubah dari baseline. WP-03 tidak mengklaim lint bersih dan QC tidak menuntutnya. | `lint-results-debug.txt` |
| QC-16 | info | Penghapusan rute hanya dibuktikan pada tingkat kode dan build. Belum ada permintaan HTTP nyata karena tidak ada perangkat; verifikasi `404` untuk kedua rute tetap berstatus `MANUAL-VERIFY` dan tidak boleh diklaim lulus. | Log WP-03 bagian Keterbatasan |

---

## Keamanan

| Aspek | Sebelum | Sesudah |
|---|---|---|
| Endpoint POST tanpa autentikasi yang menyalakan mikrofon | ada (`/mic/toggle`) | tidak ada |
| Rute audio keluar | ada (`/audio`) | tidak ada |
| `RECORD_AUDIO` | diminta sebagai izin wajib | masih diminta (WP-04, WP-05) |
| Permukaan serangan HTTP | 5 rute | 3 rute |

WP ini menutup sebagian P0-2 pada sisi server. Klaim penuh P0-2 baru sah setelah
WP-04 (UI) dan WP-05 (izin dan berkas) selesai. Tidak ada klaim bahwa mikrofon
sudah tidak dapat diaktifkan sama sekali — `MicManager` masih dapat dipanggil
dari `MainActivity` sampai WP-04.

---

## Rework

- [ ] **REWORK-R1** — Perbarui baris audio pada tabel "Keadaan saat ini" di
  `README.md` sehingga akurat untuk commit ini: endpoint `/mic/toggle` sudah
  dihapus (WP-03), sementara `MicManager`, tombol mikrofon di UI, dan izin
  `RECORD_AUDIO` masih ada sampai WP-04 dan WP-05. Ubah kolom "Ditutup oleh"
  bila perlu agar menyebut pemisahan itu.
  Cara uji: `grep -n "mic/toggle" README.md` hanya menemukan pernyataan bahwa
  endpoint itu **sudah dihapus**, bukan bahwa masih ada;
  `grep -rn "mic/toggle" app/src` tetap 0 hasil.
- [ ] **REWORK-R2** — Setelah REWORK-R1, buktikan tidak ada regresi:
  `./gradlew assembleDebug` lulus dan `grep -n "audio\|mic" MjpegHttpServer.kt`
  tetap kosong. Catat keluaran pada Log WP.

Catatan untuk Planner: `README.md` perlu masuk daftar `File:` WP-03 agar rework
ini sah menurut kontrak WP. Sudah dicatat sebagai amendemen pada PLAN.

---

## Catatan push

Tidak ada push. Verdict FAIL, jadi commit artefak QC dilakukan tetapi `main`
tidak di-push sampai WP-03 `done`.
