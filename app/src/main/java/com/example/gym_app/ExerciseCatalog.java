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
        List<String> defaults = getDefaultExercises(context, defaultArrayRes);
        List<String> custom = getCustomExercises(context, workoutType);

        Set<String> merged = new LinkedHashSet<>();
        for (String entry : defaults) {
            if (entry != null && !entry.trim().isEmpty()) {
                merged.add(entry.trim());
            }
        }
        for (String entry : custom) {
            if (entry != null && !entry.trim().isEmpty()) {
                merged.add(entry.trim());
            }
        }

        return new ArrayList<>(merged);
    }

    public static List<String> getDefaultExercises(Context context, int defaultArrayRes) {
        String[] items = context.getResources().getStringArray(defaultArrayRes);
        List<String> defaults = new ArrayList<>();
        for (String item : items) {
            defaults.add(item);
        }
        return defaults;
    }

    public static List<String> getCustomExercises(Context context, String workoutType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(getKey(workoutType), "");
        List<String> result = new ArrayList<>();

        if (stored == null || stored.trim().isEmpty()) {
            return result;
        }

        String[] items = stored.split("\\n");
        for (String item : items) {
            String clean = item.trim();
            if (!clean.isEmpty() && !containsIgnoreCase(result, clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    public static boolean addCustomExercise(Context context, String workoutType, String exerciseName) {
        String clean = normalize(exerciseName);
        if (clean.isEmpty()) {
            return false;
        }

        List<String> custom = getCustomExercises(context, workoutType);
        if (containsIgnoreCase(custom, clean)) {
            return false;
        }

        custom.add(clean);
        saveCustomExercises(context, workoutType, custom);
        return true;
    }

    public static boolean removeCustomExercise(Context context, String workoutType, String exerciseName) {
        List<String> custom = getCustomExercises(context, workoutType);
        int index = indexOfIgnoreCase(custom, exerciseName);
        if (index < 0) {
            return false;
        }

        custom.remove(index);
        saveCustomExercises(context, workoutType, custom);
        return true;
    }

    public static boolean isDefaultExercise(Context context, int defaultArrayRes, String exerciseName) {
        List<String> defaults = getDefaultExercises(context, defaultArrayRes);
        return containsIgnoreCase(defaults, exerciseName);
    }

    public static boolean containsIgnoreCase(List<String> list, String value) {
        return indexOfIgnoreCase(list, value) >= 0;
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

    private static void saveCustomExercises(Context context, String workoutType, List<String> exercises) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < exercises.size(); i++) {
            String clean = normalize(exercises.get(i));
            if (clean.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(clean);
        }
        prefs.edit().putString(getKey(workoutType), builder.toString()).apply();
    }

    private static String getKey(String workoutType) {
        return "custom_" + workoutType;
    }
}
