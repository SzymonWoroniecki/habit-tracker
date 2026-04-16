package io.github.habittracker.domain.service;

import io.github.habittracker.domain.model.Habit;
import io.github.habittracker.domain.model.HabitEntry;
import io.github.habittracker.domain.model.HabitFrequency;
import io.github.habittracker.domain.repository.HabitEntryRepository;
import io.github.habittracker.domain.repository.HabitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HabitServiceTest {

    private final HabitService service = new HabitService(
            mock(HabitRepository.class),
            mock(HabitEntryRepository.class)
    );

    private final LocalDate today = LocalDate.of(2026, 4, 16);

    private Habit habit() {
        return new Habit("Czytanie", "test", HabitFrequency.DAILY);
    }

    private HabitEntry entry(LocalDate date, boolean completed) {
        HabitEntry e = new HabitEntry(habit(), date);
        e.setCompleted(completed);
        return e;
    }

    // -- calculateCurrentStreak ---

    @Test
    @DisplayName("Current streak: empty list returns 0")
    void currentStreak_empty_returnsZero() {
        int result = service.calculateCurrentStreak(List.of(), today);
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Current streak: today + 2 previous days = streak 3")
    void currentStreak_threeConsecutiveDaysIncludingToday_returnsThree() {
        List<HabitEntry> entries = List.of(
                entry(today, true),
                entry(today.minusDays(1), true),
                entry(today.minusDays(2), true)
        );

        int result = service.calculateCurrentStreak(entries, today);

        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("Current streak: missed today but yesterday+before completed = grace day, streak 2")
    void currentStreak_missedToday_usesGraceDay() {
        List<HabitEntry> entries = List.of(
                entry(today.minusDays(1), true),
                entry(today.minusDays(2), true)
        );

        int result = service.calculateCurrentStreak(entries, today);

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("Current streak: gap of 2 days breaks streak = 0")
    void currentStreak_missedTodayAndYesterday_returnsZero() {
        List<HabitEntry> entries = List.of(
                entry(today.minusDays(2), true),
                entry(today.minusDays(3), true)
        );

        int result = service.calculateCurrentStreak(entries, today);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Current streak: incomplete entries are ignored")
    void currentStreak_incompleteEntriesAreIgnored() {
        List<HabitEntry> entries = List.of(
                entry(today, true),
                entry(today.minusDays(1), false), // unchecked
                entry(today.minusDays(2), true)
        );

        int result = service.calculateCurrentStreak(entries, today);

        assertThat(result).isEqualTo(1); // only today counts, yesterday break
    }

    // --- calculateLongestStreak ---

    @Test
    @DisplayName("Longest streak: finds longest sequence, not current one")
    void longestStreak_findsHistoricalMax() {
        // 5-day streak in the past, then gap, then 2 recent days
        List<HabitEntry> entries = List.of(
                entry(today, true),
                entry(today.minusDays(1), true),
                // gap at day-2, day-3
                entry(today.minusDays(4), true),
                entry(today.minusDays(5), true),
                entry(today.minusDays(6), true),
                entry(today.minusDays(7), true),
                entry(today.minusDays(8), true)
        );

        int result = service.calculateLongestStreak(entries);

        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("Longest streak: empty returns 0")
    void longestStreak_empty_returnsZero() {
        int result = service.calculateLongestStreak(List.of());
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Longest streak: single day returns 1")
    void longestStreak_singleDay_returnsOne() {
        int result = service.calculateLongestStreak(List.of(entry(today, true)));
        assertThat(result).isEqualTo(1);
    }
}