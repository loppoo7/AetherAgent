# AetherAgent

Agen AI untuk coding dan manajemen repositori GitHub, dibuat dengan Kotlin, Jetpack Compose, Hilt, Room, DataStore, dan Ktor.

## Fitur

- **Multi-provider LLM**: OpenAI (GPT-4o), Anthropic (Claude 3.5 Sonnet), DeepSeek (`deepseek-chat`, atau `deepseek-coder` lewat kolom model), dan OpenRouter. Pilihan provider dan model bisa diganti kapan saja di Pengaturan. API key disimpan per provider, jadi berpindah provider tidak menghapus key yang sudah diisi.
- **Tool calling / agen otonom**: model bisa memanggil tool GitHub dalam loop sampai menghasilkan jawaban akhir (maksimal 8 langkah per instruksi).
  - Baca: daftar file (tree), isi file, riwayat commit, issue dan PR terbuka.
  - Tulis: buat atau ubah file, hapus file, buat branch, buat pull request.
  - Pencarian web (DuckDuckGo HTML).
- **Konfirmasi perubahan**: aksi tulis (write, delete, PR) menampilkan diff baris sebelum dijalankan. Bisa dimatikan di Pengaturan.
- **Action card**: setiap tool call tampil sebagai kartu Berjalan / Selesai / Gagal / Ditolak. Ketuk kartu untuk melihat output.
- **Markdown**: heading, list, daftar bernomor, **bold**, `inline code`, dan blok kode dengan highlight sederhana serta tombol Salin.
- **System prompt** dapat diedit di Pengaturan.
- **Riwayat chat** disimpan di Room dan dipakai sebagai konteks untuk pesan berikutnya.

## Yang belum ada

- Lampiran file atau context picker.
- Streaming token (jawaban muncul sekaligus setelah model selesai).
- Cache tree repo di Room.
- Login GitHub OAuth. Saat ini hanya Personal Access Token.
- Anthropic memakai Messages API native; provider lain memakai `/chat/completions`.

## Build

Butuh JDK 17, Android SDK 35, dan Gradle 8.9.

```bash
./gradlew assembleDebug
```

Workflow di `.github/workflows/build-apk.yml` membuat APK debug untuk pengembangan dan APK **release bertanda tangan permanen** untuk instalasi/update dari branch `main`. APK release hanya dibuat setelah empat GitHub Actions Secrets signing dikonfigurasi.

### Instalasi dan update tanpa uninstall

- Gunakan hanya artifact `AetherAgent-update-vN` (APK release) untuk pemakaian normal. Artifact debug bersifat pengembangan dan memakai application ID berbeda (`com.aether.agent.debug`).
- Application ID release dipertahankan sebagai `com.aether.agent`; jangan menggantinya setelah rilis pertama.
- `versionCode` diambil otomatis dari nomor workflow run agar selalu meningkat.
- Semua APK release harus ditandatangani dengan keystore dan alias yang sama. Jangan membuat atau mengganti keystore untuk build berikutnya.
- Jika sebelumnya memasang APK debug, Android menganggap APK release sebagai aplikasi berbeda. Migrasi ke release memerlukan pemasangan pertama secara terpisah; setelah itu update release berikutnya cukup install di atas versi lama tanpa menghapus data.
- Simpan keystore dan password secara offline. Kehilangan signing key akan memutus kemampuan update untuk instalasi yang sudah memakai key tersebut.

#### Menambahkan signing secrets (sekali saja)

Buka **Settings → Secrets and variables → Actions → New repository secret** pada GitHub, lalu buat:
- `KEYSTORE_BASE64`: isi lengkap Base64 dari file keystore yang disediakan.
- `KEYSTORE_PASSWORD`: password keystore.
- `KEY_ALIAS`: alias key.
- `KEY_PASSWORD`: password key.

Jangan pernah memasukkan keystore atau password ke repository.

## Konfigurasi

Buka **Pengaturan** di aplikasi:

- **Provider dan API key** untuk model yang dipilih.
- **Personal access token GitHub** (fine-grained, izin "Contents: Read and write" dan "Pull requests: Read and write" untuk repo yang dituju).
- **Owner, nama repo, dan branch kerja**. Disarankan branch khusus seperti `ai-changes`, bukan `main`.

## Catatan keamanan

- API key dan token GitHub disimpan lewat Preferences DataStore di perangkat. DataStore tidak mengenkripsi nilai. Untuk penyimpanan terenkripsi, ganti `SettingsDataStore` dengan Android Keystore atau EncryptedSharedPreferences.
- Jangan commit key ke repo. `.gitignore` sudah mengabaikan `local.properties` dan keystore.
- Agen mengirim isi file repo ke provider LLM yang dipilih. Jangan gunakan repo yang berisi rahasia.

## Struktur

```
data/      api (GitHub client, LLM gateway), local (DataStore, Room), repository, mapper
domain/    model, repository (interface), agent (AgentRunner, AgentEvent), agent_tools, usecase
presentation/  ui (chat, settings, theme), components, viewmodel, navigation
di/        modul Hilt
```
