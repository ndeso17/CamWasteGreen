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

Status: qc

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

---

## WP-03 — Hapus endpoint `/audio` dan `/mic/toggle`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt`

Dependensi: WP-01.

Tugas:
- [ ] T-006 Hapus cabang rute `/audio` dan `/mic/toggle`, fungsi
      `audioInputStream()`, serta field `mic` pada respons `/status` (CWG-14).
- [ ] T-007 Pastikan server tidak lagi merujuk `MicManager` (CWG-14).

Kriteria terima:
- `grep -n "audio\|mic" app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt`
  tidak menemukan rute maupun rujukan `MicManager`.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan ringkasan build pada Log WP.

Risiko: klien demo lama memanggil `/audio` dan menerima 404.
Mitigasi: tidak ada klien sah pada arsitektur D-23; dicatat di Log sebagai
perubahan yang disengaja.

---

## WP-04 — Hapus kontrol mikrofon dan equalizer dari UI

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`

Dependensi: WP-03.

Tugas:
- [ ] T-008 Hapus field `btnMicToggle` dan `equalizerView`, listener mikrofon,
      serta pemanggilan `MicManager.toggle()`, `MicManager.stop()`, dan
      `setActive(...)` (CWG-14).
- [ ] T-009 Hapus `RECORD_AUDIO` dari `REQUIRED_PERMISSIONS` sehingga hanya
      `CAMERA` yang diminta (CWG-14).
- [ ] T-010 Hapus view tombol mikrofon dan `EqualizerView` dari
      `activity_main.xml` (CWG-14).

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

---

## WP-05 — Hapus `MicManager`, `EqualizerView`, izin audio, dan drawable mikrofon

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MicManager.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/EqualizerView.kt` (hapus), `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_mic.xml` (hapus), `app/src/main/res/drawable/ic_mic_off.xml` (hapus)

Dependensi: WP-03, WP-04.

Tugas:
- [ ] T-011 Hapus tiga berkas audio/equalizer (CWG-14).
- [ ] T-012 Hapus `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
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

---

## WP-06 — Transformasi NV21 murni untuk rotasi frame

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/Nv21Transform.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/Nv21TransformTest.kt` (baru)

Dependensi: WP-05.

Tugas:
- [ ] T-013 Implementasikan `rotateNv21(yuv: ByteArray, width: Int, height: Int, degrees: Int): ByteArray`
      untuk 0/90/180/270 derajat, Kotlin murni tanpa `android.*`, dengan
      penanganan dimensi ganjil untuk bidang chroma (CWG-06, CWG-N01).
- [ ] T-014 Tulis unit test: identitas pada 0 derajat, hasil 90 dan 270 saling
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

---

## WP-07 — `FrameEncoder` dengan satu kali enkode JPEG

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameEncoder.kt` (baru)

Dependensi: WP-06.

Tugas:
- [ ] T-015 Implementasikan `FrameEncoder.encode(image: ImageProxy, quality: Int): EncodedFrame?`
      yang mengubah YUV_420_888 ke NV21, merotasi memakai `Nv21Transform`
      sesuai `image.imageInfo.rotationDegrees`, lalu memanggil
      `YuvImage.compressToJpeg` **tepat satu kali** (CWG-N01, CWG-06).
- [ ] T-016 Kembalikan lebar dan tinggi hasil rotasi bersama byte JPEG agar
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

---

## WP-08 — `FramePacket` dan `FrameSource` dengan nomor urut serta waktu tangkap

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/FramePacket.kt` (baru), `app/src/main/java/dev/ndeso17/camwastegreen/stream/FrameSource.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/FrameSourceTest.kt` (baru)

Dependensi: WP-07.

Tugas:
- [ ] T-017 Buat `FramePacket(seq: Long, capturedAtMs: Long, width: Int, height: Int, jpeg: ByteArray)`
      sebagai data class murni (CWG-02, CWG-N04).
- [ ] T-018 Buat `FrameSource` dengan `publish(jpeg, width, height, capturedAtMs): FramePacket`,
      penghitung `seq` monoton yang dimulai dari 1, dan `latest: FramePacket?`;
      waktu tangkap berasal dari pemanggil, bukan dari jam di dalam `FrameSource`
      (CWG-02, CWG-03).
