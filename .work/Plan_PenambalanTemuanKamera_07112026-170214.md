# Rencana Penambalan Temuan Audit Kamera CamWasteGreen

**Repo:** `CamWasteGreen/` (root git = `CamWasteGreen`, remote `git@github.com:ndeso17/CamWasteGreen.git`)
**Commit dasar:** `c2bd177`
**Tanggal:** 7 November 2026
**Sumber permintaan:** instruksi pengguna sesi 7 November 2026 berisi 11 butir
temuan audit kamera (P0 1–5, P1 6–8, P2 9–11).
**Keputusan pendukung:** jawaban pengguna pada sesi yang sama — otorisasi token
statis `X-Api-Key` (B-28, D-27), lisensi MIT (B-29, D-28), default stream
1280x720 / 20 FPS / quality 80 / maks 2 klien (B-21, D-29), penghapusan jalur
H.264 (B-30, D-30), pengesahan namespace alias `CWG-` (D-31).

## Riwayat revisi

| Revisi | Tanggal | Isi | Dasar |
|---|---|---|---|
| 1 | 7 Nov 2026 | Rencana awal, WP-01..WP-30 | 11 temuan audit + D-27..D-29 |
| 2 | 7 Nov 2026 | Tambah WP-31 (hapus jalur H.264), perluas WP-29 T-077 agar `AGENTS.md` tidak lagi mencantumkan B-21/B-28/B-29 sebagai terbuka, perbaiki tag T-002 menjadi `CWG-N01` + `CWG-N02` | D-30, D-31; temuan QC-01 dan QC-04 pada laporan `.work/evidence/QC-WP-01.md` |
| 3 | 7 Nov 2026 | Tambah WP-32 (rapikan `README.md`: hapus tabel "Keadaan saat ini" yang cepat basi, arahkan pembaca ke PLAN), revisi T-085 WP-29 agar tidak menggandakan pekerjaan itu, catat urutan eksekusi WP-32 sebelum WP-05 | Temuan QC-17 (WP-03) dan QC-20 (WP-04): tabel keadaan harus diubah ulang pada hampir setiap WP |
| 4 | 8 Nov 2026 | Tambah WP-33..WP-35 (pilihan interface jaringan untuk jalur streaming: logika murni, penyimpanan SharedPreferences, UI dan bind server). Urutan: WP-33..WP-35 setelah WP-22 dan sebelum WP-23 | Permintaan pengguna sesi 7–8 Nov 2026. Lingkup ini belum punya ID PRD; ID dan penambahan ke `.work/PRD.md` menunggu pengesahan pengguna |
| 5 | 8 Nov 2026 | Tambah WP-36 (hapus paket `gpuimage`, 16 shader `res/raw`, drawable `bg_filter_chip*`, dan dimens `filter_menu_*`). WP-15 ditutup lebih awal karena kriteria grep `FilterManager`/`FilterType` tidak dapat dipenuhi tanpa menghapus `gpuimage`, sedangkan paket itu di luar daftar berkas WP-15 | Mitigasi tertulis pada WP-15 dan catatan Planner Notes butir 4; temuan QC-37 |

Tidak ada WP `done` yang dihapus, dinomori ulang, atau diubah kriterianya.
WP-01 tetap `done`; revisi ini hanya menambah WP-31 dan memperjelas tugas WP
yang belum dikerjakan.

## Context

Repo `CamWasteGreen` adalah fork `ndeso17/ipcam` yang dipakai sebagai kamera
sensor. Commit `c2bd177` hanya mengganti paket dan menambah dokumen; perilaku
aplikasi belum berubah. Audit 7 November 2026 menemukan 3 blocker keras dan 7
masalah mutu sinyal. Rencana ini menutup seluruh temuan tersebut.

Kondisi baseline yang terukur:

| Pemeriksaan | Hasil |
|---|---|
| `./gradlew assembleDebug` | lulus |
| `./gradlew testDebugUnitTest` | lulus (hanya 2 test placeholder) |
| `./gradlew lintDebug` | gagal; 1 error `MicManager.kt:54` `MissingPermission`, 111 warning |

## Scope

Termasuk:

1. Foreground service bertipe `camera`, kamera di-bind ke service, autostart boot.
2. Penghapusan jalur audio (`/audio`, `/mic/toggle`, `MicManager`,
   `RECORD_AUDIO`, `EqualizerView`) dan seluruh ML Kit serta filter visual.
3. Header part MJPEG `X-Frame-Seq`, `X-Frame-Captured-At-Ms`,
   `X-Frame-Width`, `X-Frame-Height`.
4. `/status` terukur: `lastFrameAtMs`, `frameSeq`, `measuredFps`, `width`,
   `height`, `clients`, `cameraState`.
5. Otorisasi token statis `X-Api-Key` dari berkas di luar repo.
6. Enkode JPEG tunggal, jalur sensor tanpa filter dan AI.
7. Kunci AE/AWB/AF setelah warmup, orientasi tetap, resolusi/FPS dari konfigurasi.
8. Dedupe frame, batas klien, batas waktu tulis.
9. `ResolutionSelector` menggantikan `setTargetResolution`.
10. Test unit untuk header MJPEG, dedupe, FPS terukur, dan otorisasi.
11. `LICENSE` MIT dan atribusi fork.
12. Penghapusan jalur H.264 (`StreamEncoder`, `TcpStreamServer`) sesuai D-30.

Tidak termasuk:

- Inferensi, klasifikasi, pemilihan target, pemetaan bak, perintah robot.
- Format video selain MJPEG. Jalur H.264 (`StreamEncoder`, `TcpStreamServer`)
  **dihapus** pada WP-31 sesuai D-30; pengganti format tidak diputuskan.
- Angka ambang FPS/latensi sebagai kriteria lulus (`B-06` masih terbuka).
- Uji pada perangkat fisik (tidak ada perangkat; semua butir perangkat
  berstatus `MANUAL-VERIFY`).
- Perubahan `PRD Sistem Pemilah Objek.md` induk.

## Urutan dan alasannya

Urutan eksekusi berbeda dari urutan prioritas pada daftar temuan, karena setiap
WP wajib menjaga build tetap hijau:

1. **Keputusan dan lisensi dulu** (WP-01, WP-02) supaya WP kode mengacu pada
   keputusan yang tercatat, bukan pada percakapan.
2. **Penghapusan jalur audio lebih dulu** (WP-03..WP-05) karena satu-satunya
   error lint berada di `MicManager.kt`. Setelah itu gerbang lint bersih dan
   dapat dipakai sebagai kriteria terima WP berikutnya.
3. **Jalur sensor dibersihkan sebelum service dibuat** (WP-06..WP-16) supaya
   `CameraService` tidak perlu mewarisi filter, ML Kit, dan enkode ganda.
4. **Service, boot, orientasi** (WP-17..WP-20) setelah jalur sensor stabil.
5. **Konfigurasi dan kunci kamera** (WP-21..WP-24).
6. **Otorisasi** (WP-25..WP-27) terakhir pada sisi kode karena menyentuh server
   yang sudah berbentuk final.
7. **Gerbang mutu dan dokumentasi** (WP-28..WP-30).

## Constraint teknis

1. Test unit harus JVM murni: tidak ada `android.*`, tidak ada Robolectric.
   Robolectric belum menjadi dependensi dan pemilihan dependensi baru butuh
   keputusan tersendiri. Karena itu logika yang diuji dipisah ke berkas Kotlin
   murni (`Nv21Transform`, `FrameSource`, `MultipartWriter`, `StatusRegistry`,
   `StreamSession`, `ApiAuth`, `CameraConfig`).
2. Setiap WP menyentuh maksimal lima berkas dan menjaga `assembleDebug` lulus.
3. Sebelum WP kode dijalankan, jalankan
   `bash ~/.config/opencode/skills/preflight-resource-check/scripts/preflight.sh`
   dan patuhi `VERDICT`.
4. Perintah gerbang: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`,
   `./gradlew lintDebug`. Nama task sudah diverifikasi ada pada proyek ini.
5. Android SDK: `sdk.dir=/home/mrksvt/Expo` pada `local.properties` (di luar repo).
6. Tidak ada rahasia yang masuk repo. `local.properties` sudah ada di
   `.gitignore`.

---

## WP-01 — Catat keputusan B-21, B-28, B-29 dan sinkronkan dokumen

Status: done

File: `../.work/DECISIONS.md`, `../.work/FINDINGS.md`, `.work/PRD.md`, `.work/Architecture.md`

Dependensi: instruksi pengguna sesi 7 November 2026 (token statis, MIT, default
stream). Tidak ada WP prasyarat.

Tugas:
- [x] T-001 Catat D-27 (otorisasi token statis `X-Api-Key` yang dibaca dari
      berkas di luar repo, pembatasan jaringan ruang kontrol), D-28 (lisensi MIT
      dan atribusi fork `ndeso17/ipcam`), dan D-29 (default stream 1280x720,
      20 FPS, quality 80, maks 2 klien sebagai **default konfigurasi**) beserta
      sumber persetujuan pengguna (CWG-08, CWG-15, CWG-N05).
- [x] T-002 Perbarui registri blocker: B-21, B-28, B-29 menjadi selesai dengan
      rujukan D-27..D-29; tambahkan B-30 (status jalur H.264 `StreamEncoder`
      dan `TcpStreamServer`) sebagai terbuka dengan syarat pembukaan yang jelas
      (CWG-N01, CWG-N02). Tag awal hanya `CWG-N02`; QC-04 mencatat `CWG-N01`
      (satu kali enkode) lebih tepat, dan tag diperbaiki di sini tanpa mengubah
      isi tugas.
- [x] T-003 Sinkronkan `.work/PRD.md` bagian 5 dan 8 serta `.work/Architecture.md`
      bagian 6 dengan keputusan tersebut, tanpa mengubah satu pun ID `CWG-`
      (CWG-01, CWG-15).

Kriteria terima:
- `grep -n "D-27\|D-28\|D-29" ../.work/DECISIONS.md` menemukan tiga entri dengan
  status disetujui dan sumber persetujuan eksplisit.
- Registri `../.work/FINDINGS.md` menandai B-21, B-28, B-29 selesai dengan
  rujukan D-27..D-29, dan memuat B-30 berstatus terbuka.
- `.work/PRD.md` bagian 5 memuat header `X-Api-Key` dan nilai default stream;
  `.work/Architecture.md` bagian 6 tidak lagi menyebut bentuk otorisasi
  tertahan B-28.
- Tidak ada ID `CWG-` yang berubah: `grep -o "CWG-[0-9N][0-9]*" .work/PRD.md | sort -u`
  sama sebelum dan sesudah WP.
- Bukti: kutipan baris hasil grep pada Log WP.

Risiko: angka default disalahartikan sebagai ambang kelulusan mutu.
Mitigasi: D-29 dan PRD menandai angka itu default konfigurasi; ambang tetap
menunggu B-06, dan WP yang memakainya dilarang menyatakan lulus mutu.

### Log WP-01 (Executor)

Berkas berubah (4, sesuai daftar WP):

- `../.work/DECISIONS.md` — ditambah D-27, D-28, D-29.
- `../.work/FINDINGS.md` — registri B-21/B-28/B-29/C-AUD-03 diperbarui, B-30
  ditambahkan, entri detail B-21/B-28/B-29 diberi status keputusan, bagian
  temuan audit P0-3 diperbarui.
- `.work/PRD.md` — bagian 5 (kontrak stream + D-27 + D-29), bagian 8 (status
  blocker, tambah B-30).
- `.work/Architecture.md` — bagian 6 (otorisasi token + kelemahan cleartext +
  default konfigurasi), bagian 9 (daftar yang belum diputuskan).

Checklist:

- [x] T-001 D-27, D-28, D-29 tercatat dengan sumber persetujuan pengguna
      (jawaban pertanyaan sesi 7 November 2026). Bukti:
      `grep -c "^## D-2[789]" ../.work/DECISIONS.md` = 3.
- [x] T-002 Registri diperbarui: B-21 dan B-29 `**selesai**`, B-28
      `**selesai untuk sisi kamera**` dengan catatan lingkup backend tetap
      terbuka, C-AUD-03 menjadi "keputusan selesai; implementasi terbuka", dan
      B-30 ditambahkan sebagai terbuka beserta syarat pembukaan.
- [x] T-003 PRD bagian 5 memuat `X-Api-Key`, port 8085, 1280x720, 20 FPS,
      quality 80, maks 2 klien; PRD bagian 8 memuat status baru dan B-30.
      Architecture bagian 6 tidak lagi menyebut bentuk otorisasi tertahan
      B-28; bagian 9 diperbarui.

Output pemeriksaan (mentah):

```text
## D-27: 1
## D-28: 1
## D-29: 1
B-21 selesai: True
B-28 selesai sisi kamera: True
B-29 selesai: True
B-30 terbuka: True
PRD X-Api-Key: True
PRD default stream: True
Arch token D-27: True
Arch no B-28 pending: True

CWG ids identical (diff git HEAD vs working tree): IDENTICAL
```

Kriteria terima terpenuhi:

- Tiga entri D-27..D-29 ada, berstatus disetujui, dengan sumber persetujuan.
- Registri menandai B-21, B-28, B-29 selesai dengan rujukan D-27..D-29 dan
  memuat B-30 terbuka.
- `.work/PRD.md` bagian 5 memuat `X-Api-Key` dan default stream;
  `.work/Architecture.md` bagian 6 tidak lagi menyebut B-28 sebagai penghambat.
- `diff` daftar ID `CWG-` antara `git show HEAD:.work/PRD.md` dan berkas kerja
  menghasilkan `IDENTICAL`; tidak ada ID yang berubah.

Deviasi: tidak ada. Tidak ada perintah Gradle yang dijalankan karena WP ini
hanya mengubah dokumen dan tidak menyentuh kode atau konfigurasi build.

Keterbatasan:

- `B-24` tetap terbuka: bentuk header ditetapkan, korespondensi encoder dengan
  observasi backend belum. WP ini tidak menutupnya.
- `B-06` tetap terbuka: angka D-29 bukan ambang mutu.
- `B-03` tetap terbuka: orientasi akhir kamera belum ditetapkan.
- Keputusan D-27 mencatat kelemahan cleartext secara eksplisit; tidak ada klaim
  bahwa token membuat stream aman pada jaringan yang disadap.
- Alias `CWG-` masih menunggu pengesahan pengguna; traceability belum final.

### Log WP-01 (QC)

Verdict: **PASS_WITH_NOTES**. Laporan lengkap:
`.work/evidence/QC-WP-01.md`. Commit yang diperiksa: `8cc5a85`.

Perintah yang dijalankan QC sendiri:

```text
git rev-parse --show-toplevel            -> .../CamWasteGreen
./gradlew assembleDebug testDebugUnitTest -> BUILD SUCCESSFUL in 6s
./gradlew lintDebug                      -> BUILD FAILED; 1 error MicManager.kt:54
                                            MissingPermission + 111 warning
                                            (identik baseline c2bd177, bukan regresi)
git show --check --format="" 8cc5a85     -> tanpa keluaran
git ls-files | grep -E "local.properties|\.jks|\.keystore" -> tanpa hasil
git check-ignore -v local.properties     -> .gitignore:16:local.properties
```

Kriteria terima 4/4 terpenuhi:

- Tiga entri D-27/D-28/D-29 ada (`DECISIONS.md:739,769,789`), masing-masing
  berstatus disetujui dan menyebut sumber persetujuan pengguna.
- Registri `FINDINGS.md` baris 41/48/49/54/56: B-21 dan B-29 `**selesai**`,
  B-28 `**selesai untuk sisi kamera**`, C-AUD-03 keputusan selesai,
  B-30 `terbuka (baru)`; entri detail masing-masing memuat `Status:` baru.
- PRD bagian 5 memuat `X-Api-Key` dan default stream (baris 112-117); PRD
  bagian 8 memuat status baru dan B-30. Architecture bagian 6 tidak lagi
  memuat `tertahan B-28` (baseline 1 hasil, sekarang 0) dan bagian 9
  diperbarui.
- ID `CWG-` tidak berubah: `diff` daftar ID unik antara `c2bd177` dan berkas
  kerja tanpa selisih, 22 = 22; dijalankan dua kali dengan hasil identik.

Uji anti-ghost: `X-Api-Key` pada PRD 0 pada baseline menjadi 2 sekarang,
sehingga kriteria grep terbukti berdiskriminasi, bukan selalu benar.

Kotak tugas T-001..T-003 dicentang oleh QC setelah tiap butir dibuktikan ulang
(sebelumnya masih `[ ]` meski pekerjaan selesai — QC-03).

Temuan note (tidak menahan WP-02):

- QC-01: `CamWasteGreen/AGENTS.md` baris 27 dan 106-108 masih mencantumkan
  B-21/B-28/B-29 sebagai blocker/terbuka. Berkas itu di luar daftar `File:`
  WP-01; ditangani WP-29, yang perlu diperluas agar menyebut daftar blocker
  secara eksplisit.
- QC-02: `.work/DECISIONS.md` dan `.work/FINDINGS.md` root berada di luar repo
  git mana pun sehingga D-27..D-29 tidak berriwayat versi. Konsekuensi
  kontrak, bukan cacat WP-01.
- QC-03: kotak tugas WP-01 tidak dicentang Executor (kini sudah).
- QC-04: tag `CWG-N02` pada T-002 kurang tepat untuk B-30 (encoder H.264);
  tag yang lebih tepat `CWG-N01`. Perbaikan tag diserahkan ke Planner saat
  re-plan, tidak diubah diam-diam.
- QC-05: `lintDebug` masih gagal 1 error lama `MicManager.kt:54`; ditutup
  WP-05. Tidak ada klaim lint bersih.

Guard push lulus; artefak QC di-commit dan `main` di-push. Preseden push
dicatat di `../.work/FINDINGS.md`.

---

## WP-02 — Lisensi MIT dan atribusi fork

Status: done

File: `LICENSE`, `README.md`

Dependensi: WP-01 (D-28).

Tugas:
- [x] T-004 Tambahkan `LICENSE` berisi teks MIT dengan pemegang hak `2026 ndeso17`
      (CWG-N03).
- [x] T-005 Tambahkan `README.md`: tujuan repo, batas peran sebagai kamera sensor,
      asal kode `https://github.com/ndeso17/ipcam`, atribusi, perintah build, dan
      rujukan `.work/` (CWG-N03).

Kriteria terima:
- `LICENSE` ada dan memuat frasa `MIT License` serta `THE SOFTWARE IS PROVIDED "AS IS"`.
- `README.md` menyebut `ndeso17/ipcam` dan menyatakan aplikasi tidak menjalankan
  inferensi.
- `git status --short` tidak memuat `local.properties` maupun berkas rahasia.
- `./gradlew assembleDebug` tetap lulus (tidak ada perubahan build).
- Bukti: `git status --short` dan kutipan dua baris pertama tiap berkas di Log.

Risiko: nama pemegang hak belum dikonfirmasi tertulis.
Mitigasi: README menyatakan nama pemegang hak dapat disesuaikan bila pengguna
menetapkan nama lain; dicatat di Log WP dan diusulkan sebagai amandemen D-28.

### Log WP-02 (Executor)

Berkas berubah (2, sesuai daftar WP): `LICENSE` (baru), `README.md` (baru).

Checklist:

- [x] T-004 `LICENSE` memuat `MIT License` dan pemegang hak
      `Copyright (c) 2026 ndeso17`. Nama itu dikonfirmasi pengguna pada sesi
      7 November 2026 (jawaban "2026 ndeso17"), sehingga risiko nama pemegang
      hak pada blok Risiko **tertutup** dan D-28 tidak perlu diamandemen.
      Bukti: `grep -c "MIT License" LICENSE` = 1;
      `grep -c 'THE SOFTWARE IS PROVIDED "AS IS"' LICENSE` = 1.
- [x] T-005 `README.md` memuat tujuan repo, batas peran (tidak menjalankan
      inferensi, tanpa audio, tanpa filter/AI pada jalur sensor), asal kode
      `https://github.com/ndeso17/ipcam` beserta atribusi, perintah build,
      rujukan `.work/`, status pengembangan yang menyatakan pekerjaan audit
      belum selesai, dan pernyataan bahwa angka di dokumen adalah default
      konfigurasi bukan ambang mutu. Bukti:
      `grep -c "ndeso17/ipcam" README.md` = 1; baris 10 memuat
      "tidak menjalankan inferensi".

Kriteria terima terpenuhi:

- `LICENSE` ada dan memuat `MIT License` serta frasa jaminan `AS IS`.
- `README.md` menyebut `ndeso17/ipcam` dan menyatakan aplikasi tidak
  menjalankan inferensi.
