[![CI](https://github.com/SzymonWoroniecki/habit-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/SzymonWoroniecki/habit-tracker/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/tests-15%20passing-success.svg)](https://github.com/SzymonWoroniecki/habit-tracker/actions)

# Habit Tracker API

REST API do śledzenia codziennych nawyków. Projekt portfolio demonstrujący pracę z ekosystemem Spring Boot, PostgreSQL, migracjami bazy danych i logiką biznesową (obliczanie serii odhaczeń, statystyk).

## Co robi aplikacja

- Tworzenie i zarządzanie nawykami (np. "Czytanie 30 min", "Trening", "Medytacja")
- Codzienne odhaczanie wykonania nawyku (check-in)
- Obliczanie **aktualnej serii (streak)** — ile dni z rzędu nawyk został wykonany
- **Najdłuższa seria w historii** — rekord użytkownika
- **% ukończenia** — stosunek odhaczonych dni do wszystkich dni od utworzenia nawyku
- Archiwizacja nawyków (soft delete) oraz twarde usuwanie

## Stack

- **Java 17+** / **Spring Boot 3.2.5**
- **Spring Data JPA** + **Hibernate 6**
- **PostgreSQL 16** (produkcyjnie) / **H2** (do testów)
- **Flyway** — wersjonowanie schematu bazy
- **Spring Security** — Basic Auth (in-memory userdetails)
- **springdoc-openapi** — Swagger UI dokumentacja API
- **Maven** — build system
- **JUnit 5** + **Mockito** + **AssertJ** — testy (planowane)

## Architektura

Projekt zorganizowany warstwowo z rozdzieleniem domeny i adaptera:

```
io.github.habittracker
├── adapter.controller    # REST controllers + global exception handler
├── config                # Spring configuration (Security)
├── domain
│   ├── model            # Entities (Habit, HabitEntry, HabitFrequency)
│   ├── repository       # Spring Data JPA repositories
│   └── service          # Logika biznesowa (HabitService)
├── model.dto            # Data Transfer Objects
└── security             # CurrentUserService
```

Encja **Habit** reprezentuje nawyk, **HabitEntry** pojedynczy wpis wykonania w danym dniu.
Relacja `Habit 1:N HabitEntry`, z unique constraint `(habit_id, entry_date)` zapobiegającym duplikatom.

## Uruchomienie lokalnie

### Wymagania
- JDK 17+
- PostgreSQL 16+ (lokalnie albo zdalnie)
- Maven nie jest wymagany — IntelliJ używa wbudowanego

### Konfiguracja bazy
Utwórz bazę i użytkownika w PostgreSQL:
```sql
CREATE DATABASE habit_tracker;
CREATE USER habit_user WITH PASSWORD 'habit_pass';
GRANT ALL PRIVILEGES ON DATABASE habit_tracker TO habit_user;
\c habit_tracker
GRANT ALL ON SCHEMA public TO habit_user;
```

Dane połączenia można zmienić w `src/main/resources/application.yml`.

### Start
W IntelliJ uruchom klasę `HabitTrackerApplication` albo:
```bash
./mvnw spring-boot:run
```

Aplikacja startuje na `http://localhost:8080`.

### Swagger UI
Dokumentacja interaktywna: **http://localhost:8080/swagger-ui.html**

### Domyślne konta (Basic Auth)
- `admin` / `admin` — role ADMIN + USER (dostęp do Actuator)
- `user` / `user` — rola USER

## Przykładowe wywołania API

### 1. Utwórz nawyk
```http
POST /api/habits
Content-Type: application/json
Authorization: Basic dXNlcjp1c2Vy

{
  "name": "Czytanie 30 min",
  "description": "Czytam codziennie przed snem",
  "frequency": "DAILY"
}
```

### 2. Odhacz na dzisiaj
```http
POST /api/habits/1/check-in
Content-Type: application/json
```
Ciało opcjonalne — bez podania daty odhacza dzisiaj:
```json
{ "date": "2026-04-16", "note": "Rozdział o SOLID" }
```

### 3. Pobierz statystyki
```http
GET /api/habits/1/statistics
```
Odpowiedź:
```json
{
  "habitId": 1,
  "habitName": "Czytanie 30 min",
  "currentStreak": 5,
  "longestStreak": 12,
  "totalCompletions": 28,
  "completionRate": 93.33
}
```

## Endpointy

| Metoda  | Path                              | Opis                                         |
|---------|-----------------------------------|----------------------------------------------|
| GET     | `/api/habits`                     | Lista aktywnych nawyków                      |
| GET     | `/api/habits/{id}`                | Pobierz nawyk po ID                          |
| POST    | `/api/habits`                     | Utwórz nawyk                                 |
| PUT     | `/api/habits/{id}`                | Aktualizuj nawyk                             |
| POST    | `/api/habits/{id}/archive`        | Zarchiwizuj (soft delete)                    |
| DELETE  | `/api/habits/{id}`                | Usuń nawyk wraz z wpisami                    |
| POST    | `/api/habits/{id}/check-in`       | Odhacz na dany dzień (default: dziś)         |
| DELETE  | `/api/habits/{id}/check-in?date=` | Cofnij odhaczenie                            |
| GET     | `/api/habits/{id}/entries`        | Lista wpisów                                 |
| GET     | `/api/habits/{id}/statistics`     | Statystyki: streaki, % ukończenia            |

## Logika streaków — edge cases

Obliczanie **aktualnej serii** obsługuje następujące przypadki:
- Brak odhaczenia dzisiaj — liczy się seria zakończona wczoraj ("dzień łaski")
- Brak odhaczenia wczoraj i dziś — streak = 0
- Wstecznie dodany wpis nie łamie istniejącej serii jeśli wypełnia lukę

**Completion rate** jest cappowany do 100% i bierze pod uwagę najwcześniejszy wpis (nie tylko datę utworzenia nawyku) — pozwala to na poprawne statystyki gdy user odhacza dni wcześniejsze niż utworzenie nawyku w systemie.

## Testing

Projekt ma 15 testów pokrywających kluczowe części:

### Unit tests (`HabitServiceTest`) — 8 testów
Testy logiki biznesowej bez uruchamiania Springa. Sprawdzają edge cases w obliczaniu serii:
- Pusta lista wpisów → streak 0
- Trzy dni z rzędu włącznie z dzisiaj → streak 3
- Brak wpisu dziś, ale wczoraj wykonane → **"dzień łaski"**, streak liczony od wczoraj
- Dwudniowa luka → streak = 0
- Niezaznaczone wpisy ignorowane
- Najdłuższy streak historyczny (nie tylko aktualny)

### Integration tests (`HabitControllerIntegrationTest`) — 7 testów
Testy REST API z użyciem `@SpringBootTest` + `MockMvc`. Podnoszą pełen kontekst Springa i testują przepływy HTTP end-to-end przeciw bazie H2 in-memory:
- `GET /api/habits` na pustej bazie zwraca `[]`
- `POST /api/habits` zwraca 201 z lokalizacją
- Walidacja pustego `name` zwraca 400 z opisem błędu
- Nieautoryzowany request zwraca 401
- Nieistniejący habit → 404
- Check-in bez body używa dzisiejszej daty
- Statystyki świeżego habita zwracają same zera

### Uruchomienie

```bash
mvn test                    # wszystkie testy
mvn test -Dtest=HabitServiceTest    # tylko unity
```

Testy uruchamiają się automatycznie w CI na każdym pushu — zobacz [Actions](https://github.com/SzymonWoroniecki/habit-tracker/actions).

## Co następnie (roadmap)

- [X] Testy jednostkowe dla logiki streaków (JUnit 5 + AssertJ)
- [X] Testy integracyjne MockMvc dla kontrolera
- [X] GitHub Actions CI (build + testy na każdym PR)
- [ ] Docker + docker-compose (Postgres + aplikacja)
- [ ] JWT zamiast Basic Auth
- [ ] Frontend (React) — oddzielne repo

## Szymon Woroniecki

Projekt portfolio — nauka Spring Boot.