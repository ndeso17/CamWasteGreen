# Baseline gerbang mutu CamWasteGreen

Tanggal pengambilan: 2026-11-08
Commit yang diuji: `380fd359da005dbb9712b1f75ddebd6f982b2ace`
(`refactor(wp-31): hapus jalur H.264 StreamEncoder dan TcpStreamServer`)

Berkas ini adalah catatan hasil perintah, bukan sertifikat kelulusan produk.
Yang dibuktikan: build, unit test, dan lint berjalan pada commit di atas.
Yang **tidak** dibuktikan: perilaku kamera, jaringan, atau perangkat nyata.

## Toolchain

| Komponen | Versi |
|---|---|
| Gradle wrapper | 8.13 |
| AGP | 8.13.0 (`gradle/libs.versions.toml`) |
| Kotlin | 2.0.21 (`gradle/libs.versions.toml`) |
| Launcher JVM | 21.0.8 (JetBrains s.r.o.) |
| compileSdk / targetSdk | 36 / 34 |
| minSdk | 24 |

## Perintah dan hasil

Setiap perintah dijalankan terpisah, exit code dibaca langsung (tanpa pipe).

```text
./gradlew assembleDebug          -> ASSEMBLE_EXIT=0
./gradlew testDebugUnitTest      -> TEST_EXIT=0
./gradlew lintDebug              -> LINT_EXIT=0
```

APK hasil build:

```text
app/build/outputs/apk/debug/IPCam_08November2026_0629.apk
8.700.751 byte
```

Lint: **0 error**, 61 warning.

## Unit test

Total **81 test**, 0 skipped, 0 failure, 0 error.

| Kelas test | Jumlah |
|---|---|
| `ExampleUnitTest` | 1 |
| `stream.ApiAuthTest` | 9 |
| `stream.CameraConfigTest` | 10 |
| `stream.FrameSourceTest` | 8 |
| `stream.MultipartWriterTest` | 7 |
| `stream.NetworkChoiceTest` | 8 |
| `stream.Nv21TransformTest` | 10 |
| `stream.StatusRegistryTest` | 9 |
| `stream.StreamInterfacePrefsTest` | 7 |
| `stream.StreamSessionTest` | 12 |

## Rincian warning lint (61)

| ID | Jumlah | Catatan |
|---|---|---|
| `UnusedResources` | 17 | sisa sumber daya upstream; belum dibersihkan |
| `HardcodedText` | 14 | teks layout lama; teks baru sudah `@string` |
| `GradleDependency` | 11 | versi dependensi lebih baru tersedia |
| `UseTomlInstead` | 7 | dependensi literal di `build.gradle.kts` |
| `AndroidGradlePluginVersion` | 2 | AGP lebih baru tersedia |
| `MonochromeLauncherIcon` | 2 | ikon launcher |
| `OldTargetApi` | 1 | targetSdk 34 |
| `LockedOrientationActivity` | 1 | akibat WP-20 (orientasi dikunci) |
| `NewerVersionAvailable` | 1 | — |
| `DiscouragedApi` | 1 | QC-52, menunggu Planner |
| `PluralsCandidate` | 1 | — |
| `Overdraw` | 1 | — |
| `TypographyDashes` | 1 | — |
| `SetTextI18n` | 1 | — |

Tidak ada warning yang menunjuk kelas baru hasil WP-23..WP-27 atau WP-31.

## Perbandingan dengan baseline sebelumnya

| Waktu | Error | Warning | Test |
|---|---|---|---|
| Setelah rename paket (dokumen `AGENTS.md`) | 1 (`MicManager.kt:54`) | 111 | belum dicatat |
| Sebelum WP-35 (2026-11-08) | 0 | 63 | belum dicatat terpisah |
| Baseline ini (`380fd35`) | 0 | 61 | 81 |

Penurunan 111 → 0 error berasal dari penghapusan jalur audio (WP-03..WP-05).
Penurunan 63 → 61 berasal dari dua teks UI baru pada WP-35 yang memakai
`@string` (bukan `HardcodedText`).

## Yang tidak dibuktikan di sini

- Kamera, encoder, dan server tidak dijalankan: tidak ada perangkat yang
  terhubung saat pengambilan baseline (`adb devices` kosong).
- Token API belum diisi di `local.properties` mesin ini, sehingga APK pada
  commit ini membawa `API_KEY` kosong dan `/video` serta `/status` akan
  menolak semua permintaan.
- Seluruh butir `MANUAL-VERIFY` tetap `BELUM DIUJI`; lihat
  `.work/evidence/manual-verify-kamera.md` (WP-30).

## Cara mengulang

```bash
cd CamWasteGreen
./gradlew assembleDebug > /tmp/assemble.log 2>&1; echo "ASSEMBLE_EXIT=$?"
./gradlew testDebugUnitTest > /tmp/test.log 2>&1; echo "TEST_EXIT=$?"
./gradlew lintDebug > /tmp/lint.log 2>&1; echo "LINT_EXIT=$?"
ls -l app/build/outputs/apk/debug/*.apk
python3 - <<'PY'
import glob, re
tot = fail = 0
for p in glob.glob('app/build/test-results/testDebugUnitTest/TEST-*.xml'):
    x = open(p).read()
    tot += int(re.search(r'tests="(\d+)"', x).group(1))
    fail += int(re.search(r'failures="(\d+)"', x).group(1))
print("tests", tot, "failures", fail)
PY
```
