package io.github.habittracker.domain.service;

import io.github.habittracker.domain.model.Habit;
import io.github.habittracker.domain.model.HabitEntry;
import io.github.habittracker.domain.repository.HabitEntryRepository;
import io.github.habittracker.domain.repository.HabitRepository;
import io.github.habittracker.model.dto.HabitStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitEntryRepository entryRepository;

    public HabitService(HabitRepository habitRepository, HabitEntryRepository entryRepository) {
        this.habitRepository = habitRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public HabitEntry markCompletedForDay(Long habitId, LocalDate date, String note) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found: id=" + habitId));

        return entryRepository.findByHabitIdAndEntryDate(habitId, date)
                .map(existing -> {
                    existing.setCompleted(true);
                    if (note != null) existing.setNote(note);
                    return existing;
                })
                .orElseGet(() -> {
                    HabitEntry entry = new HabitEntry(habit, date, note);
                    return entryRepository.save(entry);
                });
    }

    @Transactional
    public void removeEntryForDay(Long habitId, LocalDate date) {
        entryRepository.findByHabitIdAndEntryDate(habitId, date)
                .ifPresent(entryRepository::delete);
    }

    @Transactional(readOnly = true)
    public HabitStatistics getStatistics(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found: id=" + habitId));

        List<HabitEntry> entries = entryRepository.findAllByHabitIdOrderByEntryDateDesc(habitId);

        int currentStreak = calculateCurrentStreak(entries, LocalDate.now());
        int longestStreak = calculateLongestStreak(entries);
        int totalCompletions = (int) entries.stream().filter(HabitEntry::isCompleted).count();
        double completionRate = calculateCompletionRate(habit, entries);

        return new HabitStatistics(
                habit.getId(),
                habit.getName(),
                currentStreak,
                longestStreak,
                totalCompletions,
                completionRate
        );
    }

    int calculateCurrentStreak(List<HabitEntry> entriesDesc, LocalDate today) {
        if (entriesDesc.isEmpty()) return 0;

        Set<LocalDate> completedDays = entriesDesc.stream()
                .filter(HabitEntry::isCompleted)
                .map(HabitEntry::getEntryDate)
                .collect(Collectors.toSet());

        if (completedDays.isEmpty()) return 0;

        LocalDate cursor = completedDays.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    int calculateLongestStreak(List<HabitEntry> entries) {
        if (entries.isEmpty()) return 0;

        List<LocalDate> sortedCompleted = entries.stream()
                .filter(HabitEntry::isCompleted)
                .map(HabitEntry::getEntryDate)
                .sorted()
                .toList();

        if (sortedCompleted.isEmpty()) return 0;

        int longest = 1;
        int current = 1;
        for (int i = 1; i < sortedCompleted.size(); i++) {
            if (sortedCompleted.get(i).minusDays(1).equals(sortedCompleted.get(i - 1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    double calculateCompletionRate(Habit habit, List<HabitEntry> entries) {
        if (entries.isEmpty()) return 0.0;

        LocalDate today = LocalDate.now();
        LocalDate creationDate = habit.getCreatedOn().toLocalDate();

        LocalDate earliestEntry = entries.stream()
                .map(HabitEntry::getEntryDate)
                .min(LocalDate::compareTo)
                .orElse(creationDate);

        LocalDate start = earliestEntry.isBefore(creationDate) ? earliestEntry : creationDate;

        long totalDays = ChronoUnit.DAYS.between(start, today) + 1;
        if (totalDays <= 0) return 0.0;

        long completedDays = entries.stream().filter(HabitEntry::isCompleted).count();
        double rate = ((double) completedDays / totalDays) * 100.0;

        // Cap at 100% just in case
        rate = Math.min(rate, 100.0);

        return Math.round(rate * 100.0) / 100.0;
    }
}