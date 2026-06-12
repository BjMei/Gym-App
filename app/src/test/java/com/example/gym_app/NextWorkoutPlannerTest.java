package com.example.gym_app;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class NextWorkoutPlannerTest {

    @Test
    public void emptyHistoryStartsWithPush() {
        assertEquals(
                WorkoutStorage.TYPE_PUSH,
                NextWorkoutPlanner.findNextWorkoutType(Collections.emptyList())
        );
    }

    @Test
    public void pushThenLegKeepsSkippedPullNext() {
        assertEquals(
                WorkoutStorage.TYPE_PULL,
                NextWorkoutPlanner.findNextWorkoutType(Arrays.asList(
                        event(WorkoutStorage.TYPE_PUSH, 9),
                        event(WorkoutStorage.TYPE_LEG, 10)
                ))
        );
    }

    @Test
    public void changedOrderSelectsLeastRecentlyTrainedType() {
        assertEquals(
                WorkoutStorage.TYPE_LEG,
                NextWorkoutPlanner.findNextWorkoutType(Arrays.asList(
                        event(WorkoutStorage.TYPE_PUSH, 9),
                        event(WorkoutStorage.TYPE_PULL, 10)
                ))
        );
    }

    @Test
    public void completedRoundStartsAgainWithOldestType() {
        assertEquals(
                WorkoutStorage.TYPE_PUSH,
                NextWorkoutPlanner.findNextWorkoutType(Arrays.asList(
                        event(WorkoutStorage.TYPE_PUSH, 9),
                        event(WorkoutStorage.TYPE_LEG, 10),
                        event(WorkoutStorage.TYPE_PULL, 11)
                ))
        );
    }

    @Test
    public void repeatingUnexpectedWorkoutDoesNotHideSkippedType() {
        assertEquals(
                WorkoutStorage.TYPE_PULL,
                NextWorkoutPlanner.findNextWorkoutType(Arrays.asList(
                        event(WorkoutStorage.TYPE_PUSH, 9),
                        event(WorkoutStorage.TYPE_LEG, 10),
                        event(WorkoutStorage.TYPE_PUSH, 11)
                ))
        );
    }

    @Test
    public void latestEntryPerTypeDeterminesRecommendation() {
        assertEquals(
                WorkoutStorage.TYPE_LEG,
                NextWorkoutPlanner.findNextWorkoutType(Arrays.asList(
                        event(WorkoutStorage.TYPE_PUSH, 1),
                        event(WorkoutStorage.TYPE_LEG, 2),
                        event(WorkoutStorage.TYPE_PULL, 3),
                        event(WorkoutStorage.TYPE_PUSH, 4)
                ))
        );
    }

    private NextWorkoutPlanner.WorkoutEvent event(String type, int day) {
        return new NextWorkoutPlanner.WorkoutEvent(
                type,
                LocalDateTime.of(2026, 6, day, 18, 0)
        );
    }
}
