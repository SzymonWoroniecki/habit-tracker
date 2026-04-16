package io.github.habittracker.model.dto;

import io.github.habittracker.domain.model.HabitEntry;

import java.time.LocalDate;

public class HabitEntryDto {

    private Long id;
    private LocalDate entryDate;
    private boolean completed;
    private String note;

    public HabitEntryDto() {}

    public static HabitEntryDto from(HabitEntry entry) {
        HabitEntryDto dto = new HabitEntryDto();
        dto.id = entry.getId();
        dto.entryDate = entry.getEntryDate();
        dto.completed = entry.isCompleted();
        dto.note = entry.getNote();
        return dto;
    }

    public Long getId() { return id; }
    public LocalDate getEntryDate() { return entryDate; }
    public boolean isCompleted() { return completed; }
    public String getNote() { return note; }
}