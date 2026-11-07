# Rules — CamWasteGreen

**Repo:** `CamWasteGreen/`
**Status:** Draf kerja
**Dokumen terkait:** `PRD.md`, `Architecture.md`, `Structure.md`

Aturan di bawah bersifat keras. Pelanggaran berarti work package ditolak,
bukan diperbaiki sepihak.

## 1. Batas kewenangan

1. Aplikasi tidak menjalankan inferensi model, tidak mengklasifikasi objek,
   tidak memilih target, dan tidak mengirim perintah ke robot. Perubahan
   pada batas ini harus melalui keputusan di `.work/DECISIONS.md` root.
2. Aplikasi tidak memiliki endpoint yang meneruskan perintah ke ESP32-S3.
3. Penghentian darurat adalah urusan perangkat keras, bukan fitur aplikasi.

## 2. Batas jalur sensor

4. Tidak ada pemrosesan AI, deteksi wajah, segmentasi selfie, atau filter
   visual pada jalur yang menghasilkan frame untuk backend.
5. Setiap frame di-encode JPEG tepat satu kali. Enkode ganda dilarang.
6. `STRATEGY_KEEP_ONLY_LATEST` wajib dipakai pada `ImageAnalysis`. Antrean
   frame yang menumpuk membuat usia pengamatan menyesatkan.

## 3. Metadata frame

7. Nomor urut frame monoton dan diambil saat frame ditangkap, bukan saat
   frame dikirim.
8. Waktu pengambilan memakai jam perangkat dalam epoch milidetik.
9. Server tidak boleh mengirim frame dengan `seq` yang sama dua kali ke klien
   yang sama.
10. Header part multipart minimal memuat `X-Frame-Seq`,
    `X-Frame-Captured-At-Ms`, `X-Frame-Width`, `X-Frame-Height`.

## 4. Status dan angka

11. `/status` hanya melaporkan nilai yang diukur. Nilai konfigurasi tidak boleh
    dilaporkan sebagai hasil pengukuran.
12. Tidak ada angka FPS, latensi, bandwidth, atau tingkat keberhasilan yang
    dikarang. Ambang hanya boleh dipakai setelah disepakati.
13. Nilai yang tidak diketahui diisi `null` atau tidak dikirim, bukan `0`.
14. Kegagalan streaming, kamera, atau jaringan harus terlihat pada status
    beserta alasannya. Status tidak boleh tampak sehat saat ada gangguan.

## 5. Operation

15. Kamera di-bind ke foreground service bertipe `camera`, bukan ke Activity.
16. Service dimulai otomatis saat boot.
17. Service tidak berhenti hanya karena Activity selesai atau layar mati.
18. Izin `RECORD_AUDIO` dan seluruh kelas audio tidak boleh ada.

## 6. Keamanan

19. Server tidak listening ke semua antarmuka tanpa batas. Bentuk final
    mengikuti keputusan otorisasi B-28; sebelum itu akses dibatasi dan
    penolakan dicatat.
20. Tidak ada tunneling ke internet dan tidak ada cloud proxy.
21. Token, kunci, dan kredensial berada pada berkas di luar repo. Repo tidak
    memuat rahasia, termasuk pada berkas contoh.
22. Cleartext HTTP hanya boleh dipakai pada jaringan ruang kontrol dan harus
    dicatat sebagai keterbatasan yang diketahui, bukan disembunyikan.

## 23. Mutu dan bukti

23. Build, unit test, dan lint wajib lulus untuk setiap perubahan kode atau
    konfigurasi build.
24. Lint error tidak boleh ditutup dengan baseline tanpa alasan tertulis di
    work package.
25. Uji harus menguji perilaku, bukan menyalin implementasi.
26. Bukti manual untuk kamera dan USB pada HP sasaran, kelangsungan service
    saat layar mati, autostart saat boot, FPS nyata, dan bandwidth ditandai
    `MANUAL-VERIFY` sampai perangkat tersedia. Tidak ada klaim lulus untuk
    hal yang belum diuji.
27. Angka ambang yang dipakai sebagai kriteria terima harus berasal dari
    pengukuran perangkat, bukan estimasi.

## 7. Proses

28. Semua perubahan mengikuti pipeline Planner, Executor, dan QC pada
    `AGENTS.md` root.
29. Alias ID kebutuhan memakai namespace `CWG-` sesuai `PRD.md` repo ini.
    Alias ini belum disahkan pengguna; sampai disahkan, traceability dicatat
    sebagai belum final.
30. Keputusan baru yang mengubah arsitektur dicatat di `.work/DECISIONS.md`
    root beserta sumber persetujuan.

## 8. Baseline saat ini

| Pemeriksaan | Status | Catatan |
|---|---|---|
| `./gradlew assembleDebug` | lulus | build baseline setelah rename paket |
| `./gradlew testDebugUnitTest` | lulus | hanya test placeholder upstream |
| `./gradlew lintDebug` | gagal | 1 error `MicManager.kt:54` MissingPermission; 111 warning |

Error lint tersebut hilang bersama penghapusan audio pada work package
pembersihan jalur sensor.
