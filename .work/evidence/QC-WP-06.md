# QC Report — WP-06

**WP:** WP-06 — Transformasi NV21 murni untuk rotasi frame
**QC date:** 7 November 2026
**Repo:** `CamWasteGreen/`
**Commit yang diperiksa:** `9f665a5`
**QC by:** Orchestrator menjalankan peran QC (tanpa subagen)

## Verdict: PASS

Kriteria terima WP-06 terpenuhi seluruhnya. Kebenaran rotasi tidak hanya
dibuktikan oleh test milik Executor: QC menulis **oracle Python independen**
yang membangun bidang luma dan kisi chroma sendiri, lalu membandingkan byte
per byte dengan keluaran Kotlin pada 20 kombinasi dimensi/sudut. Semua cocok.
Tidak ada rework dan tidak ada temuan blocker. Dua catatan (note) tercatat.

---

## Gate — perintah yang QC jalankan sendiri

| Gate | Perintah | Hasil | Bukti |
|---|---|---|---|
| Root repo | `git rev-parse --show-toplevel` | `/home/mrksvt/Documents/Kuliah/RPL/CamWasteGreen` | sesi QC |
| Clean build | `./gradlew clean` | selesai tanpa error | sesi QC |
| Test targeted | `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.Nv21TransformTest"` | `BUILD SUCCESSFUL in 31s`, 27 task executed | sesi QC |
| Test suite penuh | `./gradlew testDebugUnitTest assembleDebug` | `BUILD SUCCESSFUL in 26s`; `ExampleUnitTest` 1/0, `Nv21TransformTest` 10/0 | `app/build/test-results/testDebugUnitTest/*.xml` |
| Lint | `./gradlew lintDebug` | `BUILD SUCCESSFUL`; 0 error, 108 warning | `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` |
| Diff warning vs WP-05 | `diff` daftar ternormalisasi | `IDENTICAL` — tidak ada warning baru | `/tmp/opencode/w5.txt`, `/tmp/opencode/w6.txt` |
| Cakupan commit | `git show --name-only --format="" 9f665a5` | `Nv21Transform.kt`, `Nv21TransformTest.kt`, PLAN | sesi QC |
| Pohon kerja | `git status --short` | bersih | sesi QC |
| Diff hygiene | `git show --check --format="" 9f665a5` | tanpa keluaran | sesi QC |
| Identitas | `git log -1 --format="%an <%ae>"` | `ndeso17 <ndeso17@users.noreply.github.com>`; trailer `Signed-off-by:` ada | sesi QC |
| Rahasia | `git show 9f665a5 --format="" \| grep -E "ghp_\|github_pat_\|AKIA..."` | tanpa hasil | sesi QC |

---

## Checklist — klaim Executor dibuktikan ulang

