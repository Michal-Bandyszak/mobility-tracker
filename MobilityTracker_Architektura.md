# MobilityTracker — Architektura i Plan Implementacji
## Aplikacja Android do cyklicznego programu treningowego mobilności (17 filmów)

---

## 1. PRZEGLĄD PROJEKTU

### Co budujemy
Natywna aplikacja Android (Kotlin + Jetpack Compose) do śledzenia cyklicznego programu treningowego z mobilności. Program składa się z **17 filmów** oglądanych po 1 dziennie — po zakończeniu cyklu zaczyna się od nowa (bez końca). Aplikacja wyświetla dzienny filmik, wysyła powiadomienia push i prowadzi statystyki realizacji.

### Rozszerzenie: harmonogram i trening siłowy
Aplikacja została rozszerzona z prostego trackera codziennej mobilności do lokalnego planera treningowego:
- mobilność i siłownia mają osobne dni tygodnia ustawiane w `SettingsScreen`,
- mobilność nadal korzysta z 17-elementowego cyklu, ale cykl przesuwa się tylko w wybrane dni mobilności,
- trening siłowy rotuje między dwoma jednostkami z planu PDF: Dzień 1, Dzień 2, Dzień 1...,
- logi siłowe są osobną tabelą Room `strength_set_logs`, bo użytkownik wpisuje ciężar i powtórzenia per seria,
- reset flow usuwa datę startu, ukończenia mobilności i logi siłowe, ale zostawia harmonogram oraz przypomnienia.

Najważniejsza decyzja architektoniczna: nie mieszać ukończeń mobilności z logami siłowymi. Mobilność jest binarna (`done/not done`), a siłownia jest pomiarem serii (`weightKg/reps`). Wspólny model wymusiłby nullable pola i nieczytelne statystyki.

### Kluczowe wymagania
- 17 filmów treningowych w cyklu (1 dziennie, cykl się powtarza)
- Odtwarzanie filmików treningowych (lokalne pliki .mp4 z telefonu/SD card)
- Oznaczanie treningów jako zrealizowane
- Powiadomienia push przypominające o treningu
- Statystyki: liczba ukończonych cykli, seria dni z rzędu, % realizacji
- 100% offline (bez backendu/serwera)
- Dane lokalne na urządzeniu

---

## 2. STOS TECHNOLOGICZNY

| Warstwa | Technologia | Dlaczego |
|---------|-------------|----------|
| **Język** | Kotlin | Natywny Android, nowoczesna składnia (podobna do TS) |
| **UI** | Jetpack Compose | Deklaratywny UI (jak React, ale dla Androida) |
| **Nawigacja** | Compose Navigation | Oficjalna biblioteka nawigacji |
| **Baza danych** | Room (SQLite) | Lokalne przechowywanie, offline-first |
| **DI** | Hilt (Dagger) | Dependency injection — standard w Androidzie |
| **Powiadomienia** | AlarmManager + NotificationCompat | Lokalne push bez Firebase |
| **Architektura** | MVVM + Clean Architecture | Testowalność, separacja warstw |
| **Build** | Gradle (Kotlin DSL) | Standard Android |
| **Min SDK** | API 26 (Android 8.0) | ~95% urządzeń |

### Dlaczego Kotlin + Compose, a nie React Native/Flutter?
- **Powiadomienia** — natywny AlarmManager jest niezawodny, RN/Flutter wymaga dodatkowych wtyczek
- **Wydajność** — zero overheadu z bridge'a JS
- **Rozmiar APK** — ~5-8 MB vs ~20-30 MB (Flutter)
- **Nauka** — Kotlin to wartościowy skill dla Android dev

---

## 3. ARCHITEKTURA APLIKACJI

