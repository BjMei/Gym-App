package com.example.gym_app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class TrainingSummaryCalculator {

    private static final String TIMESTAMP_PATTERN = "dd.MM.yyyy HH:mm";

    private TrainingSummaryCalculator() {
    }

    static Summary calculate(
            List<WorkoutStorage.DetailedWorkout> allWorkouts,
            List<WorkoutStorage.CardioSession> allCardio,
            String currentSessionId) {
        String activeSessionId =
                currentSessionId == null ? "" : currentSessionId;
        Summary summary = new Summary();
        Map<String, Double> previousBestByExercise = new HashMap<>();
        Map<String, SessionVolume> previousSessions = new HashMap<>();
        Map<String, Double> currentBestByExercise = new HashMap<>();
        Set<String> currentExerciseNames = new LinkedHashSet<>();

        for (WorkoutStorage.DetailedWorkout workout : allWorkouts) {
            boolean current = activeSessionId.equals(workout.sessionId);
            double workoutVolume = 0d;

            for (WorkoutStorage.WorkoutSet set : workout.sets) {
                double setVolume = set.weight * set.reps;
                double estimatedOneRm = estimateOneRm(set);
                workoutVolume += setVolume;

                if (current) {
                    summary.setCount++;
                    summary.volume += setVolume;
                    currentExerciseNames.add(workout.exercise);
                    currentBestByExercise.merge(
                            normalize(workout.exercise),
                            estimatedOneRm,
                            Math::max
                    );
                    if (summary.bestSet == null
                            || estimatedOneRm > summary.bestSet.estimatedOneRm) {
                        summary.bestSet = new BestSet(
                                workout.exercise,
                                set.weight,
                                set.reps,
                                estimatedOneRm
                        );
                    }
                } else {
                    previousBestByExercise.merge(
                            normalize(workout.exercise),
                            estimatedOneRm,
                            Math::max
                    );
                }
            }

            if (!current && workoutVolume > 0d) {
                String groupKey = sessionGroupKey(workout);
                SessionVolume session = previousSessions.get(groupKey);
                if (session == null) {
                    session = new SessionVolume();
                    previousSessions.put(groupKey, session);
                }
                session.volume += workoutVolume;
                session.timestamp = Math.max(
                        session.timestamp,
                        parseTimestamp(workout.timestamp)
                );
            }
        }

        summary.exerciseCount = currentExerciseNames.size();
        for (Map.Entry<String, Double> currentBest : currentBestByExercise.entrySet()) {
            Double previousBest = previousBestByExercise.get(currentBest.getKey());
            if (previousBest != null && currentBest.getValue() > previousBest + 0.001d) {
                summary.recordExercises.add(findDisplayName(
                        currentExerciseNames,
                        currentBest.getKey()
                ));
            }
        }

        SessionVolume previous = null;
        for (SessionVolume candidate : previousSessions.values()) {
            if (previous == null || candidate.timestamp > previous.timestamp) {
                previous = candidate;
            }
        }
        if (previous != null && previous.volume > 0d) {
            summary.previousVolume = previous.volume;
            summary.hasPreviousVolume = true;
        }

        for (WorkoutStorage.CardioSession cardio : allCardio) {
            if (activeSessionId.equals(cardio.sessionId)) {
                summary.cardioSessions.add(cardio);
                summary.cardioMinutes += cardio.minutes;
            }
        }
        return summary;
    }

    private static double estimateOneRm(WorkoutStorage.WorkoutSet set) {
        return set.weight * (1d + set.reps / 30d);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String findDisplayName(Set<String> names, String normalizedName) {
        for (String name : names) {
            if (normalize(name).equals(normalizedName)) {
                return name;
            }
        }
        return normalizedName;
    }

    private static String sessionGroupKey(WorkoutStorage.DetailedWorkout workout) {
        if (workout.sessionId != null && !workout.sessionId.trim().isEmpty()) {
            return "session:" + workout.sessionId;
        }
        String timestamp = workout.timestamp == null ? "" : workout.timestamp.trim();
        int separator = timestamp.indexOf(' ');
        String date = separator >= 0 ? timestamp.substring(0, separator) : timestamp;
        return "legacy-date:" + date;
    }

    private static long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return 0L;
        }
        try {
            Date parsed = new SimpleDateFormat(
                    TIMESTAMP_PATTERN,
                    Locale.getDefault()
            ).parse(timestamp);
            return parsed == null ? 0L : parsed.getTime();
        } catch (ParseException ignored) {
            return 0L;
        }
    }

    static final class Summary {
        int exerciseCount;
        int setCount;
        int cardioMinutes;
        double volume;
        double previousVolume;
        boolean hasPreviousVolume;
        BestSet bestSet;
        final List<String> recordExercises = new ArrayList<>();
        final List<WorkoutStorage.CardioSession> cardioSessions = new ArrayList<>();
    }

    static final class BestSet {
        final String exercise;
        final double weight;
        final int reps;
        final double estimatedOneRm;

        BestSet(String exercise, double weight, int reps, double estimatedOneRm) {
            this.exercise = exercise;
            this.weight = weight;
            this.reps = reps;
            this.estimatedOneRm = estimatedOneRm;
        }
    }

    private static final class SessionVolume {
        double volume;
        long timestamp;
    }
}
