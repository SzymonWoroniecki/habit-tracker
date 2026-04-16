package io.github.habittracker.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "habit_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_habit_entry_day",
                columnNames = {"habit_id", "entry_date"}
        )
)
public class HabitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private boolean completed = true;

    @Column(length = 500)
    private String note;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    public HabitEntry() {
    }

    public HabitEntry(Habit habit, LocalDate entryDate) {
        this.habit = habit;
        this.entryDate = entryDate;
    }

    public HabitEntry(Habit habit, LocalDate entryDate, String note) {
        this.habit = habit;
        this.entryDate = entryDate;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
    }

    public Long getId() { return id; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public Habit getHabit() { return habit; }
    public void setHabit(Habit habit) { this.habit = habit; }
}