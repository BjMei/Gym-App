package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExerciseCatalog {

    private static final String PREFS_NAME = "CustomExercises";

    private ExerciseCatalog() {
    }

    public static List<String> getExercises(Context context, int defaultArrayRes, String workoutType) {
        List<String> active = getActiveExercises(context, workoutType);
        if (!active.isEmpty()) {
            return active;
        }

        List<String> defaults = getDefaultExercises(context, defaultArrayRes);
        List<String> legacyCustom = getLegacyCustomExercises(context, workoutType);

        Set<String> merged = new LinkedHashSet<>();
        for (String entry : defaults) {
            String clean = normalize(entry);
            if (!clean.isEmpty()) {
                merged.add(clean);
            }
        }
        for (String entry : legacyCustom) {
            String clean = normalize(entry);
            if (!clean.isEmpty()) {
                merged.add(clean);
            }
        }

        List<String> initial = new ArrayList<>(merged);
        saveExercises(context, workoutType, initial);
        return initial;
    }

    public static List<String> getDefaultExercises(Context context, int defaultArrayRes) {
        String[] items = context.getResources().getStringArray(defaultArrayRes);
        List<String> defaults = new ArrayList<>();
        for (String item : items) {
            String clean = normalize(item);
            if (!clean.isEmpty()) {
                defaults.add(clean);
            }
        }
        return defaults;
    }

    public static boolean addExercise(Context context, int defaultArrayRes, String workoutType, String exerciseName) {
        String clean = normalize(exerciseName);
        if (clean.isEmpty()) {
            return false;
        }

        List<String> exercises = getExercises(context, defaultArrayRes, workoutType);
        if (containsIgnoreCase(exercises, clean)) {
            return false;
        }

        exercises.add(clean);
        saveExercises(context, workoutType, exercises);
        return true;
    }

    public static boolean removeExercise(Context context, int defaultArrayRes, String workoutType, String exerciseName) {
        List<String> exercises = getExercises(context, defaultArrayRes, workoutType);
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

    public static List<String> getLegacyCustomExercises(Context context, String workoutType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(getLegacyCustomKey(workoutType), "");
        return parseList(stored);
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

    private static String getLegacyCustomKey(String workoutType) {
        return "custom_" + workoutType;
    }
}
