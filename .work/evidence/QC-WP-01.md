# QC Report — WP-01

**WP:** WP-01 — Catat keputusan B-21, B-28, B-29 dan sinkronkan dokumen
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/` (root git = `CamWasteGreen`, remote `git@github.com:ndeso17/CamWasteGreen.git`)
**Commit yang diperiksa:** `8cc5a85`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: PASS_WITH_NOTES

Kriteria terima WP-01 terpenuhi 4/4 dengan perintah yang dijalankan QC sendiri.
Tidak ada rework. Lima catatan (note) tercatat di bagian Temuan; tidak satu pun
menahan WP-02.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo benar | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Build | `./gradlew assembleDebug testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 6s` | sesi QC |
| Lint | `./gradlew lintDebug --console=plain` | `BUILD FAILED`; 1 error `MicManager.kt:54` `MissingPermission`, 111 warning — identik baseline `c2bd177`, bukan regresi WP-01 (WP-01 tidak menyentuh `app/`) | `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| Cakupan commit | `git show --name-only --format="" 8cc5a85` | hanya `.work/Architecture.md`, `.work/PRD.md`, `.work/Plan_...md` | sesi QC |
| Pohon kerja | `git status --short` | bersih; `main...origin/main [ahead 1]` | sesi QC |
| Diff hygiene | `git show --check --format="" 8cc5a85` | tanpa keluaran (tidak ada whitespace rusak) | sesi QC |

Lint tetap gagal karena satu error lama pada `MicManager.kt`, yaitu tepat berkas
yang dihapus pada WP-05. WP-01 hanya mengubah dokumen, sehingga gerbang build
dan lint tidak berubah terhadap baseline. Tidak ada klaim bahwa lint sudah bersih.

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC (dijalankan sendiri) |
|---|---|---|
| T-001 | D-27, D-28, D-29 tercatat dengan status disetujui dan sumber persetujuan | `grep -n "^## D-2[789]" ../.work/DECISIONS.md` → tiga entri di baris 739, 769, 789; tiap entri memuat `- Status: disetujui ...` dan `- Sumber persetujuan: jawaban eksplisit pengguna ...` |
| T-001 | Isi keputusan sesuai jawaban pengguna, bukan tafsiran | D-29 memuat `1280x720, 20 FPS, quality JPEG 80, maksimum dua klien` dan mengutip pertanyaan/jawaban; D-28 memuat `MIT` + `README.md` + `https://github.com/ndeso17/ipcam`; D-27 memuat `X-Api-Key`, `local.properties`, `MessageDigest.isEqual`, `401` |
| T-002 | Registri B-21/B-28/B-29 selesai, B-30 terbuka | `awk -F'\|' '/^\| (B-21\|B-28\|B-29\|B-30) \|/' ../.work/FINDINGS.md` → B-21 `**selesai** (D-27, D-29 ...)`, B-28 `**selesai untuk sisi kamera** (D-27 ...)`, B-29 `**selesai** (D-28 ...)`, B-30 `terbuka (baru)` |
| T-002 | Entri detail blocker ikut diperbarui | `awk '/^## BLOCKED: B-2[189]/,/^- Status:/'` → B-21 `Status: selesai pada bentuk kontrak`; B-28 `Status: selesai untuk sisi kamera; sisa lingkup backend terbuka`; B-29 `Status: selesai pada bentuk lisensi dan atribusi`; B-30 punya `Syarat pembukaan: keputusan pengguna tertulis pada .work/DECISIONS.md` |
| T-002 | C-AUD-03 tidak lagi menyatakan keputusan tertahan | baris 54 `../.work/FINDINGS.md` → `keputusan selesai (D-27); implementasi terbuka` |
| T-003 | PRD bagian 5 memuat `X-Api-Key` dan default stream | `.work/PRD.md:112-117` → `X-Api-Key`, port 8085, 1280x720, 20, 80, 2; `PRD.md:120-131` menyatakan default konfigurasi bukan ambang dan menunggu B-06 |
| T-003 | PRD bagian 8 diperbarui dan memuat B-30 | `PRD.md:162-172` → B-21/B-28/B-29 selesai dengan rujukan D-*, baris B-30 terbuka |
| T-003 | Architecture bagian 6 tidak lagi menyebut otorisasi tertahan | `grep -n "tertahan B-28" .work/Architecture.md` → 0 hasil (baseline `c2bd177` → 1 hasil); `Architecture.md:97` memuat token `X-Api-Key` (D-27) |
| T-003 | Bagian 9 Architecture diperbarui | `Architecture.md:131-136` → B-24, B-03, B-06, B-30, alias `CWG-` terbuka; D-27/D-29 dinyatakan menetapkan |
| Kriteria | Tidak ada ID `CWG-` yang berubah | `diff` antara `git show c2bd177:.work/PRD.md` dan berkas kerja → tanpa selisih; jumlah ID unik 22 = 22. Dijalankan dua kali, hasil identik |

---

## Anti-ghost — kriteria dibuktikan berdiskriminasi

