package com.example.gym_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

public class TrainingGoalPlannerTest {

    @Test
    public void progressPercent_isCappedAtOneHundred() {
        assertEquals(67, TrainingGoalPlanner.progressPercent(2, 3));
        assertEquals(100, TrainingGoalPlanner.progressPercent(4, 3));
    }

    @Test
    public void nextPreferredDate_skipsMissedAndCompletedDays() {
        LocalDate monday = LocalDate.of(2026, 6, 15);
        assertEquals(
                LocalDate.of(2026, 6, 17),
                TrainingGoalPlanner.nextPreferredTrainingDate(
                        monday,
                        new HashSet<>(Arrays.asList(
                                DayOfWeek.MONDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.FRIDAY
                        )),
                        new HashSet<>(Arrays.asList(monday)),
                        3
                )
        );
    }

    @Test
    public void nextPreferredDate_returnsNullWhenWeeklyGoalIsMet() {
        LocalDate monday = LocalDate.of(2026, 6, 15);
        assertNull(TrainingGoalPlanner.nextPreferredTrainingDate(
                monday,
                new HashSet<>(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
                new HashSet<>(Arrays.asList(
                        monday,
                        LocalDate.of(2026, 6, 16),
                        LocalDate.of(2026, 6, 17)
                )),
                3
        ));
    }
}
