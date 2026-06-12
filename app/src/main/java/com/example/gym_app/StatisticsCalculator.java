package com.example.gym_app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class StatisticsCalculator {

    private StatisticsCalculator() {
    }

    static List<Map.Entry<String, Double>> topDescending(
            Map<String, Double> values,
            int limit) {
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(values.entrySet());
        sorted.sort(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()));
        if (sorted.size() > limit) {
            return new ArrayList<>(sorted.subList(0, limit));
        }
        return sorted;
    }

    static PauseStats calculatePauseStats(List<LocalDate> dates) {
        List<LocalDate> sorted = new ArrayList<>(dates);
        sorted.sort(LocalDate::compareTo);

        if (sorted.size() < 2) {
            return new PauseStats(0, 0, new int[4]);
        }

        long totalGapDays = 0;
        long totalRestDays = 0;
        int[] restDayBins = new int[4];

        for (int i = 1; i < sorted.size(); i++) {
            long gapDays = Math.max(0, ChronoUnit.DAYS.between(
                    sorted.get(i - 1),
                    sorted.get(i)
            ));
            long restDays = Math.max(0, gapDays - 1);
            totalGapDays += gapDays;
            totalRestDays += restDays;

            if (restDays <= 1) {
                restDayBins[0]++;
            } else if (restDays <= 3) {
                restDayBins[1]++;
            } else if (restDays <= 6) {
                restDayBins[2]++;
            } else {
                restDayBins[3]++;
            }
        }

        int intervals = sorted.size() - 1;
        return new PauseStats(
                (double) totalRestDays / intervals,
                (double) totalGapDays / intervals,
                restDayBins
        );
    }

    static double weeklyFrequency(
            int sessionCount,
            LocalDate firstActivity,
            LocalDate periodEnd,
            Integer selectedDays) {
        if (sessionCount <= 0) {
            return 0;
        }

        double weeks;
        if (selectedDays != null) {
            weeks = Math.max(1.0, selectedDays / 7.0);
        } else if (firstActivity != null && periodEnd != null) {
            long spanDays = ChronoUnit.DAYS.between(firstActivity, periodEnd) + 1;
            weeks = Math.max(1.0, spanDays / 7.0);
        } else {
            weeks = 1.0;
        }
        return sessionCount / weeks;
    }

    static String formatChange(double current, double previous) {
        if (previous == 0) {
            return current == 0 ? "0%" : "NEU";
        }
        double percentage = ((current - previous) / Math.abs(previous)) * 100.0;
        return String.format(
                java.util.Locale.GERMAN,
                "%+.0f%%",
                percentage
        );
    }

    static final class PauseStats {
        final double averageRestDays;
        final double averageGapDays;
        final int[] restDayBins;

        PauseStats(double averageRestDays, double averageGapDays, int[] restDayBins) {
            this.averageRestDays = averageRestDays;
            this.averageGapDays = averageGapDays;
            this.restDayBins = restDayBins;
        }
    }
}
