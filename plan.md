# Plan implementacji: katalog ćwiczeń, podmiany na dziś i nowa rozgrzewka

Status: gotowy do implementacji po zatwierdzeniu danych z sekcji 15

Change ID: `customizable-strength-daily-swaps`

Repo: `/Users/michalbandyszak/Documents/repos/mobilne-cialo`

## 1. Wynik docelowy

Aplikacja zachowuje obecny offline-first stack i historię treningową, ale plan
siłowy przestaje być jedną nierozdzielną listą. Użytkownik dostaje:

- bazowy trening A bez RDL, z hip thrustem jednonóż;
- niezmieniony mechanizm harmonogramu A/B;
- możliwość podmiany każdego ćwiczenia siłowego tylko dla bieżącej daty;
- wspólną rozgrzewkę przed A/B;
- brak osobnej sekcji „Aktywacja”;
- spójną historię starego RDL i nowych zamienników.

To jest jedna logiczna zmiana. Nie wymaga nowej architektury aplikacji,
backendu ani migracji Room.

## 2. Interpretacja ustaleń

Plan przyjmuje literalnie ostatnią decyzję użytkownika:

- stała zmiana dotyczy wyłącznie bazowego slotu RDL;
- wszystkie pozostałe zmiany ćwiczeń są „tylko dzisiaj”;
- nie budujemy permanentnego edytora całych planów A/B;
- gotowy katalog jest częścią aplikacji; użytkownik nie tworzy własnych
  ćwiczeń;
- rozgrzewka jest stałą, wspólną listą instrukcji;
- sekcja „Aktywacja” zostaje usunięta, a nie zastąpiona pustym UI.

Jeżeli później potrzebna będzie trwała edycja dowolnego slotu, ten sam resolver
można rozszerzyć o permanentny override przed fallbackiem do planu bazowego.
Nie jest to część tej zmiany.

## 3. Zakres

### W zakresie

- Rozdzielenie katalogu ćwiczeń od slotów A/B.
- Stabilne `exerciseId` i `slotId`.
- Hip thrust jednonóż jako bazowe ćwiczenie dawnego slotu RDL.
- Usunięcie aktywacji z aktywnych szablonów.
- Osiem kroków wspólnej rozgrzewki.
- Override `data + planDay + slotId -> exerciseId` w istniejącym `settings`.
- Picker zamienników w Today.
- Status „Zmienione na dziś” i „Przywróć z planu”.
- Ochrona istniejących logów oraz blokada duplikatu ćwiczenia.
- Przepięcie statystyk, kalendarza, podglądu ustawień i powiadomień na
  wspólne źródło definicji.
- Jawny stan błędu bez kierowania użytkownika do onboardingu.
- Minimalny test resolvera i walidacji danych katalogu.

### Poza zakresem

- Tworzenie, edycja i usuwanie własnych ćwiczeń.
- Permanentny edytor każdego slotu A/B.
- Osobna zakładka lub ekran „Ćwiczenia”.
- Drag-and-drop rozgrzewki lub planu.
- Trwałe checkboxy i historia rozgrzewki.
- Automatyczne rekomendacje „bezpieczne dla biodra”.
- Nowa tabela Room, `slotId` w `StrengthSetLog` albo snapshot sesji.
- Globalny redesign wszystkich ekranów.
- Naprawa nietransakcyjnego importu backupu; to osobny follow-up bezpieczeństwa.

## 4. Stan wejściowy i bramka bezpieczeństwa

Obecny worktree ma 12 zmian użytkownika, w tym pliki, których dotknie ta
funkcja. Przed implementacją:

1. Przejrzeć i zapisać nazwany checkpoint obecnej iteracji UI.
2. Naprawić istniejące dwa błędy `git diff --check` w `SettingsScreen.kt`.
3. Przywrócić kompletny Gradle Wrapper: `gradlew`, `gradlew.bat` oraz
   `gradle-wrapper.jar`; usunąć ignorowanie jar-a z `.gitignore`.
