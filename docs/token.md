# Token API `/video` dan `/status`

Dokumen ini menjelaskan token statis yang dipakai server MJPEG (D-27, D-34).
Token **dibuat saat server start** dan tidak pernah disimpan di repositori
maupun di dalam APK.

## Ringkas

| Hal | Nilai |
|---|---|
| Header permintaan | `X-Api-Key: <token>` |
| Endpoint yang dijaga | `/video`, `/status` |
| Endpoint bebas | `/` (halaman diagnosa, hanya tautan) |
| Balasan bila ditolak | `401` dengan badan JSON `{"error":{"code":"...","message":"..."}}` |
| Sumber token | dibuat acak oleh `SessionToken.generate()` setiap kali server start |
| Panjang token | 16 karakter hex huruf besar (8 byte acak) |
| Tempat melihat token | layar aplikasi, baris `Token X-Api-Key: …` |
| Cara mengambil token | tombol salin (ikon salin) atau tombol bagikan (ikon bagikan) |
| Masa berlaku | sampai stream dihentikan; start berikutnya menghasilkan token baru |

## Cara kerja

1. Operator menekan tombol mulai. `CameraService` mengikat kamera lalu memanggil
   `MJPEGServerInstance.start`.
2. `MJPEGServerInstance` membuat token 8 byte acak (`SecureRandom`), dikodekan
   hex huruf besar menjadi 16 karakter. Panjang ini masih cukup acak untuk
  /brute force lewat HTTP di LAN, tetapi ringkas untuk dibaca di layar.
3. Token hanya disimpan di memori proses dan diberikan ke `MjpegHttpServer`.
   Tidak ditulis ke disk, tidak dikirim ke log, tidak masuk `BuildConfig`.
4. `MainActivity` menampilkan token pada layar selama stream berjalan. Setelah
   stream dihentikan, baris token kembali kosong.
5. Dua tombol di samping baris token: **salin** menyalin token ke papan klip,
   **bagikan** membuka chooser untuk mengirim teks
   `Token X-Api-Key CamWasteGreen: <token>`. Keduanya dinonaktifkan saat stream
   tidak berjalan karena token belum ada.
6. Klien mengirim token itu pada setiap permintaan `/video` dan `/status`.

Karena token dibuat per sesi, tidak ada lagi langkah menyunting
`local.properties` sebelum build. APK yang beredar tidak memuat kunci yang bisa
diekstrak, dan token tidak perlu diganti dengan membangun ulang APK.

## Konsekuensi yang harus diketahui

1. **Token harus diambil dari layar setiap sesi** lewat tombol salin atau
   bagikan. Klien yang berjalan otomatis perlu diperbarui setelah stream
   dihentikan dan dimulai lagi.
2. **Token ikut terlihat di layar.** Siapa pun yang melihat layar perangkat
   dapat membaca token. Batas jaringan (`pilihan interface` pada WP-33..WP-35)
   tetap lapis pertama.
3. **`usesCleartextTraffic=true`.** Token dan video dikirim tanpa TLS, jadi
   dapat dibaca pihak yang menyadap jaringan. Perubahan ke TLS menunggu
   keputusan tersendiri (bukan bagian dari penambalan ini).
4. **Log tidak memuat token.** Penolakan dicatat sebagai kode alasan dan alamat
   klien saja.

## Daftar alamat yang diizinkan

`camwastegreen.allowedAddresses` masih dibaca dari `local.properties` saat
build (dipisah koma). Kosong atau tidak ada berarti **tanpa batas alamat**;
token tetap wajib. Bila diisi, jalankan `curl` dari alamat terdaftar; alamat
lain menerima `401` dengan kode `ADDRESS_NOT_ALLOWED` meskipun tokennya benar.

## Menguji dari mesin lain

```bash
# Tanpa token: harus 401 dengan code MISSING_KEY
curl -i http://<ip-perangkat>:8085/status

# Token salah: harus 401 dengan code WRONG_KEY
curl -i -H "X-Api-Key: salah" http://<ip-perangkat>:8085/status

# Token benar (salin dari layar aplikasi): harus 200 dan badan JSON
curl -i -H "X-Api-Key: <token-dari-layar>" http://<ip-perangkat>:8085/status

# Stream: harus 200 dan aliran multipart
curl -s -H "X-Api-Key: <token-dari-layar>" -o /tmp/video.mjpeg http://<ip-perangkat>:8085/video
```

Status `BELUM DIUJI`: seluruh langkah di atas belum dijalankan pada perangkat
setelah token dibuat per sesi.
