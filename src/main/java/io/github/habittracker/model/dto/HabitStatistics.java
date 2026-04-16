package io.github.habittracker.model.dto;

public class HabitStatistics {

    private final Long habitId;
    private final String habitName;
    private final int currentStreak;
    private final int longestStreak;
    private final int totalCompletions;
    private final double completionRate;

    public HabitStatistics(Long habitId, String habitName,
                           int currentStreak, int longestStreak,
                           int totalCompletions, double completionRate) {
        this.habitId = habitId;
        this.habitName = habitName;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.totalCompletions = totalCompletions;
        this.completionRate = completionRate;
    }

    public Long getHabitId() { return habitId; }
    public String getHabitName() { return habitName; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public int getTotalCompletions() { return totalCompletions; }
    public double getCompletionRate() { return completionRate; }
}