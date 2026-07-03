# Leafivy

Aplikasi Android untuk mendeteksi penyakit pada daun apel menggunakan model TensorFlow Lite yang berjalan sepenuhnya di perangkat (on-device inference). Tidak ada data yang dikirim ke server.

## Penyakit yang Dideteksi

| Label | Nama Penyakit | Patogen |
|-------|---------------|---------|
| `scab` | Kudis Apel | *Venturia inaequalis* |
| `rust` | Karat Apel | *Gymnosporangium juniperi-virginianae* |
| `frog_eye_leaf_spot` | Bercak Daun Mata Katak | *Botryosphaeria obtusa* |
| `powdery_mildew` | Embun Tepung | *Podosphaera leucotricha* |
| `healthy` | Sehat | — |

Model saat ini: **Faraday v2026.04.14** — arsitektur MobileNetV2 dengan input 512×512×3 piksel, preprocessing `[-1, 1]` (identik dengan `tf.keras.applications.mobilenet_v2.preprocess_input`).

## Persyaratan

- Android Studio Ladybug (2024.2+) atau lebih baru
- JDK 17
- Android SDK dengan `compileSdk 36`
- Perangkat atau emulator Android **API 24** (Android 7.0) ke atas

## Cara Build

```bash
# Clone repository
git clone https://github.com/<owner>/leafivy.git
cd leafivy

# Build debug APK
./gradlew :app:assembleDebug

# Atau langsung install ke perangkat yang terhubung
./gradlew :app:installDebug
```

Build release menggunakan ProGuard (`isMinifyEnabled = true`, `isShrinkResources = true`):

```bash
./gradlew :app:assembleRelease
```

> File `.tflite` di-exclude dari kompresi (`noCompress += "tflite"`) agar interpreter dapat memap langsung dari APK tanpa perlu menyalin ke storage.

## Struktur Project

```
leafivy/
├── app/
│   ├── build.gradle.kts          # Konfigurasi module app
│   ├── proguard-rules.pro        # Keep rule untuk TensorFlow Lite JNI
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── latestmodel2.tflite   # Model TFLite (~12.2 MB)
│       │   ├── labels.txt            # 5 label kelas
│       │   └── model_version.txt     # String versi model
│       ├── java/com/hafd/leafivy3/
│       │   ├── MainActivity.kt       # Entry point, navigasi, permission handling
│       │   ├── ml/                   # Paket machine learning
│       │   │   ├── DiseaseClassifier.kt    # Klasifikasi penyakit (model utama)
│       │   │   ├── LeafGateClassifier.kt   # Gate classifier (validasi input daun)
│       │   │   ├── ModelInfo.kt            # Data class info model
│       │   │   └── Prediction.kt           # Data class hasil prediksi
│       │   ├── ui/                   # Paket UI (Jetpack Compose)
│       │   │   ├── HomeScreen.kt           # Halaman utama
│       │   │   ├── CameraScreen.kt         # Layar kamera dengan CameraX
│       │   │   ├── PreviewScreen.kt        # Preview gambar sebelum proses
│       │   │   ├── ResultScreen.kt         # Tampilan hasil deteksi
│       │   │   ├── CareGuideScreen.kt      # Panduan perawatan per penyakit
│       │   │   ├── CareGuideData.kt        # Data panduan perawatan
│       │   │   ├── DiagnosticsScreen.kt    # Info diagnostik & kualitas
│       │   │   ├── SettingsScreen.kt       # Pengaturan tema, bahasa, warna
│       │   │   ├── AboutPrivacyScreen.kt   # Halaman tentang & privasi
│       │   │   ├── SplashScreen.kt         # Splash screen animasi
│       │   │   ├── LeafivyViewModel.kt     # ViewModel & UI state
│       │   │   ├── components/             # Komponen UI reusable
│       │   │   │   ├── Animations.kt       # ConfidenceBar, PulsingDot
│       │   │   │   ├── Charts.kt           # ConfidenceBarChart (Canvas)
│       │   │   │   └── EngagingBackground.kt # Background wrapper
│       │   │   └── theme/                  # Material 3 theme system
│       │   │       ├── Theme.kt            # Color scheme builder
│       │   │       ├── ThemePreferences.kt # Preferensi tema (SharedPreferences)
│       │   │       ├── Color.kt
│       │   │       ├── Shapes.kt
│       │   │       └── Type.kt
│       │   └── utils/                # Utilitas
│       │       ├── BitmapUtils.kt    # EXIF rotation, downsampling, decode URI
│       │       ├── LocalLogger.kt    # File logger dengan rotasi
│       │       └── Result.kt         # Sealed class Result<T>
│       └── res/
│           ├── values/strings.xml         # String Indonesia (default)
│           ├── values-en/strings.xml      # String Inggris
│           └── xml/locales_config.xml     # Deklarasi locale id, en
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Module & repository config
├── gradle/libs.versions.toml     # Version catalog
└── .github/workflows/
    ├── android-ci.yml            # CI: lint, unit test, assemble release
    └── codeql.yml                # Code scanning
```

## Arsitektur

### Alur Aplikasi

```
Splash → Home → [Kamera / Galeri] → Preview → Result → [Care Guide | Diagnostics]
                                                  ↓
                                              Settings
                                              About & Privacy
```

### Navigasi

Menggunakan `androidx.navigation.compose` dengan route string:

