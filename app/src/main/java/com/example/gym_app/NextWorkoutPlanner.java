package com.example.gym_app;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class NextWorkoutPlanner {

    private static final String[] ROTATION = {
            WorkoutStorage.TYPE_PUSH,
            WorkoutStorage.TYPE_PULL,
            WorkoutStorage.TYPE_LEG
    };

    private NextWorkoutPlanner() {
    }

    static String findNextWorkoutType(List<WorkoutEvent> events) {
        return findNextWorkoutType(events, Arrays.asList(ROTATION));
    }

    static String findNextWorkoutType(
            List<WorkoutEvent> events,
            List<String> rotation
    ) {
        if (rotation == null || rotation.isEmpty()) {
            return WorkoutStorage.TYPE_PUSH;
        }
        Map<String, LocalDateTime> latestByType = new HashMap<>();

        if (events != null) {
            for (WorkoutEvent event : events) {
                if (event == null
                        || event.timestamp == null
                        || !rotation.contains(event.workoutType)) {
                    continue;
                }

                LocalDateTime currentLatest = latestByType.get(event.workoutType);
                if (currentLatest == null || event.timestamp.isAfter(currentLatest)) {
                    latestByType.put(event.workoutType, event.timestamp);
                }
            }
        }

        String nextType = rotation.get(0);
        LocalDateTime oldestTimestamp = latestByType.get(nextType);

        for (int i = 1; i < rotation.size(); i++) {
            String candidateType = rotation.get(i);
            LocalDateTime candidateTimestamp = latestByType.get(candidateType);

            if (isOlder(candidateTimestamp, oldestTimestamp)) {
                nextType = candidateType;
                oldestTimestamp = candidateTimestamp;
            }
        }

        return nextType;
    }

    private static boolean isOlder(LocalDateTime candidate, LocalDateTime currentOldest) {
        if (candidate == null) {
            return currentOldest != null;
        }
        if (currentOldest == null) {
            return false;
        }
        return candidate.isBefore(currentOldest);
    }

    static final class WorkoutEvent {
        final String workoutType;
        final LocalDateTime timestamp;

        WorkoutEvent(String workoutType, LocalDateTime timestamp) {
            this.workoutType = workoutType;
            this.timestamp = timestamp;
        }
    }
}