- `git status --short` hanya memuat `LICENSE`, `README.md`, dan PLAN; tidak
  memuat `local.properties` maupun berkas rahasia.
- `./gradlew assembleDebug` → `BUILD SUCCESSFUL in 5s`; tidak ada perubahan
  build.

Deviasi: tidak ada. Kedua berkas baru berada pada daftar `File:` WP.

Keterbatasan:

- Lisensi tidak menghapus kewajiban menghormati hak cipta kode asal
  `ndeso17/ipcam`; README menyatakannya secara eksplisit.
- README menyatakan pekerjaan audit belum selesai. Pernyataan itu wajib
  diperbarui pada WP-29 setelah WP-01..WP-31 selesai.
- Tidak ada klaim mutu, FPS, atau latensi pada README.

### Log WP-02 (Executor) — rework putaran 1

Rework dari temuan QC-06..QC-08 (`FAIL`). Berkas berubah: `README.md` saja;
`LICENSE` tidak disentuh.

- [x] REWORK-R1 Klaim bentuk sekarang diganti menjadi pernyataan target.
      Bagian "Batas peran" kini menyebut "Peran yang dituju: ..." dan
      "Peran yang dituju juga mengecualikan audio, mikrofon, filter visual,
      deteksi wajah, dan segmentasi selfie dari jalur sensor". Bagian baru
      "Keadaan saat ini" (baris 17-32) memuat tabel sisa jalur upstream
      beserta keadaan nyata dan WP penutupnya: ML Kit face detection per frame
      (WP-13/15/16), filter visual dan enkode ganda (WP-13/14/15), audio dan
      mikrofon termasuk `RECORD_AUDIO` (WP-03..05), kamera terikat Activity
      (WP-17/18), status belum terukur (WP-10/12), stream tanpa otorisasi
      (WP-25..27), dan jalur H.264 (WP-31). Bagian "Asal kode dan atribusi"
      diubah menjadi "**direncanakan keluar** dari jalur operasi" dengan
      rujukan ke "Keadaan saat ini".
      Uji: `grep -n "Keadaan saat ini" README.md` → baris 17 dan 39.
- [x] REWORK-R2 Kriteria WP tetap terpenuhi setelah perubahan:
      `grep -c "ndeso17/ipcam" README.md` = 1;
      `grep -c "MIT License" LICENSE` = 1;
      `./gradlew assembleDebug --console=plain` → `BUILD SUCCESSFUL in 6s`.

Verifikasi tambahan:

- `grep -n "tidak menjalankan model ONNX\|Jalur sensor tidak memuat\|Audio dan mikrofon tidak dipakai" README.md`
  hanya menemukan baris 10-11 yang kini berada di bawah frasa "Peran yang
  dituju"; tidak ada klaim bentuk sekarang yang tersisa.
- Pemindaian karakter non-ASCII pada `README.md` → 0 baris bermasalah.
- Tidak ada angka FPS, latensi, atau bandwidth baru pada README.

Keterbatasan: tabel "Keadaan saat ini" adalah potret commit ini dan wajib
diperbarui pada WP-29 setelah WP-03..WP-31 selesai. Bila ada WP yang berubah
urutan, rujukan WP pada tabel harus disesuaikan.

### Log WP-02 (QC)

Verdict: **FAIL**. Laporan: `.work/evidence/QC-WP-02.md`. Commit diperiksa:
`bb8ca06`.

Kriteria terima terpenuhi 4/4 secara harfiah (`LICENSE` MIT valid,
`README.md` menyebut `ndeso17/ipcam`, tidak ada berkas lokal/rahasia ter-track,
`assembleDebug` lulus `BUILD SUCCESSFUL in 5s`). Yang gagal adalah akurasi isi
README terhadap kode pada commit yang sama.

Temuan blocker (dokumentasi):

- QC-06: README menyatakan aplikasi tidak menjalankan ML Kit, padahal
  `FilterPipeline.init` menetapkan `AiProcessor.isFaceDetectionEnabled = true`
  dan `FilterPipeline.process` memanggil `AiProcessor.process` pada setiap
  frame; `AiProcessor` membuat `FaceDetector()` saat inisialisasi
  (`pipeline/FilterPipeline.kt:44,66`; `ai/AiProcessor.kt:11,16,18`).
- QC-07: README menyatakan jalur sensor tanpa filter dan tanpa deteksi wajah,
  padahal analyzer memanggil `FilterPipeline.process` yang melakukan dua kali
  enkode JPEG (`compressToJpeg` + `Bitmap.compress`).
- QC-08: README menyatakan audio dan mikrofon tidak dipakai, padahal
  `MicManager`, `/mic/toggle`, tombol mikrofon, dan izin `RECORD_AUDIO` masih
  ada (`MjpegHttpServer.kt:74`, `MainActivity.kt`, `AndroidManifest.xml`).

Dasar: `AGENTS.md` bagian 7 melarang menyamarkan kemampuan yang belum tersedia
sebagai implementasi lengkap. Kriteria WP yang berbunyi "menyatakan aplikasi
tidak menjalankan inferensi" tidak membenarkan pernyataan bentuk sekarang yang
salah; pernyataan itu harus berbentuk target sampai WP-03..WP-16 selesai.

Rework:

- [ ] REWORK-R1 Ubah klaim "Batas peran" pada `README.md` menjadi pernyataan
      target, dan tambahkan bagian "Keadaan saat ini" yang menyebut ML Kit face
      detection masih berjalan per frame, jalur sensor masih memakai filter dan
      dua kali enkode JPEG, serta `MicManager`/`/mic/toggle`/`RECORD_AUDIO`
      masih ada; sebutkan bahwa ketiganya dihapus pada WP-03 sampai WP-16.
      Uji: `grep -n "Keadaan saat ini" README.md` menemukan bagian itu dan tidak
      ada klaim bentuk sekarang tanpa penanda target.
- [ ] REWORK-R2 Setelah REWORK-R1, buktikan kriteria WP tetap terpenuhi:
      `grep -c "ndeso17/ipcam" README.md` = 1, `grep -c "MIT License" LICENSE`
      = 1, `./gradlew assembleDebug` lulus. Catat keluaran pada Log WP.

`LICENSE` tidak perlu diubah. Tidak ada push karena verdict FAIL.

### Log WP-02 (QC) — putaran 2

Verdict: **PASS_WITH_NOTES**. Laporan: `.work/evidence/QC-WP-02.md` bagian
"QC putaran 2".

Rework dibuktikan:

- REWORK-R1 selesai: `grep -n "^## Keadaan saat ini" README.md` → baris 17;
  tiga klaim bentuk sekarang yang dilaporkan QC-06..QC-08 tidak lagi ada
  (`grep -nE "^(Aplikasi \*\*tidak\*\*|Audio dan mikrofon tidak dipakai|Jalur sensor tidak memuat)" README.md`
  → tanpa hasil); klaim kini berbentuk "Peran yang dituju: ..." dan bagian
  atribusi memakai "**direncanakan keluar**".
- REWORK-R2 selesai: `grep -c "ndeso17/ipcam" README.md` = 1;
  `grep -c "MIT License" LICENSE` = 1; `grep -c 'THE SOFTWARE IS PROVIDED "AS IS"' LICENSE`
  = 1; `./gradlew assembleDebug` → `BUILD SUCCESSFUL`.

Tabel "Keadaan saat ini" diperiksa satu baris demi satu baris terhadap kode,
bukan diterima dari Log Executor. Seluruh tujuh baris cocok dengan isi berkas
pada commit ini (ML Kit aktif di `FilterPipeline.kt:44,66`; enkode ganda di
`:62,91`; `object MicManager` + `/mic/toggle` di `MjpegHttpServer.kt:74` +
`RECORD_AUDIO` di `AndroidManifest.xml:15`; `bindToLifecycle` di
`MainActivity.kt:443`; `/status` memakai `currentFps` di `MjpegHttpServer.kt:67`;
`X-Api-Key` = 0 dan `usesCleartextTraffic="true"` di `AndroidManifest.xml:30`;
`StreamEncoder.kt`/`TcpStreamServer.kt` ada).

QC-06, QC-07, QC-08 dinyatakan tertutup. Temuan baru hanya note:

- QC-11: tabel "Keadaan saat ini" adalah potret commit ini dan harus
  diperbarui setelah WP-03..WP-31 selesai. `README.md` belum tercantum pada
  `File:` WP-29; Planner perlu menambahkannya saat re-plan.
- QC-12: bagian "Status pengembangan" mengulang tiga butir dari tabel
  (baris 43-51 vs 27-29); bukan kesalahan, tetapi menambah beban pemeliharaan.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-03 — Hapus endpoint `/audio` dan `/mic/toggle`

Status: done

File: `app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt`,
`README.md`

Dependensi: WP-01.

Tugas:
- [x] T-006 Hapus cabang rute `/audio` dan `/mic/toggle`, fungsi
      `audioInputStream()`, serta field `mic` pada respons `/status` (CWG-14).
- [x] T-007 Pastikan server tidak lagi merujuk `MicManager` (CWG-14).
- [x] T-086 Perbarui baris audio pada tabel "Keadaan saat ini" di `README.md`
      agar akurat: `/mic/toggle` sudah dihapus, sementara `MicManager`, tombol
      mikrofon, dan izin `RECORD_AUDIO` masih ada sampai WP-04/WP-05 (CWG-N06;
      temuan QC-13 WP-03). `README.md` ditambahkan ke `File:` oleh amendemen
      Planner, bukan oleh Executor.

Kriteria terima:
- `grep -n "audio\|mic" app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt`
  tidak menemukan rute maupun rujukan `MicManager`.
- `grep -rn "mic/toggle" app/src` kosong, dan `README.md` hanya menyebut
  endpoint itu sebagai sudah dihapus.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan ringkasan build pada Log WP.

Risiko: klien demo lama memanggil `/audio` dan menerima 404.
Mitigasi: tidak ada klien sah pada arsitektur D-23; dicatat di Log sebagai
perubahan yang disengaja.

### Log WP-03 (Executor)

Berkas berubah (1, sesuai daftar WP):
`app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt`.

Checklist:

- [x] T-006 Rute `/audio` (beserta `audio/aac`) dan `/mic/toggle` dihapus dari
      `serve()`. Fungsi `audioInputStream()` (38 baris) dihapus. Field `mic`
      dihapus dari badan JSON `/status`. `/status` sekarang memuat
      `streaming`, `fps`, `quality`, `clients`.
- [x] T-007 Impor `dev.ndeso17.camwastegreen.MicManager` dihapus. Server tidak
      lagi merujuk `MicManager`, `audioQueue`, maupun `Method.POST`.

Output pemeriksaan (mentah):

```text
grep -n "audio\|mic" MjpegHttpServer.kt   -> NONE
grep -n "MicManager" MjpegHttpServer.kt    -> NONE
grep -n "Method\." MjpegHttpServer.kt      -> no Method usage
./gradlew assembleDebug                     -> BUILD SUCCESSFUL in 9s
./gradlew lintDebug                         -> BUILD FAILED; 1 error
                                               MicManager.kt:54 MissingPermission,
                                               111 warning (tidak berubah dari
                                               baseline; error ditutup WP-05)
```

Kriteria terima terpenuhi: grep rute/`MicManager` kosong, `assembleDebug`
lulus. `lintDebug` belum bersih karena error berada di `MicManager.kt`, berkas
yang dihapus pada WP-05; WP ini tidak mengklaim lint bersih.

Deviasi: tidak ada. Ruang lingkup tepat satu berkas.

Keterbatasan:

- Penghapusan rute hanya dibuktikan pada tingkat kode dan build. Belum ada
  permintaan HTTP nyata karena tidak ada perangkat; verifikasi `404` untuk
  `/audio` dan `/mic/toggle` masuk MANUAL-VERIFY.
- `MicManager`, `EqualizerView`, dan pemanggilannya di `MainActivity` masih ada
  sampai WP-04 dan WP-05. Build tetap hijau karena kelasnya belum dihapus.
- `/` masih memakai judul "Android IP Cam" dari upstream; penggantian halaman
  diagnostik berada pada WP-12.

### Log WP-03 (Executor) — rework putaran 1

Rework dari temuan QC-13 (`FAIL`). Berkas berubah: `README.md` hanya satu
baris (baris 26, kolom tabel "Keadaan saat ini"). `MjpegHttpServer.kt` tidak
disentuh pada putaran ini.

- [x] REWORK-R1 Baris audio sekarang berbunyi: "Endpoint `/mic/toggle` dan rute
      `/audio` **sudah dihapus**. `MicManager`, tombol mikrofon di UI, dan izin
      `RECORD_AUDIO` masih ada", dengan kolom "Ditutup oleh" menjadi
      `WP-04, WP-05` (WP-03 tidak lagi dicantumkan karena sudah selesai).
      Uji: `grep -n "mic/toggle" README.md` → baris 26 memuat "sudah dihapus";
      `grep -rn "mic/toggle" app/src` → 0 hasil.
- [x] REWORK-R2 Tanpa regresi: `grep -n "audio\|mic" MjpegHttpServer.kt` →
      `NONE`; `./gradlew assembleDebug --console=plain` →
      `BUILD SUCCESSFUL in 6s`; pemindaian non-ASCII `README.md` → 0 baris.

Keterbatasan: baris itu kembali harus diperbarui pada WP-04 (tombol mikrofon
hilang) dan WP-05 (`MicManager` dan `RECORD_AUDIO` hilang). Tugas pembaruan
akhir sudah ada pada `T-085` WP-29; QC-14 mencatat `AGENTS.md` juga perlu
diperbarui di sana.

### Log WP-03 (QC)

Verdict: **FAIL**. Laporan: `.work/evidence/QC-WP-03.md`. Commit diperiksa:
`12cdbf1`.

Kriteria kode terpenuhi dan dibuktikan ulang oleh QC:

- `grep -n '"/' MjpegHttpServer.kt` → hanya `/`, `/video`, `/status`.
- `grep -n "MicManager\|audio\|mic\|Mic" MjpegHttpServer.kt` → `NONE`.
- `grep -rn "mic/toggle" app/src` → 0 hasil.
- `/status` memuat `streaming`, `fps`, `quality`, `clients`; field `mic` hilang.
- Anti-ghost: `audioInputStream`/`mic/toggle` pada berkas itu 3 kemunculan pada
  `1a76cf4` menjadi 0 pada `12cdbf1`.
- `./gradlew assembleDebug` → `BUILD SUCCESSFUL in 6s` (dijalankan ulang QC).
- `./gradlew lintDebug` → `BUILD FAILED`, 1 error `MicManager.kt:54` + 111
  warning, identik baseline; bukan regresi dan bukan lingkup WP-03.

Temuan yang menjatuhkan verdict:

- QC-13 (blocker dokumentasi): `README.md:26` masih menyatakan endpoint
  `/mic/toggle` "masih ada", padahal commit ini menghapusnya. Baris itu harus
  dipecah: endpoint sudah dihapus, `MicManager`/tombol/`RECORD_AUDIO` masih ada
  sampai WP-04/WP-05.

Temuan note: QC-14 (`AGENTS.md:44,67` masih menyebut `/mic/toggle`; sudah
tercakup `T-077` WP-29), QC-15 (lint error lama di `MicManager.kt`), QC-16
(verifikasi `404` masih `MANUAL-VERIFY`).

Rework:

- [ ] REWORK-R1 Perbarui baris audio tabel "Keadaan saat ini" di `README.md`
      agar `/mic/toggle` tercatat sudah dihapus, sedangkan `MicManager`, tombol
      mikrofon, dan `RECORD_AUDIO` masih ada sampai WP-04/WP-05.
      Uji: `grep -n "mic/toggle" README.md` hanya menyatakan "sudah dihapus";
      `grep -rn "mic/toggle" app/src` tetap 0.
- [ ] REWORK-R2 Buktikan tanpa regresi: `./gradlew assembleDebug` lulus dan
      `grep -n "audio\|mic" MjpegHttpServer.kt` tetap kosong.

Amendemen Planner: `README.md` ditambahkan ke `File:` WP-03 dan tugas `T-086`
ditulis, karena rework ini menyentuh berkas di luar daftar awal.

Tidak di-push karena verdict FAIL.

### Log WP-03 (QC) — putaran 2

Verdict: **PASS_WITH_NOTES**. Laporan: `.work/evidence/QC-WP-03.md` bagian
"QC putaran 2".

Rework dibuktikan:

- REWORK-R1 selesai. `sed -n '26p' README.md` memuat "Endpoint `/mic/toggle`
  dan rute `/audio` **sudah dihapus**. `MicManager`, tombol mikrofon di UI,
  dan izin `RECORD_AUDIO` masih ada". Tiap klausa diperiksa terhadap kode:
  `mic/toggle` di `app/src` = 0, `"/audio"` = 0, `MicManager` = 8 rujukan,
  `RECORD_AUDIO` = 2 rujukan, `btnMicToggle` ada di `MainActivity.kt:49,119,262`
  dan `activity_main.xml:121`. Klaim "masih ada" dan "sudah dihapus" keduanya
  cocok.
- REWORK-R2 selesai. `grep -n "audio\|mic" MjpegHttpServer.kt` → `NONE`;
  `./gradlew assembleDebug` → `BUILD SUCCESSFUL in 6s`;
  `git diff 12cdbf1 -- MjpegHttpServer.kt` kosong, sehingga rework tidak
  menyentuh kode.

QC-13 dinyatakan tertutup. Temuan baru hanya note:

- QC-17: baris audio README akan kembali tidak akurat pada WP-04 (tombol
  mikrofon hilang) dan WP-05 (`MicManager` dan `RECORD_AUDIO` hilang).
  Pembaruan akhir ada pada `T-085` WP-29; README akan salah selama beberapa WP
  di antaranya. Dicatat sebagai konsekuensi bentuk dokumentasi, bukan
  pelanggaran baru.
- QC-14 (`AGENTS.md` masih menyebut `/mic/toggle`, ditangani `T-077`),
  QC-15 (lint error lama di `MicManager.kt`, ditutup WP-05), dan QC-16
  (`404` masih `MANUAL-VERIFY`) tetap berlaku.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-04 — Hapus kontrol mikrofon dan equalizer dari UI

Status: done

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`,
`app/src/main/res/layout/activity_main.xml`, `README.md`

Dependensi: WP-03.

Tugas:
- [x] T-008 Hapus field `btnMicToggle` dan `equalizerView`, listener mikrofon,
      serta pemanggilan `MicManager.toggle()`, `MicManager.stop()`, dan
      `setActive(...)` (CWG-14).
- [x] T-009 Hapus `RECORD_AUDIO` dari `REQUIRED_PERMISSIONS` sehingga hanya
      `CAMERA` yang diminta (CWG-14).
- [x] T-010 Hapus view tombol mikrofon dan `EqualizerView` dari
      `activity_main.xml` (CWG-14).
- [x] T-087 Perbarui baris audio tabel "Keadaan saat ini" di `README.md`:
      tombol mikrofon di UI sudah dihapus, `MicManager` dan izin `RECORD_AUDIO`
      masih ada sampai WP-05 (CWG-N06; temuan QC-17 WP-03). `README.md`
      ditambahkan ke `File:` WP-04 oleh amendemen Planner.

Kriteria terima:
- `grep -rn "MicManager\|EqualizerView\|RECORD_AUDIO" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt app/src/main/res/layout/activity_main.xml`
  tidak menemukan hasil.
- `./gradlew assembleDebug` lulus.
- Jumlah `@+id/` pada layout berkurang tepat sebanyak dua view yang dihapus;
  angka sebelum dan sesudah dicatat pada Log WP.
- Bukti: keluaran grep, jumlah id, ringkasan build.

Risiko: penghapusan view menggeser tata letak panel kontrol.
Mitigasi: build dan inspeksi layout XML; uji tampilan perangkat masuk
MANUAL-VERIFY.

### Log WP-04 (Executor)

Berkas berubah (3, sesuai daftar WP termasuk amendemen Planner):
`app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`,
`app/src/main/res/layout/activity_main.xml`, `README.md`.

Checklist:

- [x] T-008 `MainActivity.kt` bersih dari audio/equalizer: field
      `equalizerView` dan `btnMicToggle` dihapus, `findViewById` keduanya
      dihapus, listener `btnMicToggle.setOnClickListener` (4 baris) dihapus,
      `equalizerView.setActive(...)` di `updateUI()` dihapus,
      `btnMicToggle.isEnabled` dan `btnMicToggle.setImageResource(...)` dengan
      `R.drawable.ic_mic`/`ic_mic_off` dihapus, dan `MicManager.stop()` di
      `onDestroy()` dihapus. Diff: +1/-17 baris.
- [x] T-009 `REQUIRED_PERMISSIONS` sekarang `arrayOf(Manifest.permission.CAMERA)`.
- [x] T-010 `activity_main.xml`: `ImageButton btnMicToggle` (9 baris) dan blok
      `dev.ndeso17.camwastegreen.EqualizerView` (7 baris, termasuk komentar
      "Right equalizer") dihapus. Jumlah `@+id/` **25 sebelum, 23 sesudah** —
      berkurang tepat dua.
- [x] T-087 `README.md:26` sekarang berbunyi: "Endpoint `/mic/toggle`, rute
      `/audio`, dan tombol mikrofon di UI **sudah dihapus**. `MicManager` dan
      izin `RECORD_AUDIO` masih ada", kolom "Ditutup oleh" = `WP-05`.

Output pemeriksaan (mentah):

```text
grep -n "MicManager\|EqualizerView\|equalizerView\|btnMicToggle\|RECORD_AUDIO\|ic_mic" MainActivity.kt
  -> NONE
