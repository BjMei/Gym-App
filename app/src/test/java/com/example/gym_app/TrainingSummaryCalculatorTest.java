package com.example.gym_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TrainingSummaryCalculatorTest {

    @Test
    public void calculatesOnlyCurrentSessionAndFindsPreviousVolume() {
        TrainingSummaryCalculator.Summary summary =
                TrainingSummaryCalculator.calculate(
                        Arrays.asList(
                                workout(
                                        "Bankdrücken",
                                        "12.06.2026 18:00",
                                        "current",
                                        set(100d, 5),
                                        set(90d, 8)
                                ),
                                workout(
                                        "Schulterdrücken",
                                        "12.06.2026 18:10",
                                        "current",
                                        set(50d, 10)
                                ),
                                workout(
                                        "Bankdrücken",
                                        "10.06.2026 18:00",
                                        "previous",
                                        set(80d, 5)
                                )
                        ),
                        Collections.singletonList(new WorkoutStorage.CardioSession(
                                "Laufband",
                                12,
                                "12.06.2026 18:20",
                                WorkoutStorage.TYPE_PUSH,
                                "current"
                        )),
                        "current"
                );

        assertEquals(2, summary.exerciseCount);
        assertEquals(3, summary.setCount);
        assertEquals(1_720d, summary.volume, 0.001d);
        assertEquals(400d, summary.previousVolume, 0.001d);
        assertTrue(summary.hasPreviousVolume);
        assertEquals(12, summary.cardioMinutes);
        assertEquals(1, summary.cardioSessions.size());
    }

    @Test
    public void detectsBestSetAndNewRecordAgainstOlderData() {
        TrainingSummaryCalculator.Summary summary =
                TrainingSummaryCalculator.calculate(
                        Arrays.asList(
                                workout(
                                        "Kniebeugen",
                                        "12.06.2026 18:00",
                                        "current",
                                        set(120d, 5)
                                ),
                                workout(
                                        "Kniebeugen",
                                        "08.06.2026 18:00",
                                        "previous",
                                        set(100d, 5)
                                )
                        ),
                        Collections.emptyList(),
                        "current"
                );

        assertNotNull(summary.bestSet);
        assertEquals("Kniebeugen", summary.bestSet.exercise);
        assertEquals(120d, summary.bestSet.weight, 0.001d);
        assertEquals(5, summary.bestSet.reps);
        assertEquals(Collections.singletonList("Kniebeugen"), summary.recordExercises);
    }

    @Test
    public void firstTrackedExerciseIsNotReportedAsNewRecord() {
        TrainingSummaryCalculator.Summary summary =
                TrainingSummaryCalculator.calculate(
                        Collections.singletonList(workout(
                                "Flys",
                                "12.06.2026 18:00",
                                "current",
                                set(20d, 12)
                        )),
                        Collections.emptyList(),
                        "current"
                );

        assertFalse(summary.hasPreviousVolume);
        assertTrue(summary.recordExercises.isEmpty());
    }

    private static WorkoutStorage.DetailedWorkout workout(
            String exercise,
            String timestamp,
            String sessionId,
            WorkoutStorage.WorkoutSet... sets) {
        return new WorkoutStorage.DetailedWorkout(
                exercise,
                timestamp,
                Arrays.asList(sets),
                WorkoutStorage.TYPE_PUSH,
                sessionId
        );
    }

    private static WorkoutStorage.WorkoutSet set(double weight, int reps) {
        return new WorkoutStorage.WorkoutSet(weight, reps);
    }
}
