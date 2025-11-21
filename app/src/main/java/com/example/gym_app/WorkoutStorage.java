package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class WorkoutStorage {

    private static final String PREFS_NAME = "WorkoutHistory";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_PULL = "pull";
    public static final String TYPE_LEG = "leg";
    private static final int MAX_ENTRIES = 10;

    public static void addWorkout(Context context, String type, String summary) {
        if (summary == null || summary.isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();
        newArray.put(summary);

        for (int i = 0; i < storedArray.length() && i < MAX_ENTRIES - 1; i++) {
            try {
                newArray.put(storedArray.getString(i));
            } catch (JSONException ignored) {
            }
        }

        prefs.edit().putString(type, newArray.toString()).apply();
    }

    public static List<String> getWorkouts(Context context, String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        List<String> workouts = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                workouts.add(array.getString(i));
            } catch (JSONException ignored) {
            }
        }
        return workouts;
    }

    private static JSONArray parseArray(String data) {
        try {
            return new JSONArray(data);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}