4. Uzyskać zielony baseline:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   git diff --check
   ```

5. Przed instalacją na istniejącym telefonie wyeksportować aktualny backup.

Implementacja jest NO-GO, dopóki nie ma checkpointu i odtwarzalnego builda.

## 5. Kontrakt danych

### 5.1 Katalog

Katalog pozostaje statyczny w kodzie i zawiera również historyczne,
nieaktywne pozycje. Minimalny model:

```kotlin
data class ExerciseCatalogEntry(
    val id: String,
    val name: String,
    val notes: String = "",
    val videoUrl: String? = null,
    val trackingType: TrackingType,
    val selectable: Boolean = true
)
```

Zasady:

- istniejące ID nie zmieniają się;
- `day1_dumbbell_rdl` zostaje dla historii, ale `selectable = false`;
- stare ID aktywacji zostają dla historii, ale `selectable = false`;
- hip thrust jednonóż dostaje nowe ID, np. `single_leg_hip_thrust`;
- katalog używany przez statystyki jest pełny, nie tylko aktywny plan.

### 5.2 Slot planu

Parametry programu należą do slotu, a nie do chwilowo wybranego ćwiczenia:

```kotlin
data class StrengthExerciseSlot(
    val id: String,
    val defaultExerciseId: String,
    val availableExerciseIds: List<String>,
    val section: String,
    val sets: Int,
    val reps: String,
    val tempo: String = "",
    val rir: String = "",
    val restSeconds: Int? = null
)
```

Każdy zamiennik slotu musi:

- istnieć w katalogu i być `selectable`;
- mieć ten sam `TrackingType` co ćwiczenie bazowe;
- nie występować w innym slocie tego samego rozwiązanego planu;
- zachować serie, tempo, RIR i przerwę danego slotu.

Pozwala to zachować formularz logowania i historyczny mianownik statystyk bez
snapshotu sesji.

### 5.3 Model rozwiązany dla UI

Obecny `StrengthExerciseDefinition` może pozostać modelem wynikowym, ale musi
znać slot i stan override:

```kotlin
data class StrengthExerciseDefinition(
    val slotId: String,
    val defaultExerciseId: String,
    val id: String,
    val section: String,
    val name: String,
    val sets: Int,
    val reps: String,
    val tempo: String = "",
    val rir: String = "",
    val restSeconds: Int? = null,
    val notes: String = "",
    val videoUrl: String? = null,
    val trackingType: TrackingType,
    val choices: List<ExerciseCatalogEntry> = emptyList()
) {
    val isTodayOverride: Boolean get() = id != defaultExerciseId
}
```

### 5.4 Resolver

Jedna czysta funkcja:

```kotlin
resolveStrengthWorkout(template, overridesBySlot)
```

Odpowiada za:

- walidację slotu i wybranego ID;
- fallback do ćwiczenia domyślnego przy uszkodzonym ustawieniu;
- zgodność `TrackingType`;
- brak duplikatu ćwiczenia;
- zbudowanie finalnych `StrengthExerciseDefinition` dla UI i logowania.

Nie dodajemy interfejsu, fabryki ani osobnego `ExerciseRepository`.

## 6. Override „tylko dzisiaj”

Nowy prefix w `SettingsKeys`:

```text
strength_exercise_override_
```

Format klucza:

```text
strength_exercise_override_<YYYY-MM-DD>_<planDay>_<slotId>
```

Przykład:

```text
strength_exercise_override_2026-07-14_1_day1_horizontal_push
```

Wartością jest faktyczne `exerciseId`.

`StrengthRepository` dostaje `SettingsDao` i metody:

```kotlin
fun getExerciseCatalog(): Map<String, ExerciseCatalogEntry>
fun getBaseWorkouts(): List<StrengthWorkoutDefinition>
suspend fun getWorkoutForDate(planDay: Int, date: LocalDate): StrengthWorkoutDefinition?
suspend fun saveExerciseOverrideForDate(date, planDay, slotId, exerciseId): Boolean
suspend fun clearExerciseOverrideForDate(date, planDay, slotId)
suspend fun clearExerciseOverrides()
```

Wybranie ćwiczenia bazowego usuwa wpis zamiast zapisywać zbędne ustawienie.
Reset flow usuwa wszystkie klucze z nowym prefiksem. Stare klucze mogą zostać
do resetu; przy lokalnej aplikacji nie budujemy osobnego mechanizmu sprzątania.

Backup obejmuje override automatycznie, bo już eksportuje wszystkie rekordy
`settings`. `AppDatabase.version` pozostaje `3`.

## 7. Zmiany treści planu

### 7.1 Plan A

- Slot dawnego RDL dostaje neutralne ID, np. `day1_hip_extension`.
- `defaultExerciseId = single_leg_hip_thrust`.
- Robocza recepta: `3 x 10/strona`, tempo i przerwa do zatwierdzenia.
- `TrackingType = WEIGHT_AND_REPS`, przy czym ciężar może pozostać pusty przy
  wersji bez obciążenia.
- RDL pozostaje wyłącznie historyczną pozycją katalogu.

To ustawienie opisuje wybór użytkownika do testu; UI nie przedstawia go jako
medycznie bezpiecznego zamiennika.

### 7.2 Usunięcie aktywacji

Z aktywnych A/B znikają:

- `day1_cuban_rotation`;
- `day1_monster_walk`;
- `day2_banded_glute_bridge`;
- `day2_9090_hip_lift`;
- wszystkie nagłówki i grupowania sekcji `Aktywacja`.

Definicje i stare ID zostają w katalogu dla historii. Nie usuwamy starych
logów.

### 7.3 Wspólna rozgrzewka

Minimalny model:

```kotlin
data class WarmupStep(
    val id: String,
    val name: String,
    val prescription: String,
    val notes: String = "",
    val videoUrl: String? = null
)
```

Bazowa kolejność:

1. `warmup_90_90` — 90/90.
2. `warmup_bodyweight_hip_thrust` — hip thrust obunóż bez obciążenia.
3. `warmup_cat_cow` — koci grzbiet.
4. `warmup_up_down_dog` — pies z głową w górę / w dół.
5. `warmup_lunge_thoracic_rotation` — rotacje tułowia w wykroku,
   10 na stronę.
6. `warmup_wrist_mobility` — mobilność nadgarstków.
7. `warmup_band_narrow_row` — wiosłowanie gumą wąskim chwytem.
8. `warmup_cuban_rotation` — rotacje kubańskie z gumą.

Bieganie nie jest elementem bazowej listy. Rozgrzewka nie ma encji Room,
trwałych checkboxów ani statystyk.

Stary akapit „3–5 min cardio...” oraz link „Rozgrzewka dynamiczna z planu”
znikają. Linki o tempie i opcjonalnym rozciąganiu mogą zostać jako zwinięte
„Materiały”.

## 8. Flow UX

### 8.1 Today

Kolejność ekranu:

1. Nagłówek dnia.
2. Mobilność, jeśli zaplanowana.
3. Jedna zwarta karta „Rozgrzewka”, jeśli istnieje trening siłowy.
4. Ćwiczenia główne.
5. Ćwiczenia dodatkowe.

Każda karta ćwiczenia ma drugorzędną akcję:

```text
Zmień tylko dzisiaj
```

Akcja otwiera natywny Material 3 `ModalBottomSheet`:

- tytuł „Zmień tylko na <data>”;
- wyjaśnienie, że kolejnego dnia wróci plan bazowy;
- lista jawnie dozwolonych opcji;
- aktualne ćwiczenie oznaczone jako wybrane;
- brak CRUD i wyszukiwarki w pierwszej wersji.

Po zmianie:

- karta pokazuje „Zmienione na dziś” również semantycznie, nie tylko kolorem;
- dostępne są „Wybierz inne” i „Przywróć z planu”;
- snackbar potwierdza wynik;
- scroll nie wraca na górę i nie pojawia się pełnoekranowy spinner.

Jeżeli slot ma już zapisany log tego dnia, podmiana jest zablokowana. Nie
usuwamy ani nie ukrywamy istniejących danych po cichu.

### 8.2 Settings

- Obecny podgląd otrzymuje tytuł „Plan bazowy”.
- Pokazuje A/B po rozwiązaniu planu bez override dnia.
- Dodaje zwarty podgląd „Wspólna rozgrzewka”.
- Informuje: „Ćwiczenia możesz podmieniać na jeden dzień na ekranie Dzisiaj”.
- Nie ma przycisków edycji ani nowej trasy nawigacji.

### 8.3 Terminologia i dostępność

- UI używa „Trening A” i „Trening B”; wewnętrzne 1/2 zostaje bez migracji.
- Cele dotykowe mają co najmniej 48 dp.
- TalkBack dostaje np. „Zmień tylko dzisiaj: hip thrust jednonóż”.
- Stan override ma `stateDescription`.
- Bottom sheet działa przy powiększeniu tekstu do 200% i jest przewijalny.
- Nowe teksty i znaczniki nie opierają znaczenia wyłącznie na kolorze.

## 9. Fazy implementacji

### Faza 0 — checkpoint, build i treść

Pliki techniczne:

- `.gitignore`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`