| Item | Klaim | Bukti QC (dijalankan sendiri) |
|---|---|---|
| T-013 | `rotateNv21(yuv, width, height, degrees)` ada dengan tanda tangan itu | `grep -n "fun rotateNv21" Nv21Transform.kt` → `fun rotateNv21(yuv: ByteArray, width: Int, height: Int, degrees: Int): ByteArray` |
| T-013 | Mendukung 0/90/180/270 | `when (degrees)` dengan empat cabang; `require` menolak nilai lain |
| T-013 | Kotlin murni tanpa `android.*` | `grep -rn "android"` pada `stream/` hanya menemukan **komentar** KDoc (baris 15 dan 10), bukan `import`; `grep -n "^import"` pada `Nv21Transform.kt` → tanpa hasil (berkas tanpa satu pun impor) |
| T-013 | Chroma ditangani terpisah dengan pembulatan ganjil | `chromaWidth = (w+1)/2`, `chromaHeight = (h+1)/2`; `nv21Size(5,3)` = 27 (dibuktikan test) |
| T-013 | Pasangan V,U tidak terpisah | loop chroma menulis `out[dstPair]` dan `out[dstPair+1]` dari `yuv[srcPair]`/`yuv[srcPair+1]` — pasangan selalu berpindah bersama |
| T-013 | Penolakan input tidak sah | `IllegalArgumentException` untuk sudut di luar himpunan, dimensi non-positif, dan buffer kekecilan; ketiganya diuji |
| T-014 | Test identitas 0 derajat | `rotateZero_isIdentityAndReturnsCopy` lulus; juga memeriksa `assertNotSame` sehingga bukan sekadar mengembalikan objek yang sama |
| T-014 | 90 dan 270 saling membalik | `rotateNinety_thenTwoSeventy_returnsOriginalOnOddDimensions` (5x3) lulus |
| T-014 | 180 dua kali kembali ke asal | `rotateOneEighty_twiceReturnsOriginalOnOddDimensions` (5x3) lulus |
| T-014 | Penolakan derajat tidak sah | `invalidDegrees_rejected` menguji 45, 360, -90, 1 |
| T-014 | Minimal empat kasus | 10 kasus (`tests="10"` pada XML hasil) |
| Kriteria | Test targeted lulus | `BUILD SUCCESSFUL`, 10 test 0 failure, dijalankan setelah `clean` |
| Kriteria | Test gagal bila rotasi salah | dibuktikan dua kali, lihat bagian Mutasi |
| Kriteria | `grep -n "import android"` kosong | tanpa hasil |
| Kriteria | Bukti pada Log WP | Log WP-06 memuat keluaran test, grep, dan mutasi |

---

## Verifikasi independen: oracle Python

Test Executor dan implementasi Executor ditulis oleh pihak yang sama; lulusnya
test tidak membuktikan rotasinya benar, hanya membuktikan keduanya sepakat. QC
karena itu menulis oracle terpisah di `/tmp/opencode/qc-oracle-nv21.py` yang:

1. membangun bidang luma `w x h` dan kisi chroma `ceil(w/2) x ceil(h/2)`
   sendiri, tanpa membaca kode Kotlin;
2. merotasi bidang 2D dengan operasi matriks biasa (`zip(*m[::-1])` untuk
   searah jarum jam, `zip(*m)[::-1]` untuk berlawanan);
3. menyusun ulang NV21 (luma lalu pasangan V,U) dan membandingkan byte per
   byte dengan keluaran Kotlin yang di-dump lewat test sementara
   `QcOracleDumpTest` (berkas itu sudah dihapus, tidak masuk commit).

Hasil:

```text
OK 3x2  deg=0/90/180/270
OK 5x3  deg=0/90/180/270
OK 4x4  deg=0/90/180/270
OK 7x5  deg=0/90/180/270
OK 2x2  deg=0/90/180/270
checked=20 mismatches=0
```

Dimensi ganjil (3x2, 5x3, 7x5) ikut diperiksa, bukan hanya dimensi genap, dan
kisi chroma yang dibulatkan ke atas menghasilkan jumlah byte yang sama sebelum
dan sesudah rotasi.

---

## Mutasi — kriteria "test gagal bila implementasi salah"

QC menjalankan dua mutasi sendiri, berbeda dari mutasi Executor:

| Mutasi | Perubahan | Hasil |
|---|---|---|
| QC-1 | `90 -> Pair(ch - 1 - cy, cx)` diubah menjadi `Pair(cy, cx)` (lapis chroma) | `rotateNinety_thenTwoSeventy_returnsOriginalOnOddDimensions` FAILED, `rotateNinety_keepsChromaPairsTogether` FAILED; `10 tests completed, 2 failed`; `BUILD FAILED` |
| Executor | `Pair(height - 1 - iy, ix)` menjadi `Pair(height - 1 - iy, width - ix)` (lapis luma) | 2 test FAILED, dicatat pada Log WP |

Kedua lapis (luma dan chroma) punya test yang benar-benar menangkap kesalahan.
Setelah mutasi QC, berkas dipulihkan dari cadangan dan `grep` mengonfirmasi
baris kembali ke `90 -> Pair(ch - 1 - cy, cx)`; suite kembali hijau.

---

## Anti-ghost — kriteria dibuktikan berdiskriminasi