```
┌─────────────────────────────────────────────────┐
│                   UI LAYER                       │
│  ┌───────────┐ ┌──────────┐ ┌────────────────┐  │
│  │ TodayScreen│ │ Calendar │ │  StatsScreen   │  │
│  │ (Główna)  │ │  Screen  │ │  (Statystyki)  │  │
│  └─────┬─────┘ └────┬─────┘ └───────┬────────┘  │
│        │             │               │           │
│  ┌─────▼─────────────▼───────────────▼────────┐  │
│  │              ViewModels                     │  │
│  │  TodayVM  │  CalendarVM  │  StatsVM         │  │
│  └─────────────────┬───────────────────────────┘  │
├────────────────────┼─────────────────────────────┤
│               DOMAIN LAYER                       │
│  ┌─────────────────▼───────────────────────────┐  │
│  │             Use Cases                        │  │
│  │  GetTodayWorkout │ MarkComplete │ GetStats   │  │
│  └─────────────────┬───────────────────────────┘  │
├────────────────────┼─────────────────────────────┤
│                DATA LAYER                        │
│  ┌─────────────────▼───────────────────────────┐  │
│  │           Repository                         │  │
│  │  WorkoutRepository │ CompletionRepository    │  │
│  └──────┬──────────────────────┬───────────────┘  │
│  ┌──────▼──────┐        ┌─────▼──────────────┐   │
│  │ Room DB     │        │ WorkoutDataSource   │   │
│  │ (completions│        │ (hardcoded plan)    │   │
│  │  + settings)│        │                     │   │
│  └─────────────┘        └─────────────────────┘   │
├──────────────────────────────────────────────────┤
│              SERVICES                            │
│  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ NotificationMgr  │  │ AlarmScheduler       │  │
│  │ (push notif.)    │  │ (daily reminder)     │  │
│  └──────────────────┘  └──────────────────────┘  │
└──────────────────────────────────────────────────┘
```

---

## 4. STRUKTURA PROJEKTU

```
app/src/main/java/com/mobilitytracker/
├── MobilityApp.kt                    # Application class (Hilt)
├── MainActivity.kt                   # Single Activity
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── CompletionDao.kt          # DAO dla oznaczeń
│   │   └── SettingsDao.kt            # DAO ustawień (czas powiadomienia)
│   ├── model/
│   │   ├── WorkoutDay.kt             # Entity: dzień treningowy
│   │   ├── Completion.kt             # Entity: ukończony trening
│   │   └── WorkoutPlan.kt            # Hardcoded plan 17 filmów
│   ├── video/
│   │   └── VideoResolver.kt          # Wyszukiwanie video w MediaStore
│   └── repository/
│       ├── WorkoutRepository.kt
│       └── CompletionRepository.kt
│
├── domain/
│   ├── model/
│   │   ├── DayWorkout.kt             # Domain model
│   │   └── CycleStats.kt             # Statystyki cyklu
│   └── usecase/
│       ├── GetTodayWorkoutUseCase.kt
│       ├── GetCycleWorkoutsUseCase.kt
│       ├── ToggleCompletionUseCase.kt
│       └── GetStatsUseCase.kt
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                  # Material 3 theming
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt               # Bottom nav routing
│   ├── today/
│   │   ├── TodayScreen.kt            # Ekran główny — dzisiejszy trening
│   │   └── TodayViewModel.kt
│   ├── calendar/
│   │   ├── CalendarScreen.kt         # Widok tygodniowy/kalendarz
│   │   └── CalendarViewModel.kt
│   ├── stats/
│   │   ├── StatsScreen.kt            # Statystyki + progres
│   │   └── StatsViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt         # Ustawienia (godzina powiadomienia)
│   │   └── SettingsViewModel.kt
│   └── components/
│       ├── WorkoutCard.kt            # Karta treningu
│       ├── ProgressRing.kt           # Pierścień postępu
│       ├── CycleStrip.kt             # Pasek postępu cyklu
│       └── VideoPlayer.kt            # Lokalne video (MediaStore)
│
├── notification/
│   ├── NotificationHelper.kt         # Tworzenie powiadomień
│   ├── AlarmScheduler.kt             # Planowanie alarmów
│   ├── ReminderReceiver.kt           # BroadcastReceiver
│   └── BootReceiver.kt               # Ponowne ustawienie po restarcie
│
└── di/
    └── AppModule.kt                  # Hilt modules
```

---

## 5. MODEL DANYCH

### 5.1 Plan treningowy (hardcoded w kodzie)

```kotlin
data class WorkoutDefinition(
    val order: Int,             // 1-17 — pozycja w cyklu
    val title: String,          // np. "Kręgosłup ze sprzętem"
    val videoFileName: String,  // nazwa pliku, np. "biodra z sprzętem.mp4"
    val durationMinutes: Int,   // Czas trwania (0 = nieznany)
    val category: String        // "biodra", "kregoslup", "barki", "tylna_tasma", "przednia_tasma", "inne"
)
```

