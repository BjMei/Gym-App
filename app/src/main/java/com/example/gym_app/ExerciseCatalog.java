package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExerciseCatalog {

    private static final String PREFS_NAME = "CustomExercises";
    private static final String SETTINGS_PREFS_NAME = "ExerciseSettings";

    private ExerciseCatalog() {
    }

    public static List<String> getExercises(Context context, String workoutType) {
        return getActiveExercises(context, workoutType);
    }

    public static boolean addExercise(Context context, String workoutType, String exerciseName) {
        String clean = normalize(exerciseName);
        if (clean.isEmpty()) {
            return false;
        }

        List<String> exercises = getExercises(context, workoutType);
        if (containsIgnoreCase(exercises, clean)) {
            return false;
        }

        exercises.add(clean);
        saveExercises(context, workoutType, exercises);
        return true;
    }

    public static boolean removeExercise(Context context, String workoutType, String exerciseName) {
        List<String> exercises = getExercises(context, workoutType);
        int index = indexOfIgnoreCase(exercises, exerciseName);
        if (index < 0) {
            return false;
        }

        exercises.remove(index);
        saveExercises(context, workoutType, exercises);
        return true;
    }

    public static boolean containsIgnoreCase(List<String> list, String value) {
        return indexOfIgnoreCase(list, value) >= 0;
    }

    public static String settingsKey(String workoutType, String exerciseName) {
        return normalize(workoutType) + "|" + normalize(exerciseName);
    }

    public static void migrateLegacyExerciseSettings(Context context) {
        SharedPreferences settings =
                context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> storedValues = settings.getAll();
        SharedPreferences.Editor editor = settings.edit();
        boolean changed = false;
        String[] workoutTypes = {
                WorkoutStorage.TYPE_PUSH,
                WorkoutStorage.TYPE_PULL,
                WorkoutStorage.TYPE_LEG
        };

        for (Map.Entry<String, ?> entry : storedValues.entrySet()) {
            String legacyExercise = entry.getKey();
            if (legacyExercise.contains("|") || !(entry.getValue() instanceof String)) {
                continue;
            }

            boolean migrated = false;
            for (String workoutType : workoutTypes) {
                if (!containsIgnoreCase(getExercises(context, workoutType), legacyExercise)) {
                    continue;
                }
                String typedKey = settingsKey(workoutType, legacyExercise);
                if (!storedValues.containsKey(typedKey)) {
                    editor.putString(typedKey, (String) entry.getValue());
                }
                migrated = true;
            }
            if (migrated) {
                editor.remove(legacyExercise);
                changed = true;
            }
        }

        if (changed) {
            editor.apply();
        }
    }

    private static List<String> getActiveExercises(Context context, String workoutType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(getActiveKey(workoutType), "");
        return parseList(stored);
    }

    private static List<String> parseList(String data) {
        List<String> result = new ArrayList<>();
        if (data == null || data.trim().isEmpty()) {
            return result;
        }

        String[] items = data.split("\\n");
        for (String item : items) {
            String clean = normalize(item);
            if (!clean.isEmpty() && !containsIgnoreCase(result, clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    private static int indexOfIgnoreCase(List<String> list, String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < list.size(); i++) {
            if (normalize(list.get(i)).equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static void saveExercises(Context context, String workoutType, List<String> exercises) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder builder = new StringBuilder();
        for (String exercise : exercises) {
            String clean = normalize(exercise);
            if (clean.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(clean);
        }
        prefs.edit().putString(getActiveKey(workoutType), builder.toString()).apply();
    }

    private static String getActiveKey(String workoutType) {
        return "active_" + workoutType;
    }
}