Kroki:

1. Zapisać checkpoint obecnych zmian UI po akceptacji użytkownika.
2. Przywrócić działający wrapper i zielony baseline.
3. Zatwierdzić parametry hip thrustu jednonóż.
4. Zatwierdzić `prescription` ośmiu kroków rozgrzewki.
5. Zbudować macierz `slotId -> availableExerciseIds` z minimum jednym
   zamiennikiem poza bazowym dla każdego aktywnego slotu.

### Faza 1 — katalog, sloty i resolver

Główne pliki:

- `data/model/StrengthWorkoutPlan.kt`
- nowy mały test resolvera pod `app/src/test/...`
- `app/build.gradle.kts` tylko dla minimalnej zależności testowej, jeśli
  obecny toolchain jej nie zapewnia.

Kroki:

1. Rozdzielić katalog od szablonów.
2. Zachować wszystkie istniejące ID.
3. Dodać hip thrust jednonóż.
4. Usunąć aktywację z A/B.
5. Dodać `WARMUP_ROUTINE`.
6. Zaimplementować czysty resolver i walidację danych.

Bramka:

- unikalne `exerciseId` i `slotId`;
- każda referencja istnieje;
- każdy slot ma bazę i zamiennik tego samego `TrackingType`;
- brak duplikatów w rozwiązanym planie;
- nieznane override daje fallback, nie crash.