| Kriteria | Tanpa mutasi | Dengan mutasi | Kesimpulan |
|---|---|---|---|
| Test targeted | 10 lulus | 2 gagal | test bergantung pada kebenaran rotasi, bukan pada keberadaan berkas |
| Oracle independen | 20 cocok | (mutasi tidak diuji ke oracle, oracle dipakai untuk memvalidasi keadaan akhir) | implementasi cocok dengan rotasi matriks standar |

---

## Pemeriksaan tambahan yang tidak diminta kriteria

| Aspek | Hasil |
|---|---|
| Buffer lebih besar dari kebutuhan | `require(yuv.size >= expectedSize)` dan hasil memakai `expectedSize`, jadi byte sisa tidak bocor ke keluaran |
| Sudut 0 derajat | mengembalikan salinan baru (`copyOf`), bukan referensi yang sama — diuji `assertNotSame`; pemanggil tidak bisa merusak buffer sumber lewat hasil |
| Overflow ukuran | `width * height` pada `Int`; batas praktis frame kamera (mis. 4000x3000 = 12 juta) jauh di bawah `Int.MAX_VALUE`, jadi tidak ada risiko untuk kasus pakai nyata. Tidak ada frame di atas 2 miliar piksel yang masuk akal pada perangkat |
| Konvensi arah rotasi | 90 derajat = searah jarum jam, sesuai `ImageProxy.imageInfo.rotationDegrees` Android. Keselarasan dengan sensor nyata baru dibuktikan WP-14/WP-17 di perangkat (`MANUAL-VERIFY`) |
| Lint | tidak ada entri `Nv21` pada laporan lint; total 108 warning, `diff` terhadap WP-05 `IDENTICAL` |
| Karakter rusak | pemindaian non-ASCII pada `Nv21Transform.kt`, `Nv21TransformTest.kt`, dan PLAN → 0 baris bermasalah |
| Berkas QC sementara | `QcOracleDumpTest.kt` dihapus; `git status --short` bersih sebelum commit artefak QC |

---

## Temuan

| ID | Severity | Detail | Bukti |
|---|---|---|---|
| QC-26 | note | `Nv21Transform` belum dipanggil dari mana pun. `grep -rn "Nv21Transform" app/src/main` hanya menemukan berkas kelas itu sendiri; jalur sensor masih memakai `FilterPipeline.process` dengan rotasi berbasis `Bitmap`. Penyambungan dijadwalkan WP-14 dan WP-17/WP-18. Bukan cacat WP-06, tetapi berarti WP ini belum mengubah perilaku aplikasi. | `grep -rn "Nv21Transform" app/src/main` |
| QC-27 | note | Kebenaran visual rotasi pada dimensi ganjil belum terbukti. Pembulatan kisi chroma ke atas mempertahankan jumlah byte, tetapi pergeseran sampel chroma terhadap luma pada lebar/tinggi ganjil hanya bisa dinilai dari frame nyata. Masuk `MANUAL-VERIFY`. | `MANUAL-VERIFY` pada PLAN; oracle memverifikasi struktur byte, bukan warna |

Tidak ada temuan severity blocker. Tidak ada klaim Executor yang tidak
terbukti. Satu deviasi Executor (nilai harapan chroma yang salah pada percobaan
pertama, bukan kesalahan implementasi) sudah dicatat di Log WP dan cocok dengan
apa yang QC lihat: kode akhir benar menurut oracle.

---

## Keamanan

Tidak ada permukaan keamanan baru: kelas murni tanpa I/O, tanpa izin, tanpa
jaringan, tanpa penyimpanan. Tidak ada rahasia pada diff.

---

## Rework

Tidak ada.

---

## Catatan push

Guard push dijalankan pada `9f665a5`: root repo benar, pohon kerja bersih,
tidak ada berkas lokal/rahasia ter-track, author `ndeso17`, `diff --check`
bersih, tidak ada pola rahasia pada diff. Verdict PASS → commit artefak QC
lalu push `main`.