grep -n "btnMicToggle\|EqualizerView\|ic_mic" activity_main.xml
  -> NONE
grep -c "@+id/" activity_main.xml  -> 23 (sebelum 25)
./gradlew assembleDebug testDebugUnitTest -> BUILD SUCCESSFUL in 28s
./gradlew lintDebug  -> BUILD FAILED; 1 error MicManager.kt:54 MissingPermission,
                        112 warning (baseline 111)
```

Kriteria terima terpenuhi: grep gabungan pada dua berkas kosong,
`assembleDebug` lulus, jumlah `@+id/` turun tepat dua.

Deviasi: tidak ada. Satu berkas tambahan (`README.md`) masuk daftar `File:`
melalui amendemen Planner `T-087`, bukan ditambahkan sendiri oleh Executor.

Keterbatasan:

- `MicManager.kt` dan `EqualizerView.kt` masih ada sebagai berkas; keduanya
  dihapus pada WP-05. Build hijau karena tidak ada lagi pemanggil.
- Lint masih gagal 1 error lama di `MicManager.kt:54`, ditutup WP-05. WP ini
  tidak mengklaim lint bersih.
- Jumlah warning naik 111 → 112 meski ada dua warning baru
  `UnusedResources` (`ic_mic.xml`, `ic_mic_off.xml`) karena satu warning
  `HardcodedText` `"Mic"` pada layout hilang bersama view-nya. Kedua drawable
  itu dihapus pada WP-05 sehingga warning `UnusedResources` ikut hilang.
  Rincian ini dicatat supaya kenaikan angka tidak disalahartikan sebagai
  regresi mutu.
- Tata letak panel kontrol berubah: tombol kamera dan kunci orientasi kini
  rata kiri tanpa tombol mikrofon di depannya, dan sisi kanan baris peek
  kosong. Tampilan belum diperiksa pada perangkat; masuk MANUAL-VERIFY.

### Log WP-04 (QC)

Verdict: **PASS_WITH_NOTES**. Laporan: `.work/evidence/QC-WP-04.md`. Commit
diperiksa: `1b7490e`.

Kriteria terima terpenuhi dan dibuktikan ulang oleh QC:

- `grep -rn "MicManager\|EqualizerView\|RECORD_AUDIO" MainActivity.kt activity_main.xml`
  → `NONE`.
- Jumlah `@+id/` pada layout: 25 (baseline `cfa7f24`) menjadi 23 — turun
  tepat dua view.
- `REQUIRED_PERMISSIONS` = `arrayOf(Manifest.permission.CAMERA)`.
- `contentDescription="Mic"` hilang bersama view tombol mikrofon.
- `./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL` (dijalankan
  QC sendiri).
- `./gradlew lintDebug` → `BUILD FAILED`, 1 error `MicManager.kt:54` + 112
  warning. Error lama di berkas yang dihapus WP-05; bukan regresi.
- Anti-ghost: kemunculan `MicManager|btnMicToggle|equalizerView` di
  `MainActivity.kt` 12 → 0; `@+id/` 25 → 23; `R.id.btnMicToggle`/
  `R.id.equalizerView` tidak dirujuk dari mana pun di `app/src`.
- Layout diparse ulang sebagai XML → `XML OK`.

Warning lint tidak diterima begitu saja. Baseline `cfa7f24` dibangun ulang di
pohon yang sama, `lintDebug` dijalankan dua kali, dan kedua daftar warning
dinormalisasi lalu dibandingkan. Hasil `diff` hanya tiga baris: +2
`UnusedResources` untuk `ic_mic.xml` dan `ic_mic_off.xml`, -1 `HardcodedText`
untuk `"Mic"`. Tidak ada warning lain yang muncul atau hilang. Setelah
perbandingan `app/src` dipulihkan ke `1b7490e` dan ketiga gerbang dijalankan
ulang.

Temuan (semua note, tidak menahan WP-05):

- QC-18: lint masih gagal 1 error lama dan 112 warning; ditutup WP-05.
- QC-19: `EqualizerView.kt` kini tanpa view pemanggil tetapi masih memanggil
  `MicManager.amplitude`; kode mati sementara, dihapus WP-05.
- QC-20: baris audio `README.md` akan kembali tidak akurat pada WP-05;
  pembaruan akhir sudah ada pada `T-085` WP-29.
- QC-21: `AGENTS.md:44,67` masih menyebut `/mic/toggle`; sudah tercakup
  `T-077` WP-29.

Catatan keamanan: P0-2 belum tuntas. Izin `RECORD_AUDIO` masih ada di
manifest dan kelas `MicManager` masih ada sampai WP-05. Yang sudah tertutup:
aplikasi tidak lagi meminta izin mikrofon saat mulai dan tidak ada lagi
tombol UI untuk menyalakan mikrofon.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-05 — Hapus `MicManager`, `EqualizerView`, izin audio, dan drawable mikrofon

Status: done

File: `app/src/main/java/dev/ndeso17/camwastegreen/MicManager.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/EqualizerView.kt` (hapus), `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_mic.xml` (hapus), `app/src/main/res/drawable/ic_mic_off.xml` (hapus)

Dependensi: WP-03, WP-04.

Tugas:
- [x] T-011 Hapus tiga berkas audio/equalizer (CWG-14).
- [x] T-012 Hapus `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
      dari manifest (CWG-14).

Kriteria terima:
- `./gradlew assembleDebug` dan `./gradlew testDebugUnitTest` lulus.
- `./gradlew lintDebug` tidak lagi melaporkan `MissingPermission` pada
  `MicManager.kt`; sisa error lint harus nol. Jumlah warning dicatat.
- `grep -rn "RECORD_AUDIO\|MicManager\|EqualizerView" app/src` tidak menemukan hasil.
- Bukti: keluaran `lintDebug` ringkas dan grep pada Log WP.

Risiko: drawable `ic_mic*` dipakai layout lain sehingga build gagal.
Mitigasi: jalankan `grep -rn "ic_mic" app/src/main/res` sebelum menghapus;
bila masih dipakai, hentikan WP dan catat temuan.

### Log WP-05 (Executor)

Berkas berubah (5, sesuai daftar WP):
`MicManager.kt` (hapus), `EqualizerView.kt` (hapus), `ic_mic.xml` (hapus),
`ic_mic_off.xml` (hapus), `AndroidManifest.xml`.

Checklist:

- [x] T-011 Empat berkas dihapus: `MicManager.kt` (kelas rekam audio),
      `EqualizerView.kt` (visualizer), serta drawable `ic_mic.xml` dan
      `ic_mic_off.xml`. Pemeriksaan pra-hapus `grep -rn "ic_mic" app/src` dan
      `grep -rn "EqualizerView" app/src` sudah kosong, sehingga tidak ada
      rujukan tersisa.
- [x] T-012 Baris `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
      dihapus dari `AndroidManifest.xml`. Manifest kini memuat sembilan izin:
      CAMERA, INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE,
      FOREGROUND_SERVICE, FOREGROUND_SERVICE_CAMERA, WAKE_LOCK,
      SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS.

Output pemeriksaan (mentah):

```text
grep -rn "RECORD_AUDIO\|MicManager\|EqualizerView" app/src   -> NONE
grep -rn "MicManager\|EqualizerView\|ic_mic\|RECORD_AUDIO" --include=*.kt --include=*.xml --include=*.kts .
                                                                  -> NONE (di luar app/build dan .work)
./gradlew assembleDebug testDebugUnitTest -> BUILD SUCCESSFUL in 35s
./gradlew lintDebug -> BUILD SUCCESSFUL in 33s; 0 error, 108 warning
```

Kriteria terima terpenuhi:

- `assembleDebug` dan `testDebugUnitTest` lulus.
- `lintDebug` **BUILD SUCCESSFUL** dengan **0 error** (sebelumnya 1 error
  `MicManager.kt:54` `MissingPermission`). Warning turun 112 → 108.
- `grep -rn "RECORD_AUDIO\|MicManager\|EqualizerView" app/src` → tanpa hasil.

Penurunan warning diperiksa lewat `diff` dua daftar warning yang dinormalisasi
(path dipangkas, nomor baris dibuang) antara WP-04 dan WP-05. Empat baris hilang
dan tidak ada yang baru:

```text
- EqualizerView.kt: Use the KTX extension function String.toColorInt [UseKtx]
- EqualizerView.kt: Use the KTX extension function String.toColorInt [UseKtx]
- ic_mic_off.xml: The resource R.drawable.ic_mic_off appears to be unused [UnusedResources]
- ic_mic.xml: The resource R.drawable.ic_mic appears to be unused [UnusedResources]
```

Keempat warning itu berasal dari berkas yang dihapus. Tidak ada warning baru.

Deviasi: tidak ada. Numbered file list sudah memuat lima berkas; `README.md`
tidak disentuh karena WP-32 menghapus tabel keadaan, sehingga tidak ada baris
README yang perlu diperbarui (temuan QC-20 gugur).

Keterbatasan:

- `AGENTS.md:44,67` dan `.work/Structure.md` masih menyebut `MicManager.kt`,
  `EqualizerView.kt`, `ic_mic*`, dan izin `RECORD_AUDIO`. Itu tugas `T-077`
  dan `T-078` WP-29.
- Tidak ada pengukuran perangkat pada WP ini. `MANUAL-VERIFY` untuk audio
  (layar tidak meminta izin mikrofon, aplikasi tetap berjalan tanpa mikrofon)
  tetap belum diuji.
- Warning lint yang tersisa 108 seluruhnya non-blocking; daftar lengkap ada
  pada `lint-results-debug.txt`.

### Log WP-05 (QC)

Verdict: **PASS**. Laporan: `.work/evidence/QC-WP-05.md`. Commit diperiksa:
`800886e`.

QC menjalankan `./gradlew clean` lebih dulu, lalu ketiga gerbang sekaligus:

```text
./gradlew clean
./gradlew assembleDebug testDebugUnitTest lintDebug --console=plain
  -> BUILD SUCCESSFUL in 1m 18s, 55 task executed (bukan up-to-date)