#### Wykrywanie ścieżki SD card — automatyczne (MediaStore)

Aplikacja przy starcie szuka plików po nazwie w MediaStore (indeks multimediów Androida). Użytkownik nic nie konfiguruje.

```kotlin
// VideoResolver.kt — injectable przez Hilt (testowalny)
@Singleton
class VideoResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun findVideoUri(fileName: String): Uri? {
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null  // plik nie znaleziony
    }
}
```

**Wymagane uprawnienia (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

**Jak to działa:**
1. Android indeksuje automatycznie wszystkie pliki `.mp4` na wszystkich nośnikach (internal + SD card)
2. `VideoResolver.findVideoUri()` odpytuje ten indeks po nazwie pliku
3. Zwraca `content://` URI — działa niezależnie od ścieżki fizycznej karty
4. `Uri` przekazujesz do ExoPlayer lub systemowego odtwarzacza

Plan jest zdefiniowany jako lista 17 elementów w `WorkoutPlan.kt` — nie wymaga bazy danych, bo się nie zmienia.

#### Lista filmów — kolejność ułożona tak, żeby podobne kategorie były jak najdalej od siebie

Zasada: biodra/kręgosłup/barki/tylna taśma rozmieszczone co **5-6 dni** (maksimum możliwe przy 3 filmach w 17-dniowym cyklu).

```kotlin
val WORKOUT_PLAN = listOf(
    // Dzień 1-6
    WorkoutDefinition(1,  "Biodra z sprzętem",               "biodra z sprzętem.mp4",                    0, "biodra"),
    WorkoutDefinition(2,  "Kręgosłup ze sprzętem",           "kręgosłup ze sprzetem.mp4",                0, "kregoslup"),
    WorkoutDefinition(3,  "Obręcz barkowa z sprzętem",       "Obręcz barkowa z sprzętem.mp4",            0, "barki"),
    WorkoutDefinition(4,  "Tylna taśma z sprzętem",          "tylna taśma z sprzętem.mp4",               0, "tylna_tasma"),
    WorkoutDefinition(5,  "Przednia taśma",                  "Przednia taśma.mp4",                       0, "przednia_tasma"),
    WorkoutDefinition(6,  "Biodra bez sprzętu",              "biodra bez sprzętu.mp4",                   0, "biodra"),
    // Dzień 7-12
    WorkoutDefinition(7,  "Staw skokowy",                    "staw skokowy.mp4",                         0, "inne"),
    WorkoutDefinition(8,  "Kręgosłup bez sprzętu",           "Kręgosłup bez sprzętu.mp4",                0, "kregoslup"),
    WorkoutDefinition(9,  "Obręcz barkowa bez sprzętu",      "obręcz barkowa bez sprzętu.mp4",           0, "barki"),
    WorkoutDefinition(10, "Tylna taśma bez sprzętu",         "Tylna taśma bez sprzętu.mp4",              0, "tylna_tasma"),
    WorkoutDefinition(11, "Nadgarstki",                      "nadgarstki.mp4",                           0, "inne"),
    WorkoutDefinition(12, "Biodra bez sprzętu zestaw 2",     "Biodra bez sprzętu zestaw 2.mp4",          0, "biodra"),
    // Dzień 13-17
    WorkoutDefinition(13, "Przednia taśma zestaw 2",         "Przednia taśma zestaw 2.mp4",              0, "przednia_tasma"),
    WorkoutDefinition(14, "Kręgosłup bez sprzętu zestaw 2",  "kręgosłup bez sprzętu zestaw 2.mp4",       0, "kregoslup"),
    WorkoutDefinition(15, "Obręcz barkowa sprzęt zestaw 2",  "Obręcz barkowa sprzęt zestaw 2.mp4",       0, "barki"),
    WorkoutDefinition(16, "Tylna taśma bez sprzętu zestaw 2","Tylna taśma bez sprzętu zestaw 2.mp4",     0, "tylna_tasma"),
    WorkoutDefinition(17, "Odcinek szyjny",                  "odcinek szyjny.mp4",                       0, "kregoslup"),
)
// Odstępy między tą samą kategorią:
// biodra:        1 → 6 → 12  (5, 6 dni)
// kręgosłup:     2 → 8 → 14  (6, 6 dni)  + odcinek szyjny na 17
// barki:         3 → 9 → 15  (6, 6 dni)
// tylna taśma:   4 → 10 → 16 (6, 6 dni)
// przednia taśma:5 → 13      (8 dni)
```

### 5.2 Room Database — completions + settings

```kotlin
// Tabela: oznaczenia ukończonych treningów
@Entity(
    tableName = "completions",
    indices = [Index(value = ["cycleNumber", "videoOrder"], unique = true)]
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleNumber: Int,       // Numer cyklu (1, 2, 3, ...)
    val videoOrder: Int,        // Który film w cyklu (1-17)
    val completedAt: Long,      // Timestamp
    val date: String            // "2026-03-14" — łatwe grupowanie
)

// Tabela: ustawienia użytkownika
@Entity(tableName = "settings")
data class UserSettings(
    @PrimaryKey val key: String,
    val value: String
)
// Klucze: "program_start_date", "reminder_hour", "reminder_minute"
```

### 5.3 Logika przypisywania filmiku

```
totalDays      = daysBetween(programStartDate, today)   // 0-based
currentCycle   = (totalDays / 17) + 1                   // cykl 1, 2, 3...
currentVideo   = (totalDays % 17) + 1                   // film 1-17

Jeśli totalDays < 0 → program jeszcze się nie zaczął
Program nie ma końca — cykl 17 dni powtarza się w nieskończoność
```

---

## 6. EKRANY APLIKACJI

### 6.1 Ekran główny — "Dzisiaj" (TodayScreen)
```
┌──────────────────────────────┐
│  CYKL 3        Film 7 / 17   │
│  ━━━━━━━━━━━●━━━━━━━━━━━━━━  │  ← progress bar cyklu (7/17)
│                              │
│  ┌────────────────────────┐  │
│  │  Tylna taśma bez sprzętu│  │  ← karta treningu
│  │  15 min                │  │
│  │                        │  │
│  │  ▶ Odtwórz filmik      │  │  ← otwiera lokalne video
│  │                        │  │
│  │  ☐ Oznacz jako zrobione│  │  ← checkbox
│  └────────────────────────┘  │
│                              │
│  Ukończone cykle: 2          │  ← mini statystyka
│  🔥 Seria: 5 dni z rzędu    │  ← streak
└──────────────────────────────┘
```

### 6.2 Kalendarz (CalendarScreen)
```
┌──────────────────────────────────┐
│  ◀  CYKL 3  ▶                    │
│                                  │
│  1   2   3   4   5   6   7      │
│  ✅  ✅  ✅  ✅  ✅  ✅  🔵      │
│                                  │  ✅ = zrobione
│  8   9   10  11  12  13  14     │  🔵 = dzisiaj
│  ○   ○   ○   ○   ○   ○   ○      │  ○ = do zrobienia / przyszłe
│                                  │
│  15  16  17                      │
│  ○   ○   ○                       │
│                                  │
│  Lista filmów cyklu:             │
│  ┌──────────────────────────┐    │
│  │ 1: Biodra z sprzętem     │    │
│  │ 2: Kręgosłup ze sprzętem│    │
│  │ ...                      │    │
│  └──────────────────────────┘    │
└──────────────────────────────────┘
```

### 6.3 Statystyki (StatsScreen)
```
┌──────────────────────────────┐
│  TWÓJ POSTĘP                 │
│                              │
│     ╭───────╮                │
│     │  58%  │  ← % bieżącego │
│     │  7/17 │     cyklu      │
│     ╰───────╯                │
│                              │
│  Ukończone cykle:    2       │
│  Najdłuższa seria:  12 dni   │
│  Aktualna seria:     5 dni   │
│                              │
│  ROZKŁAD CYKLI               │
│  C1 █████████████████ 17/17 ⭐│
│  C2 █████████████████ 17/17 ⭐│
│  C3 ███████░░░░░░░░░░  7/17  │
│                              │
│  KATEGORIE (wszystkie cykle) │
│  Kręgosłup:   6 razy         │
│  Biodra:      4 razy         │
│  Tylna taśma: 4 razy         │
│  Staw skokowy:2 razy         │
└──────────────────────────────┘
```

---

## 7. POWIADOMIENIA PUSH (BEZ SERWERA)

### Mechanizm
Używamy `AlarmManager` z `setExactAndAllowWhileIdle()` — działa bez internetu, bez Firebase.

### Flow
```
1. User ustawia godzinę powiadomienia (np. 07:00)
2. AlarmScheduler → AlarmManager.setExactAndAllowWhileIdle()
3. O 07:00 → ReminderReceiver (BroadcastReceiver) się odpala
4. ReminderReceiver → tworzy Notification z treścią dzisiejszego treningu
5. ReminderReceiver → planuje kolejny alarm na jutro
6. Klik w powiadomienie → otwiera TodayScreen
```

### Przetrwanie restartu telefonu
```
BootReceiver (BOOT_COMPLETED) → ponownie planuje alarm
```

### Inteligentne powiadomienia
- Jeśli trening na dziś już oznaczony → nie wysyła
- Dynamiczny tekst: "Czas na trening! Dziś: Mobilność bioder — 15 min"
- Akcja bezpośrednia w powiadomieniu: "Oznacz jako zrobione"

---

## 8. PLAN IMPLEMENTACJI — KROK PO KROKU

Podzielony na fazy do pisania z Claude Code. Każda faza to samodzielna, testowalna jednostka.

### FAZA 0: Setup projektu (czas: ~1h)
```
□ Utwórz projekt Android Studio (Empty Compose Activity)
□ Skonfiguruj build.gradle.kts z zależnościami
□ Skonfiguruj Hilt (Application class + @HiltAndroidApp)
□ Utwórz strukturę pakietów (data/, domain/, ui/, notification/, di/)
□ Ustaw Material 3 theme
□ Zbuduj i uruchom pusty projekt — sprawdź że się kompiluje
```

### FAZA 1: Model danych i plan treningowy (~1-2h)
```
□ Zdefiniuj WorkoutDefinition data class (order, title, videoFileName, category, durationMinutes)
□ Wypełnij WorkoutPlan.kt — lista 17 filmów (dane uzupełnione w sekcji 5.1)
□ Utwórz Room Database (AppDatabase)
□ Utwórz Completion Entity + DAO (cycleNumber, videoOrder, date)
□ Utwórz UserSettings Entity + DAO
□ Napisz WorkoutRepository + CompletionRepository
□ Utwórz Hilt Module (AppModule) z providerami
```

### FAZA 2: Ekran "Dzisiaj" (~3-4h)
```
□ GetTodayWorkoutUseCase — logika: totalDays = daysSince(start), currentVideo = totalDays % 17 + 1
□ TodayViewModel — state: dzisiejszy filmik, numer cyklu, czy zrobiony, streak
□ WorkoutCard composable — karta z tytułem, opisem, przyciskiem video
□ TodayScreen composable — pełny ekran z kartą i mini-stats (cykl X, film Y/17)
□ Checkbox "Oznacz jako zrobione" → zapis do Room (cycleNumber + videoOrder)
□ Obsługa stanu: przed programem (program jeszcze się nie zaczął)
□ Odtwarzanie lokalnego video (MediaStore URI → systemowy odtwarzacz)
```

### FAZA 3: Kalendarz cyklu (~2-3h)
```
□ GetCycleWorkoutsUseCase — 17 filmów danego cyklu + statusy ukończenia
□ CalendarViewModel — state: aktualny cykl, lista 17 filmów, statusy
□ CycleStrip composable — siatka 1-17 z oznaczeniami (7+7+3)
□ CalendarScreen — nawigacja między cyklami + lista filmów
□ Możliwość oznaczania filmów z przeszłych cykli
```

### FAZA 4: Statystyki (~2-3h)
```
□ GetStatsUseCase — ukończone cykle, streak, % bieżącego cyklu, rozkład kategorii
□ StatsViewModel — state: numer cyklu, progres, streak, dane per cykl
□ ProgressRing composable — animowany pierścień (% bieżącego cyklu)
□ StatsScreen — pełny ekran ze wszystkimi statystykami
□ Wykres cykli (Canvas / prostokąty w Compose)
```

### FAZA 5: Nawigacja (~1h)
```
□ Bottom Navigation Bar (3 zakładki: Dzisiaj / Kalendarz / Statystyki)
□ NavGraph z Compose Navigation
□ Animacje przejść między ekranami
```

### FAZA 6: Powiadomienia push (~2-3h)
```
□ NotificationHelper — kanał powiadomień (Android 8+)
□ AlarmScheduler — planowanie codziennych alarmów
□ ReminderReceiver (BroadcastReceiver) — obsługa alarmu
□ BootReceiver — ponowne planowanie po restarcie
□ Ekran ustawień — wybór godziny powiadomienia (TimePicker)
□ Akcja "Oznacz jako zrobione" bezpośrednio z powiadomienia
□ AndroidManifest: permissions + receiver declarations
```

### FAZA 7: Polish + onboarding (~2h)
```
□ Ekran startowy — wybór daty rozpoczęcia programu
□ Empty states i edge cases
□ Animacje w Compose (animateContentSize, AnimatedVisibility)
□ Ciemny motyw (automatic z Material 3)
□ Ikona aplikacji
□ Testowanie na różnych rozmiarach ekranu
```

### FAZA 8: Finalizacja i release (~1-2h)
```
□ Weryfikacja nazw plików video w WorkoutPlan.kt (17 filmów)
□ Uzupełnienie durationMinutes dla każdego filmu (po skopiowaniu na telefon)
□ Finalne testy całego flow (cykl → reset → nowy cykl)
□ Build release APK → zainstaluj na telefonie
```

---

## 9. KLUCZOWE ZALEŻNOŚCI (build.gradle.kts)

```kotlin
// Wersje
val composeVersion = "2024.12.01"  // BOM
val roomVersion = "2.6.1"
val hiltVersion = "2.51.1"

// Jetpack Compose (BOM)
implementation(platform("androidx.compose:compose-bom:$composeVersion"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.navigation:navigation-compose:2.8.4")

// Room
implementation("androidx.room:room-runtime:$roomVersion")
implementation("androidx.room:room-ktx:$roomVersion")
ksp("androidx.room:room-compiler:$roomVersion")

// Hilt
implementation("com.google.dagger:hilt-android:$hiltVersion")
ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```

---

## 10. PERMISSIONS (AndroidManifest.xml)

```xml
<!-- Powiadomienia (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Dokładne alarmy (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<!-- Ponowne planowanie po restarcie -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Dostęp do lokalnych filmów -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Internet (opcjonalnie, jeśli w przyszłości potrzebne) -->
<!-- <uses-permission android:name="android.permission.INTERNET" /> -->
```

---

## 11. WSKAZÓWKI DLA CLAUDE CODE

Kiedy będziesz pisać z Claude Code, dawaj mu polecenia faza po fazie:

```
Faza 0: "Utwórz nowy projekt Android z Kotlin + Jetpack Compose.
         Skonfiguruj Gradle z Room, Hilt, Navigation Compose.
         Ustaw Material 3 theme. Min SDK 26."

Faza 1: "Na podstawie tego pliku architektury, utwórz warstwę
         danych: data classes, Room DB, DAOs, Repositories.
         Plan treningowy hardcoded z placeholder danymi."

Faza 2: "Zbuduj TodayScreen — główny ekran z kartą dzisiejszego
         treningu, checkboxem do oznaczania, mini statystyką.
         ViewModel z logiką obliczania aktualnego dnia programu."

... itd.
```

**Kluczowe rady:**
- Zawsze dawaj Claude Code kontekst tego dokumentu na starcie sesji
- Proś o kompilowalne fragmenty — testuj po każdej fazie
- Kotlin jest jak TypeScript: `val` = `const`, `var` = `let`, `data class` = `interface`, `when` = `switch`
- Compose jest jak React: `@Composable fun` = komponent, `remember` = `useState`, `LaunchedEffect` = `useEffect`

---

## 12. SZACOWANY CZAS REALIZACJI

| Faza | Opis | Czas |
|------|------|------|
| 0 | Setup projektu | 1h |
| 1 | Model danych + 17 filmów | 1-2h |
| 2 | Ekran "Dzisiaj" | 3-4h |
| 3 | Kalendarz cyklu | 2-3h |
| 4 | Statystyki | 2-3h |
| 5 | Nawigacja | 1h |
| 6 | Powiadomienia | 2-3h |
| 7 | Polish + onboarding | 2h |
| 8 | Finalizacja + release APK | 1-2h |
| **Suma** | | **~15-21h** |

Z Claude Code i znajomością TS powinno iść sprawnie. Kotlin + Compose to przyjemne combo do nauki.
