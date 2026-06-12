package com.example.gym_app;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ProgressCalculatorTest {

    @Test
    public void streaks_workAcrossMonthAndYearBoundaries() {
        Set<LocalDate> dates = new LinkedHashSet<>(Arrays.asList(
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2)
        ));

        ProgressCalculator.StreakStats result =
                ProgressCalculator.calculateStreaks(
                        dates,
                        null,
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 2)
                );

        assertEquals(3, result.currentStreak);
        assertEquals(3, result.bestStreak);
        assertEquals(LocalDate.of(2025, 12, 31), result.bestStart);
        assertEquals(LocalDate.of(2026, 1, 2), result.bestEnd);
    }

    @Test
    public void currentStreak_acceptsYesterdayAsLatestTrainingDay() {
        Set<LocalDate> dates = new LinkedHashSet<>(Arrays.asList(
                LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 10)
        ));

        ProgressCalculator.StreakStats result =
                ProgressCalculator.calculateStreaks(
                        dates,
                        null,
                        LocalDate.of(2026, 6, 11),
                        LocalDate.of(2026, 6, 11)
                );

        assertEquals(2, result.currentStreak);
    }

    @Test
    public void streaks_respectSelectedPeriod() {
        Set<LocalDate> dates = new LinkedHashSet<>(Arrays.asList(
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2)
        ));

        ProgressCalculator.StreakStats result =
                ProgressCalculator.calculateStreaks(
                        dates,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 2),
                        LocalDate.of(2026, 6, 2)
                );

        assertEquals(2, result.bestStreak);
        assertEquals(LocalDate.of(2026, 6, 1), result.bestStart);
    }

    @Test
    public void weekLabels_includeWeekBasedYear() {
        Set<LocalDate> dates = new LinkedHashSet<>(Arrays.asList(
                LocalDate.of(2025, 12, 29),
                LocalDate.of(2026, 1, 5)
        ));

        List<ProgressCalculator.WeekSummary> result =
                ProgressCalculator.buildWeeklySummaries(
                        dates,
                        LocalDate.of(2025, 12, 29),
                        LocalDate.of(2026, 1, 5)
                );

        assertEquals("KW1/2026", result.get(0).label);
        assertEquals("KW2/2026", result.get(1).label);
        assertEquals(1, result.get(0).sessionDays);
        assertEquals(1, result.get(1).sessionDays);
    }

    @Test
    public void goalAchievement_usesWeeklyTarget() {
        assertEquals(100, ProgressCalculator.goalAchievement(3.0, 3));
        assertEquals(67, ProgressCalculator.goalAchievement(2.0, 3));
        assertEquals(133, ProgressCalculator.goalAchievement(4.0, 3));
    }

    @Test
    public void previousPeriod_hasSameInclusiveLength() {
        ProgressCalculator.Period period = ProgressCalculator.previousPeriod(
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 11)
        );

        assertEquals(LocalDate.of(2026, 5, 23), period.start);
        assertEquals(LocalDate.of(2026, 6, 1), period.end);
    }
}