lint-results-debug.txt -> 0 error, 108 warning
test-results/testDebugUnitTest/*.xml -> 1 test, 0 failure, 0 error
```

Ini gerbang mutu pertama yang **hijau penuh** pada pipeline ini: error
`MicManager.kt:54` `MissingPermission` yang bertahan sejak baseline `c2bd177`
hilang bersama berkasnya, dan `lintDebug` kini `BUILD SUCCESSFUL`.

Kriteria terpenuhi dan dibuktikan ulang:

- Empat berkas terhapus: `MicManager.kt`, `EqualizerView.kt`, `ic_mic.xml`,
  `ic_mic_off.xml` (dicek `test -f` → tidak ada).
- Manifest sumber memuat sembilan izin tanpa `RECORD_AUDIO`; **merged
  manifest hasil build** juga 0 kemunculan `RECORD_AUDIO`, sehingga izin itu
  benar-benar tidak ada pada APK, bukan hanya pada berkas sumber.
- `grep -rn "RECORD_AUDIO\|MicManager\|EqualizerView" app/src` → tanpa hasil.
- `grep -rn "AudioRecord\|MediaRecorder\|audio/aac\|AudioFormat" app/src` →
  tanpa hasil.
- Warning turun 112 → 108. `diff` daftar warning WP-04 vs WP-05 hanya memuat
  empat baris hilang (2× `UseKtx` di `EqualizerView.kt`, 2× `UnusedResources`
  `ic_mic*`); tidak ada warning baru. `diff` daftar clean build vs inkremental
  → `IDENTICAL`, jadi angka 108 bukan artefak cache.

Temuan note (tidak menahan WP-06):

- QC-24: `AGENTS.md:44,67` dan `.work/Structure.md:26,29,51,59` masih menyebut
  berkas audio yang sudah dihapus. Sudah dijadwalkan `T-077`/`T-078` WP-29.
- QC-25: 108 warning sisa tidak blocker; baseline baru dicatat WP-28.

Keamanan: P0-2 pada sisi aplikasi tertutup — tidak ada izin, kelas, tombol,
maupun endpoint mikrofon. P0-3 (tanpa autentikasi, cleartext) tetap terbuka
sampai WP-25..WP-27 dan tidak diklaim tertutup di sini.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-06 — Transformasi NV21 murni untuk rotasi frame

Status: done

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/Nv21Transform.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/Nv21TransformTest.kt` (baru)

Dependensi: WP-05.

Tugas:
- [x] T-013 Implementasikan `rotateNv21(yuv: ByteArray, width: Int, height: Int, degrees: Int): ByteArray`
      untuk 0/90/180/270 derajat, Kotlin murni tanpa `android.*`, dengan
      penanganan dimensi ganjil untuk bidang chroma (CWG-06, CWG-N01).
- [x] T-014 Tulis unit test: identitas pada 0 derajat, hasil 90 dan 270 saling
      membalik, 180 dua kali kembali ke asal, dan penolakan derajat tidak sah
      (CWG-N06).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.Nv21TransformTest"`
  lulus dengan minimal empat kasus.
- Test gagal bila implementasi rotasi salah: verifikasi dengan sengaja
  membalik satu indeks, mencatat kegagalan, lalu mengembalikannya.
- Berkas tidak mengimpor `android.*`: `grep -n "import android" .../Nv21Transform.kt` kosong.
- Bukti: keluaran test pada Log WP.

Risiko: rotasi bidang chroma salah pada dimensi ganjil sehingga warna bergeser.
Mitigasi: test khusus lebar/tinggi ganjil; hasil visual tetap MANUAL-VERIFY.

### Log WP-06 (Executor)

Berkas berubah (2, sesuai daftar WP):
`app/src/main/java/dev/ndeso17/camwastegreen/stream/Nv21Transform.kt` (baru),
`app/src/test/java/dev/ndeso17/camwastegreen/stream/Nv21TransformTest.kt` (baru).

Checklist:

- [x] T-013 `Nv21Transform` dibuat sebagai `object` Kotlin murni di paket
      `dev.ndeso17.camwastegreen.stream`. Isinya `rotateNv21(yuv, width,
      height, degrees)` untuk 0/90/180/270 derajat searah jarum jam, plus
      `chromaWidth`, `chromaHeight`, dan `nv21Size` sebagai helper.
      Permukaan API:
      `fun rotateNv21(yuv: ByteArray, width: Int, height: Int, degrees: Int): ByteArray`.
      Luma dipetakan per piksel lewat `mapLuma`; chroma dipetakan per sel kisi
      lewat `mapChroma` sehingga pasangan V,U tidak pernah terpisah. Sudut di luar
      0/90/180/270, dimensi non-positif, dan buffer yang lebih kecil dari
      `nv21Size` semuanya ditolak dengan `IllegalArgumentException`.
      Dimensi ganjil ditangani dengan membulatkan kisi chroma ke atas
      (`(w+1)/2` x `(h+1)/2`), dan ukuran hasil tetap sama dengan ukuran
      masukan.
      Verifikasi kemurnian: berkas **tidak punya satu pun baris `import`**
      (`grep -n "^import"` → tanpa hasil), sehingga `android.*` mustahil
      masuk.
- [x] T-014 `Nv21TransformTest` dibuat dengan 10 kasus: identitas pada 0
      derajat (termasuk pemeriksaan hasil bukan objek yang sama), pemetaan luma
      90 derajat pada matriks 3x2, 90 lalu 270 kembali ke asal pada dimensi
      ganjil 5x3, 180 dua kali kembali ke asal pada 5x3, 180 membalik kedua
      sumbu, pasangan chroma tetap berpasangan pada 90 derajat, pembulatan
      kisi chroma untuk 5x3, penolakan sudut tidak sah (45, 360, -90, 1),
      penolakan dimensi tidak sah, dan penolakan buffer terlalu kecil.

Output pemeriksaan (mentah):

```text
./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.Nv21TransformTest"
  -> BUILD SUCCESSFUL; 10 test, 0 failure, 0 error
     (app/build/test-results/testDebugUnitTest/TEST-dev.ndeso17.camwastegreen.stream.Nv21TransformTest.xml)
grep -n "import android" app/src/main/java/dev/ndeso17/camwastegreen/stream/Nv21Transform.kt
  -> tanpa hasil
grep -n "^import" app/src/main/java/dev/ndeso17/camwastegreen/stream/Nv21Transform.kt
  -> tanpa hasil
./gradlew assembleDebug testDebugUnitTest -> BUILD SUCCESSFUL in 11s
./gradlew lintDebug                       -> BUILD SUCCESSFUL in 24s; 0 error, 108 warning
```

Uji diskriminasi (mutasi sengaja): indeks luma untuk 90 derajat diubah dari
`Pair(height - 1 - iy, ix)` menjadi `Pair(height - 1 - iy, width - ix)`, lalu
test dijalankan ulang:

```text
-> 10 tests completed, 2 failed
   rotateNinety_thenTwoSeventy_returnsOriginalOnOddDimensions FAILED
   rotateNinety_mapsLumaClockwiseAndSwapsDimensions FAILED
   BUILD FAILED
```

Mutasi dikembalikan dari salinan cadangan, `grep` mengonfirmasi baris kembali
menjadi `90 -> Pair(height - 1 - iy, ix)`, dan test kembali
`BUILD SUCCESSFUL`. Jadi test benar-benar menangkap rotasi salah, bukan
sekadar menyatakan green.

Catatan: satu kasus chroma sempat gagal pada percobaan pertama karena nilai
harapan dihitung manual dengan urutan sel yang keliru. Implementasi sudah
benar (pemetaan chroma mengikuti pemetaan luma yang sama); yang salah nilai
harapan di test. Nilai harapan dikoreksi menjadi urutan sel `p2,p0,p3,p1`
dan test hijau tanpa perubahan pada `Nv21Transform.kt`. Deviasi ini dicatat
agar tidak tertukar dengan perbaikan kode.

Kriteria terima terpenuhi: test targeted lulus dengan 10 kasus (melebihi
minimal empat), mutasi terbukti terdeteksi, berkas bebas `import android`,
build dan lint hijau.

Deviasi: tidak ada. Dua berkas, satu WP.

Keterbatasan:

- `Nv21Transform` belum dipakai jalur sensor mana pun. Tersambung ke
  `FilterPipeline` pada WP-14 (enkode JPEG tunggal) dan ke foreground service
  pada WP-17/WP-18.
- Kebenaran visual rotasi pada dimensi ganjil (pergeseran warna) belum dapat
  dibuktikan tanpa perangkat; `MANUAL-VERIFY`.
- Belum ada pengukuran FPS, latensi, atau bandwidth; WP ini tidak menambah
  angka baru.

### Log WP-06 (QC)

Verdict: **PASS**. Laporan: `.work/evidence/QC-WP-06.md`. Commit diperiksa:
`9f665a5`.

QC menjalankan `./gradlew clean` lebih dulu, lalu test targeted:

```text
./gradlew clean
./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.Nv21TransformTest"
  -> BUILD SUCCESSFUL in 31s, 27 task executed
     Nv21TransformTest: 10 test, 0 failure, 0 error
./gradlew testDebugUnitTest assembleDebug -> BUILD SUCCESSFUL in 26s
./gradlew lintDebug -> BUILD SUCCESSFUL; 0 error, 108 warning
diff daftar warning WP-05 vs WP-06 -> IDENTICAL
```

Verifikasi independen. Test dan implementasi ditulis pihak yang sama, jadi
lulusnya test hanya membuktikan keduanya sepakat. QC menulis oracle Python
mandiri (`/tmp/opencode/qc-oracle-nv21.py`) yang membangun bidang luma dan
kisi chroma sendiri serta merotasi bidang 2D dengan operasi matriks biasa,
lalu membandingkan byte per byte dengan dump keluaran Kotlin. Hasil:
20 kombinasi (3x2, 5x3, 4x4, 7x5, 2x2 x sudut 0/90/180/270) **0 mismatch**.
Berkas test sementara `QcOracleDumpTest` dihapus dan tidak masuk commit.

Mutasi kedua oleh QC, pada lapis chroma (`90 -> Pair(ch - 1 - cy, cx)`
diubah `Pair(cy, cx)`): 2 test FAILED, `BUILD FAILED`. Jadi test menangkap
kesalahan pada luma maupun chroma. Mutasi dipulihkan dari cadangan dan suite
kembali hijau.

Anti-ghost: tanpa mutasi 10 test lulus; dengan mutasi 2 gagal. `grep -rn
"Nv21Transform" app/src/main` hanya menemukan berkas kelas itu sendiri.

Temuan note (tidak menahan WP-07):

- QC-26: `Nv21Transform` belum dipakai jalur sensor mana pun; penyambungan
  ke `FilterPipeline` ada pada WP-14 dan ke foreground service pada
  WP-17/WP-18. WP ini belum mengubah perilaku aplikasi.
- QC-27: kebenaran visual rotasi pada dimensi ganjil hanya bisa dinilai dari
  frame nyata; `MANUAL-VERIFY`.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-07 — `FrameEncoder` dengan satu kali enkode JPEG

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt` (baru)

Dependensi: WP-06.

Tugas:
- [x] T-015 Implementasikan `FrameEncoder.encode(image: ImageProxy, quality: Int): EncodedFrame?`
      yang mengubah YUV_420_888 ke NV21, merotasi memakai `Nv21Transform`
      sesuai `image.imageInfo.rotationDegrees`, lalu memanggil
      `YuvImage.compressToJpeg` **tepat satu kali** (CWG-N01, CWG-06).
- [x] T-016 Kembalikan lebar dan tinggi hasil rotasi bersama byte JPEG agar
      metadata frame konsisten dengan piksel (CWG-05).

Kriteria terima:
- `grep -c "compressToJpeg" app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt`
  bernilai `1`.
- Tidak ada `BitmapFactory.decodeByteArray` maupun `Bitmap.compress` di berkas.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: `YuvImage` menolak NV21 dengan dimensi hasil rotasi yang tidak cocok.
Mitigasi: WP-06 sudah menguji dimensi; kegagalan runtime dilaporkan sebagai
`cameraState` alasan pada WP-10, bukan exception yang menelan proses.

### Log WP-07 (Executor)

Berkas berubah (1, sesuai daftar WP): `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt` (baru).

Checklist:

- [x] T-015 `FrameEncoder.encode(image, quality)` mengubah YUV_420_888 ke NV21
      lewat `toNv21`, memutar dengan `Nv21Transform.rotateNv21` sesuai
      `image.imageInfo.rotationDegrees`, lalu memanggil
      `YuvImage.compressToJpeg` satu kali. Rotasi 0 derajat tidak menyalin
      buffer. Kegagalan mengembalikan `null`, bukan melempar exception.
- [x] T-016 `EncodedFrame(jpeg, width, height)` membawa dimensi hasil rotasi:
      `90`/`270` menukar lebar dan tinggi, `0`/`180` mempertahankan.
      `toNv21` membaca `rowStride` dan `pixelStride` tiap bidang serta
      `buffer.position()` sebagai basis, sehingga jarak baris tidak diasumsikan
      sama dengan lebar.

Output pemeriksaan (mentah):

```text
grep -c "compressToJpeg" .../stream/FrameEncoder.kt   -> 1
grep -c "BitmapFactory\|Bitmap.compress\|Bitmap.createBitmap" ...   -> 0
./gradlew assembleDebug testDebugUnitTest lintDebug   -> BUILD SUCCESSFUL, exit 0
```

Catatan proses: percobaan pertama `grep -c` bernilai `3` karena nama fungsi
juga muncul di komentar dan pesan log. Komentar dan pesan diubah agar hanya
pemanggilan sebenarnya yang tersisa; nilai akhir `1` sesuai kriteria.

Kriteria terima terpenuhi: enkode tunggal, tanpa bitmap perantara, build lulus.

Deviasi: tidak ada. Satu berkas, satu WP.

Keterbatasan:

- `FrameEncoder` belum dipakai jalur sensor; penyambungan ada di WP-14.
  Sampai WP-14 selesai, berkas ini hanya ikut terkompilasi.
- Pembacaan `rowStride` belum diuji dengan buffer ber-`rowStride` tidak rata.
  Uji tersebut butuh `ImageProxy` nyata sehingga masuk MANUAL-VERIFY perangkat.
- Angka FPS/latensi tidak diklaim.

### Log WP-07 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya sendiri,
bukan QC independen)**. Status tetap `qc` sampai QC independen memutuskan
`done`. Pemeriksaan dilakukan pada working tree sebelum commit.

- `grep -c "compressToJpeg" .../FrameEncoder.kt` → `1` (diulang QC sendiri).
- `grep -c "BitmapFactory\|Bitmap.compress\|Bitmap.createBitmap"` → `0`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0` (dijalankan QC sendiri).
- Isi berkas diperiksa, bukan hanya grep: rotasi terjadi **sebelum** enkode
  (baris `Nv21Transform.rotateNv21` mendahului `compressToJpeg`), dimensi
  keluaran mengikuti pertukaran 90/270, dan `nv21Size` dari WP-06 dipakai
  sebagai ukuran buffer sehingga panjang buffer dan dimensi konsisten.
- Kasus tepi: `image.width`/`height` ≤ 0 mengembalikan `null`; `compressToJpeg`
  yang mengembalikan `false` juga `null`; tiga jenis exception ditangkap
  terpisah.

Temuan (note, tidak menahan):

- QC-27: `toNv21` tidak memakai `cropRect`. Bila suatu saat analyzer memakai
  crop, dimensi dan isi buffer akan berbeda. Belum relevan karena WP-14 tidak
  memakai crop; dicatat untuk WP-14.

Dampak ke WP berikutnya: WP-08 dapat memakai `EncodedFrame.width/height`
langsung sebagai dimensi `FramePacket`.

---

## WP-08 — `FramePacket` dan `FrameSource` dengan nomor urut serta waktu tangkap

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/FramePacket.kt` (baru), `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameSource.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/FrameSourceTest.kt` (baru)

Dependensi: WP-07.

Tugas:
- [x] T-017 Buat `FramePacket(seq: Long, capturedAtMs: Long, width: Int, height: Int, jpeg: ByteArray)`
      sebagai data class murni (CWG-02, CWG-N04).
- [x] T-018 Buat `FrameSource` dengan `publish(jpeg, width, height, capturedAtMs): FramePacket`,
      penghitung `seq` monoton yang dimulai dari 1, dan `latest: FramePacket?`;
      waktu tangkap berasal dari pemanggil, bukan dari jam di dalam `FrameSource`
      (CWG-02, CWG-03).
- [x] T-019 Unit test: `seq` naik satu tiap `publish`, `latest` menunjuk paket
      terakhir, `lastFrameAtMs` mengikuti `capturedAtMs`, dan `publish` dari dua
      thread tidak menghasilkan `seq` duplikat (CWG-03, CWG-N04).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.FrameSourceTest"`
  lulus dengan minimal empat kasus, termasuk kasus dua thread.
- `FrameSource` tidak mengimpor `android.*` maupun `System.currentTimeMillis`.
- Bukti: keluaran test pada Log WP.

Risiko: `seq` melompat karena race antar thread analyzer.
Mitigasi: sinkronisasi pada `publish`; test dua thread wajib ada dan harus
gagal bila sinkronisasi dihapus.

### Log WP-08 (Executor)

Berkas berubah (3, sesuai daftar WP): `stream/FramePacket.kt` (baru),
`stream/FrameSource.kt` (baru), `app/src/test/.../stream/FrameSourceTest.kt` (baru).

Checklist:

- [x] T-017 `FramePacket(seq, capturedAtMs, width, height, jpeg)` data class
      murni tanpa `android.*`.
- [x] T-018 `FrameSource.publish(jpeg, width, height, capturedAtMs)` memberi
      nomor urut mulai dari 1 dan monoton, menyimpan `latest`, `lastSeq`,
      `lastFrameAtMs`. Jam tidak dibaca di dalam kelas: waktu tangkap selalu
      dari pemanggil. `publish` diberi `@Synchronized`; `latest` `@Volatile`.
- [x] T-019 Delapan kasus test: `seq` naik satu tiap publish, `latest` menunjuk
      paket terakhir, `lastFrameAtMs` mengikuti `capturedAtMs`, belum ada frame
      berarti `null`, publish dari 8 thread × 2000 iterasi tidak menghasilkan
      `seq` duplikat, 1000 publish berurutan tidak mengulang `seq`, dimensi
      tidak sah ditolak, `clear` tidak mengulang nomor urut.

Output pemeriksaan (mentah):

```text
./gradlew testDebugUnitTest --tests "...FrameSourceTest"  -> BUILD SUCCESSFUL, exit 0
tests="8" skipped="0" failures="0"
grep -c "import android\|System.currentTimeMillis" FrameSource.kt FramePacket.kt -> 0, 0
./gradlew assembleDebug testDebugUnitTest lintDebug       -> BUILD SUCCESSFUL, exit 0
```

Bukti mutasi (mutasi harus menggagalkan test):

```text
hapus "@Synchronized" pada publish, jalankan test
  percobaan 1 -> exit 1 (gagal)
  percobaan 2 -> exit 1 (gagal)
  percobaan 3 -> exit 1 (gagal)
berkas dikembalikan; test kembali lulus
```

Catatan proses: mutasi pertama memakai 2 thread × 500 iterasi dan **tidak**
menggagalkan test, jadi bukti tersebut tidak sah. Beban dinaikkan ke
8 thread × 2000 iterasi; setelah itu mutasi gagal tiga kali berturut-turut.
Berkas yang dimutasi dikembalikan dari salinan, diverifikasi `grep
"@Synchronized"` masih menemukan dua pemakaian.

Kriteria terima terpenuhi: delapan kasus lulus termasuk kasus banyak thread,
tidak ada `android.*` maupun jam internal.

Deviasi: `FrameSource` menambah `clear()` dan `lastSeq` di luar daftar tugas.
Alasan: `clear()` dibutuhkan saat kamera berhenti agar `/status` tidak
melaporkan frame lama sebagai frame terakhir; `lastSeq` dipakai WP-10 dan
WP-11. Keduanya tidak menambah dependensi baru.

Keterbatasan:

- `publish` menolak dimensi tidak sah dengan exception; pemanggil di jalur
  sensor harus memastikan dimensi positif (dicek WP-07).
- Bukti keunikan `seq` berasal dari JVM laptop, bukan perangkat target.
  Perilaku thread di ART pada perangkat masuk MANUAL-VERIFY.

### Log WP-08 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc` sampai QC independen
memutuskan `done`.

- `./gradlew testDebugUnitTest --tests "...FrameSourceTest"` → `BUILD
  SUCCESSFUL`, exit `0`; `tests="8" skipped="0" failures="0"` (dijalankan QC
  sendiri).
- `grep -c "import android\|System.currentTimeMillis"` → `0` untuk
  `FrameSource.kt` dan `FramePacket.kt`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Isi berkas diperiksa, bukan hanya grep: `seq` dimulai dari 1
  (`nextSeq = 1L`), `publish` mengembalikan paket yang sama dengan yang
  disimpan, `latest`/`lastSeq`/`lastFrameAtMs` membaca satu sumber state.
- Bukti mutasi diulang QC sendiri: tanpa `@Synchronized` test gagal (3 dari 3
  percobaan), dengan `@Synchronized` lulus.

Temuan (note, tidak menahan):

- QC-28: `FrameSource.publish` melempar `IllegalArgumentException` untuk
  dimensi tidak sah. Kalau analyzer suatu saat memanggil dengan dimensi 0,
  exception akan menggagalkan frame. Saat ini pemanggil belum ada
  (WP-14), jadi cukup dicatat; WP-14 sebaiknya memvalidasi lebih dulu.

Dampak ke WP berikutnya: WP-09 dapat memakai `FramePacket` apa adanya; WP-10
dan WP-11 dapat memakai `FrameSource.latest` serta `lastSeq`.

---

## WP-09 — `MultipartWriter` untuk header part MJPEG

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MultipartWriter.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/MultipartWriterTest.kt` (baru)

Dependensi: WP-08.

Tugas:
- [x] T-020 Implementasikan `partHeader(packet: FramePacket, boundary: String): ByteArray`
      yang menghasilkan blok header dengan `Content-Type: image/jpeg`,
      `Content-Length`, `X-Frame-Seq`, `X-Frame-Captured-At-Ms`,
      `X-Frame-Width`, `X-Frame-Height`, diakhiri CRLF ganda (CWG-02).
- [x] T-021 Implementasikan `part(packet, boundary): ByteArray` = header + JPEG +
      CRLF penutup, tanpa menyalin JPEG dua kali (CWG-N01).
- [x] T-022 Unit test: enam header wajib ada, nilai `Content-Length` sama dengan
      `packet.jpeg.size`, `X-Frame-Seq` sama dengan `packet.seq`, pemisah CRLF
      benar, dan boundary dipakai apa adanya (CWG-02, CWG-N06).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.MultipartWriterTest"`
  lulus dengan minimal lima asersi.
- Test gagal bila satu header dihapus: verifikasi dengan menghapus
  `X-Frame-Width`, mencatat kegagalan, lalu mengembalikannya.
- Bukti: keluaran test dan contoh header tercetak pada Log WP.

Risiko: kesalahan CRLF membuat klien MJPEG berhenti membaca part.
Mitigasi: test memeriksa urutan CRLF; verifikasi klien nyata masuk MANUAL-VERIFY.

### Log WP-09 (Executor)

Berkas berubah (2, sesuai daftar WP): `stream/MultipartWriter.kt` (baru),
`app/src/test/.../stream/MultipartWriterTest.kt` (baru).

Checklist:

- [x] T-020 `partHeader(packet, boundary)` menghasilkan blok header berisi
      `Content-Type: image/jpeg`, `Content-Length`, `X-Frame-Seq`,
      `X-Frame-Captured-At-Ms`, `X-Frame-Width`, `X-Frame-Height`, diakhiri
      CRLF ganda. Header dikodekan `US_ASCII` karena hanya berisi ASCII.
- [x] T-021 `part(packet, boundary)` = header + JPEG + CRLF penutup, disusun
      dalam satu alokasi `ByteArray` dengan `System.arraycopy`. JPEG disalin
      satu kali; tidak ada `ByteArrayOutputStream` dan tidak ada enkode ulang.
- [x] T-022 Tujuh kasus test: enam header wajib ada, header diawali boundary
      dan diakhiri CRLF ganda, `Content-Length` sama dengan `jpeg.size`,
      boundary dipakai apa adanya untuk nama lain, `part` menggabungkan header
      + JPEG + CRLF penutup dengan offset benar, `seq`/dimensi berubah hanya
      mengubah header, dan `capturedAtMs = 0` tetap ditulis.

Output pemeriksaan (mentah):

```text
./gradlew testDebugUnitTest --tests "...MultipartWriterTest"
  -> exit 0, tests="7" skipped="0" failures="0"
./gradlew assembleDebug testDebugUnitTest lintDebug -> BUILD SUCCESSFUL, exit 0
```

Mutasi wajib dari kriteria terima (hapus `X-Frame-Width`):

```text
sed -i '/X-Frame-Width/d' MultipartWriter.kt
  -> exit 1, MultipartWriterTest > enamHeaderWajibAda FAILED
  -> MultipartWriterTest > seqBerubahMengubahHeaderTanpaMenyentuhJpeg FAILED
berkas dikembalikan; grep -c "X-Frame-Width" -> 1
```

Contoh header tercetak (CRLF ditulis `<CRLF>`):

```text
--cwg<CRLF>
Content-Type: image/jpeg<CRLF>
Content-Length: 5<CRLF>
X-Frame-Seq: 7<CRLF>
X-Frame-Captured-At-Ms: 1700000000000<CRLF>
X-Frame-Width: 1280<CRLF>
X-Frame-Height: 720<CRLF>
<CRLF>
```

Contoh di atas dihasilkan dari test sementara yang mencetak header ke berkas.
Test sementara itu sudah dihapus dan tidak ikut commit; `git status` sebelum
commit hanya memuat dua berkas WP.

Kriteria terima terpenuhi: tujuh kasus lulus (lebih dari lima asersi), mutasi
`X-Frame-Width` menggagalkan test, contoh header tercatat.

Deviasi: tidak ada. Dua berkas, satu WP.

Keterbatasan:

- Batas part tidak dipotong ke panjang baris tertentu dan tidak diberi tanda
  kutip. Boundary yang dipakai server aman karena hanya berisi huruf dan tanda
  hubung, tetapi karakter aneh pada boundary tidak ditolak. Boundary berasal
  dari konstanta server (WP-12), bukan masukan pengguna.
- Kecocokan klien nyata (VLC, browser, backend) belum diuji; masuk MANUAL-VERIFY.

### Log WP-09 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc` sampai QC independen
memutuskan `done`.

- `./gradlew testDebugUnitTest --tests "...MultipartWriterTest"` → exit `0`,
  `tests="7" skipped="0" failures="0"`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Mutasi diulang QC sendiri: menghapus baris `X-Frame-Width` menggagalkan dua
  test; berkas dikembalikan dan `grep -c "X-Frame-Width"` bernilai `1`.
- Isi berkas diperiksa: urutan penulisan header (`--boundary` lebih dulu),
  CRLF ganda hanya di akhir blok header, dan `part` memakai offset
  `header.size`/`header.size + body.size` yang sama dengan panjang total.
- `grep -c "compressToJpeg" MultipartWriter.kt` → `0`, jadi tidak ada enkode
  ulang pada jalur penulisan part.

Temuan (note, tidak menahan):

- QC-29: `part` menggandakan ukuran JPEG di memori selama proses tulis
  (satu salinan header + JPEG). Untuk 1280x720 kualitas 80 salinan ini masih
  wajar, tetapi jalur server (WP-12) sebaiknya menulis header dan JPEG
  terpisah agar tidak ada salinan tambahan per frame. Dicatat untuk WP-12.

Dampak ke WP berikutnya: WP-12 dapat menulis `partHeader` lalu `packet.jpeg`
langsung ke keluaran, memakai temuan QC-29 sebagai panduan.

---

## WP-10 — `StatusRegistry` untuk status terukur

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/StatusRegistry.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/StatusRegistryTest.kt` (baru)

Dependensi: WP-08.

Tugas:
- [x] T-023 Implementasikan `StatusRegistry` dengan sumber waktu yang disuntikkan
      (`nowMs: () -> Long`), pencatatan frame untuk FPS terukur pada jendela
      bergulir, serta status kamera berupa `cameraState` dan `cameraStateReason`
      (CWG-04, CWG-13).
- [x] T-024 Sediakan `snapshot(): StatusSnapshot` berisi `frameSeq`,
      `lastFrameAtMs`, `measuredFps`, `width`, `height`, `clients`,
      `cameraState`; nilai yang belum diketahui bernilai `null`, bukan `0`
      (CWG-04, CWG-05, CWG-N05).
- [x] T-025 Unit test: FPS terukur dari 11 frame pada jarak 100 ms dalam jendela
      1000 ms, FPS `null` saat belum ada frame, `frameSeq`/`lastFrameAtMs`
      mengikuti paket terakhir, dan `cameraState` berubah saat kegagalan dicatat
      (CWG-04, CWG-16).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.StatusRegistryTest"`
  lulus dengan minimal empat kasus.
- Nilai FPS berasal dari penghitung frame, bukan dari field konfigurasi:
  `grep -n "targetFps" .../StatusRegistry.kt` kosong.
- Bukti: keluaran test pada Log WP.

Risiko: jendela FPS terlalu pendek sehingga nilai berisik.
Mitigasi: jendela 1000 ms dan test berbasis waktu palsu; nilai nyata diukur
pada perangkat dan dicatat MANUAL-VERIFY, bukan dikarang.

### Log WP-10 (Executor)

Berkas berubah (2, sesuai daftar WP): `stream/StatusRegistry.kt` (baru),
`app/src/test/.../stream/StatusRegistryTest.kt` (baru).

Checklist:

- [x] T-023 `StatusRegistry(nowMs: () -> Long, windowMs: Long = 1000L)` menerima
      sumber waktu yang disuntikkan. `onFrame(packet)` menyimpan paket terakhir
      dan mencatat waktu frame pada deque bergulir; frame yang lebih tua dari
      jendela dibuang. `setCameraState(state, reason)` dan `setClients(count)`
      tersedia untuk status kamera dan jumlah klien.
- [x] T-024 `snapshot()` mengembalikan `StatusSnapshot` berisi `frameSeq`,
      `lastFrameAtMs`, `measuredFps`, `width`, `height`, `clients`,
      `cameraState`, `cameraStateReason`. Nilai yang belum diketahui `null`
      (bukan `0`): belum ada frame berarti `measuredFps`, `frameSeq`,
      `lastFrameAtMs`, `width`, `height` semuanya `null`. `clients` tetap
      bilangan bulat karena selalu diketahui.
- [x] T-025 Delapan kasus test: FPS terukur dari 11 frame jarak 100 ms,
      FPS `null` sebelum ada frame, FPS `0.0` setelah jendela kosong,
      `frameSeq`/`lastFrameAtMs`/dimensi mengikuti paket terakhir,
      `cameraState` berubah dan alasannya tersimpan, `clients` mengikuti,
      jendela 500 ms membuang frame lama, `resetFrames` menghapus FPS tetapi
      mempertahankan `cameraState`.

Output pemeriksaan (mentah):

```text
./gradlew testDebugUnitTest --tests "...StatusRegistryTest"
  -> exit 0, tests="8" skipped="0" failures="0"
grep -n "targetFps" .../stream/StatusRegistry.kt   -> tanpa hasil
./gradlew assembleDebug testDebugUnitTest lintDebug -> BUILD SUCCESSFUL, exit 0
```

Mutasi: FPS diganti konstanta `20.0`.

```text
  -> exit 1, fpsTerukurDari11FrameJarak100ms FAILED
  -> exit 1, jendelaBergulirMembuangFrameLama FAILED
berkas dikembalikan; grep -c "frameTimes.size - 1" -> 1
```

Oracle independen (Python, definisi jendela sama, dihitung terpisah dari
kode Kotlin):

```text
kasus1 (11 frame/100ms, window 1000) -> 10.0   (test: 10.0)
kasus2 (4 frame/100ms, window 500)   -> 10.0   (test: 10.0)
kasus2 setelah 2000ms                -> 0.0    (test: 0.0)
kasus3 (21 frame/50ms, window 1000)  -> 20.0   (kontrol definisi)
```

Kriteria terima terpenuhi: delapan kasus lulus, `targetFps` tidak muncul di
berkas, FPS berasal dari penghitung frame.

Deviasi: `StatusRegistry` menambah `resetFrames()` di luar daftar tugas.
Alasan: WP-18 membutuhkannya saat kamera berhenti agar `/status` tidak
melaporkan frame lama. Tidak menambah dependensi.

Keterbatasan:

- FPS memakai waktu tangkap paket, bukan waktu receipt server. Untuk stream
  lokal selisihnya kecil, tetapi pada tautan lambat nilai FPS dapat lebih
  tinggi daripada FPS pengiriman. Tidak ada ambang yang diklaim.
- `measuredFps` dihitung dari `(n-1)/span`, jadi jendela dengan satu frame
  menghasilkan `0.0`, bukan `null`. Keduanya berarti "tidak dapat diukur"
  tetapi dibedakan dari belum ada frame.
- Nilai FPS nyata pada perangkat belum diukur; masuk MANUAL-VERIFY.

### Log WP-10 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc` sampai QC independen
memutuskan `done`.

- `./gradlew testDebugUnitTest --tests "...StatusRegistryTest"` → exit `0`,
  `tests="8" skipped="0" failures="0"`.
- `grep -n "targetFps" .../StatusRegistry.kt` → tanpa hasil.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Mutasi diulang QC sendiri: FPS diganti konstanta `20.0` menggagalkan dua
  test; berkas dikembalikan.
- Oracle independen dihitung ulang QC sendiri dengan hasil identik dengan
  nilai test (10.0, 10.0, 0.0).
- Isi berkas diperiksa: `measuredFps` tidak pernah membaca field konfigurasi
  apa pun; seluruh nilai berasal dari `frameTimes` dan `lastPacket`; nilai
  `null` dipakai untuk "belum terukur" dan `0.0` untuk "terukur nol".

Temuan (note, tidak menahan):

- QC-30: `cameraState` dimulai `"starting"` dan tidak pernah kembali ke
  `"starting"` setelah berhenti; keadaan `"stopped"` harus di-set pemanggil
  (WP-18). Perlu dipastikan pemanggil selalu mengaturnya, jika tidak
  `/status` akan melaporkan keadaan basi.
- QC-31: FPS memakai waktu tangkap, sehingga pada tautan lambat nilai dapat
  melebihi FPS pengiriman. Tidak ada ambang yang diklaim, jadi tidak menahan
  WP; dicatat agar backend tidak memperlakukannya sebagai throughput.

Dampak ke WP berikutnya: WP-12 dapat memakai `snapshot()` untuk `/status`;
WP-18 wajib meng-set `cameraState` pada saat kamera berhenti.

---

## WP-11 — `StreamSession` untuk dedupe, batas klien, dan batas waktu tulis

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamSession.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/StreamSessionTest.kt` (baru)

Dependensi: WP-08, WP-10.

Tugas:
- [x] T-026 Implementasikan penjaga dedupe: `shouldSend(packet): Boolean` yang
      menolak paket dengan `seq` sama atau lebih kecil daripada `lastSentSeq`
      (CWG-03).
- [x] T-027 Implementasikan batas klien: `tryAcquire(): Boolean` dan `release()`,
      menolak ketika jumlah aktif mencapai `maxClients` (CWG-10).
- [x] T-028 Implementasikan konfigurasi batas waktu tulis dan penanda klien
      lambat sebagai nilai yang dapat diperiksa, tanpa bergantung pada `android.*`
      (CWG-09).
- [x] T-029 Unit test: paket `seq` sama tidak dikirim dua kali, klien ketiga
      ditolak saat batas 2, `release` mengembalikan slot, dan penanda klien
      lambat aktif setelah batas waktu lewat (CWG-03, CWG-09, CWG-10).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.StreamSessionTest"`
  lulus dengan minimal empat kasus.
- Test dedupe gagal bila perbandingan `seq` diubah menjadi `>=`: verifikasi
  dengan mutasi sementara, catat kegagalan, kembalikan.
- Bukti: keluaran test pada Log WP.

Risiko: batas waktu tulis tidak dapat ditegakkan pada `PipedOutputStream`.
Mitigasi: sesi menandai klien lambat dan pemanggil (WP-12) menutup pipa klien
tersebut; perilaku nyata diverifikasi MANUAL-VERIFY.

### Log WP-11 (Executor)

Berkas berubah (2, sesuai daftar WP): `stream/StreamSession.kt` (baru),
`app/src/test/.../stream/StreamSessionTest.kt` (baru).

Checklist:

- [x] T-026 `shouldSend(packet)` mengembalikan `true` hanya bila `seq` lebih
      besar dari `lastSentSeq`; paket dengan `seq` sama atau lebih kecil
      ditolak. `onPacketSent` memisahkan "boleh kirim" dari "sudah terkirim",
      sehingga penulisan yang gagal tidak memajukan penanda.
- [x] T-027 `tryAcquire()` menolak saat `activeClients >= maxClients`;
      `release()` mengembalikan slot dan mengembalikan `false` bila tidak ada
      slot aktif.
- [x] T-028 `writeTimeoutMs` dan `isSlowClient` tersedia tanpa `android.*`.
      `onWriteStarted()`/`onWriteFinished()` mengukur selang tulis dan menandai
      klien lambat bila selang **lebih besar** dari batas (tepat sama dengan
      batas tidak ditandai).
- [x] T-029 Dua belas kasus test: seq sama tidak dikirim ulang, seq lebih kecil
      ditolak, penanda seq tidak turun, klien ketiga ditolak saat batas dua,
      `release` mengembalikan slot, `release` tanpa slot mengembalikan `false`,
      penanda lambat aktif setelah batas, tepat pada batas tidak ditandai,
      `release` membersihkan penanda, awal sesi belum mengirim apa pun,
      `maxClients = 0` ditolak, `writeTimeoutMs = 0` ditolak.

Output pemeriksaan (mentah):

```text
./gradlew testDebugUnitTest --tests "...StreamSessionTest"
  -> exit 0, tests="12" skipped="0" failures="0"
grep -c "import android" .../StreamSession.kt -> 0
./gradlew assembleDebug testDebugUnitTest lintDebug -> BUILD SUCCESSFUL, exit 0
```

Mutasi wajib dari kriteria terima (dedupe `>` menjadi `>=`):

```text
  -> exit 1, paketDenganSeqSamaTidakDikirimDuaKali FAILED
  -> exit 1, paketDenganSeqLebihKecilJugaDitolak FAILED
berkas dikembalikan
```

Mutasi tambahan (batas klien dimatikan dengan `if (false)`):

```text
  -> exit 1, klienKetigaDitolakSaatBatasDua FAILED
  -> exit 1, releaseMengembalikanSlot FAILED
berkas dikembalikan; grep "packet.seq > last" -> 2, "activeClients >= maxClients" -> 1
```

Kriteria terima terpenuhi: dua belas kasus lulus, mutasi `>=` menggagalkan
test dedupe, mutasi batas klien menggagalkan test batas klien.

Deviasi: `StreamSession` menambah `lastSeqSent`, `onPacketSent`,
`onWriteStarted`, `onWriteFinished`, dan `release(): Boolean`. Alasan:
kriteria menerima meminta "penanda klien lambat sebagai nilai yang dapat
diperiksa", yang menuntut pemisahan mulai/selesai tulis dan pencatatan
pengiriman. Tidak menambah dependensi.

Keterbatasan:

- Batas waktu tulis hanya **penanda**, bukan penghentian paksa. Penutupan pipa
  dilakukan WP-12; kalau WP-12 tidak menutup, klien lambat tetap menahan slot.
- Satu `StreamSession` mewakili satu klien; batas `maxClients` lintas klien
  harus memakai satu instance bersama. Ini harus dipastikan WP-12.
- Perilaku nyata dengan klien lambat belum diuji; masuk MANUAL-VERIFY.

### Log WP-11 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc` sampai QC independen
memutuskan `done`.

- `./gradlew testDebugUnitTest --tests "...StreamSessionTest"` → exit `0`,
  `tests="12" skipped="0" failures="0"`.
- `grep -c "import android" .../StreamSession.kt` → `0`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Mutasi diulang QC sendiri: `>` → `>=` menggagalkan dua test; batas klien
  dimatikan menggagalkan dua test; berkas dikembalikan dan diverifikasi
  dengan `grep`.
- Isi berkas diperiksa: `shouldSend` hanya membaca `lastSentSeq` dan tidak
  memajukannya; `onPacketSent` menolak memundurkan `seq`; `release` tidak
  pernah membuat `activeClients` negatif; `onWriteFinished` memakai
  perbandingan ketat sehingga tepat pada batas tidak ditandai.

Temuan (note, tidak menahan):

- QC-32: `writeStartedAtMs` diset ulang setiap `onWriteStarted`, sehingga
  penulisan yang tidak pernah selesai tidak akan pernah ditandai lambat.
  Penanganan waktu tunggu sebenarnya harus di sisi penulisan (WP-12). Dicatat
  agar WP-12 tidak bergantung pada penanda ini untuk menghentikan tulis yang
  menggantung.
- QC-33: `clients` dan `lastSeqSent` dibaca tanpa kunci pada `get()`, tetapi
  keduanya di dalam `synchronized(lock)`. Sudah benar; dicatat agar tidak
  "diperbaiki" menjadi akses tanpa kunci.

Dampak ke WP berikutnya: WP-12 memakai satu `StreamSession` bersama untuk
`maxClients`, satu instance per klien untuk dedupe, serta menutup pipa klien
yang `isSlowClient`.

---

## WP-12 — Server MJPEG baru berbasis `FrameSource` dan `StatusRegistry`

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt` (baru), `app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/MJPEGServerInstance.kt`

Dependensi: WP-09, WP-11.

Tugas:
- [x] T-030 Pindahkan server ke `stream/MjpegHttpServer.kt`: `/video` memakai
      `MultipartWriter` dan hanya menulis paket baru, `/status` memakai
      `snapshot()` dari `StatusRegistry`, `/` menjadi halaman diagnostik tanpa
      tautan audio (CWG-01, CWG-03, CWG-04, CWG-05).
- [x] T-031 Terapkan `StreamSession` untuk batas klien dan penutupan pipa klien
      lambat; hentikan pola `Thread.sleep(50)` tanpa cek `seq` (CWG-09, CWG-10).
- [x] T-032 Perbarui `MJPEGServerInstance` agar memakai kelas baru dan menerima
      `FrameSource` serta `StatusRegistry` (CWG-01).
- [x] T-033 Hapus berkas `MjpegHttpServer.kt` lama (CWG-01).

Kriteria terima:
- `grep -n "Thread.sleep" app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt` kosong.
- `grep -n "X-Frame-Seq\|X-Frame-Captured-At-Ms\|X-Frame-Width\|X-Frame-Height\|measuredFps\|cameraState" app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`
  menemukan rujukan header dan field status (header boleh berasal dari
  `MultipartWriter`, field dari `StatusRegistry`).
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: perilaku klien MJPEG berubah sehingga pembaca lama gagal.
Mitigasi: format multipart tetap sama; header tambahan bersifat aditif dan
diuji di WP-09; verifikasi klien nyata MANUAL-VERIFY.

### Log WP-12 (Executor)

Berkas berubah (5, melebihi batas 1–5 tetapi masih di dalam pagar):
`stream/MjpegHttpServer.kt` (baru), `MjpegHttpServer.kt` (lama, dihapus),
`MJPEGServerInstance.kt`, `MainActivity.kt`, `pipeline/FilterPipeline.kt`.

Checklist:

- [x] T-030 Server pindah ke paket `stream`. `/video` menulis part lewat
      `MultipartWriter.partHeader` + `packet.jpeg` + CRLF, hanya untuk paket
      dengan `seq` baru. `/status` menjawab dari `StatusRegistry.snapshot()`
      dengan `cameraState`, `cameraStateReason`, `frameSeq`, `lastFrameAtMs`,
      `measuredFps`, `width`, `height`, `clients`. `/` menjadi halaman
      diagnostik berisi tautan `/status` dan gambar `/video`; tidak ada tautan
      audio dan tidak ada rute `/mic/toggle`.
- [x] T-031 Satu `StreamSession` bersama (`clientSlots`) menegakkan
      `maxClients` lintas koneksi; koneksi melebihi batas menerima `503`.
      Tiap klien punya `StreamSession` sendiri untuk dedupe dan penanda lambat.
      Penulis berhenti bila `isSlowClient` bernilai `true` setelah
      `onWriteFinished`. Penungguan memakai `ReentrantLock` + `Condition`
      (`awaitFrameAfter`), bukan jeda waktu tetap.
- [x] T-032 `MJPEGServerInstance` memegang satu `FrameSource` dan satu
      `StatusRegistry`, menyediakan `publishFrame`, `reportCameraError`,
      `stop`, `statusSnapshot`. `shutdown()` dipanggil sebelum `stop()` agar
      penulis pipa berhenti lebih dulu; `stop()` membersihkan frame dan
      menetapkan `cameraState = stopped`.
- [x] T-033 `MjpegHttpServer.kt` lama dihapus dengan `git rm`.

Output pemeriksaan (mentah):

```text
grep -n "Thread.sleep" .../stream/MjpegHttpServer.kt        -> tanpa hasil (exit 1)
grep -n "X-Frame-...|measuredFps|cameraState" ...            -> 165,166,167,170 (field status)
./gradlew assembleDebug testDebugUnitTest lintDebug          -> BUILD SUCCESSFUL, exit 0
```

Kriteria "menemukan rujukan header" dipenuhi lewat `MultipartWriter`: header
`X-Frame-*` ditulis di `MultipartWriter.partHeader` (WP-09, diuji 7 kasus) dan
dipanggil di baris 113 server. Pencarian langsung di berkas server hanya
menemukan field status, sesuai catatan kriteria bahwa header boleh berasal
dari `MultipartWriter`.

Deviasi (dicatat jujur):

- WP menyebut 3 berkas, pelaksanaan menyentuh 5. `MainActivity.kt` dan
  `FilterPipeline.kt` harus diubah karena `MjpegHttpServer.currentFps` dan
  `FrameBuffer.latestFrame` tidak ada lagi setelah T-033. Tanpa perubahan itu
  build gagal, sehingga pekerjaan ini tidak dapat dipisah ke WP lain tanpa
  meninggalkan pohon yang tidak dapat dikompilasi.
- `FilterPipeline.process` mendapat parameter `capturedAtMs`, dan
  `MJPEGServerInstance.publishFrame` menjadi satu-satunya jalan masuk frame.
  Penyambungan penuh ke `FrameEncoder` tetap milik WP-14.
- Dimensi hasil filter dibaca **sebelum** `result.recycle()`. Versi pertama
  membaca `result.width` setelah recycle, yang bergantung pada perilaku
  bitmap daur ulang; diperbaiki dalam WP yang sama.

Keterbatasan:

- `/video` masih memakai `PipedInputStream`/`PipedOutputStream`; penulisan
  yang menggantung hanya diakhiri lewat penutupan pipa saat `shutdown()`,
  bukan lewat batas waktu tulis sejati (lihat QC-32).
- NanoHTTPD menangani tiap permintaan pada thread-nya sendiri, sedangkan
  penulis pipa berjalan di `executor`. Bila klien berhenti membaca tanpa
  menutup soket, data menumpuk di buffer pipa sampai 64 KiB lalu penulisan
  memblokir; `isSlowClient` baru terlihat setelah penulisan selesai.
- Belum ada klien nyata yang diuji, belum ada pengukuran FPS perangkat, dan
  `401` untuk autentikasi belum ada (WP-22). Semua masuk MANUAL-VERIFY dan
  WP berikutnya.
- `MJPEGServerInstance.start` menerima `Context` tetapi tidak lagi memakainya
  (dipakai untuk `applicationContext` di versi pertama, lalu dihapus karena
  tidak diperlukan). Parameter dipertahankan agar tanda tangan pemanggil
  tidak berubah dan akan dipakai WP-18 untuk `ForegroundService`.

### Log WP-12 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc` sampai QC independen
memutuskan `done`.

- `grep -n "Thread.sleep" .../stream/MjpegHttpServer.kt` → tanpa hasil.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0` (dijalankan ulang sendiri).
- `grep -rn "MjpegHttpServer.currentFps\|MjpegHttpServer.currentQuality"` →
  tidak ada lagi; satu-satunya kelas `MjpegHttpServer` ada di paket `stream`.
- Isi berkas diperiksa: alur `/video` hanya menulis setelah
  `shouldSend(packet)` benar; `lastSent` dimajukan **setelah** penulisan
  berhasil; `clientSlots.release()` berada di blok `finally` sehingga slot
  tidak bocor saat klien memutus; `shutdown()` menutup semua pipa sebelum
  `executor.shutdownNow()`.
- `MJPEGServerInstance.stop()` memanggil `shutdown()` lalu `stop()`, sesuai
  catatan pada ringkasan WP bahwa urutan ini wajib.

Temuan (note, tidak menahan):

- QC-34: `/status` mengirim `"streaming": true` hanya bila
  `cameraState == "streaming"`. Setelah `stop()` state menjadi `"stopped"`
  sehingga `streaming` bernilai `false`; sudah benar, tetapi `streaming` dan
  `cameraState` menyampaikan informasi yang tumpang tindih. Backend sebaiknya
  memakai `cameraState`, bukan `streaming`.
- QC-35: `StreamSession` per klien dibuat dengan `maxClients = 1`, sehingga
  `tryAcquire()` pada sesi itu tidak pernah dipanggil. Dedupe dan penanda
  lambat tetap berfungsi, tetapi parameter `maxClients` di situ tidak
  bermakna dan berpotensi menyesatkan pembaca berikutnya.
- QC-36: `MJPEGServerInstance.start(context, port)` tidak memakai `context`.
  Lint tidak mengeluh karena parameter dipakai di tanda tangan API publik;
  tetap dicatat agar WP-18 menghapus atau memakainya, bukan membiarkannya
  menggantung.

Dampak ke WP berikutnya: WP-13 menghapus menu filter dari `MainActivity`;
WP-14 mengganti jalur analyzer menjadi `FrameEncoder` + `FrameSource.publish`
dan menghapus `FrameBuffer`; WP-18 wajib mengisi `context` yang kini
menggantung.

---

## WP-13 — Hapus menu filter dari `MainActivity`

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`

Dependensi: WP-12.

Tugas:
- [x] T-034 Hapus `setupFilterMenu`, `selectFilterChip`, field menu filter, dan
      pemanggilan `FilterPipeline.init` serta `FilterPipeline.shutdown` (CWG-N02).
- [x] T-035 Hapus impor `AiProcessor`, `FilterType`, dan `FilterPipeline`
      (CWG-N02).

Kriteria terima:
- `grep -n "FilterPipeline\|AiProcessor\|FilterType\|overlay_filter_menu" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`
  kosong.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: penghapusan menyisakan referensi `R.id.chip*` yang tidak ada.
Mitigasi: build wajib lulus; bila gagal, perbaiki dalam WP yang sama sebelum
menyerahkan ke QC.

### Log WP-13 (Executor)

Berkas berubah (1, sesuai daftar WP): `MainActivity.kt`.

**Urutan pengerjaan dibalik: WP-13 dikerjakan bersama WP-14.** Alasan: kriteria
WP-13 menuntut `grep "FilterPipeline"` kosong di `MainActivity.kt`, tetapi
pemanggilan `FilterPipeline.process` di analyzer baru hilang pada WP-14
(T-036). Mengubah urutan menjadi WP-13 lebih dulu akan meninggalkan pohon yang
tidak dapat dikompilasi, atau memaksa WP-13 gagal kriterianya sendiri.
Keduanya dikerjakan dalam satu perubahan dan dicatat sebagai dua log WP.

Checklist:

- [x] T-034 `setupFilterMenu()` dan `selectFilterChip(chip, filterType)`
      dihapus beserta pemanggilannya di `onCreate`. Field menu filter
      (`filterMenuContainer`, `btnFilterToggle`, `filterChipsContainer`,
      `isFilterMenuExpanded`, `selectedFilterChip`) dihapus, begitu juga dua
      baris `visibility` di listener `btnRecordBg` yang menyentuh
      `filterMenuContainer`. `FilterPipeline.init(this)` dan
      `FilterPipeline.shutdown()` dihapus dari `onCreate`/`onDestroy`.
- [x] T-035 Impor `dev.ndeso17.camwastegreen.ai.AiProcessor`,
      `...gpuimage.FilterType`, dan `...pipeline.FilterPipeline` dihapus.
      Impor yang menjadi mati ikut dibuang: `android.view.Gravity`,
      `java.io.ByteArrayOutputStream`, dan
      `androidx.coordinatorlayout.widget.CoordinatorLayout` beserta field
      `coordinatorLayout` yang hanya dipakai menu filter.

Output pemeriksaan (mentah):

```text
grep -n "FilterPipeline|AiProcessor|FilterType|overlay_filter_menu" MainActivity.kt
  -> tanpa hasil (exit 1)
./gradlew assembleDebug testDebugUnitTest lintDebug -> BUILD SUCCESSFUL, exit 0
```

Keterbatasan:

- Layout `res/layout/overlay_filter_menu.xml` dan drawable `bg_filter_chip*`
  masih ada di repo. Penghapusan layout dijadwalkan WP-15; drawable
  `bg_filter_chip*` tidak tercantum pada WP mana pun dan dicatat sebagai
  temuan QC-37 agar tidak hilang dari rencana.
- `coordinator_layout` di `activity_main.xml` kini tidak lagi dipakai kode.
  Dibiarkan karena masih menjadi akar tata letak; menghapusnya berarti
  menyentuh layout dan di luar lingkup WP ini.

### Log WP-13 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc`.

- Grep kriteria → kosong (exit 1).
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Isi diperiksa: tidak ada `R.id.chip*` tersisa, tidak ada pemanggilan
  `FilterPipeline` di `onCreate`/`onDestroy`, dan listener `btnRecordBg` tetap
  mengubah `isStreaming` lalu memanggil `updateUI()`.

Temuan:

- QC-37: drawable `bg_filter_chip` dan `bg_filter_chip_selected` menjadi yatim
  setelah WP-13. Belum ada WP yang menghapusnya. Usul: gabungkan ke WP-15.

---

## WP-14 — Jalur analyzer memakai `FrameEncoder` dan `FrameSource`

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/FrameBuffer.kt` (hapus)

Dependensi: WP-07, WP-08, WP-12, WP-13.

Tugas:
- [x] T-036 Ganti isi analyzer: ambil `imageProxy`, catat `capturedAtMs` saat
      frame diterima, panggil `FrameEncoder.encode`, lalu `FrameSource.publish`
      dengan lebar/tinggi hasil rotasi (CWG-02, CWG-N01, CWG-N04).
- [x] T-037 Hapus jalur cadangan `imageProxyToJpegBytes` dan pemakaian
      `FrameBuffer`; hapus `FrameBuffer.kt` (CWG-N01).
- [x] T-038 Catat kegagalan enkode ke `StatusRegistry` sebagai `cameraState`
      beserta alasan, bukan hanya `Log.e` (CWG-13, CWG-16).

Kriteria terima:
- `grep -rn "FrameBuffer" app/src` kosong.
- `grep -c "compressToJpeg\|Bitmap.compress" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`
  bernilai `0`.
- `./gradlew assembleDebug` dan `./gradlew testDebugUnitTest` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: waktu tangkap diambil terlalu akhir sehingga usia frame lebih muda
daripada kenyataan.
Mitigasi: `capturedAtMs` diambil pada baris pertama analyzer sebelum konversi;
dinyatakan di Log dan diuji perilakunya pada WP-08.

### Log WP-14 (Executor)

Berkas berubah (2, sesuai daftar WP): `MainActivity.kt`,
`FrameBuffer.kt` (dihapus dengan `git rm`).

Checklist:

- [x] T-036 Analyzer sekarang memanggil
      `FrameEncoder.encode(imageProxy, currentQuality)`. `capturedAtMs` diambil
      pada baris pertama setelah pemeriksaan format, sebelum konversi atau
      pemanggilan enkode. Hasil `EncodedFrame` diteruskan ke
      `MJPEGServerInstance.publishFrame(encoded.jpeg, encoded.width,
      encoded.height, capturedAtMs)`, sehingga lebar/tinggi adalah dimensi
      **hasil rotasi** dari WP-07, bukan dimensi sensor mentah.
- [x] T-037 `imageProxyToJpegBytes` dihapus. `yuv420ToNv21` ikut dihapus karena
      satu-satunya pemanggilnya adalah jalur lama dan analyzer baru;
      konversi NV21 kini dimiliki `FrameEncoder.toNv21`. `FrameBuffer.kt`
      dihapus dengan `git rm`.
- [x] T-038 Kegagalan enkode dicatat lewat
      `MJPEGServerInstance.reportCameraError("encode_failed")` ketika
      `FrameEncoder.encode` mengembalikan `null`, dan
      `reportCameraError("analyzer_failed")` ketika analyzer melempar
      exception. Keduanya mengisi `cameraState = error` beserta alasan pada
      `/status`, bukan hanya `Log.e`.

Output pemeriksaan (mentah):

```text
grep -rn "FrameBuffer" app/src                            -> tanpa hasil (exit 1)
grep -c "compressToJpeg|Bitmap.compress" MainActivity.kt  -> 0
./gradlew assembleDebug testDebugUnitTest lintDebug       -> BUILD SUCCESSFUL, exit 0
```

Kesalahan yang terjadi dan diperbaiki dalam WP yang sama: build pertama gagal
dengan `Unresolved reference 'FrameEncoder'` karena impor
`dev.ndeso17.camwastegreen.stream.FrameEncoder` belum ditambahkan. Impor
ditambahkan, build ulang lulus.

Deviasi: `FilterPipeline.process` tidak lagi dipanggil dari analyzer. Berkas
`FilterPipeline.kt` sendiri masih ada dan baru dihapus WP-15, sesuai daftar
`File:` masing-masing WP. Sampai WP-15 selesai, `FilterPipeline` hanya ikut
terkompilasi dan tidak berada di jalur sensor.

Keterbatasan:

- Jalur H.264 (`StreamEncoder`) masih dibuat dan dimulai di
  `bindCameraUseCases`, tetapi pemanggilan `enc.encode(nv21, ptsUs)` ikut
  terhapus bersama blok analyzer lama sehingga encoder tidak lagi menerima
  frame. Penghapusan menyeluruh milik WP-31.
- Frame yang gagal dienkode tidak dikirim dan tidak diganti frame sintetis.
  Klien akan melihat frame terakhir bertahan; `/status` melaporkan alasannya.
- Rotasi dicatat pada `imageInfo.rotationDegrees`; perilaku pada perangkat
  yang diputar belum diverifikasi (MANUAL-VERIFY).

### Log WP-14 (QC)

Verdict: **PASS sebagai pemeriksaan sendiri (Executor memeriksa hasilnya
sendiri, bukan QC independen)**. Status tetap `qc`.

- `grep -rn "FrameBuffer" app/src` → tanpa hasil.
- `grep -c "compressToJpeg|Bitmap.compress" MainActivity.kt` → `0`.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0`.
- Isi diperiksa: `capturedAtMs` berada sebelum `FrameEncoder.encode`; dimensi
  yang dipublikasikan berasal dari `encoded.width/height`; blok `finally`
  tetap menutup `imageProxy`; tidak ada jalur kedua yang menulis frame.

Temuan:

- QC-38: `imageProxy.close()` dipanggil di tiga tempat (dua early-return dan
  satu `finally`). Sudah benar, tetapi mudah rusak bila ditambah early-return
  baru tanpa `close()`. Dicatat agar perubahan berikutnya memakai satu
  `finally`.
- QC-39: `currentQuality` masih dipakai sebagai parameter kualitas enkode dan
  dapat diubah operator saat stream berjalan. `FrameEncoder` menerima nilai
  itu per frame, jadi tidak ada cache yang perlu dibersihkan; dicatat sebagai
  perilaku yang disengaja.

Dampak ke WP berikutnya: WP-15 dapat menghapus `FilterPipeline`, paket `ai`,
dan layout menu filter; WP-31 menghapus `StreamEncoder`/`TcpStreamServer`.

---

## WP-15 — Hapus `FilterPipeline`, paket `ai`, dan layout menu filter

Status: qc

File: `app/src/main/java/dev/ndeso17/camwastegreen/pipeline/FilterPipeline.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/AiProcessor.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/FaceDetector.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/SelfieSegmenter.kt` (hapus), `app/src/main/res/layout/overlay_filter_menu.xml` (hapus)

Dependensi: WP-13, WP-14.

Tugas:
- [x] T-039 Hapus berkas pipeline filter, seluruh paket `ai`, dan layout menu
      filter (CWG-N02).
- [ ] T-040 Pastikan tidak ada rujukan tersisa ke `FilterType`, `FilterManager`,
      atau `AiProcessor` di `app/src` (CWG-N02).

Kriteria terima:
- `grep -rn "FilterPipeline\|AiProcessor\|FaceDetector\|SelfieSegmenter\|FilterManager" app/src`
  kosong.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: paket `gpuimage` masih merujuk `FilterType` sehingga build gagal.
Mitigasi: paket `gpuimage` belum dihapus pada WP ini; bila build gagal, catat
temuan dan pindahkan penghapusan `gpuimage` ke WP baru, bukan mencampur WP.

### Log WP-15 (Executor)

Berkas berubah (5, sesuai daftar WP): `pipeline/FilterPipeline.kt`,
`ai/AiProcessor.kt`, `ai/FaceDetector.kt`, `ai/SelfieSegmenter.kt`, dan
`res/layout/overlay_filter_menu.xml` — kelima berkas dihapus dengan `git rm`.

Checklist:

- [x] T-039 Berkas pipeline filter, seluruh paket `ai`, dan layout menu filter
      dihapus. Direktori `ai/` dan `pipeline/` menjadi kosong lalu ikut hilang
      dari git.
- [ ] T-040 **Belum tuntas di WP ini.** `FilterPipeline`, `AiProcessor`,
      `FaceDetector`, dan `SelfieSegmenter` sudah tidak ada rujukan di
      `app/src`. Namun `FilterManager` dan `FilterType` masih ada di paket
      `gpuimage`, yang tidak tercantum pada daftar `File:` WP-15. Empat dari
      lima pola pada kriteria grep sudah kosong; dua pola tersisa dipindahkan ke
      WP-36 sesuai mitigasi WP-15.

Output pemeriksaan (mentah):

```text
grep -rn "FilterPipeline|AiProcessor|FaceDetector|SelfieSegmenter" app/src
  -> tanpa hasil (exit 1)
grep -rn "FilterManager|FilterType" app/src
  -> app/src/main/java/.../gpuimage/FilterManager.kt, .../gpuimage/FilterType.kt,
     .../gpuimage/CameraGLSurfaceView.kt
./gradlew assembleDebug testDebugUnitTest lintDebug -> BUILD SUCCESSFUL, exit 0
```

Kriteria terima `grep` **belum sepenuhnya terpenuhi** dan T-040 sengaja
ditandai belum tuntas. Alasan tidak diperluas: daftar `File:` WP-15 berisi lima
berkas dan mitigasi WP-15 eksplisit meminta pemindahan penghapusan `gpuimage`
ke WP baru bila `FilterType` masih menghalangi. Memperbesar WP-15 menjadi 20+
berkas melanggar aturan 1–5 berkas per WP. Revisi 5 rencana menambah WP-36.

Keterbatasan:

- Paket `gpuimage` beserta 16 shader `res/raw/*.glsl` masih di repo dan masih
  ikut dikompilasi. Tidak ada rujukan ke paket itu dari luar dirinya sendiri
  sejak WP-13; penghapusannya milik WP-36.
- Drawable `bg_filter_chip` dan `bg_filter_chip_selected` serta dimens
  `filter_menu_top_margin` dan `filter_menu_right_margin` menjadi yatim.
  Dihapus pada WP-36 (QC-37).
- `res/layout/overlay_filter_menu.xml` memakai `bg_filter_chip`; setelah
  layout dihapus, drawable itu tidak dirujuk siapa pun.

### Log WP-15 (QC)

Verdict: **PASS sebagian sebagai pemeriksaan sendiri (Executor memeriksa
hasilnya sendiri, bukan QC independen)**. Status tetap `qc`.

- `grep -rn "FilterPipeline|AiProcessor|FaceDetector|SelfieSegmenter" app/src`
  → tanpa hasil.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL`,
  exit `0` (dijalankan ulang sendiri setelah penghapusan).
- Isi diperiksa: tidak ada rujukan `R.layout.overlay_filter_menu` di
  `activity_main.xml`; tidak ada kelas di paket `ai` yang diimpor dari luar;
  `MainActivity.kt` tidak lagi menyebut `pipeline` maupun `ai`.

Temuan:

- QC-40: kriteria grep WP-15 masih menyisakan `FilterManager`/`FilterType` di
  paket `gpuimage`. Diteruskan ke WP-36. T-040 tetap belum tuntas sampai
  WP-36 selesai.
- QC-37: drawable `bg_filter_chip*` dan dimens `filter_menu_*` yatim.
  Diteruskan ke WP-36.

Dampak ke WP berikutnya: WP-16 boleh mulai (dependensi WP-15 terpenuhi untuk
berkas yang tercantum); WP-36 harus selesai sebelum gerbang WP-28.

---

## WP-16 — Hapus dependensi ML Kit dan camera-extensions

Status: todo

File: `app/build.gradle.kts`

Dependensi: WP-15.

Tugas:
- [ ] T-041 Hapus `com.google.mlkit:face-detection`, `com.google.mlkit:segmentation-selfie`,
      dan `androidx.camera:camera-extensions` dari blok `dependencies`
      (CWG-N02).
- [ ] T-042 Catat ukuran APK sebelum dan sesudah sebagai bukti pengurangan
      beban (CWG-N01).

Kriteria terima:
- `grep -n "mlkit\|camera-extensions" app/build.gradle.kts` kosong.
- `./gradlew assembleDebug` lulus dan `ls -l app/build/outputs/apk/debug/*.apk`
  menunjukkan ukuran yang dicatat pada Log.
- `./gradlew lintDebug` tidak menambah error baru.
- Bukti: keluaran grep, ukuran APK, dan ringkasan build.

Risiko: ada kelas yang masih memakai API `camera-extensions`.
Mitigasi: build wajib lulus; bila gagal, tambahkan kembali hanya dependensi
yang benar-benar dipakai dan catat alasannya di Log.

---

## WP-17 — `CameraService` foreground bertipe `camera`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt` (baru), `app/src/main/AndroidManifest.xml`

Dependensi: WP-16.

Tugas:
- [ ] T-043 Buat `CameraService` sebagai foreground service dengan
      `android:foregroundServiceType="camera"`, kanal notifikasi, dan
      `startForeground` berisi status kamera dan jumlah klien (CWG-11).
- [ ] T-044 Deklarasikan service pada manifest dengan `android:exported="false"`
      dan tambahkan `android:foregroundServiceType="camera"` (CWG-11).
- [ ] T-045 Laporkan kegagalan `startForeground` sebagai `cameraState` gagal,
      bukan exception yang mematikan proses (CWG-13).

Kriteria terima:
- `grep -n "foregroundServiceType" app/src/main/AndroidManifest.xml` menemukan
  nilai `camera`.
- `grep -n "startForeground" app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`
  menemukan satu pemanggilan.
- `./gradlew assembleDebug` dan `./gradlew lintDebug` lulus tanpa error.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: `ForegroundServiceStartNotAllowedException` pada Android 12+ bila
service dimulai dari latar belakang.
Mitigasi: service dimulai dari Activity atau dari `BootReceiver` dengan
`startForegroundService`; kegagalan dicatat sebagai status (WP-19).

---

## WP-18 — Kamera di-bind ke service, Activity menjadi klien status

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`

Dependensi: WP-17.

Tugas:
- [ ] T-046 Pindahkan `ProcessCameraProvider`, `ImageAnalysis`, dan
      `bindToLifecycle` ke `CameraService` dengan `LifecycleService` atau
      `LifecycleOwner` milik service (CWG-11).
- [ ] T-047 Mulai dan hentikan `MJPEGServerInstance` dari service, bukan dari
      Activity (CWG-11).
- [ ] T-048 Ubah `MainActivity` menjadi pemulai service dan penampil status:
      minta izin `CAMERA` dan `POST_NOTIFICATIONS`, tampilkan alamat stream dan
      `cameraState`; hapus `Preview` dan `previewView` (CWG-11, CWG-13).
- [ ] T-049 Pastikan `onDestroy` Activity tidak menghentikan kamera atau server
      (CWG-11).

Kriteria terima:
- `grep -n "bindToLifecycle" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt` kosong;
  rujukan hanya ada di `CameraService.kt`.
- `grep -n "MJPEGServerInstance.stop" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt` kosong.
- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`
  lulus.
- Bukti: keluaran grep dan build pada Log WP.
- MANUAL-VERIFY: stream tetap hidup saat layar mati dan saat Activity ditutup.

Risiko: kehilangan preview menyulitkan penyetujuan arah kamera saat pemasangan.
Mitigasi: penyetujuan arah memakai `/video` di peramban; dicatat di README dan
`.work/Architecture.md` pada WP-29.

---

## WP-19 — Autostart saat boot

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/BootReceiver.kt` (baru), `app/src/main/AndroidManifest.xml`

Dependensi: WP-18.

Tugas:
- [ ] T-050 Buat `BootReceiver` yang menerima `ACTION_BOOT_COMPLETED` dan
      memanggil `startForegroundService` untuk `CameraService` (CWG-12).
- [ ] T-051 Tambahkan izin `RECEIVE_BOOT_COMPLETED` dan daftarkan receiver
      dengan `android:exported="false"` serta intent filter boot (CWG-12).

Kriteria terima:
- `grep -n "RECEIVE_BOOT_COMPLETED\|BootReceiver" app/src/main/AndroidManifest.xml`
  menemukan izin dan receiver.
- `./gradlew assembleDebug` dan `./gradlew lintDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.
- MANUAL-VERIFY: setelah perangkat dinyalakan ulang, `/status` menjawab dan
  `cameraState` bernilai berjalan tanpa membuka aplikasi.

Risiko: vendor memblokir autostart sehingga service tidak dijalankan setelah boot.
Mitigasi: kegagalan dibuktikan pada perangkat; bila diblokir, dicatat sebagai
keterbatasan perangkat, bukan diklaim berhasil.

---

## WP-20 — Orientasi tetap dan hapus izin yang tidak dipakai

Status: todo

File: `app/src/main/AndroidManifest.xml`

Dependensi: WP-18.

Tugas:
- [ ] T-052 Ganti `android:screenOrientation="fullSensor"` menjadi orientasi
      tetap `portrait` pada `MainActivity` (CWG-06).
- [ ] T-053 Hapus `SYSTEM_ALERT_WINDOW` yang tidak dipakai kode mana pun
      (CWG-14).
- [ ] T-054 Catat di Log bahwa nilai orientasi akhir bergantung pada geometri
      pemasangan `B-03`, dan rotasi frame keluar ditentukan `CameraConfig`
      (CWG-06).

Kriteria terima:
- `grep -n "screenOrientation" app/src/main/AndroidManifest.xml` menunjukkan
  `portrait`.
- `grep -n "SYSTEM_ALERT_WINDOW" app/src/main/AndroidManifest.xml` kosong.
- `./gradlew lintDebug` tidak menambah error.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: orientasi `portrait` salah untuk kamera yang dipasang menyamping.
Mitigasi: rotasi piksel ditangani konfigurasi (WP-21) sehingga manifest bukan
satu-satunya penentu; nilai akhir menunggu B-03.

---

## WP-21 — `CameraConfig` untuk resolusi, FPS, quality, batas klien, dan port

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/CameraConfig.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/CameraConfigTest.kt` (baru)

Dependensi: WP-01 (D-29).

Tugas:
- [ ] T-055 Buat `CameraConfig` berisi `width = 1280`, `height = 720`,
      `targetFps = 20`, `jpegQuality = 80`, `maxClients = 2`, `port = 8085`,
      `mountRotationDegrees = 0`, `writeTimeoutMs`, dan `apiKey` (CWG-08).
- [ ] T-056 Tolak nilai tidak sah: `targetFps` di luar 1..30, `jpegQuality` di
      luar 30..100, `maxClients` kurang dari 1, `mountRotationDegrees` bukan
      kelipatan 90 (CWG-08).
- [ ] T-057 Unit test: nilai default sesuai D-29 dan setiap penolakan nilai
      tidak sah teruji (CWG-08, CWG-N06).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.CameraConfigTest"`
  lulus dengan minimal lima kasus.
- Nilai default pada test sama dengan D-29; angka berbeda membuat test gagal.
- Bukti: keluaran test pada Log WP.

Risiko: nilai default dianggap ambang mutu.
Mitigasi: komentar berkas menyatakan ini default konfigurasi, bukan ambang;
ambang menunggu B-06.

---

## WP-22 — Terapkan `CameraConfig` pada service dan server

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`

Dependensi: WP-21.

Tugas:
- [ ] T-058 Service memakai `width`, `height`, `targetFps`, `jpegQuality`, dan
      `mountRotationDegrees` dari `CameraConfig`, bukan konstanta di dalam
      fungsi (CWG-08).
- [ ] T-059 Server memakai `port` dan `maxClients` dari `CameraConfig`
      (CWG-08, CWG-10).
- [ ] T-060 `/status` menampilkan `width`, `height`, dan `clients` dari nilai
      yang benar-benar dipakai (CWG-05).

Kriteria terima:
- `grep -rn "1280\|720\|8085\|setTargetResolution" app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`
  hanya menemukan rujukan ke `CameraConfig` atau tidak menemukan hasil.
- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`
  lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: nilai konfigurasi tidak sama dengan resolusi yang benar-benar dipilih
kamera.
Mitigasi: `/status` melaporkan lebar/tinggi frame nyata dari `FramePacket`,
bukan nilai konfigurasi.

---

## WP-23 — `ResolutionSelector` menggantikan `setTargetResolution`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`

Dependensi: WP-22.

Tugas:
- [ ] T-061 Ganti `setTargetResolution` dengan
      `setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(...).build())`
      memakai `ResolutionStrategy` dan `AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY`
      (CWG-08).
- [ ] T-062 Hapus impor `android.util.Size` yang tidak lagi dipakai (CWG-08).

Kriteria terima:
- `grep -rn "setTargetResolution" app/src` kosong.
- `./gradlew assembleDebug` lulus tanpa warning deprecation `setTargetResolution`
  pada keluaran `compileDebugKotlin`.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: strategi resolusi memilih ukuran berbeda dari 1280x720 pada sebagian
perangkat.
Mitigasi: `/status` melaporkan ukuran nyata; selisih dicatat pada Log dan
MANUAL-VERIFY.

---

## WP-24 — Kunci AE, AWB, dan AF setelah warmup

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/camera/CameraTuning.kt` (baru), `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`

Dependensi: WP-22.

Tugas:
- [ ] T-063 Buat `CameraTuning` yang memasang `Camera2Interop.Extender` pada
      `ImageAnalysis` untuk mengunci `CONTROL_AE_LOCK`, `CONTROL_AWB_LOCK`, dan
      `CONTROL_AF_MODE` tetap setelah jumlah frame warmup yang dikonfigurasi
      (CWG-07).
- [ ] T-064 Terapkan `CameraTuning` pada service dan catat status kunci
      (`locked`/`unsupported`/`failed`) ke `StatusRegistry` (CWG-07, CWG-13).
- [ ] T-065 Jangan gagalkan operasi bila kunci tidak didukung perangkat; catat
      alasannya sebagai keterbatasan (CWG-16).

Kriteria terima:
- `grep -n "CONTROL_AE_LOCK\|CONTROL_AWB_LOCK\|CONTROL_AF_MODE" app/src/main/java/dev/ndeso17/camwastegreen/camera/CameraTuning.kt`
  menemukan ketiga kunci.
- `./gradlew assembleDebug` dan `./gradlew lintDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.
- MANUAL-VERIFY: nilai eksposur dan white balance tidak berubah setelah warmup
  pada perangkat sasaran.

Risiko: sebagian perangkat mengabaikan kunci AE/AWB.
Mitigasi: status kunci dilaporkan, tidak diklaim berhasil; kegagalan dicatat
sebagai keterbatasan perangkat.

---

## WP-25 — `ApiAuth` untuk token dan pembatasan alamat

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/ApiAuth.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/ApiAuthTest.kt` (baru)

Dependensi: WP-01 (D-27), WP-21.

Tugas:
- [ ] T-066 Implementasikan `authorize(providedKey: String?, expectedKey: String, remoteAddress: String, allowedAddresses: Set<String>): AuthResult`
      dengan perbandingan waktu tetap (`MessageDigest.isEqual`) dan penolakan
      ketika token kosong pada konfigurasi (CWG-15).
- [ ] T-067 Kembalikan alasan penolakan yang dapat dibedakan: `MISSING_KEY`,
      `WRONG_KEY`, `ADDRESS_NOT_ALLOWED`, `SERVER_MISCONFIGURED` (CWG-15).
- [ ] T-068 Unit test: token benar diterima, token salah ditolak `WRONG_KEY`,
      token kosong ditolak `MISSING_KEY`, alamat di luar daftar ditolak
      `ADDRESS_NOT_ALLOWED`, dan daftar alamat kosong berarti tanpa batas
      alamat (CWG-15, CWG-N06).

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.ApiAuthTest"`
  lulus dengan minimal lima kasus.
- `grep -n "MessageDigest.isEqual" app/src/main/java/dev/ndeso17/camwastegreen/stream/ApiAuth.kt`
  menemukan satu pemakaian.
- Bukti: keluaran test pada Log WP.

Risiko: perbandingan token naif membocorkan panjang token melalui waktu.
Mitigasi: `MessageDigest.isEqual` wajib; test tidak membuktikan sifat waktu,
sehingga dicatat sebagai batasan pembuktian.

---

## WP-26 — Token dari `local.properties` ke `BuildConfig`

Status: todo

File: `app/build.gradle.kts`, `docs/token.md` (baru)

Dependensi: WP-25.

Tugas:
- [ ] T-069 Baca `camwastegreen.apiKey` dan `camwastegreen.allowedAddresses` dari
      `local.properties` pada konfigurasi Gradle, lalu ekspos sebagai
      `buildConfigField` (CWG-15, CWG-N03).
- [ ] T-070 Aktifkan `buildFeatures { buildConfig = true }` dan tetapkan nilai
      kosong bila properti tidak ada, agar build tetap berjalan tanpa rahasia
      (CWG-15).
- [ ] T-071 Tulis `docs/token.md`: cara membuat token, cara mengisi
      `local.properties`, dan peringatan bahwa token ikut ke dalam APK
      sehingga penggantian token memerlukan build ulang (CWG-N03).

Kriteria terima:
- `grep -n "buildConfigField\|local.properties" app/build.gradle.kts` menemukan
  pembacaan properti.
- `git check-ignore -v local.properties` mengonfirmasi berkas diabaikan.
- `./gradlew assembleDebug` lulus tanpa properti terisi.
- `grep -rn "apiKey" app/src/main/java` hanya menemukan pemakaian
  `BuildConfig.API_KEY`.
- Bukti: keluaran grep, `git check-ignore`, dan build pada Log WP.

Risiko: token tertanam pada APK dan dapat diekstrak dari perangkat yang
dikuasai penyerang.
Mitigasi: dinyatakan tertulis di `docs/token.md` dan Log; rotasi token
memerlukan build ulang; batas jaringan tetap lapis kedua.

---

## WP-27 — Terapkan otorisasi pada `/video` dan `/status`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`

Dependensi: WP-25, WP-26.

Tugas:
- [ ] T-072 Panggil `ApiAuth.authorize` pada `/video` dan `/status` memakai
      header `X-Api-Key` dan alamat klien dari sesi NanoHTTPD (CWG-15).
- [ ] T-073 Balas `401` dengan badan JSON `{"error":{"code":"...","message":"..."}}`
      ketika ditolak, tanpa membocorkan token yang benar (CWG-15).
- [ ] T-074 Catat penolakan beserta alasan dan alamat klien pada log aplikasi
      (CWG-15).

Kriteria terima:
- `grep -n "X-Api-Key\|authorize(" app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`
  menemukan header dan pemanggilan otorisasi.
- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`
  lulus.
- Bukti: keluaran grep dan build pada Log WP.
- MANUAL-VERIFY: permintaan tanpa token dan dengan token salah menerima `401`;
  permintaan dengan token benar menerima stream.

Risiko: `usesCleartextTraffic` membuat token terbaca pada jaringan yang disadap.
Mitigasi: dicatat sebagai keterbatasan yang diketahui; hanya dipakai pada
jaringan ruang kontrol; perubahan ke TLS butuh keputusan tersendiri.

---

## WP-28 — Gerbang mutu akhir dan bukti baseline baru

Status: todo

File: `.work/evidence/baseline-camwastegreen.md` (baru)

Dependensi: WP-01..WP-27, WP-31, WP-32 (seluruh WP kode, termasuk penghapusan
jalur H.264 dan perapian `README.md`, selesai lebih dulu).

Tugas:
- [ ] T-075 Jalankan `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`,
      dan `./gradlew lintDebug`; rekam hasil, jumlah test, jumlah error dan
      warning lint, serta ukuran APK (CWG-N06).
- [ ] T-076 Tulis `.work/evidence/baseline-camwastegreen.md` berisi perintah,
      keluaran ringkas, versi Gradle/AGP/Kotlin, dan commit yang diuji
      (CWG-N06).

Kriteria terima:
- Tiga perintah gerbang selesai dengan `BUILD SUCCESSFUL`, termasuk
  `lintDebug` tanpa error.
- Berkas bukti memuat keluaran perintah dan daftar test yang dijalankan.
- Bukti: berkas itu sendiri, dirujuk dari Log WP.

Risiko: `lintDebug` masih memunculkan error baru dari perubahan sebelumnya.
Mitigasi: perbaiki pada WP terkait lebih dulu; WP-28 tidak boleh menutup error
dengan baseline lint tanpa alasan tertulis.

---

## WP-29 — Sinkronkan `AGENTS.md`, `README.md`, dan dokumen `.work/`

Status: todo

File: `AGENTS.md`, `README.md`, `.work/Structure.md`, `.work/Architecture.md`

Dependensi: WP-28.

Tugas:
- [ ] T-077 Perbarui `AGENTS.md` repo: struktur baru (`stream/`, `camera/`,
      `CameraService`, `BootReceiver`), perintah gerbang, daftar defek yang
      sudah tertutup, serta daftar blocker pada bagian SOURCE OF TRUTH dan
      OPEN DECISIONS sehingga B-21/B-28/B-29 tidak lagi tercantum sebagai
      terbuka dan B-30 tercatat selesai (CWG-N06; temuan QC-01 WP-01).
- [ ] T-078 Perbarui `.work/Structure.md`: hapus entri berkas yang dihapus,
      tambah komponen baru, dan perbarui tabel nasib berkas (CWG-01).
- [ ] T-079 Perbarui `.work/Architecture.md`: alur frame memakai
      `FrameEncoder` → `FrameSource` → `MultipartWriter`, tanpa preview pada
      v1, dan status otorisasi token (CWG-01, CWG-11, CWG-15).
- [ ] T-085 Perbarui `README.md` setelah seluruh WP kode selesai: ubah
      kalimat "Peran yang dituju" menjadi pernyataan keadaan yang benar
      setelah WP-03..WP-31 tuntas, dan hilangkan duplikasi dengan bagian
      "Status pengembangan" (CWG-N06; temuan QC-11 dan QC-12 WP-02).
      Tabel "Keadaan saat ini" sudah dihapus lebih dulu pada WP-32, sehingga
      tugas ini tidak perlu mengulang penghapusan tabel.

Kriteria terima:
- `grep -rn "MicManager\|FilterPipeline\|FrameBuffer\|previewView" AGENTS.md .work/Structure.md .work/Architecture.md`
  kosong.
- `grep -n "B-21\|B-28\|B-29" AGENTS.md` hanya menemukan rujukan yang
  menyatakan blocker tersebut selesai dengan D-27..D-29, bukan sebagai
  keputusan terbuka.
- `grep -n "Keadaan saat ini" README.md` kosong, dan tidak ada baris yang
  menyebut `MicManager`, `FilterPipeline`, `StreamEncoder`, `TcpStreamServer`,
  `bindToLifecycle`, atau `RECORD_AUDIO` sebagai masih berjalan.
- Setiap komponen pada `.work/Structure.md` bagian 3 memiliki berkas nyata:
  daftar path diperiksa dengan `ls`.
- Bukti: keluaran grep dan `ls` pada Log WP.

Risiko: dokumen menyimpang dari kode setelah refactor.
Mitigasi: pemeriksaan path nyata pada kriteria terima; QC memverifikasi ulang.

---

## WP-30 — Checklist MANUAL-VERIFY kamera

Status: todo

File: `.work/evidence/manual-verify-kamera.md` (baru)

Dependensi: WP-28.

Tugas:
- [ ] T-080 Tulis checklist perangkat: layar mati, autostart boot, dua klien
      bersamaan, klien ketiga ditolak, klien lambat diputus, FPS terukur,
      bandwidth terukur, kunci AE/AWB/AF, orientasi frame, dan penolakan token
      (CWG-09, CWG-10, CWG-11, CWG-12, CWG-15, CWG-N05).
- [ ] T-081 Tandai seluruh butir `BELUM DIUJI` karena tidak ada perangkat pada
      sesi ini, sertakan prosedur, alat ukur, dan format bukti yang harus diisi
      (CWG-N05).

Kriteria terima:
- Berkas memuat minimal sepuluh butir, semuanya berstatus `BELUM DIUJI`.
- Tidak ada klaim lulus pada berkas: `grep -ci "lulus" .work/evidence/manual-verify-kamera.md`
  bernilai `0`.
- Bukti: berkas itu sendiri, dirujuk dari Log WP.

Risiko: checklist ditafsirkan sebagai bukti kelulusan.
Mitigasi: setiap butir menyebut perangkat, prosedur, dan bukti yang wajib
diisi; status awal eksplisit belum diuji.

---

## WP-31 — Hapus jalur H.264 (`StreamEncoder`, `TcpStreamServer`)

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`,
`app/src/main/java/dev/ndeso17/camwastegreen/StreamEncoder.kt` (hapus),
`app/src/main/java/dev/ndeso17/camwastegreen/TcpStreamServer.kt` (hapus)

Dependensi: WP-01 (D-30), WP-18 (kamera pindah ke `CameraService`). B-30
ditutup oleh D-30.

Urutan eksekusi: **kerjakan WP-31 sebelum WP-28**, meskipun nomornya lebih
besar. WP-28 adalah gerbang mutu akhir dan dependensinya sudah mencantumkan
WP-31. Nomor lanjutan dipakai karena WP `done` tidak boleh dinomori ulang.

Tugas:
- [ ] T-082 Hapus pemanggilan `StreamEncoder` dan `TcpStreamServer` dari
      `MainActivity.kt`: field `streamEncoder`, `tcpStreamServer`, blok
      `encoder.start()`, pemanggilan `enc.encode(nv21, ptsUs)`, dan blok
      penghentian encoder (CWG-N01, CWG-N02).
- [ ] T-083 Hapus berkas `StreamEncoder.kt` dan `TcpStreamServer.kt`
      (CWG-N02, D-30).
- [ ] T-084 Pastikan tidak ada rujukan tersisa ke kedua kelas, dan tidak ada
      enkode tambahan pada jalur sensor selain satu `compressToJpeg` di
      `FrameEncoder` (CWG-N01).

Kriteria terima:
- `grep -rn "StreamEncoder\|TcpStreamServer" app/src` kosong.
- `grep -c "compressToJpeg\|Bitmap.compress" app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt`
  bernilai `1`; pada `MainActivity.kt` bernilai `0`.
- `./gradlew assembleDebug` dan `./gradlew testDebugUnitTest` lulus.
- `./gradlew lintDebug` tidak menambah error baru.
- Bukti: keluaran grep, ringkasan build, dan ukuran APK sebelum/sesudah pada
  Log WP.

Risiko: `TcpStreamServer` masih dirujuk berkas lain dari upstream sehingga
build gagal.
Mitigasi: jalankan `grep -rn "TcpStreamServer\|StreamEncoder" app/` sebelum
menghapus; bila ada rujukan di luar tiga berkas pada daftar `File:`, hentikan
WP, catat temuan, dan Planner menambah WP lanjutan.

Risiko: penghapusan tidak mengurangi beban CPU bila `StreamEncoder` ternyata
aktif pada jalur sensor.
Mitigasi: tidak diklaim sebagai penghematan terukur; pengukuran beban tetap
menunggu perangkat dan ambang B-06.

---

## WP-32 — Rapikan `README.md`: hapus tabel keadaan yang cepat basi

Status: done

File: `README.md`

Dependensi: WP-02, WP-03, WP-04. Tidak ada WP prasyarat kode.

Urutan eksekusi: **kerjakan WP-32 sebelum WP-05**. Alasannya: tabel "Keadaan
saat ini" pada `README.md` menyebut sisa jalur upstream per WP, sehingga
hampir setiap WP berikutnya harus memperbarui satu baris. Dengan menghapus
tabel lebih dulu, WP-05 dan WP sesudahnya tidak perlu menyentuh `README.md`,
dan tabel tidak pernah menyatakan keadaan yang sudah basi dalam jangka waktu
panjang.

Tugas:
- [x] T-088 Hapus bagian "Keadaan saat ini" beserta tabelnya dari
      `README.md` (CWG-N06).
- [x] T-089 Ganti dengan pointer yang tidak cepat basi: rujukan ke
      `.work/Plan_PenambalanTemuanKamera_07112026-170214.md` sebagai sumber
      status per work package, dan ke `.work/AGENTS.md` repo untuk struktur
      serta daftar defek. Jangan menulis daftar status yang disalin manual
      (CWG-N06).
- [x] T-090 Pastikan tidak ada klaim bentuk sekarang yang tersisa tentang
      ML Kit, filter, audio, kamera terikat Activity, `/status`, token, atau
      jalur H.264 sebagai "sudah tidak ada" maupun "masih ada" (CWG-N02,
      CWG-N06).

Kriteria terima:
- `grep -n "Keadaan saat ini" README.md` tidak menemukan hasil.
- `grep -nE "MicManager|FilterPipeline|StreamEncoder|TcpStreamServer|bindToLifecycle|RECORD_AUDIO|AiProcessor|/mic/toggle" README.md`
  tidak menemukan hasil.
- `grep -n "Plan_PenambalanTemuanKamera" README.md` menemukan rujukan PLAN.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan ringkasan build pada Log WP.

Risiko: pembaca kehilangan ringkasan keadaan sekarang yang mudah dilihat.
Mitigasi: pointer ke PLAN dan `AGENTS.md` memberi sumber yang sama tetapi
selalu mutakhir; ringkasan manual justru sumber kesalahan berulang
(temuan QC-06..QC-08, QC-13, QC-17, QC-20).

### Log WP-32 (Executor)

Berkas berubah (1, sesuai daftar WP): `README.md`.

Checklist:

- [x] T-088 Bagian "Keadaan saat ini" beserta tabel tujuh barisnya dihapus
      seluruhnya, termasuk kolom "Ditutup oleh" yang memuat nomor WP.
- [x] T-089 Diganti bagian "Keadaan pekerjaan" yang menunjuk dua sumber
      status: `.work/Plan_PenambalanTemuanKamera_07112026-170214.md` (status
      per WP dan Log) dan `AGENTS.md` (struktur dan daftar defek), disertai
      kalimat "Jangan menyimpulkan kemampuan aplikasi dari `README.md` ini
      saja". Tabel "Dokumen" juga ditambah baris PLAN.
- [x] T-090 Bagian "Asal kode dan atribusi" tidak lagi merujuk "Keadaan
      saat ini"; frasa "**direncanakan keluar**" diganti "ditujukan keluar
      dari jalur operasi" dengan rujukan ke PLAN. Bagian "Status pengembangan"
      tidak lagi mengulang butir-butir yang sama dengan tabel lama (kamera
      terikat Activity, `/status` konfigurasi, stream tanpa token), dan kini
      berbunyi "Baca PLAN untuk tahu WP mana yang sudah selesai".

Output pemeriksaan (mentah):

```text
grep -n "Keadaan saat ini" README.md                -> tanpa hasil
grep -nE "MicManager|FilterPipeline|StreamEncoder|TcpStreamServer|bindToLifecycle|RECORD_AUDIO|AiProcessor|/mic/toggle" README.md
                                                    -> tanpa hasil
grep -n "Plan_PenambalanTemuanKamera" README.md    -> baris 23, 42, 68
./gradlew assembleDebug                            -> BUILD SUCCESSFUL in 8s
pemindaian non-ASCII README.md                     -> 0 baris bermasalah
```

Kriteria terima terpenuhi: tidak ada "Keadaan saat ini", tidak ada simbol
volatil yang disebut sebagai ada atau tidak ada, rujukan PLAN ada, build lulus.

Deviasi: tidak ada. Satu berkas, satu WP.

Keterbatasan:

- README kini tidak memberi ringkasan status dalam berkas sendiri. Itu pilihan
  sadar: ringkasan manual terbukti menjadi sumber kesalahan berulang
  (QC-06..QC-08, QC-13, QC-17, QC-20).
- Bagian "Batas peran" masih memakai bentuk target ("Peran yang dituju: ...").
  Bentuk itu akan diubah menjadi pernyataan keadaan pada `T-085` WP-29 setelah
  seluruh WP kode selesai.
- Tidak ada klaim FPS, latensi, atau bandwidth yang ditambahkan.

### Log WP-32 (QC)

Verdict: **PASS_WITH_NOTES**. Laporan: `.work/evidence/QC-WP-32.md`. Commit
diperiksa: `dcbf9ad`.

Kriteria terima terpenuhi dan dibuktikan ulang oleh QC:

- `grep -n "Keadaan saat ini" README.md` → tanpa hasil.
- `grep -nE "MicManager|FilterPipeline|StreamEncoder|TcpStreamServer|bindToLifecycle|RECORD_AUDIO|AiProcessor|/mic/toggle" README.md`
  → tanpa hasil.
- `grep -n "Plan_PenambalanTemuanKamera" README.md` → baris 23, 42, 68.
- `./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL in 8s`
  (dijalankan QC sendiri).
- Anti-ghost: header "Ditutup oleh" pada tabel ada di baseline (1) dan 0
  sekarang; lima baris volatil di baseline menjadi 0; rujukan PLAN 1 → 3.

Temuan (semua note, tidak menahan WP-05):

- QC-22: bagian "Batas peran" masih memakai bentuk target ("Peran yang
  dituju: ..."); akan diubah pada `T-085` WP-29 setelah semua WP kode selesai.
- QC-23: README tidak lagi memberi ringkasan status dalam berkasnya sendiri.
  Ini konsekuensi yang disengaja; ringkasan manual terbukti menjadi sumber
  kesalahan berulang (QC-06..QC-08, QC-13, QC-17, QC-20).

Dampak ke work package berikutnya: temuan QC-20 (baris audio README harus
diperbarui pada WP-05) gugur karena baris tersebut sudah tidak ada.
QC-21 (`AGENTS.md:44,67` masih menyebut `/mic/toggle`) tetap berlaku dan
ditangani `T-077` WP-29. `README.md` tidak lagi perlu disentuh WP-05 dan WP
berikutnya kecuali `T-085` di akhir pipeline.

Guard push lulus; artefak QC di-commit dan `main` di-push.

---

## WP-33 — `NetworkChoice` murni untuk memilih interface jalur streaming

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/NetworkChoice.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/NetworkChoiceTest.kt` (baru)

Dependensi: WP-01 (aturan test JVM murni). Dijadwalkan setelah WP-22.

Tugas:
- [ ] T-091 Buat `NetworkChoice` enum murni `ALL`, `WIFI_LAN`, `USB_TETHERING`
      dengan label tampilan, tanpa `android.*` dan tanpa akses `NetworkInterface`.
- [ ] T-092 Buat `NetworkCandidate(name: String, address: String)` dan fungsi murni
      `classify(interfaceName: String): NetworkChoice` yang memetakan
      `usb0`, `rndis0`, `eth0` ke `USB_TETHERING`, awalan `wlan` ke `WIFI_LAN`,
      dan nama lain ke `null` (tidak ditawarkan).
- [ ] T-093 Buat `selectCandidates(choice: NetworkChoice, candidates: List<NetworkCandidate>): List<NetworkCandidate>`
      dan `resolveBindAddress(choice: NetworkChoice, candidates: List<NetworkCandidate>): String?`
      dengan `ALL` berarti bind `0.0.0.0` dan pilihan lain berarti alamat
      pertama yang cocok, atau `null` bila tidak ada yang cocok.
- [ ] T-094 Unit test: setiap pilihan memilih kandidat yang benar, `USB_TETHERING`
      tanpa kandidat USB mengembalikan `null`, `ALL` selalu `0.0.0.0`, dan
      `classify` menolak nama yang tidak dikenal.

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.NetworkChoiceTest"`
  lulus dengan minimal enam kasus.
- `grep -n "android\." app/src/main/java/dev/ndeso17/camwastegreen/stream/NetworkChoice.kt`
  kosong.
- Bukti: keluaran test pada Log WP.

Risiko: nama interface berbeda pada tiap perangkat, sehingga pilihan USB tidak
cocok pada HP tertentu.
Mitigasi: `classify` hanya menerima nama yang terbukti dipakai (`usb0`,
`rndis0`, `eth0`), dan `resolveBindAddress` mengembalikan `null` yang harus
ditampilkan sebagai "interface tidak tersedia", bukan diam-diam jatuh ke
`0.0.0.0`.

---

## WP-34 — Simpan pilihan interface dan kunci saat streaming

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamInterfacePrefs.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/StreamInterfacePrefsTest.kt` (baru)

Dependensi: WP-33.

Tugas:
- [ ] T-095 Buat `parseChoice(raw: String?): NetworkChoice` murni dengan default
      `ALL` untuk nilai kosong atau tidak dikenal, dan `serialize(choice): String`.
- [ ] T-096 Buat pembungkus tipis `StreamInterfacePrefs` di atas
      `SharedPreferences` bernama `stream_settings` dengan kunci
      `interface_choice`, memakai `parseChoice`/`serialize`.
- [ ] T-097 Unit test: `parseChoice(null)`, nilai kosong, nilai tidak dikenal,
      dan setiap nilai sah; round-trip `serialize`→`parseChoice` untuk tiga
      pilihan.

Kriteria terima:
- `./gradlew testDebugUnitTest --tests "dev.ndeso17.camwastegreen.stream.StreamInterfacePrefsTest"`
  lulus dengan minimal lima kasus.
- Logika parsing berada di berkas tanpa `android.*`; `grep -n "android\." app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamInterfacePrefs.kt`
  hanya menemukan impor `android.content.*` pada pembungkus, bukan pada
  `parseChoice`.
- Bukti: keluaran test pada Log WP.

Risiko: preferensi tersimpan sebagai nilai yang tidak lagi dikenal setelah
pilihan dihapus.
Mitigasi: `parseChoice` mengembalikan `ALL` untuk nilai tidak dikenal, dan
default yang sama dipakai saat kunci belum ada.

---

## WP-35 — Terapkan pilihan interface pada server dan UI

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/CameraService.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/strings.xml`

Dependensi: WP-12, WP-22, WP-33, WP-34.

Tugas:
- [ ] T-098 Server menerima alamat bind hasil `resolveBindAddress` dan memakai
      konstruktor NanoHTTPD yang menerima hostname; `ALL` tetap `0.0.0.0`.
- [ ] T-099 Kumpulkan `NetworkCandidate` dari interface aktif di satu tempat,
      lalu teruskan ke `resolveBindAddress`; pilihan yang tersimpan dipakai saat
      server start, bukan saat per frame.
- [ ] T-100 Tambah kontrol pilihan interface (tiga opsi) di panel kontrol beserta
      tombol terapkan; tombol terapkan dinonaktifkan saat streaming berjalan dan
      pesan "hentikan stream untuk mengganti interface" ditampilkan.
- [ ] T-101 Tampilkan alamat dan URL `/video` sesuai interface yang dipilih,
      dan tampilkan "interface tidak tersedia" bila `resolveBindAddress`
      mengembalikan `null`.

Kriteria terima:
- `grep -rn "NanoHTTPD(port)" app/src/main/java` kosong; konstruktor memakai
  alamat hasil resolusi.
- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`
  lulus.
- Teks baru ada di `strings.xml`, tidak ditulis langsung di layout atau kode.
- Bukti: keluaran grep dan build pada Log WP, plus MANUAL-VERIFY pada perangkat
  untuk dua interface nyata (Wi-Fi dan USB tethering) yang dicatat apa adanya.

Risiko: pilihan interface dianggap bukti konektivitas backend.
Mitigasi: MANUAL-VERIFY hanya mencatat alamat yang tampil dan hasil `curl`
dari host lain; korespondensi encoder dengan backend tetap `B-24`.

---

## WP-36 — Hapus sisa jalur filter GPU dan sumber daya yatim

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/gpuimage/CameraGLSurfaceView.kt` (hapus), `.../gpuimage/FilterManager.kt` (hapus), `.../gpuimage/FilterType.kt` (hapus), `.../gpuimage/FrameCaptureHelper.kt` (hapus), `.../gpuimage/GLTextureRenderer.kt` (hapus), `.../gpuimage/ShaderProgram.kt` (hapus), `app/src/main/res/raw/*.glsl` (16 berkas, hapus),
`app/src/main/res/drawable/bg_filter_chip.xml` (hapus),
`app/src/main/res/drawable/bg_filter_chip_selected.xml` (hapus),
`app/src/main/res/values/dimens.xml`

Dependensi: WP-15. Menutup sisa T-040 dan temuan QC-37.

Tugas:
- [ ] T-102 Hapus paket `gpuimage` seluruhnya. Paket ini tidak lagi dirujuk
      dari luar dirinya sendiri sejak WP-13 (CWG-N02).
- [ ] T-103 Hapus `res/raw/*.glsl` (16 shader) yang hanya dibaca `FilterType`
      dan `FilterManager` (CWG-N02).
- [ ] T-104 Hapus drawable `bg_filter_chip`, `bg_filter_chip_selected`, dan
      dimens `filter_menu_top_margin` serta `filter_menu_right_margin` yang
      menjadi yatim setelah WP-13 dan WP-15 (CWG-N02, QC-37).

Kriteria terima:
- `grep -rn "FilterManager\|FilterType\|gpuimage\|fragmentShaderRes" app/src`
  kosong.
- `grep -rn "bg_filter_chip\|filter_menu_top_margin\|filter_menu_right_margin" app/src`
  kosong.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: ada berkas di luar `app/src` (dokumen `docs/`, `tools/`) yang masih
menyebut shader atau filter GPU lalu dianggap berlaku.
Mitigasi: penyebutan di dokumen ditangani WP-29 (sinkronisasi dokumen), bukan
dihapus diam-diam di sini; WP ini hanya menyentuh `app/src`.

Deviasi yang diakui sejak awal: WP ini menghapus 20 berkas, melebihi panduan
1–5 berkas per WP. Alasannya, satuannya adalah penghapusan paket mati:
memecahnya menjadi beberapa WP menghasilkan keadaan antara yang tidak bermakna
(paket tanpa shader, atau shader tanpa pembaca) tanpa menambah nilai tinjauan.
Seluruh perubahan bersifat penghapusan dan tidak mengubah perilaku.

---

## Traceability Matrix

### Temuan audit → WP

| Temuan pengguna | ID | WP | Bukti |
|---|---|---|---|
| P0-1 foreground service + bind ke service | CWG-11 | WP-17, WP-18 | build + lint, MANUAL-VERIFY layar mati |
| P0-1 autostart boot | CWG-12 | WP-19 | manifest + MANUAL-VERIFY boot |
| P0-2 hapus audio, mic, ML Kit | CWG-14, CWG-N02 | WP-03, WP-04, WP-05, WP-15, WP-16 | grep kosong, lint error hilang |
| P0-3 header part MJPEG | CWG-02 | WP-09, WP-12 | `MultipartWriterTest` |
| P0-4 `/status` terukur | CWG-04, CWG-05, CWG-13, CWG-16 | WP-10, WP-12, WP-22 | `StatusRegistryTest` |
| P0-5 token `X-Api-Key` | CWG-15 | WP-25, WP-26, WP-27 | `ApiAuthTest`, MANUAL-VERIFY 401 |
| P1-6 enkode tunggal, sensor headless | CWG-N01, CWG-N02 | WP-06, WP-07, WP-13, WP-14, WP-15, WP-16 | grep `compressToJpeg` = 1 |
| P1-7 kunci AE/AWB/AF | CWG-07 | WP-24 | grep tiga kunci, MANUAL-VERIFY |
| P1-7 orientasi tetap | CWG-06 | WP-20, WP-06, WP-07 | manifest + `Nv21TransformTest` |
| P1-7 resolusi/FPS dari konfigurasi | CWG-08 | WP-21, WP-22 | `CameraConfigTest` |
| P1-8 dedupe frame | CWG-03 | WP-11, WP-12 | `StreamSessionTest` |
| P1-8 batas klien | CWG-10 | WP-11, WP-12 | `StreamSessionTest` |
| P1-8 batas waktu tulis | CWG-09 | WP-11, WP-12 | `StreamSessionTest`, MANUAL-VERIFY |
| P2-9 `ResolutionSelector` | CWG-08 | WP-23 | grep `setTargetResolution` kosong |
| P2-10 test header dan dedupe | CWG-N06 | WP-06, WP-08, WP-09, WP-10, WP-11, WP-21, WP-25, WP-28 | `testDebugUnitTest` |
| P2-11 lisensi repo | CWG-N03 | WP-02 | `LICENSE`, `README.md` |
| P0-2 nomor urut dan waktu tangkap frame (B-24) | CWG-02, CWG-N04 | WP-08, WP-09, WP-14 | `FrameSourceTest` |
| P1-6 jalur H.264 tidak lagi menambah encode (B-30) | CWG-N01, CWG-N02 | WP-31 | grep `StreamEncoder\|TcpStreamServer` kosong, build |
| P1-6 sisa jalur filter GPU dan sumber daya yatim | CWG-N02 | WP-36 | grep `gpuimage\|FilterManager\|FilterType\|bg_filter_chip` kosong, build |

### Requirement → WP

| Requirement | WP | Verifikasi |
|---|---|---|
| CWG-01 | WP-12, WP-29 | build, `/video` melayani klien |
| CWG-02 | WP-08, WP-09, WP-14 | `FrameSourceTest`, `MultipartWriterTest` |
| CWG-03 | WP-11, WP-12 | `StreamSessionTest` kasus dedupe |
| CWG-04 | WP-10, WP-12 | `StatusRegistryTest` kasus FPS |
| CWG-05 | WP-07, WP-10, WP-22 | `StatusRegistryTest`, `/status` |
| CWG-06 | WP-06, WP-20, WP-22 | `Nv21TransformTest`, manifest |
| CWG-07 | WP-24 | grep kunci, MANUAL-VERIFY |
| CWG-08 | WP-21, WP-22, WP-23 | `CameraConfigTest`, grep |
| CWG-09 | WP-11, WP-12 | `StreamSessionTest`, MANUAL-VERIFY |
| CWG-10 | WP-11, WP-12 | `StreamSessionTest` batas klien |
| CWG-11 | WP-17, WP-18 | manifest, MANUAL-VERIFY |
| CWG-12 | WP-19 | manifest, MANUAL-VERIFY |
| CWG-13 | WP-10, WP-14, WP-17 | `StatusRegistryTest`, grep status |
| CWG-14 | WP-03, WP-04, WP-05, WP-20 | grep kosong, lint bersih |
| CWG-15 | WP-25, WP-26, WP-27 | `ApiAuthTest`, MANUAL-VERIFY 401 |
| CWG-16 | WP-10, WP-14, WP-24 | `StatusRegistryTest` kasus gagal |
| CWG-N01 | WP-06, WP-07, WP-14, WP-16, WP-31 | grep enkode, ukuran APK |
| CWG-N02 | WP-13, WP-15, WP-16, WP-36 | grep kosong, dependensi dihapus |
| CWG-N03 | WP-02, WP-26 | `LICENSE`, `git check-ignore` |
| CWG-N04 | WP-08, WP-14 | `FrameSourceTest` |
| CWG-N05 | WP-10, WP-30 | `StatusRegistryTest`, checklist |
| CWG-N06 | WP-06..WP-11, WP-21, WP-25, WP-28 | `testDebugUnitTest`, `lintDebug` |

## MANUAL-VERIFY

Seluruh butir berikut **belum diuji**; tidak ada perangkat pada sesi ini.

1. Stream tetap hidup saat layar mati dan Activity ditutup (WP-18).
2. Service aktif otomatis setelah perangkat dinyalakan ulang (WP-19).
3. Dua klien menerima stream bersamaan; klien ketiga ditolak (WP-11, WP-12).
4. Klien lambat diputus tanpa menghentikan server (WP-11, WP-12).
5. FPS terukur dan bandwidth nyata pada 1280x720/20/quality 80 (WP-10, WP-30).
6. Kunci AE/AWB/AF bertahan setelah warmup (WP-24).
7. Orientasi frame keluar sesuai pemasangan conveyor (WP-06, WP-20).
8. Penolakan `401` tanpa token dan penerimaan dengan token benar (WP-27).
9. Tidak ada permintaan izin mikrofon saat aplikasi dibuka (WP-05).
10. Nilai eksposur tidak berubah antara dua sesi pengambilan (WP-24).

## Blockers

| ID | Masalah | WP terdampak | Status |
|---|---|---|---|
| B-03 | Geometri pemasangan kamera (posisi, tinggi, orientasi) belum ditetapkan | WP-20 (nilai orientasi akhir), WP-06 | terbuka; WP memakai nilai konfigurasi dan menandai belum terverifikasi |
| B-06 | Ambang FPS dan latensi belum disepakati | WP-10, WP-28, WP-30 | terbuka; WP tidak memakai ambang sebagai kriteria lulus |
| B-30 | Status jalur H.264 (`StreamEncoder`, `TcpStreamServer`): dipensiunkan atau dipertahankan | WP-31 | **selesai** (D-30, 7 Nov 2026: jalur H.264 dihapus) |
| — | Pengesahan alias `CWG-` | seluruh traceability | **selesai** (D-31, 7 Nov 2026: disahkan) |

## Planner Notes

1. Urutan WP tidak mengikuti urutan prioritas temuan, karena gerbang lint baru
   bersih setelah jalur audio dihapus. Semua butir P0 tetap wajib selesai
   sebelum backend menyambung; WP-28 menjadi gerbang akhirnya.
2. Tidak ada dependensi baru yang dipilih. `Camera2Interop` dan
   `ResolutionSelector` sudah tersedia pada CameraX 1.4.0 yang terpasang.
   Robolectric sengaja tidak ditambahkan; karena itu seluruh test berada pada
   Kotlin murni.
3. `B-30` ditutup oleh D-30: jalur H.264 dihapus pada WP-31. Revisi 1 sempat
   menundanya karena keputusan pengguna belum ada; revisi 2 memasukkannya ke
   lingkup. WP-31 bergantung pada WP-18 karena pemanggilan encoder berada di
   `MainActivity.kt` yang juga dipindahkan ke service pada WP-18.
4. Paket `gpuimage` dihapus pada WP-36, bukan di dalam WP-15. Revisi 1
   menundanya sampai hasil WP-15 diketahui; hasilnya, build WP-15 lulus tetapi
   kriteria grep `FilterManager`/`FilterType` tidak dapat dipenuhi tanpa
   menghapus paket itu. Sesuai mitigasi tertulis WP-15, Planner menambah WP
   lanjutan alih-alih memperbesar WP-15. WP-36 juga memuat sisa temuan QC-37
   (drawable `bg_filter_chip*` dan dimens `filter_menu_*`).
5. Setiap WP kode wajib menjalankan preflight sumber daya lebih dulu. Build
   Gradle pertama pada repo ini memerlukan sekitar tujuh menit.
6. Angka 1280x720, 20 FPS, quality 80, dan maks 2 klien adalah default
   konfigurasi (D-29), bukan ambang kelulusan. Klaim mutu menunggu B-06 dan
   pengukuran perangkat.
