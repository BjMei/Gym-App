package com.example.gym_app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class ProfileInsights {

    private ProfileInsights() {
    }

    static SessionRecommendation recommendSessions(
            String activityLevel,
            String experienceLevel,
            int configuredGoal) {
        int min;
        int max;
        if (ProfileRepository.EXPERIENCE_EXPERIENCED.equals(experienceLevel)) {
            min = 4;
            max = 5;
        } else if (ProfileRepository.EXPERIENCE_INTERMEDIATE.equals(experienceLevel)) {
            min = 3;
            max = 4;
        } else {
            min = 2;
            max = 3;
        }

        if (ProfileRepository.ACTIVITY_HIGH.equals(activityLevel)) {
            max = Math.max(min, max - 1);
        } else if (ProfileRepository.ACTIVITY_VERY_HIGH.equals(activityLevel)) {
            min = Math.max(2, min - 1);
            max = Math.max(min, max - 1);
        }

        int difference;
        if (configuredGoal < min) {
            difference = configuredGoal - min;
        } else if (configuredGoal > max) {
            difference = configuredGoal - max;
        } else {
            difference = 0;
        }
        return new SessionRecommendation(min, max, difference);
    }

    static WeightProjection projectWeightGoal(
            double currentWeight,
            double targetWeight,
            LocalDate today,
            LocalDate targetDate,
            LocalDate firstMeasurementDate,
            double firstMeasurementWeight,
            LocalDate latestMeasurementDate,
            double latestMeasurementWeight) {
        double requiredPerWeek = 0;
        long daysRemaining = 0;
        if (currentWeight > 0 && targetWeight > 0
                && today != null && targetDate != null && targetDate.isAfter(today)) {
            daysRemaining = ChronoUnit.DAYS.between(today, targetDate);
            double weeksRemaining = Math.max(1.0 / 7.0, daysRemaining / 7.0);
            requiredPerWeek = (targetWeight - currentWeight) / weeksRemaining;
        }

        double actualPerWeek = 0;
        boolean hasTrend = firstMeasurementDate != null
                && latestMeasurementDate != null
                && latestMeasurementDate.isAfter(firstMeasurementDate)
                && firstMeasurementWeight > 0
                && latestMeasurementWeight > 0;
        if (hasTrend) {
            long trendDays = ChronoUnit.DAYS.between(
                    firstMeasurementDate,
                    latestMeasurementDate
            );
            actualPerWeek = (latestMeasurementWeight - firstMeasurementWeight)
                    * 7.0 / trendDays;
        }

        LocalDate projectedDate = null;
        double remaining = targetWeight - currentWeight;
        if (hasTrend && Math.abs(actualPerWeek) > 0.001
                && remaining * actualPerWeek > 0 && today != null) {
            double weeks = Math.abs(remaining / actualPerWeek);
            projectedDate = today.plusDays(Math.max(1, Math.round(weeks * 7.0)));
        }

        return new WeightProjection(
                requiredPerWeek,
                actualPerWeek,
                daysRemaining,
                hasTrend,
                projectedDate
        );
    }

    static int progressPercent(double current, double target) {
        if (target <= 0 || current <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round(current * 100.0 / target)));
    }

    static final class SessionRecommendation {
        final int min;
        final int max;
        final int difference;

        SessionRecommendation(int min, int max, int difference) {
            this.min = min;
            this.max = max;
            this.difference = difference;
        }
    }

    static final class WeightProjection {
        final double requiredPerWeek;
        final double actualPerWeek;
        final long daysRemaining;
        final boolean hasTrend;
        final LocalDate projectedDate;

        WeightProjection(
                double requiredPerWeek,
                double actualPerWeek,
                long daysRemaining,
                boolean hasTrend,
                LocalDate projectedDate) {
            this.requiredPerWeek = requiredPerWeek;
            this.actualPerWeek = actualPerWeek;
            this.daysRemaining = daysRemaining;
            this.hasTrend = hasTrend;
            this.projectedDate = projectedDate;
        }
    }
}
