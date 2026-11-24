# Membuat map Dual Carbeats

## Membuka proyek
Buka folder `DualCarbeats` ini di Android Studio (folder yang berisi `settings.gradle.kts`), lalu tunggu Gradle Sync. Ini salinan proyek asli, bukan aplikasi baru. `app/src/main` berasal dari main.rar; konfigurasi Gradle berasal dari proyek Android Studio yang menyertainya. Jangan menyalin folder build ke proyek lama. Backup sebelum menggabungkan perubahan.

## File yang diedit
- Easy: `app/src/main/assets/map_easy.json` — On & On, `music_onandon_2.mp3`.
- Medium: `map_medium.json` — Safe & Sound, `music_safeandsound_1.mp3`.
- Hard: `map_hard.json` — Monkeys Spinning Monkeys, `music_monkeys_3.mp3`.

Ketiga JSON berisi daftar event tetap. Tidak ada pembuat map otomatis di aplikasi. Pola/urutan frasa dibuat eksplisit; timestamp awal disejajarkan dengan estimasi beat dari rekaman yang diberikan. Anggap sebagai chart awal yang perlu diuji sambil mendengar musik. Bagian akhir lagu sengaja diberi ruang tanpa not, terutama outro Medium; WIN tetap menunggu audio selesai.

## Contoh format
```json
{
  "title": "Latihan",
  "events": [
    {"t": 3000, "car": "R", "type": "note"},
    {"t": 3500, "car": "B", "type": "note"},
    {"t": 4000, "car": "R", "type": "hold", "d": 1000},
    {"t": 4500, "car": "B", "type": "obstacle"},
    {"t": 5500, "car": "R", "type": "note"}
  ]
}
```

`t` = waktu target sejak awal file audio, dalam milidetik. 3,5 detik ditulis 3500, bukan 3.5. Ini waktu not sampai garis target, bukan waktu muncul. `car` wajib R (merah) atau B (biru). `type` = note, hold, atau obstacle. `d` hanya untuk hold dan menyatakan durasi, bukan waktu akhir. Contoh hold t=4000,d=1000 selesai pada 5000.

Jalur tidak ditulis manual. Merah mulai jalur 2, biru jalur 4. Not/hold berikutnya selalu berada di jalur sebelah. Kerucut berada di jalur mobil saat ini, dan tujuan menghindarnya adalah jalur sebelah. R dan B boleh punya event pada waktu yang sama. Jangan menambah event pada mobil yang sama selama hold berlangsung.

Metadata title/difficulty/music/durationMs/sections hanya catatan editor; penghubung lagu sebenarnya ada di tabel game_level (`dualcarbeats.sql` dan migrasi DatabaseHelper). Mengubah metadata music saja tidak mengganti lagu yang diputar.

## Langkah menyusun chart
1. Buka MP3 asli di editor audio dengan waveform. Jangan mengubah trim, kecepatan, atau keheningan awal sesudah chart dibuat.
2. Dengarkan bagian pendek berulang. Catat waktu ketukan/aksen yang ingin diikuti, dari awal file.
3. Untuk tempo konstan, jarak satu beat = 60000 / BPM milidetik. Jika beat pertama pada 2400 ms dan tempo 120 BPM, beat berikutnya 2900,3400,3900. Untuk tempo berubah, gunakan timestamp waveform, bukan rumus tunggal sepanjang lagu.
4. Susun R/B secara sengaja. Easy memberi jeda dan bergantian; Medium menggabungkan tap bersamaan dan hold; Hard menambah kepadatan dan pergantian ritme. Jangan hanya menambah kerucut.
5. Urutkan events berdasarkan t. Simpan UTF-8, tanpa komentar atau koma sesudah item terakhir.
6. Jalankan game, dengarkan dengan speaker/headphone kabel, kemudian sesuaikan timestamp. Pengaturan offset mengubah clock penilaian: offset positif membuat objek mencapai target lebih awal terhadap audio. Gunakan offset untuk latency perangkat, bukan untuk menutupi chart yang salah.
7. Uji hold dengan dua jari, lepas lebih awal, tekan dua sisi bersamaan, miss, pause, dan pindah aplikasi. Setelah itu uji Bluetooth secara terpisah karena latency dapat berbeda.

## Batas validasi dan aturan
- t pertama minimal 1800 ms; disarankan >= waktu pendekatan (maksimum 2400 ms).
- Hold minimal 300 ms; note/obstacle tanpa d atau d=0.
- Event pada mobil yang sama harus berjarak >360 ms setelah akhir event sebelumnya, termasuk ujung hold. Sisi lain tetap independen.
- Semua event beserta toleransi dan offset harus selesai sebelum durasi MP3.
- Perfect: selisih <=75 ms. Great: <=180 ms. Not, hold, dan kerucut memakai zona yang sama di depan mobil: t-180 sampai t+180. t adalah saat objek mencapai garis target di depan bumper. Sesudah t+180 kerucut menabrak; tidak perlu menunggu objek sejajar badan mobil.
- Hold dinilai satu kali saat selesai. Lepas <=100 ms sebelum ujung masih diterima; lebih awal = Miss. Awal hold yang Great tetap Great.
- Pause/cancel melepas hold aktif; hold yang masih jauh dari ujung menjadi Miss. Ini pilihan eksplisit untuk mencegah hold tetap aktif tanpa jari.
- Invalid tap memutus combo tanpa gerak, skor, atau menambah Miss Count. Angka MISS yang muncul merupakan feedback kesalahan tap.
- Perfect 300, Great 150; Fever menggandakan skor selama 10 detik. Pemicu tiap kelipatan 100 combo, termasuk setelah combo putus lalu kembali 100. Pemicu baru menyetel ulang durasi 10 detik.
- HP awal 3; hanya tabrakan kerucut mengurangi HP. Tidak ada pemulihan HP. WIN saat audio selesai; GAME OVER saat HP=0.

## Audio
Semua MP3/WAV asli dipertahankan. Volume musik dan SFX terpisah melalui layar Settings yang sudah ada. Efek hit memakai sfx_poin, tabrakan sfx_damage, kalah sfx_loss. Countdown memakai sfx_button, fever memakai sfx_poin dengan pitch lebih tinggi karena file khusus belum tersedia. Untuk mengganti efek, tambahkan file baru bernama huruf kecil/underscore di res/raw dan ubah pemetaan aliases di GameAudio.kt; jangan menimpa aset asli.

Audio focus mengikuti panduan Android: https://developer.android.com/media/optimize/audio-focus . Saat telepon/aplikasi lain mengambil fokus atau headphone dicabut, musik dan gameplay berhenti; permainan memerlukan Resume, tidak lanjut otomatis.

## Menjalankan pemeriksaan
Dari terminal proyek: `gradlew.bat assembleDebug testDebugUnitTest lintDebug`.
Dengan emulator/perangkat tersambung: `gradlew.bat connectedDebugAndroidTest`.
Tes unit memeriksa aturan engine; tes MapIntegrationTest membaca ketiga JSON dan durasi MP3 asli lalu memainkan seluruh chart dengan input tepat. Tes ini tidak menggantikan playtest rasa ritme/audio latency.

FC hanya aktif setelah musik selesai dan seluruh event berhasil tanpa Miss atau Invalid Tap. Badge disimpan terpisah dari high score dan terikat ke isi JSON; score lama tidak membuktikan FC.