### Faza 2 — zapis override i use case

Główne pliki:

- `data/model/UserSettings.kt`
- `data/repository/StrengthRepository.kt`
- `domain/usecase/GetTodayWorkoutUseCase.kt`
- `domain/usecase/ResetProgramFlowUseCase.kt`

Kroki:

1. Wstrzyknąć `SettingsDao` do `StrengthRepository`.
2. Dodać odczyt, zapis, usuwanie i czyszczenie override.
3. `GetTodayWorkoutUseCase` pobiera trening przez `getWorkoutForDate()`.
4. Reset flow czyści dzienne podmiany.
5. Zachować wersję Room i DTO backupu bez zmian.

### Faza 3 — Today UI

Główne pliki:

- `ui/today/TodayViewModel.kt`
- `ui/today/TodayScreen.kt`

Kroki:

1. Dodać listę rozgrzewki i usunąć stary akapit/link.
2. Przejść z długiego `Column.verticalScroll` na `LazyColumn`.
3. Dodać picker, status override i przywracanie.
4. Blokować zmianę po pierwszym logu slotu.
5. Zachować zawartość podczas odświeżenia, bez pełnego spinnera.
6. Dodać błąd z ponowieniem zamiast udawania nieuruchomionego programu.
7. Dodać snackbary i semantykę.

