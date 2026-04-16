package io.github.habittracker.domain.repository;

import io.github.habittracker.domain.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findAllByArchivedFalse();

    List<Habit> findAllByOwnerIdAndArchivedFalse(String ownerId);

    @Query("select h from Habit h left join fetch h.entries where h.id = :id")
    Optional<Habit> findByIdWithEntries(Long id);
}