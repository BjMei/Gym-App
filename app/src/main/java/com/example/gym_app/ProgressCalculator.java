package com.example.gym_app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ProgressCalculator {

    private ProgressCalculator() {
    }

    static StreakStats calculateStreaks(
            Set<LocalDate> activeDays,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate today) {
        List<LocalDate> sorted = filterDates(activeDays, periodStart, periodEnd);
        if (sorted.isEmpty()) {
            return new StreakStats(0, 0, null, null);
        }

        Set<LocalDate> filtered = new LinkedHashSet<>(sorted);
        LocalDate currentCursor = today;
        if (!filtered.contains(currentCursor)) {
            currentCursor = today.minusDays(1);
        }

        int current = 0;
        while (filtered.contains(currentCursor)
                && (periodStart == null || !currentCursor.isBefore(periodStart))) {
            current++;
            currentCursor = currentCursor.minusDays(1);
        }

        int best = 1;
        int running = 1;
        LocalDate runningStart = sorted.get(0);
        LocalDate bestStart = runningStart;
        LocalDate bestEnd = runningStart;

        for (int i = 1; i < sorted.size(); i++) {
            LocalDate previous = sorted.get(i - 1);
            LocalDate currentDate = sorted.get(i);
            if (ChronoUnit.DAYS.between(previous, currentDate) == 1) {
                running++;
            } else {
                running = 1;
                runningStart = currentDate;
            }

            if (running > best) {
                best = running;
                bestStart = runningStart;
                bestEnd = currentDate;
            }
        }

        return new StreakStats(current, best, bestStart, bestEnd);
    }

    static List<WeekSummary> buildWeeklySummaries(
            Set<LocalDate> activeDays,
            LocalDate periodStart,
            LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd)) {
            return Collections.emptyList();
        }

        WeekFields weekFields = WeekFields.ISO;
        Map<String, WeekSummary> summaries = new LinkedHashMap<>();
        LocalDate weekCursor = periodStart.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate lastWeek = periodEnd.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );

        while (!weekCursor.isAfter(lastWeek)) {
            int week = weekCursor.get(weekFields.weekOfWeekBasedYear());
            int year = weekCursor.get(weekFields.weekBasedYear());
            String key = String.format(Locale.GERMANY, "%d-KW%02d", year, week);
            summaries.put(key, new WeekSummary(key, "KW" + week + "/" + year, 0));
            weekCursor = weekCursor.plusWeeks(1);
        }

        for (LocalDate activeDay : activeDays) {
            if (activeDay.isBefore(periodStart) || activeDay.isAfter(periodEnd)) {
                continue;
            }
            int week = activeDay.get(weekFields.weekOfWeekBasedYear());
            int year = activeDay.get(weekFields.weekBasedYear());
            String key = String.format(Locale.GERMANY, "%d-KW%02d", year, week);
            WeekSummary summary = summaries.get(key);
            if (summary != null) {
                summary.sessionDays++;
            }
        }
        return new ArrayList<>(summaries.values());
    }

    static double weeklyAverage(
            int sessionDays,
            LocalDate periodStart,
            LocalDate periodEnd) {
        if (sessionDays <= 0 || periodStart == null || periodEnd == null) {
            return 0;
        }
        long spanDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        double weeks = Math.max(1.0, spanDays / 7.0);
        return sessionDays / weeks;
    }

    static int goalAchievement(double sessionsPerWeek, int weeklyGoal) {
        if (weeklyGoal <= 0) {
            return 0;
        }
        return Math.max(0, Math.round((float) (sessionsPerWeek * 100.0 / weeklyGoal)));
    }

    static Period previousPeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            return new Period(null, null);
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        return new Period(previousEnd.minusDays(days - 1), previousEnd);
    }

    static List<LocalDate> filterDates(
            Set<LocalDate> dates,
            LocalDate start,
            LocalDate end) {
        List<LocalDate> sorted = new ArrayList<>();
        for (LocalDate date : dates) {
            if ((start == null || !date.isBefore(start))
                    && (end == null || !date.isAfter(end))) {
                sorted.add(date);
            }
        }
        Collections.sort(sorted);
        return sorted;
    }

    static final class StreakStats {
        final int currentStreak;
        final int bestStreak;
        final LocalDate bestStart;
        final LocalDate bestEnd;

        StreakStats(
                int currentStreak,
                int bestStreak,
                LocalDate bestStart,
                LocalDate bestEnd) {
            this.currentStreak = currentStreak;
            this.bestStreak = bestStreak;
            this.bestStart = bestStart;
            this.bestEnd = bestEnd;
        }
    }

    static final class WeekSummary {
        final String key;
        final String label;
        int sessionDays;

        WeekSummary(String key, String label, int sessionDays) {
            this.key = key;
            this.label = label;
            this.sessionDays = sessionDays;
        }
    }

    static final class Period {
        final LocalDate start;
        final LocalDate end;

        Period(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