### Faza 4 — spójność pozostałych konsumentów

Główne pliki:

- `domain/usecase/GetStrengthStatsUseCase.kt`
- `ui/calendar/CalendarViewModel.kt`
- `ui/settings/SettingsViewModel.kt`
- `ui/settings/SettingsScreen.kt`
- `notification/ReminderReceiver.kt`

Kroki:

1. Statystyki rozwiązują nazwy i tracking przez pełny katalog.
2. Stare RDL nadal jest widoczne jako osobne ćwiczenie.
3. Kalendarz nie liczy historycznych logów `TrackingType.NONE` jako serii.
4. Settings pokazuje bazowy plan i tę samą rozgrzewkę co Today.
5. Powiadomienia nie importują statycznego getter-a planu bezpośrednio.
6. Wszystkie etykiety użytkownika są A/B, nie mieszanką A/B i T1/T2.

### Faza 5 — weryfikacja i wydanie

Automatycznie:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
git diff --check
```

Przed dystrybucją:

```bash
./gradlew :app:assembleRelease
```

Podnieść `versionCode` dopiero przed rzeczywistą instalacją aktualizacji.

## 10. Minimalny test automatyczny

Jeden plik `StrengthPlanResolverTest` powinien sprawdzać:

1. Plan bazowy A zawiera hip thrust jednonóż, nie RDL.
2. Plan A/B nie zawiera sekcji `Aktywacja`.
3. Rozgrzewka zawiera dokładnie osiem ustalonych kroków.
4. Dzienne override zmienia tylko właściwy slot, datę i plan.
5. Override A nie zmienia B.
6. Brak override następnego dnia przywraca bazę.
7. Nieznane ID daje fallback do bazy.
8. Zamiennik spoza `availableExerciseIds` jest odrzucony.
9. Zamiennik z innym `TrackingType` jest odrzucony.
10. Ten sam `exerciseId` nie może wystąpić w dwóch slotach jednego planu.
11. Wszystkie ID są unikalne i wszystkie referencje rozwiązują się.
12. Historyczne `day1_dumbbell_rdl` nadal istnieje w katalogu.

Migration test nie jest potrzebny, jeśli `AppDatabase.version` pozostaje `3`.

## 11. Test aktualizacji na urządzeniu

Na kopii realnych danych, bez odinstalowania aplikacji:

1. Zanotować liczbę sesji i istniejący wynik RDL.
2. Wyeksportować backup.
3. Zainstalować APK nad obecną wersją bez czyszczenia danych.
4. Potwierdzić historię RDL i dotychczasowych sesji.
5. Sprawdzić hip thrust jednonóż w bazowym A.
6. Potwierdzić brak „Aktywacji” w A i B.
7. Potwierdzić identyczną rozgrzewkę w A i B.
8. Zmienić jedno ćwiczenie na dziś, zamknąć i uruchomić aplikację ponownie.
9. Potwierdzić status override oraz izolację A/B.
10. Przywrócić ćwiczenie bazowe.
11. Ponownie ustawić override, zalogować serię i potwierdzić właściwe ID w
    statystykach.
12. Potwierdzić blokadę dalszej zmiany po rozpoczęciu logowania.
13. Sprawdzić ekran przy skali tekstu 200% i TalkBack.
14. Wykonać backup round-trip na danych testowych.

## 12. Kryteria odbioru

### Funkcja

- Plan A pokazuje hip thrust jednonóż zamiast RDL.
- Plan A/B nie ma sekcji ani kart aktywacji.
- Rozgrzewka jest pokazana dokładnie raz przed siłą.
- Każdy aktywny slot ma co najmniej jeden zatwierdzony zamiennik.
- Podmiana przeżywa restart tego samego dnia.
- Następnego dnia nie wpływa na plan bazowy.
- „Przywróć z planu” usuwa właściwe ustawienie.
- Kilka slotów może mieć niezależne podmiany tego samego dnia.
- Jednorazowa zmiana całego A/B nadal działa.

### Dane

- Log zapisuje faktycznie wykonywane `exerciseId`, nie `slotId`.
- Nie można wybrać tego samego ćwiczenia dwa razy w jednym planie.
- Nie można zmienić slotu po rozpoczęciu jego logowania.
- Stare logi RDL pozostają widoczne i oddzielone od hip thrustu.
- Stare logi aktywacji nie są usuwane z backupu.
- Stare logi `NONE` nie zawyżają liczby serii w kalendarzu.
- Uszkodzony override daje fallback bez utraty danych.
- Room pozostaje w wersji 3.

### UX i dostępność

- Picker jasno mówi „tylko dzisiaj” i podaje datę.
- Aktywna podmiana ma tekstowy i semantyczny status.
- Zapis pokazuje snackbar, nie pełnoekranowy reload.
- Stan błędu ma „Spróbuj ponownie”.
- Cele dotykowe mają minimum 48 dp.
- UI działa przy 200% rozmiaru tekstu.
- Terminologia A/B jest spójna na wszystkich ekranach.

### Techniczne

- Resolver ma jeden mały, przechodzący test.
- Lint, debug build i `git diff --check` przechodzą.
- Release build przechodzi przed instalacją aktualizacji.
- Backup istnieje przed testem na głównym telefonie.

## 13. Pre-mortem

| Możliwa porażka | Wczesny sygnał | Ochrona |
|---|---|---|
| Historia RDL znika | Statystyki filtrują nieaktywne ID | Pełny katalog, stare ID bez zmian |
| Dwa sloty nadpisują log | Ten sam `exerciseId` wybrany dwa razy | Walidacja resolvera i filtrowanie pickera |
| Podmiana miesza serie | Zmiana po zapisaniu pierwszego setu | Blokada zmiany po rozpoczęciu logowania |
| Override przecieka na jutro | Klucz nie zawiera daty | Data w kluczu i test innej daty |
| UI wraca na górę po zmianie | `loadToday()` zeruje cały state | Ciche odświeżenie i zachowanie list state |
| Backup traci ustawienie | Override zapisany poza `settings` | Reuse `UserSettings` bez nowego DTO |
| Build nie jest odtwarzalny | Brak `gradlew`/jar | Faza 0 przed funkcją |
| Katalog udaje poradę medyczną | Etykiety „bezpieczne dla biodra” | Neutralne nazwy i jawne zatwierdzenie użytkownika |

## 14. Proponowane checkpointy implementacji

1. `chore: restore reproducible Android build`
2. `refactor: split strength catalog from workout slots`
3. `feat: persist daily exercise swaps`
4. `feat: add warmup and daily swap UX`
5. `fix: preserve strength history across plan changes`

Każdy checkpoint powinien przechodzić `git diff --check`; test/build od fazy 1.

## 15. Otwarte dane treściowe

Te punkty nie zmieniają architektury, ale blokują kompletną implementację:

1. Ostateczne tempo, przerwa i opcjonalny film hip thrustu jednonóż.
2. Krótkie `prescription` dla wszystkich kroków rozgrzewki poza rotacją
   tułowia `10/strona`.
3. Co najmniej jeden zaakceptowany zamiennik o zgodnym `TrackingType` dla
   każdego aktywnego slotu A/B, szczególnie `TIME_ONLY` plank i `REPS_ONLY`
   deadbug.

Bez punktu 3 picker nie może uczciwie obiecywać podmiany każdego ćwiczenia.
