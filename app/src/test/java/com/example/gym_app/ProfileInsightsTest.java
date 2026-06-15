package com.example.gym_app;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileInsightsTest {

    @Test
    public void veryActiveBeginnerGetsRecoveryAwareRange() {
        ProfileInsights.SessionRecommendation recommendation =
                ProfileInsights.recommendSessions(
                        ProfileRepository.ACTIVITY_VERY_HIGH,
                        ProfileRepository.EXPERIENCE_BEGINNER,
                        5
                );

        assertEquals(2, recommendation.min);
        assertEquals(2, recommendation.max);
        assertEquals(3, recommendation.difference);
    }

    @Test
    public void weightProjectionCalculatesRequiredAndActualWeeklyRate() {
        ProfileInsights.WeightProjection projection =
                ProfileInsights.projectWeightGoal(
                        80,
                        76,
                        LocalDate.of(2026, 6, 15),
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 6, 1),
                        82,
                        LocalDate.of(2026, 6, 15),
                        80
                );

        assertEquals(-0.5, projection.requiredPerWeek, 0.01);
        assertEquals(-1.0, projection.actualPerWeek, 0.01);
        assertTrue(projection.hasTrend);
        assertEquals(LocalDate.of(2026, 7, 13), projection.projectedDate);
    }

    @Test
    public void progressPercentIsCapped() {
        assertEquals(100, ProfileInsights.progressPercent(130, 120));
        assertEquals(50, ProfileInsights.progressPercent(60, 120));
    }
}