- [ ] T-019 Unit test: `seq` naik satu tiap `publish`, `latest` menunjuk paket
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

---

## WP-09 — `MultipartWriter` untuk header part MJPEG

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MultipartWriter.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/MultipartWriterTest.kt` (baru)

Dependensi: WP-08.

Tugas:
- [ ] T-020 Implementasikan `partHeader(packet: FramePacket, boundary: String): ByteArray`
      yang menghasilkan blok header dengan `Content-Type: image/jpeg`,
      `Content-Length`, `X-Frame-Seq`, `X-Frame-Captured-At-Ms`,
      `X-Frame-Width`, `X-Frame-Height`, diakhiri CRLF ganda (CWG-02).
- [ ] T-021 Implementasikan `part(packet, boundary): ByteArray` = header + JPEG +
      CRLF penutup, tanpa menyalin JPEG dua kali (CWG-N01).
- [ ] T-022 Unit test: enam header wajib ada, nilai `Content-Length` sama dengan
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

---

## WP-10 — `StatusRegistry` untuk status terukur

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/StatusRegistry.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/StatusRegistryTest.kt` (baru)

Dependensi: WP-08.

Tugas:
- [ ] T-023 Implementasikan `StatusRegistry` dengan sumber waktu yang disuntikkan
      (`nowMs: () -> Long`), pencatatan frame untuk FPS terukur pada jendela
      bergulir, serta status kamera berupa `cameraState` dan `cameraStateReason`
      (CWG-04, CWG-13).
- [ ] T-024 Sediakan `snapshot(): StatusSnapshot` berisi `frameSeq`,
      `lastFrameAtMs`, `measuredFps`, `width`, `height`, `clients`,
      `cameraState`; nilai yang belum diketahui bernilai `null`, bukan `0`
      (CWG-04, CWG-05, CWG-N05).
- [ ] T-025 Unit test: FPS terukur dari 11 frame pada jarak 100 ms dalam jendela
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

---

## WP-11 — `StreamSession` untuk dedupe, batas klien, dan batas waktu tulis

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/StreamSession.kt` (baru), `app/src/test/java/dev/ndeso17/camwastegreen/stream/StreamSessionTest.kt` (baru)

Dependensi: WP-08, WP-10.

Tugas:
- [ ] T-026 Implementasikan penjaga dedupe: `shouldSend(packet): Boolean` yang
      menolak paket dengan `seq` sama atau lebih kecil daripada `lastSentSeq`
      (CWG-03).
- [ ] T-027 Implementasikan batas klien: `tryAcquire(): Boolean` dan `release()`,
      menolak ketika jumlah aktif mencapai `maxClients` (CWG-10).
- [ ] T-028 Implementasikan konfigurasi batas waktu tulis dan penanda klien
      lambat sebagai nilai yang dapat diperiksa, tanpa bergantung pada `android.*`
      (CWG-09).
- [ ] T-029 Unit test: paket `seq` sama tidak dikirim dua kali, klien ketiga
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

---

## WP-12 — Server MJPEG baru berbasis `FrameSource` dan `StatusRegistry`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/stream/MjpegHttpServer.kt` (baru), `app/src/main/java/dev/ndeso17/camwastegreen/MjpegHttpServer.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/MJPEGServerInstance.kt`

Dependensi: WP-09, WP-11.

Tugas:
- [ ] T-030 Pindahkan server ke `stream/MjpegHttpServer.kt`: `/video` memakai
      `MultipartWriter` dan hanya menulis paket baru, `/status` memakai
      `snapshot()` dari `StatusRegistry`, `/` menjadi halaman diagnostik tanpa
      tautan audio (CWG-01, CWG-03, CWG-04, CWG-05).
- [ ] T-031 Terapkan `StreamSession` untuk batas klien dan penutupan pipa klien
      lambat; hentikan pola `Thread.sleep(50)` tanpa cek `seq` (CWG-09, CWG-10).
- [ ] T-032 Perbarui `MJPEGServerInstance` agar memakai kelas baru dan menerima
      `FrameSource` serta `StatusRegistry` (CWG-01).
