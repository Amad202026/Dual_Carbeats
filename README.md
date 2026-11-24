# Dual Carbeats

Game ritme (rhythm-action arcade) untuk Android di mana pemain mengendalikan **dua mobil sekaligus** pada empat jalur, mengikuti ketukan lagu. Dibuat dengan Kotlin dan layout XML di Android Studio.

## Gambaran Singkat

- **Genre:** Rhythm-action arcade
- **Platform:** Android
- **Konsep:** dua mobil, satu lagu. Mobil merah menempati dua jalur kiri, mobil biru dua jalur kanan, dan keduanya dikontrol bersamaan lewat sentuhan di sisi kiri dan kanan layar.
- **Level:** 3 level dengan lagu berbeda (Easy, Medium, Hard). Semua peta dibuat manual dan disimpan sebagai file JSON.

## Cara Bermain

- **Kontrol:** tap setengah layar kiri untuk mobil merah, setengah layar kanan untuk mobil biru. Kedua sisi bisa disentuh bersamaan.
- **Not:** tap saat not sampai di mobil. Mobil berpindah ke jalur not tersebut dan tetap di sana.
- **Hold note:** tekan dan tahan selama not panjang, lalu lepas di akhir.
- **Rintangan:** tap tepat sebelum mobil menabrak untuk menghindar dan pindah jalur. Jika tertabrak, HP berkurang dan mobil pindah otomatis.
- **Tap kosong:** tap saat tidak ada not memutus combo dan tidak menambah skor.

### Penilaian

| Hasil | Syarat (dari waktu not) | Poin |
|---|---|---|
| Perfect | dalam 75 ms | 300 |
| Great | dalam 180 ms | 150 |
| Miss | terlewat, hold dilepas terlalu cepat, atau rintangan tertabrak | 0 dan combo reset |

- **Combo:** naik setiap Perfect atau Great.
- **Fever:** aktif tiap combo mencapai kelipatan 100. Selama 10 detik, poin dikali 2, dengan efek visual dan suara khusus.
- **HP:** 3 hati. Hanya berkurang jika menabrak rintangan.
- **Menang:** lagu selesai sampai akhir. **Game Over:** HP habis.
- **Hasil akhir:** Total Score, Perfect, Great, Miss, Max Combo, High Score, dan rank (S, A, B, C).

## Layar Aplikasi

Splash, Main Menu, How to Play, Credits, Settings, Other, Pilih Level, Game (dengan overlay Pause), dan Hasil (Win atau Game Over).

Settings menyimpan volume musik, volume SFX, dan getar. Other menyimpan kecepatan not, audio offset (untuk menyesuaikan jeda audio tiap HP), reset highscore, dan akses ke Credits.

## Teknologi

- Kotlin dan layout XML (tanpa Jetpack Compose)
- SQLite (`SQLiteOpenHelper`) untuk highscore, daftar level, dan pengaturan
- Desain database dengan Oracle SQL Developer Data Modeler, lalu diubah ke SQLite
- `MediaPlayer` untuk musik, `SoundPool` untuk efek suara
- Animasi efek (not kena, tabrakan, Fever) berupa rangkaian gambar PNG transparan
- Desain UI dengan Figma

## Struktur Proyek

```
app/src/main/
├── java/com/kelompok6/dualcarbeats/
│   ├── SplashActivity, MainActivity, HowToPlayActivity, CreditActivity
│   ├── SettingsActivity, OtherActivity, SelectMapActivity
│   ├── GameActivity        layar permainan dan tampilan not
│   ├── GameEngine          aturan skor, combo, HP, dan penilaian
│   ├── GameModels          MapEvent dan pembaca peta JSON
│   ├── GameAudio           musik dan SFX
│   ├── DatabaseHelper      akses database
│   ├── ResultActivity, ResultAnimations
│   └── BaseActivity        mode layar penuh
├── assets/
│   ├── dualcarbeats.sql    skema dan data awal database
│   └── map_*.json          peta tiap lagu
└── res/
    ├── layout/  drawable/  font/  raw/  values/
```

