# Plan brief: katalog ćwiczeń, podmiany na dziś i nowa rozgrzewka

Status: gotowy do implementacji po zatwierdzeniu danych treściowych

Change ID: `customizable-strength-daily-swaps`

## Cel

Zastąpić RDL w bazowym treningu A hip thrustem jednonóż, usunąć całą sekcję
„Aktywacja”, pokazać wspólną rozgrzewkę oraz umożliwić podmianę każdego
ćwiczenia siłowego wyłącznie dla bieżącego dnia bez utraty historii.

## Potwierdzone decyzje

- Biblioteka zawiera przygotowane ćwiczenia; bez CRUD własnych pozycji.
- RDL zostaje trwale usunięty z aktywnego planu A, ale pozostaje w katalogu
  do odczytu historycznych logów.
- Bazowym zamiennikiem RDL jest hip thrust jednonóż.
- Każdy aktywny slot A/B ma akcję „Zmień tylko dzisiaj”.
- Nie powstaje ogólny edytor permanentnego planu A/B.
- Rozgrzewka jest jedna, wspólna i wyświetlana przed treningiem siłowym.
- Sekcja „Aktywacja” znika całkowicie z A i B.
- Rozgrzewka nie jest zapisywana do `StrengthSetLog`.
- Wewnętrzne `planDay = 1/2` zostaje; UI używa nazw A/B.

## Minimalna architektura

1. Statyczny katalog ćwiczeń ze stabilnymi `exerciseId`.
2. Szablony A/B składające się ze stabilnych slotów `slotId`.
3. Każdy slot ma bazowe ćwiczenie i jawne, kompatybilne zamienniki.
4. Override dnia zapisuje się w istniejącym `settings` jako:
   `data + planDay + slotId -> exerciseId`.
5. `StrengthRepository` jest jedynym resolverem planu dla daty.
6. Brak nowych tabel, migracji Room, ekranów i zależności produkcyjnych.

## Flow

- Today pokazuje jedną kartę rozgrzewki, potem ćwiczenia główne i dodatkowe.
- Każde ćwiczenie ma „Zmień tylko dzisiaj”.
- Material 3 bottom sheet pokazuje dozwolone opcje.
- Po wyborze karta dostaje etykietę „Zmienione na dziś” i akcję
  „Przywróć z planu”.
- Override przeżywa restart tego samego dnia, ale nie działa następnego dnia.
- Po zapisaniu pierwszej serii danego slotu podmiana jest blokowana, aby nie
  mieszać dwóch ćwiczeń w jednej historii slotu.

## Rozgrzewka

1. 90/90.
2. Hip thrust obunóż bez obciążenia.
3. Koci grzbiet.
4. Pies z głową w górę / pies z głową w dół.
5. Rotacje tułowia w wykroku — 10 na stronę.
6. Mobilność nadgarstków.
7. Wiosłowanie z gumą wąskim chwytem.
8. Rotacje kubańskie z gumą.

Bieganie nie wchodzi do bazowej rozgrzewki.

## Fazy

0. Checkpoint obecnego worktree, działający Gradle Wrapper i zatwierdzenie
   treści katalogu.
1. Katalog, sloty, rozgrzewka i czysty resolver.
2. Override per data w `settings` i integracja use case/resetu.
3. Today UI: rozgrzewka, picker, status podmiany, przywracanie i błędy.
4. Przepięcie Settings, statystyk, kalendarza i powiadomień na wspólne dane.
5. Test resolvera, build/lint i test aktualizacji na kopii danych telefonu.

## Bramka przed kodowaniem

Trzeba zatwierdzić:

- parametry i opcjonalny film hip thrustu jednonóż;
- krótkie instrukcje/powtórzenia dla kroków rozgrzewki;
- co najmniej jeden kompatybilny zamiennik dla każdego aktywnego slotu A/B.

Nie będziemy oznaczać ćwiczeń jako medycznie „bezpieczne”. Lista zamienników
ma być zatwierdzonym katalogiem użytkownika, nie automatyczną poradą.

## Definition of done

- RDL nie występuje w aktywnym planie A, ale jego historia nadal działa.
- Hip thrust jednonóż jest bazowym ćwiczeniem slotu po RDL.
- W A/B nie istnieje sekcja „Aktywacja”.
- Rozgrzewka jest pokazana dokładnie raz i zawiera osiem ustalonych kroków.
- Każde aktywne ćwiczenie ma co najmniej jedną opcję „na dziś”.
- Override jest izolowany przez datę, plan i slot oraz ma bezpieczny fallback.
- Nie można wybrać duplikatu ćwiczenia w jednym planie.
- Statystyki rozpoznają stare ID; stare logi aktywacji nie zawyżają kalendarza.
- Room pozostaje w wersji 3, a backup obejmuje override przez `settings`.
- Nowe kontrolki spełniają podstawy dostępności i nie resetują scrolla.
- Test, lint, debug build i `git diff --check` przechodzą.

Pełny plan plik po pliku znajduje się w `plan.md`.