- [ ] T-033 Hapus berkas `MjpegHttpServer.kt` lama (CWG-01).

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

---

## WP-13 — Hapus menu filter dari `MainActivity`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`

Dependensi: WP-12.

Tugas:
- [ ] T-034 Hapus `setupFilterMenu`, `selectFilterChip`, field menu filter, dan
      pemanggilan `FilterPipeline.init` serta `FilterPipeline.shutdown` (CWG-N02).
- [ ] T-035 Hapus impor `AiProcessor`, `FilterType`, dan `FilterPipeline`
      (CWG-N02).

Kriteria terima:
- `grep -n "FilterPipeline\|AiProcessor\|FilterType\|overlay_filter_menu" app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`
  kosong.
- `./gradlew assembleDebug` lulus.
- Bukti: keluaran grep dan build pada Log WP.

Risiko: penghapusan menyisakan referensi `R.id.chip*` yang tidak ada.
Mitigasi: build wajib lulus; bila gagal, perbaiki dalam WP yang sama sebelum
menyerahkan ke QC.

---

## WP-14 — Jalur analyzer memakai `FrameEncoder` dan `FrameSource`

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/MainActivity.kt`, `app/src/main/java/dev/ndeso17/camwastegreen/FrameBuffer.kt` (hapus)

Dependensi: WP-07, WP-08, WP-12, WP-13.

Tugas:
- [ ] T-036 Ganti isi analyzer: ambil `imageProxy`, catat `capturedAtMs` saat
      frame diterima, panggil `FrameEncoder.encode`, lalu `FrameSource.publish`
      dengan lebar/tinggi hasil rotasi (CWG-02, CWG-N01, CWG-N04).
- [ ] T-037 Hapus jalur cadangan `imageProxyToJpegBytes` dan pemakaian
      `FrameBuffer`; hapus `FrameBuffer.kt` (CWG-N01).
- [ ] T-038 Catat kegagalan enkode ke `StatusRegistry` sebagai `cameraState`
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

---

## WP-15 — Hapus `FilterPipeline`, paket `ai`, dan layout menu filter

Status: todo

File: `app/src/main/java/dev/ndeso17/camwastegreen/pipeline/FilterPipeline.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/AiProcessor.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/FaceDetector.kt` (hapus), `app/src/main/java/dev/ndeso17/camwastegreen/ai/SelfieSegmenter.kt` (hapus), `app/src/main/res/layout/overlay_filter_menu.xml` (hapus)

Dependensi: WP-13, WP-14.

Tugas:
- [ ] T-039 Hapus berkas pipeline filter, seluruh paket `ai`, dan layout menu
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

Dependensi: WP-01..WP-27, WP-31 (seluruh WP kode, termasuk penghapusan jalur
H.264, selesai lebih dulu).

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

## WP-29 — Sinkronkan `AGENTS.md` dan dokumen `.work/`

Status: todo

File: `AGENTS.md`, `.work/Structure.md`, `.work/Architecture.md`

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

Kriteria terima:
- `grep -rn "MicManager\|FilterPipeline\|FrameBuffer\|previewView" AGENTS.md .work/Structure.md .work/Architecture.md`
  kosong.
- `grep -n "B-21\|B-28\|B-29" AGENTS.md` hanya menemukan rujukan yang
  menyatakan blocker tersebut selesai dengan D-27..D-29, bukan sebagai
  keputusan terbuka.
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
| CWG-N02 | WP-13, WP-15, WP-16 | grep kosong, dependensi dihapus |
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
4. Paket `gpuimage` belum dihapus pada rencana ini karena penghapusannya
   bergantung pada hasil WP-15. Bila WP-15 gagal karena rujukan `FilterType`,
   Executor wajib membuat temuan dan Planner menambah WP lanjutan, bukan
   memperbesar WP-15.
5. Setiap WP kode wajib menjalankan preflight sumber daya lebih dulu. Build
   Gradle pertama pada repo ini memerlukan sekitar tujuh menit.
6. Angka 1280x720, 20 FPS, quality 80, dan maks 2 klien adalah default
   konfigurasi (D-29), bukan ambang kelulusan. Klaim mutu menunggu B-06 dan
   pengukuran perangkat.