- `res/raw/` berisi musik dan SFX (nama berhuruf kecil, tanpa spasi).
- `res/font/` berisi Orbitron dan Rajdhani.

## Database

Tiga tabel, sesuai ERD:

| Tabel | Isi |
|---|---|
| `game_level` | Judul lagu, kesulitan, nama file musik, nama file peta |
| `high_score` | Skor tertinggi per level, jumlah Perfect/Great/Miss, max combo |
| `pengaturan` | Volume musik, volume SFX, getar aktif |

Database dibuat otomatis saat aplikasi pertama dibuka, dari `assets/dualcarbeats.sql`. Jika skema diubah, naikkan nomor versi di `DatabaseHelper` atau hapus aplikasi dari perangkat agar database dibuat ulang.

## Format Peta (JSON)

Setiap lagu punya satu file peta di `assets/`. Game hanya membaca `events`. Bagian lain adalah catatan untuk pembuat peta.

```json
{
  "title": "Safe & Sound",
  "difficulty": "medium",
  "bpm": 128,
  "events": [
    { "t": 3916, "car": "R", "type": "note" },
    { "t": 4854, "car": "B", "type": "note" },
    { "t": 18916, "car": "R", "type": "hold", "d": 900 },
    { "t": 26854, "car": "B", "type": "obstacle" }
  ]
}
```

| Kolom | Arti |
|---|---|
| `t` | Waktu event dalam milidetik dari awal lagu |
| `car` | `"R"` mobil merah, `"B"` mobil biru |
| `type` | `"note"`, `"hold"`, atau `"obstacle"` |
| `d` | Lama hold dalam milidetik (hanya untuk `hold`) |

Jalur tidak ditulis di peta. Mobil merah mulai di jalur 2 dan biru di jalur 4. Setiap event memindahkan mobil ke jalur di sebelahnya. Not muncul di jalur tujuan, sedangkan rintangan muncul di jalur tempat mobil berada.

Peta yang dibuat dengan pola frasa menyimpan daftar pola di `phrases` dan urutannya di `sections` (setiap section berisi 8 ketukan).

## Menjalankan Proyek

1. Clone repositori ini.
2. Buka di Android Studio, lalu tunggu Gradle sync selesai.
3. Pastikan file musik dan SFX ada di `res/raw/` dan file peta ada di `assets/`.
4. Jalankan di emulator atau HP (**Run > Run 'app'**).

Aplikasi memakai izin `VIBRATE` untuk getaran saat mobil menabrak.

## Tim

**Kelompok 6**

- Anugrah Ridho Y.
- Ahmad Nadir
- Ahmad Ridwan
- Razan Javier R.
- Suci Tonglo

## Kredit dan Lisensi Aset

**Aset buatan tim:** desain UI, mobil, not, ikon, dan teks rating dibuat sendiri di Figma.

**Musik:**

| Lagu | Pembuat | Sumber |
|---|---|---|
| Safe And Sound | Different Heaven | NoCopyrightSounds (NCS) |
| On & On | Cartoon feat. Daniel Levi | NoCopyrightSounds (NCS) |
| Monkeys Spinning Monkeys | Kevin MacLeod | incompetech.com (CC BY) |

Lagu NCS dipakai sesuai ketentuan NoCopyrightSounds dengan mencantumkan kredit. Lagu Kevin MacLeod dipakai di bawah lisensi Creative Commons Attribution (CC BY).

**Font:** [Orbitron](https://fonts.google.com/specimen/Orbitron) dan [Rajdhani](https://fonts.google.com/specimen/Rajdhani) dari Google Fonts, di bawah SIL Open Font License.

**Efek suara:** Pixabay, di bawah Pixabay Content License.

Daftar kredit yang sama juga tampil di layar **Credits** di dalam aplikasi.
