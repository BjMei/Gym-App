package com.example.gym_app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Set;

final class TrainingGoalPlanner {

    private TrainingGoalPlanner() {
    }

    static int progressPercent(int completedSessions, int weeklyGoal) {
        if (weeklyGoal <= 0) {
            return 0;
        }
        return Math.max(
                0,
                Math.min(100, Math.round(completedSessions * 100f / weeklyGoal))
        );
    }

    static LocalDate nextPreferredTrainingDate(
            LocalDate today,
            Set<DayOfWeek> preferredDays,
            Set<LocalDate> trainedDays,
            int weeklyGoal) {
        if (today == null) {
            return null;
        }
        Set<DayOfWeek> safeDays =
                preferredDays == null ? Collections.emptySet() : preferredDays;
        Set<LocalDate> safeTrained =
                trainedDays == null ? Collections.emptySet() : trainedDays;
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate weekEnd = weekStart.plusDays(6);
        long completed = safeTrained.stream()
                .filter(date -> !date.isBefore(weekStart) && !date.isAfter(weekEnd))
                .count();
        if (completed >= weeklyGoal) {
            return null;
        }

        if (safeDays.isEmpty()) {
            return safeTrained.contains(today) ? today.plusDays(1) : today;
        }
        for (int offset = 0; offset < 14; offset++) {
            LocalDate candidate = today.plusDays(offset);
            if (safeDays.contains(candidate.getDayOfWeek())
                    && !safeTrained.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
