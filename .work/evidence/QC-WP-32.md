# QC Report — WP-32

**WP:** WP-32 — Rapikan `README.md`: hapus tabel keadaan yang cepat basi
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `dcbf9ad`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: PASS_WITH_NOTES

Kriteria terima WP-32 terpenuhi seluruhnya dan dibuktikan ulang dengan perintah
yang QC jalankan sendiri. Tidak ada rework. Dua catatan (note) tercatat di
bagian Temuan; tidak satu pun menahan WP-05.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Build + test | `./gradlew assembleDebug testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 8s` (dijalankan ulang oleh QC) | sesi QC |
| Cakupan commit | `git show --name-only --format="" dcbf9ad` | `.work/Plan_...md`, `README.md` | sesi QC |
| Pohon kerja | `git status --short` | bersih | sesi QC |
| Diff hygiene | `git show --check --format="" dcbf9ad` | tanpa keluaran | sesi QC |
| Identitas | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>`; trailer `Signed-off-by:` ada | sesi QC |
| Rahasia | `git show dcbf9ad --format="" \| grep -E "ghp_\|github_pat_\|AKIA[0-9A-Z]{16}\|-----BEGIN\|password\s*=\|secret\s*="` | tanpa hasil | sesi QC |

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC (dijalankan sendiri) |
|---|---|---|
| T-088 | Bagian "Keadaan saat ini" dan tabelnya dihapus | `grep -n "Keadaan saat ini" README.md` → tanpa hasil |
| T-088 | Tabel tujuh baris hilang | `git show cfa7f24:README.md` memuat baris tabel "ML Kit face detection", "Filter visual dan enkode ganda", "Audio dan mikrofon", "Kamera terikat Activity", "Status belum terukur", "Stream tanpa otorisasi", "Jalur H.264 ganda"; pada `dcbf9ad` tidak ada satu pun |
| T-089 | Pointer ke PLAN dan `AGENTS.md` | `grep -n "Plan_PenambalanTemuanKamera" README.md` → baris 23, 42, 68; rujukan `AGENTS.md` ada pada baris 26 dan tabel Dokumen |
| T-089 | Tabel Dokumen ditambah baris PLAN | `README.md:68` memuat `.work/Plan_PenambalanTemuanKamera_07112026-170214.md` |
| T-090 | Tidak ada simbol volatil yang disebut ada atau tidak ada | `grep -nE "MicManager\|FilterPipeline\|StreamEncoder\|TcpStreamServer\|bindToLifecycle\|RECORD_AUDIO\|AiProcessor\|/mic/toggle" README.md` → tanpa hasil |
| T-090 | Bagian atribusi tidak lagi merujuk tabel lama | `README.md` baris "Asal kode dan atribusi" berbunyi "status pekerjaan itu ada pada PLAN, bukan di berkas ini"; tidak ada frasa "Keadaan saat ini" |
| T-090 | Bagian "Status pengembangan" tidak mengulang butir lama | `README.md:40-44` memuat "Baca PLAN untuk tahu WP mana yang sudah selesai; `README.md` tidak mengulang daftar itu"; tidak ada penyebutan kamera terikat Activity, `/status`, atau token sebagai butir terpisah |
| Kriteria | `grep -n "Keadaan saat ini" README.md` kosong | ya |
| Kriteria | grep simbol volatil kosong | ya |
| Kriteria | `grep -n "Plan_PenambalanTemuanKamera" README.md` menemukan rujukan | ya baris 23, 42, 68 |
| Kriteria | `assembleDebug` lulus | ya `BUILD SUCCESSFUL in 8s` |

---

## Anti-ghost — kriteria dibuktikan berdiskriminasi

| Kriteria | Baseline `cfa7f24` | Sekarang `dcbf9ad` |
|---|---|---|
| Header "Ditutup oleh" pada tabel | ada (1 kemunculan) | 0 |
| Simbol volatil disebut di README | 5 baris berbeda | 0 |
| Rujukan PLAN di README | 1 (bagian Status pengembangan) | 3 (baris 23, 42, 68) |

Perubahan nyata dan terukur, bukan sekadar berkas yang menyentuh kata kunci.

---

## Pemeriksaan tambahan yang tidak diminta kriteria

| Aspek | Hasil |
|---|---|
| Klaim positif yang masih ada di README | "Aplikasi menangkap frame dengan CameraX dan menyajikannya di jaringan lokal sebagai stream MJPEG multipart" — benar pada kode sekarang (`MjpegHttpServer.kt` rute `/video` masih ada, `grep -rn '"/video"' app/src` → ada) |
| Klaim audio | README tidak lagi menyebut audio sebagai ada atau tidak ada; role "dituju" tetap dinyatakan sebagai target, bukan keadaan |
| Angka FPS/latensi/bandwidth | hanya pernyataan "Belum ada pengukuran FPS, latensi, atau bandwidth pada perangkat sasaran" dan "Angka yang muncul di dokumen adalah default konfigurasi, bukan ambang mutu". Tidak ada angka baru |
| Perintah build | `./gradlew assembleDebug`, `testDebugUnitTest`, `lintDebug`, `installDebug` — sesuai perintah yang dipakai sepanjang pipeline |
| Karakter rusak | pemindaian non-ASCII pada `README.md` dan PLAN → 0 baris bermasalah. Teks "kini.vertex" yang sempat muncul pada Log Executor sudah dikoreksi menjadi "kini berbunyi" sebelum commit |
| Dokumen rujukan | PLAN, `AGENTS.md`, PRD, Architecture, Structure, Rules — semuanya ada di repo dan tercatat pada tabel Dokumen |

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-22 | note | Bagian "Batas peran" masih memakai bentuk target ("Peran yang dituju: aplikasi tidak menjalankan inferensi ..."). Forma ini benar dan tidak cepat basi, tetapi setelah seluruh WP kode selesai menjadi tidak sesuai keadaan. Sudah dijadwalkan pada `T-085` WP-29. | `README.md:10-15`; PLAN `T-085` |
| QC-23 | note | `README.md` kini tidak memberi ringkasan status dalam berkas sendiri, sehingga pembaca harus membuka PLAN. Ini konsekuensi yang disengaja: ringkasan manual terbukti menjadi sumber kesalahan berulang (QC-06..QC-08, QC-13, QC-17, QC-20). Tidak ada temuan cacat, hanya catatan desain. | `README.md:17-28` |

Tidak ada temuan severity blocker. Tidak ada klaim Executor yang tidak
terbukti.

---

## Dampak ke work package berikutnya

QC-21 (temuan WP-04) dan QC-20 (temuan WP-04) sebelumnya menuntut pembaruan
`README.md` pada WP-05 dan WP-29. Setelah WP-32, tabel yang jadi penyebabnya
sudah tidak ada:

- QC-20 (baris audio README basi pada WP-05) — gugur, karena baris tersebut
  tidak lagi ada.
- QC-21 (`AGENTS.md:44,67` masih menyebut `/mic/toggle`) — tetap berlaku dan
  tetap ditangani `T-077` WP-29.

Dengan ini `README.md` tidak lagi masuk daftar berkas yang harus disentuh
oleh WP-05 dan WP berikutnya, kecuali `T-085` WP-29 pada akhir pipeline.

---

## Rework

Tidak ada.

---

## Catatan push

Guard push dijalankan di `dcbf9ad`: root repo benar, pohon kerja bersih,
tidak ada berkas lokal/rahasia ter-track, author `ndeso17`, `diff --check`
bersih, tidak ada pola rahasia pada diff. Verdict PASS_WITH_NOTES → commit
artefak QC lalu push `main`.