| Route | Screen | Deskripsi |
|-------|--------|-----------|
| `splash` | `SplashScreen` | Animasi logo saat launch |
| `home` | `HomeScreen` | Pilihan ambil foto atau pilih dari galeri |
| `camera` | `CameraScreen` | Viewfinder CameraX dengan kontrol flash dan grid |
| `preview` | `PreviewScreen` | Preview gambar sebelum diproses model |
| `result` | `ResultScreen` | Hasil klasifikasi dengan confidence score |
| `care_guide` | `CareGuideScreen` | Langkah perawatan sesuai penyakit terdeteksi |
| `diagnostics` | `DiagnosticsScreen` | Confidence meter, tips foto, versi model |
| `settings` | `SettingsScreen` | Tema warna, dark mode, dynamic color, bahasa |
| `about_privacy` | `AboutPrivacyScreen` | Informasi aplikasi dan kebijakan privasi |

### Pipeline Klasifikasi

```
Bitmap (dari kamera/galeri)
  │
  ├─ BitmapUtils.decodeSampled*()  ← downscale ke maks 1024px
  ├─ BitmapUtils.optimizeBitmap()  ← koreksi rotasi EXIF
  │
  ▼
LeafivyViewModel.processImage()
  │
  ├─ DiseaseClassifier.classify(bitmap)
  │     ├─ ensureLabels()            ← load labels.txt, fallback ke hardcoded list
  │     ├─ ensureInterpreter()       ← lazy-init TFLite Interpreter (4 thread)
  │     ├─ Bitmap.createScaledBitmap(512, 512)
  │     ├─ convertBitmapToByteBuffer()
  │     │     └─ pixel → (pixel / 127.5) - 1.0  (range [-1, 1])
  │     ├─ interpreter.run()
  │     └─ filter prediksi ≥ threshold (0.1)
  │           └─ jika semua di bawah threshold, ambil top-1 sebagai fallback
  │
  ▼
Result<List<Prediction>>  →  UI State  →  ResultScreen
```

### ViewModel & State

`LeafivyViewModel` menggunakan `AndroidViewModel` dengan `StateFlow<LeafivyUiState>`:

```kotlin
data class LeafivyUiState(
    val pendingImage: Bitmap?,         // gambar sebelum diproses (di PreviewScreen)
    val image: Bitmap?,                // gambar yang sedang/sudah diproses
    val predictions: List<Prediction>, // hasil klasifikasi
    val isLoading: Boolean,
    val error: String?
)
```

Inference dijalankan di `Dispatchers.IO`. Interpreter di-cache setelah inisialisasi pertama dan ditutup di `onCleared()`.

### Theming

Material 3 dengan color scheme yang di-generate dari seed color:

- **11 pilihan warna** (Blue, Indigo, Purple, Teal, Green, Olive, Orange, Red, Pink, Brown, Slate)
- **Dark mode**: System / Always Light / Always Dark
- **Dynamic Color**: Mengikuti wallpaper perangkat (Android 12+)
- **Bahasa**: Indonesia (default), English

Preferensi disimpan di `SharedPreferences` dan di-expose via `StateFlow<ThemePrefs>`. Perubahan bahasa memicu `Activity.recreate()`.

## Dependensi Utama

| Library | Versi | Kegunaan |
|---------|-------|----------|
| Jetpack Compose BOM | 2024.09.00 | UI toolkit |
| Material 3 | (BOM) | Design system |
| CameraX | 1.3.0 | Capture foto |
| TensorFlow Lite | 2.17.0 | Inference model on-device |
| Coil | 2.5.0 | Image loading |
| Navigation Compose | 2.7.6 | Navigasi antar screen |
| ExifInterface | 1.3.7 | Koreksi orientasi gambar |

Version catalog lengkap ada di [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## CI/CD

GitHub Actions menjalankan workflow berikut pada setiap push dan PR ke `main`/`master`:

```
Checkout → JDK 17 → Android SDK → Gradle wrapper validation
  → lintDebug → testDebugUnitTest → assembleRelease
```

File konfigurasi: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)

## Logging

`LocalLogger` menulis log ke file di `filesDir/logs/app.log` dengan mekanisme:

- **Debug build**: semua level ditulis ke file dan Logcat
- **Release build**: hanya `WARN` dan `ERROR` yang di-persist ke file; Logcat tetap menerima `WARN`/`ERROR`
- **Rotasi**: file di-rotate setelah mencapai 1 MB, maksimal 3 arsip disimpan
- **Crash log**: ditulis ke file terpisah `crash_<timestamp>.log`

## Izin

| Permission | Wajib | Keterangan |
|------------|-------|------------|
| `CAMERA` | Tidak (`required="false"`) | Diminta saat runtime. Jika ditolak, user masih bisa memilih gambar dari galeri. |

## Catatan Pengembangan

- Model `.tflite` berukuran ~12.2 MB dan di-bundle di `assets/`. Jika ingin mengganti model, pastikan:
  - Input shape tetap `[1, H, W, 3]` dengan tipe `FLOAT32`
  - Output shape `[1, NUM_CLASSES]`
  - Update `labels.txt` sesuai urutan kelas di `label_map.json`
  - Update `model_version.txt` untuk tracking
- `LeafGateClassifier` tersedia sebagai gate untuk memvalidasi apakah input benar-benar gambar daun sebelum diklasifikasi penyakitnya, tapi saat ini belum diintegrasikan ke pipeline utama
- Orientasi layar dikunci ke portrait di `AndroidManifest.xml`

## Lisensi

MIT License — lihat [LICENSE](LICENSE) untuk detail.