| Kriteria | Baseline `c2bd177` | Sekarang | Arti |
|---|---|---|---|
| `tertahan B-28` pada Architecture | 1 | 0 | kriteria benar-benar berubah karena WP-01 |
| `X-Api-Key` pada PRD | 0 | 2 | simbol baru muncul karena keputusan D-27, bukan sudah ada sebelumnya |
| ID `CWG-` unik | 22 | 22 | penambahan dokumen tidak menciptakan/menghapus ID requirement |

Tanpa uji ini, kriteria "grep kosong" tidak dapat membuktikan apa pun.

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-01 | note | `CamWasteGreen/AGENTS.md` masih mencantumkan B-21/B-28/B-29 sebagai blocker dan keputusan terbuka (baris 27 dan 106-108), padahal semuanya sudah selesai pada D-27..D-29. Berkas itu tidak ada pada daftar `File:` WP-01, sehingga tidak boleh disentuh WP-01. WP-29 sudah memuat `AGENTS.md`, tetapi tugasnya belum menyebut daftar blocker secara eksplisit. | `grep -n "B-21\|B-28\|B-29" CamWasteGreen/AGENTS.md` |
| QC-02 | note | `../.work/DECISIONS.md` dan `../.work/FINDINGS.md` (root) berada di luar repo git mana pun; root workspace bukan repository. D-27..D-29 tidak memiliki riwayat versi, sehingga bukti keputusan hanya ada di disk. Ini konsekuensi kontrak (artefak di `.work/` root), bukan cacat WP-01. | `git -C .. rev-parse --show-toplevel` → `fatal: not a git repository` |
| QC-03 | note | Kotak tugas `T-001..T-003` pada blok `Tugas:` WP-01 masih `- [ ]` meskipun pekerjaannya selesai dan dijelaskan di Log Executor. Preseden plan lain (`Plan_GaleriGambarUtuh_...md` WP-01) memakai `- [x]` untuk WP selesai. QC mencentangnya saat menutup WP ini karena tiap butir sudah dibuktikan ulang. | `grep -c "^- \[x\] T-"` sebelum QC = 3 (hanya di dalam Log), sesudah = 6 |
| QC-04 | note | Tag requirement pada T-002 memakai `CWG-N02` (larangan inferensi/filter/AI pada jalur sensor) untuk B-30 yang sebenarnya soal encoder H.264 pada jalur sensor. Tag yang lebih tepat adalah `CWG-N01` (satu kali enkode) dengan `CWG-N02` sebagai tambahan. Perlu diperbaiki Planner saat re-plan, bukan diperbaiki diam-diam. | `.work/Plan_...md` T-002 vs `.work/PRD.md:96-97` |
| QC-05 | info | `lintDebug` gagal dengan 1 error `MicManager.kt:54` dan 111 warning. Sama persis dengan baseline `c2bd177`; WP-01 hanya mengubah dokumen sehingga tidak mungkin menambah/mengurangi hasil lint. Error itu ditutup WP-05. | `lint-results-debug.txt` |

Tidak ada temuan severity blocker. Tidak ada klaim Executor yang tidak
terbukti. Tidak ada rahasia pada berkas yang diubah.

---

## Keamanan dan hygiene

| Aspek | Perintah | Hasil |
|---|---|---|
| Berkas lokal tidak ter-track | `git ls-files \| grep -E "local.properties\|\.jks\|\.keystore"` | tanpa hasil |
| `local.properties` diabaikan | `git check-ignore -v local.properties` | `.gitignore:16:local.properties` |
| Pola rahasia pada berkas yang diubah | `grep -nEi "(ghp_\|github_pat_\|sk-[A-Za-z0-9]{16,}\|AKIA[0-9A-Z]{16}\|-----BEGIN\|password\s*=\|secret\s*=)"` pada PRD, Architecture, PLAN | tanpa hasil |
| Identitas commit | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>` |
| Trailer kontrak | `git log -1 --format="%B" \| tail -3` | `Signed-off-by: ndeso17 <ndeso17@users.noreply.github.com>` |
| Tidak ada token nyata di dokumen | buatan sendiri: `X-Api-Key` hanya nama header, tanpa nilai contoh | `grep -n "X-Api-Key"` → hanya deskripsi |

Catatan keamanan yang sudah benar: D-27 menyatakan HTTP cleartext sebagai
kelemahan terbuka dan tidak mengklaim keamanan jaringan tersadap. QC tidak
menambahkan klaim apa pun di atasnya.

---

## Rework

Tidak ada.

---

## Catatan push

Guard push dijalankan di `8cc5a85` sebelum push: root repo benar, pohon kerja
bersih, tidak ada berkas lokal/rahasia ter-track, author sesuai kontrak,
`diff --check` bersih. Hasil: push `main` ke
`git@github.com:ndeso17/CamWasteGreen.git` setelah artefak QC di-commit.

Preseden dicatat di `../.work/FINDINGS.md`: kontrak root tidak mengatur hak
push; push `main` setelah QC PASS diikuti dari commit `c2bd177` yang sudah ada
di `origin/main` pada sesi yang sama.
