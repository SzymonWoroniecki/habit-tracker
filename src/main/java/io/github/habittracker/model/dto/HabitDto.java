package io.github.habittracker.model.dto;

import io.github.habittracker.domain.model.Habit;
import io.github.habittracker.domain.model.HabitFrequency;

import java.time.LocalDateTime;

public class HabitDto {

    private Long id;
    private String name;
    private String description;
    private HabitFrequency frequency;
    private boolean archived;
    private LocalDateTime createdOn;

    public HabitDto() {}

    public static HabitDto from(Habit habit) {
        HabitDto dto = new HabitDto();
        dto.id = habit.getId();
        dto.name = habit.getName();
        dto.description = habit.getDescription();
        dto.frequency = habit.getFrequency();
        dto.archived = habit.isArchived();
        dto.createdOn = habit.getCreatedOn();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public HabitFrequency getFrequency() { return frequency; }
    public boolean isArchived() { return archived; }
    public LocalDateTime getCreatedOn() { return createdOn; }
}