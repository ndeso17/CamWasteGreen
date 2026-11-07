# Token API `/video` dan `/status`

Dokumen ini menjelaskan token statis yang dipakai server MJPEG (D-27, WP-25..
WP-27). Token **tidak pernah** disimpan di dalam repositori.

## Ringkas

| Hal | Nilai |
|---|---|
| Header permintaan | `X-Api-Key: <token>` |
| Endpoint yang dijaga | `/video`, `/status` |
| Endpoint bebas | `/` (halaman diagnosa, hanya tautan) |
| Balasan bila ditolak | `401` dengan badan JSON `{"error":{"code":"...","message":"..."}}` |
| Sumber token saat build | `local.properties` → `BuildConfig.API_KEY` |

## Membuat token

Buat nilai acak yang panjang, jangan diketik manual:

```bash
# 32 byte acak, cetak sebagai hex
head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'; echo
```

Simpan hasilnya di tempat yang aman (pengelola sandi). Token yang sama harus
dimasukkan ke perangkat klien dan ke `local.properties`.

## Mengisi `local.properties`

`local.properties` berada di akar proyek dan sudah masuk `.gitignore`
(`git check-ignore -v local.properties`). Tambahkan dua baris:

```properties
camwastegreen.apiKey=<token-hasil-langkah-sebelumnya>
camwastegreen.allowedAddresses=10.42.0.10,10.42.0.11
```

- `camwastegreen.apiKey` — token untuk header `X-Api-Key`. Bila kosong atau
  tidak ada, server **menolak semua** permintaan `/video` dan `/status`
  (`401`, kode `SERVER_MISCONFIGURED`). Server tidak pernah jatuh ke mode
  "terbuka tanpa token".
- `camwastegreen.allowedAddresses` — daftar alamat IP klien yang boleh
  menghubungi, dipisah koma. Kosong atau tidak ada berarti **tanpa batas
  alamat**; token tetap wajib.
- `/` sengaja tidak dijaga token karena halaman itu hanya berisi judul dan
  tautan. `/video` dan `/status` selalu dijaga.

Nilai dinormalisasi Gradle: tanda `\` dan `"` di-escape sebelum ditulis ke
`BuildConfig`, sehingga token yang mengandung dua karakter itu tetap aman.

## Konsekuensi yang harus diketahui

1. **Token ikut tertanam di dalam APK.** Siapa pun yang memegang APK dapat
   mengekstraknya. Karena itu token hanya lapis kedua; batas jaringan
   (`allowedAddresses` dan pilihan interface pada WP-33..WP-35) tetap lapis
   pertama.
2. **Mengganti token memerlukan build ulang dan pasang ulang APK.** Tidak ada
   mekanisme ganti token saat aplikasi berjalan; token dibaca saat server
   start (`MJPEGServerInstance.start`) dari nilai yang dibangun bersama APK.
3. **`usesCleartextTraffic=true`.** Token dan video dikirim tanpa TLS, jadi
   dapat dibaca pihak yang menyadap jaringan. Ini keterbatasan yang diketahui
   dan dicatat pada Log WP-27; perubahan ke TLS menunggu keputusan tersendiri
   (bukan bagian dari penambalan ini).
4. **Log tidak memuat token.** Penolakan dicatat sebagai kode alasan dan alamat
   klien saja.

## Menguji dari mesin lain

```bash
# Tanpa token: harus 401 dengan code MISSING_KEY
curl -i http://<ip-perangkat>:8085/status

# Token salah: harus 401 dengan code WRONG_KEY
curl -i -H "X-Api-Key: salah" http://<ip-perangkat>:8085/status

# Token benar: harus 200 dan badan JSON
curl -i -H "X-Api-Key: <token>" http://<ip-perangkat>:8085/status

# Stream: harus 200 dan aliran multipart
curl -s -H "X-Api-Key: <token>" -o /tmp/video.mjpeg http://<ip-perangkat>:8085/video
```

Bila `allowedAddresses` diisi, jalankan `curl` dari alamat yang terdaftar;
alamat lain menerima `401` dengan kode `ADDRESS_NOT_ALLOWED` meskipun tokennya
benar.
