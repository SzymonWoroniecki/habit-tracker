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

## Co następnie (roadmap)

- [ ] Testy jednostkowe dla logiki streaków (JUnit 5 + AssertJ)
- [ ] Testy integracyjne MockMvc dla kontrolera
- [ ] GitHub Actions CI (build + testy na każdym PR)
- [ ] Docker + docker-compose (Postgres + aplikacja)
- [ ] JWT zamiast Basic Auth
- [ ] Frontend (React) — oddzielne repo

## Autor

Projekt portfolio — nauka Spring Boot.