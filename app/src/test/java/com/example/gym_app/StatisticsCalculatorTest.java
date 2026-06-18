package com.example.gym_app;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StatisticsCalculatorTest {

    @Test
    public void topDescending_returnsHighestValuesFirst() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("A", 10.0);
        values.put("B", 50.0);
        values.put("C", 30.0);

        List<Map.Entry<String, Double>> result =
                StatisticsCalculator.topDescending(values, 2);

        assertEquals("B", result.get(0).getKey());
        assertEquals("C", result.get(1).getKey());
    }

    @Test
    public void pauseStats_distinguishesRestDaysFromSessionGap() {
        StatisticsCalculator.PauseStats result =
                StatisticsCalculator.calculatePauseStats(Arrays.asList(
                        LocalDate.of(2026, 6, 9),
                        LocalDate.of(2026, 6, 12)
                ));

        assertEquals(2.0, result.averageRestDays, 0.001);
        assertEquals(3.0, result.averageGapDays, 0.001);
        assertArrayEquals(new int[]{0, 1, 0, 0}, result.restDayBins);
    }

    @Test
    public void weeklyFrequency_forTotal_usesObservedCalendarSpan() {
        double result = StatisticsCalculator.weeklyFrequency(
                4,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 14),
                null
        );

        assertEquals(2.0, result, 0.001);
    }

    @Test
    public void percentageChange_formatsIncreaseAndNewValue() {
        assertEquals("+50%", StatisticsCalculator.formatChange(15, 10));
        assertEquals("NEU", StatisticsCalculator.formatChange(5, 0));
        assertEquals("0%", StatisticsCalculator.formatChange(0, 0));
    }

    @Test
    public void isoWeekLabel_usesWeekBasedYearAtYearBoundary() {
        assertEquals(
                "KW01/2026",
                StatisticsCalculator.isoWeekLabel(LocalDate.of(2025, 12, 29))
        );
    }

    @Test
    public void sessionVolumes_doNotMergeTwoSessionsOnSameDay() {
        List<StatisticsCalculator.SessionVolume> sessions =
                StatisticsCalculator.aggregateSessionVolumes(Arrays.asList(
                        new StatisticsCalculator.VolumeEntry(
                                "session-a",
                                "18.06.2026",
                                "Push",
                                1000,
                                300
                        ),
                        new StatisticsCalculator.VolumeEntry(
                                "session-b",
                                "18.06.2026",
                                "Push",
                                800,
                                250
                        ),
                        new StatisticsCalculator.VolumeEntry(
                                "session-a",
                                "18.06.2026",
                                "Push",
                                200,
                                150
                        )
                ));

        assertEquals(2, sessions.size());
        assertEquals(1200, sessions.get(0).totalVolume, 0.001);
        assertEquals(300, sessions.get(0).maxSetVolume, 0.001);
        assertEquals(800, sessions.get(1).totalVolume, 0.001);
    }
}
