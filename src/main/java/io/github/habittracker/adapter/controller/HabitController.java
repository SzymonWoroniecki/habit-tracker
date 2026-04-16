package io.github.habittracker.adapter.controller;

import io.github.habittracker.domain.model.Habit;
import io.github.habittracker.domain.repository.HabitEntryRepository;
import io.github.habittracker.domain.repository.HabitRepository;
import io.github.habittracker.domain.service.HabitService;
import io.github.habittracker.model.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
@Tag(name = "Habits", description = "Habit tracking API")
public class HabitController {

    private final HabitRepository habitRepository;
    private final HabitEntryRepository entryRepository;
    private final HabitService habitService;

    public HabitController(HabitRepository habitRepository,
                           HabitEntryRepository entryRepository,
                           HabitService habitService) {
        this.habitRepository = habitRepository;
        this.entryRepository = entryRepository;
        this.habitService = habitService;
    }

    @GetMapping
    @Operation(summary = "List all active habits")
    public List<HabitDto> listActive() {
        return habitRepository.findAllByArchivedFalse().stream()
                .map(HabitDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get habit by id")
    public ResponseEntity<HabitDto> getById(@PathVariable Long id) {
        return habitRepository.findById(id)
                .map(HabitDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new habit")
    public ResponseEntity<HabitDto> create(@Valid @RequestBody CreateHabitRequest request) {
        Habit habit = new Habit(request.getName(), request.getDescription(), request.getFrequency());
        Habit saved = habitRepository.save(habit);
        return ResponseEntity
                .created(URI.create("/api/habits/" + saved.getId()))
                .body(HabitDto.from(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update habit")
    public ResponseEntity<HabitDto> update(@PathVariable Long id,
                                           @Valid @RequestBody CreateHabitRequest request) {
        return habitRepository.findById(id)
                .map(habit -> {
                    habit.setName(request.getName());
                    habit.setDescription(request.getDescription());
                    habit.setFrequency(request.getFrequency());
                    return habitRepository.save(habit);
                })
                .map(HabitDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive habit (soft delete)")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        return habitRepository.findById(id)
                .map(habit -> {
                    habit.setArchived(true);
                    habitRepository.save(habit);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard delete habit with all entries")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!habitRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        habitRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/check-in")
    @Operation(summary = "Mark habit as completed for given day (defaults to today)")
    public ResponseEntity<HabitEntryDto> checkIn(@PathVariable Long id,
                                                 @RequestBody(required = false) CheckInRequest request) {
        LocalDate date = (request != null && request.getDate() != null)
                ? request.getDate() : LocalDate.now();
        String note = request != null ? request.getNote() : null;

        var entry = habitService.markCompletedForDay(id, date, note);
        return ResponseEntity.ok(HabitEntryDto.from(entry));
    }

    @DeleteMapping("/{id}/check-in")
    @Operation(summary = "Remove check-in for given day (defaults to today)")
    public ResponseEntity<Void> removeCheckIn(@PathVariable Long id,
                                              @RequestParam(required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        habitService.removeEntryForDay(id, target);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/entries")
    @Operation(summary = "List all entries for habit (most recent first)")
    public List<HabitEntryDto> listEntries(@PathVariable Long id) {
        return entryRepository.findAllByHabitIdOrderByEntryDateDesc(id).stream()
                .map(HabitEntryDto::from)
                .toList();
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get habit statistics: streaks, completion rate")
    public ResponseEntity<HabitStatistics> statistics(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(habitService.getStatistics(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}