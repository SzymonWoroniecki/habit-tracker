package io.github.habittracker.domain.repository;

import io.github.habittracker.domain.model.HabitEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitEntryRepository extends JpaRepository<HabitEntry, Long> {

    Optional<HabitEntry> findByHabitIdAndEntryDate(Long habitId, LocalDate entryDate);

    List<HabitEntry> findAllByHabitIdOrderByEntryDateDesc(Long habitId);

    List<HabitEntry> findAllByHabitIdAndEntryDateBetweenOrderByEntryDateAsc(
            Long habitId, LocalDate from, LocalDate to);
}