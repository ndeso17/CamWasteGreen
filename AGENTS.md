# PROJECT KNOWLEDGE BASE — CamWasteGreen

Updated: 2026-11-07
Package: `dev.ndeso17.camwastegreen`
Branch: `main`

## OVERVIEW

Android app that turns a phone into a **camera sensor** for the waste-sorting
system. CameraX captures frames, the app serves them over LAN as MJPEG with
per-frame metadata, and reports camera/stream health. The app does **not**
run inference, does not pick targets, and does not talk to robots.

Upstream basis: fork of `https://github.com/ndeso17/ipcam`. Code was reused,
not rewritten. The upstream demo features (audio, ML Kit, GPU filters, virtual
camera driver, OBS bridge) are **not** part of the operating path.

## SOURCE OF TRUTH

| Dokumen | Isi |
|---|---|
| `.work/PRD.md` | Kebutuhan `CWG-01..16`, `CWG-N01..N06`, kontrak stream |
| `.work/Architecture.md` | Komponen, kepemilikan state, alur frame |
| `.work/Structure.md` | Struktur berkas dan tanggung jawab |
| `.work/Rules.md` | Batas keras implementasi |
| `../AGENTS.md` (root) | Kontrak proses pipeline Planner/Executor/QC |
| `../.work/FINDINGS.md` (root) | Blocker `B-21`, `B-24`, `B-28`, `B-29`, `B-03`, `B-06` |

## STACK

- **Language**: Kotlin
- **UI**: View-based (AppCompat + Material)
- **Camera**: CameraX (camera2 + lifecycle)
- **Server**: NanoHTTPD, MJPEG multipart over HTTP
- **Removed from sensor path**: ML Kit (face detection, selfie segmentation), OpenGL filter pipeline, `AudioRecord`
- **minSdk 24**, compile/targetSdk 36

## CURRENT STRUCTURE

```
CamWasteGreen/
├── app/src/main/java/dev/ndeso17/camwastegreen/
│   ├── MainActivity.kt          # UI operator; camera + server orchestration
│   ├── MjpegHttpServer.kt       # NanoHTTPD: /video, /status, /mic/toggle
│   ├── MJPEGServerInstance.kt   # Singleton wrapper server
│   ├── FrameBuffer.kt           # latestFrame: ByteArray?
│   ├── MicManager.kt            # AudioRecord (to be removed)
│   ├── StreamEncoder.kt         # H.264 encoder (unused in v1)
│   ├── TcpStreamServer.kt       # TCP NAL server (unused in v1)
│   ├── EqualizerView.kt         # Audio visualizer (unused in v1)
│   ├── ai/                      # ML Kit wrappers (to be removed)
│   ├── gpuimage/                # OpenGL filters (to be removed)
│   └── pipeline/FilterPipeline.kt  # filter + AI + double JPEG encode
├── .work/                       # PRD, Architecture, Structure, Rules
├── docs/                        # upstream H.264 design spec
└── tools/                       # upstream virtual camera driver scripts (unused)
```

## KNOWN DEFECTS (audit 2026-11-07)

P0:

1. No foreground service. Camera bound to Activity lifecycle
   (`MainActivity.kt` `bindToLifecycle(this, ...)`). Screen off or app killed
   stops the stream. Manifest declares `FOREGROUND_SERVICE_CAMERA` but there
   is no Service class.
2. `/mic/toggle` POST without authentication. Anyone on the LAN can enable
   the phone microphone.
3. Server has no authentication, binds all interfaces, `usesCleartextTraffic=true`.

P1:

4. Frames carry no sequence number and no capture timestamp. Blocks `B-24`.
5. Server resends the same frame every 50 ms (`Thread.sleep(50)` without seq check).
6. `/status` reports `fps` from a configuration value, not a measurement.
7. Two JPEG encodes per frame plus ML Kit on the sensor path.
8. No AE/AWB/AF lock. Color shifts between classes.
9. Manifest `screenOrientation="fullSensor"`. Mounted phone can rotate.
10. Blocking writes to `PipedOutputStream`, no client limit.

## WHERE TO LOOK

| Task | Location |
|---|---|
| Camera setup and analyzer | `MainActivity.kt` |
| MJPEG streaming and status | `MjpegHttpServer.kt` |
| Frame storage | `FrameBuffer.kt` |
| Filter and AI pipeline | `pipeline/FilterPipeline.kt`, `ai/` |
| Manifest permissions | `app/src/main/AndroidManifest.xml` |

## COMMANDS

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew installDebug
```

Baseline at package rename: build pass, unit test pass, lint fail with 1 error
(`MicManager.kt:54` MissingPermission) and 111 warnings. The lint error
disappears when the audio path is removed.

## OPEN DECISIONS

`B-21` stream contract, `B-24` frame timestamp and encoder correspondence,
`B-28` API authorization, `B-29` fork license, `B-03` camera mounting
geometry, `B-06` agreed FPS and latency thresholds. Alias namespace `CWG-`
awaiting user ratification.
