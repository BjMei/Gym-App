package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutStorage {

    private static final String PREFS_NAME = "WorkoutHistory";
    private static final String PREFS_DETAILED = "WorkoutHistoryDetailed";
    private static final String PREFS_CARDIO = "WorkoutCardio";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_PULL = "pull";
    public static final String TYPE_LEG = "leg";

    // Strukturierte Trainingsdaten speichern
    public static void saveDetailedWorkout(Context context, String type, String exercise, List<WorkoutSet> sets) {
        if (exercise == null || exercise.isEmpty() || sets == null || sets.isEmpty()) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();
        
        // Neues Training als JSON-Objekt erstellen
        JSONObject workout = new JSONObject();
        try {
            workout.put("exercise", exercise);
            workout.put("timestamp", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));
            
            JSONArray setsArray = new JSONArray();
            for (WorkoutSet set : sets) {
                JSONObject setObj = new JSONObject();
                setObj.put("weight", set.weight);
                setObj.put("reps", set.reps);
                setsArray.put(setObj);
            }
            workout.put("sets", setsArray);
            
            newArray.put(workout);
            
            // Alle bisherigen Einträge beibehalten (Langzeitdaten)
            for (int i = 0; i < storedArray.length(); i++) {
                newArray.put(storedArray.getJSONObject(i));
            }
            
            prefs.edit().putString(type, newArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveCardioSession(Context context, String type, String exercise, int minutes) {
        if (exercise == null || exercise.isEmpty() || minutes <= 0) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();

        JSONObject session = new JSONObject();
        try {
            session.put("exercise", exercise);
            session.put("minutes", minutes);
            session.put("timestamp", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));

            newArray.put(session);

            // Alle bisherigen Einträge beibehalten (Langzeitdaten)
            for (int i = 0; i < storedArray.length(); i++) {
                newArray.put(storedArray.getJSONObject(i));
            }

            prefs.edit().putString(type, newArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Letztes Training für eine spezifische Übung abrufen
    public static LastWorkout getLastWorkout(Context context, String type, String exercise) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject workout = array.getJSONObject(i);
                if (workout.getString("exercise").equals(exercise)) {
                    JSONArray setsArray = workout.getJSONArray("sets");
                    List<WorkoutSet> sets = new ArrayList<>();
                    for (int j = 0; j < setsArray.length(); j++) {
                        JSONObject setObj = setsArray.getJSONObject(j);
                        sets.add(new WorkoutSet(setObj.getDouble("weight"), setObj.getInt("reps")));
                    }
                    return new LastWorkout(
                        workout.getString("timestamp"),
                        sets
                    );
                }
            } catch (JSONException ignored) {
            }
        }
        return null;
    }

    // Alle strukturierten Trainings abrufen
    public static List<DetailedWorkout> getDetailedWorkouts(Context context, String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        List<DetailedWorkout> workouts = new ArrayList<>();
        
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject workout = array.getJSONObject(i);
                String exercise = workout.getString("exercise");
                String timestamp = workout.getString("timestamp");
                JSONArray setsArray = workout.getJSONArray("sets");
                List<WorkoutSet> sets = new ArrayList<>();
                
                for (int j = 0; j < setsArray.length(); j++) {
                    JSONObject setObj = setsArray.getJSONObject(j);
                    sets.add(new WorkoutSet(setObj.getDouble("weight"), setObj.getInt("reps")));
                }
                
                workouts.add(new DetailedWorkout(exercise, timestamp, sets));
            } catch (JSONException ignored) {
            }
        }
        return workouts;
    }

    // Cardio-Sessions abrufen
    public static List<CardioSession> getCardioSessions(Context context, String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        List<CardioSession> sessions = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject session = array.getJSONObject(i);
                sessions.add(new CardioSession(
                    session.getString("exercise"),
                    session.getInt("minutes"),
                    session.getString("timestamp")
                ));
            } catch (JSONException ignored) {
            }
        }
        return sessions;
    }

    // Trainings nach Tagen gruppieren
    public static List<DailyWorkout> getDailyWorkouts(Context context, String type) {
        List<DetailedWorkout> allWorkouts = new ArrayList<>();
        List<CardioSession> cardioSessions = new ArrayList<>();

        boolean allTypes = type == null || type.trim().isEmpty();
        if (allTypes) {
            String[] types = new String[]{TYPE_PUSH, TYPE_PULL, TYPE_LEG};
            for (String workoutType : types) {
                allWorkouts.addAll(getDetailedWorkouts(context, workoutType));
                cardioSessions.addAll(getCardioSessions(context, workoutType));
            }
        } else {
            allWorkouts = getDetailedWorkouts(context, type);
            cardioSessions = getCardioSessions(context, type);
        }

        java.util.Map<String, DailyWorkout> dailyMap = new java.util.HashMap<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        for (DetailedWorkout workout : allWorkouts) {
            try {
                String dateOnly = workout.timestamp.split(" ")[0];

                if (!dailyMap.containsKey(dateOnly)) {
                    dailyMap.put(dateOnly, new DailyWorkout(dateOnly, new ArrayList<>(), new ArrayList<>()));
                }

                dailyMap.get(dateOnly).exercises.add(workout);
            } catch (Exception e) {
                // ignore
            }
        }

        for (CardioSession session : cardioSessions) {
            try {
                String dateOnly = session.timestamp.split(" ")[0];
                if (!dailyMap.containsKey(dateOnly)) {
                    dailyMap.put(dateOnly, new DailyWorkout(dateOnly, new ArrayList<>(), new ArrayList<>()));
                }
                dailyMap.get(dateOnly).cardioSessions.add(session);
            } catch (Exception e) {
                // ignore
            }
        }

        List<DailyWorkout> dailyWorkouts = new ArrayList<>(dailyMap.values());
        dailyWorkouts.sort((a, b) -> {
            try {
                java.util.Date dateA = dateFormat.parse(a.date);
                java.util.Date dateB = dateFormat.parse(b.date);
                return dateB.compareTo(dateA);
            } catch (Exception e) {
                return 0;
            }
        });

        return dailyWorkouts;
    }

    // Alte Methode für Rückwärtskompatibilität
    public static void addWorkout(Context context, String type, String summary) {
        if (summary == null || summary.isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();
        newArray.put(summary);

        // Alle bisherigen Einträge beibehalten (Langzeitdaten)
        for (int i = 0; i < storedArray.length(); i++) {
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

    // Alle gespeicherten Daten zurücksetzen
    public static void resetAllData(Context context) {
        // Alle Workout-Daten löschen
        SharedPreferences prefsHistory = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences prefsDetailed = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        SharedPreferences prefsCardio = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        
        prefsHistory.edit().clear().apply();
        prefsDetailed.edit().clear().apply();
        prefsCardio.edit().clear().apply();
        
        // Alle Exercise Settings löschen
        SharedPreferences prefsSettings = context.getSharedPreferences("ExerciseSettings", Context.MODE_PRIVATE);
        prefsSettings.edit().clear().apply();
    }

    // Datenklassen
    public static class WorkoutSet {
        public double weight;
        public int reps;
        
        public WorkoutSet(double weight, int reps) {
            this.weight = weight;
            this.reps = reps;
        }
    }

    public static class LastWorkout {
        public String timestamp;
        public List<WorkoutSet> sets;
        
        public LastWorkout(String timestamp, List<WorkoutSet> sets) {
            this.timestamp = timestamp;
            this.sets = sets;
        }
    }

    public static class DetailedWorkout {
        public String exercise;
        public String timestamp;
        public List<WorkoutSet> sets;
        
        public DetailedWorkout(String exercise, String timestamp, List<WorkoutSet> sets) {
            this.exercise = exercise;
            this.timestamp = timestamp;
            this.sets = sets;
        }
    }

    public static class DailyWorkout {
        public String date;
        public List<DetailedWorkout> exercises;
        public List<CardioSession> cardioSessions;
        
        public DailyWorkout(String date, List<DetailedWorkout> exercises) {
            this(date, exercises, new ArrayList<>());
        }

        public DailyWorkout(String date, List<DetailedWorkout> exercises, List<CardioSession> cardioSessions) {
            this.date = date;
            this.exercises = exercises;
            this.cardioSessions = cardioSessions;
        }
    }

    public static class CardioSession {
        public String exercise;
        public int minutes;
        public String timestamp;

        public CardioSession(String exercise, int minutes, String timestamp) {
            this.exercise = exercise;
            this.minutes = minutes;
            this.timestamp = timestamp;
        }
    }
}

