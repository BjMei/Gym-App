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

    private static final String PREFS_DETAILED = "WorkoutHistoryDetailed";
    private static final String PREFS_CARDIO = "WorkoutCardio";
    private static final String PREFS_SESSIONS = "WorkoutSessions";
    private static final String KEY_SESSIONS = "sessions";
    private static final String TIMESTAMP_PATTERN = "dd.MM.yyyy HH:mm";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_PULL = "pull";
    public static final String TYPE_LEG = "leg";

    // Strukturierte Trainingsdaten speichern
    public static boolean saveDetailedWorkout(Context context, String type, String exercise, List<WorkoutSet> sets) {
        return saveDetailedWorkout(context, type, "", exercise, sets);
    }

    public static boolean saveDetailedWorkout(
            Context context,
            String type,
            String sessionId,
            String exercise,
            List<WorkoutSet> sets) {
        return saveDetailedWorkout(context, type, sessionId, currentTimestamp(), exercise, sets);
    }

    public static boolean saveDetailedWorkout(
            Context context,
            String type,
            String sessionId,
            String sessionTimestamp,
            String exercise,
            List<WorkoutSet> sets) {
        if (exercise == null || exercise.isEmpty() || sets == null || sets.isEmpty()) {
            return false;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();
        
        // Neues Training als JSON-Objekt erstellen
        JSONObject workout = new JSONObject();
        try {
            workout.put("exercise", exercise);
            workout.put("sessionId", sessionId == null ? "" : sessionId);
            workout.put("timestamp", normalizeTimestamp(sessionTimestamp));
            workout.put("completed", isLegacySession(sessionId));
            
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
            
            return prefs.edit().putString(type, newArray.toString()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveCardioSession(Context context, String type, String exercise, int minutes) {
        saveCardioSession(context, type, "", exercise, minutes);
    }

    public static boolean saveCardioSession(
            Context context,
            String type,
            String sessionId,
            String exercise,
            int minutes) {
        return saveCardioSession(context, type, sessionId, currentTimestamp(), exercise, minutes);
    }

    public static boolean saveCardioSession(
            Context context,
            String type,
            String sessionId,
            String sessionTimestamp,
            String exercise,
            int minutes) {
        if (exercise == null || exercise.isEmpty() || minutes <= 0) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(type, "[]"));
        JSONArray newArray = new JSONArray();

        JSONObject session = new JSONObject();
        try {
            session.put("exercise", exercise);
            session.put("minutes", minutes);
            session.put("sessionId", sessionId == null ? "" : sessionId);
            session.put("timestamp", normalizeTimestamp(sessionTimestamp));
            session.put("completed", isLegacySession(sessionId));

            newArray.put(session);

            // Alle bisherigen Einträge beibehalten (Langzeitdaten)
            for (int i = 0; i < storedArray.length(); i++) {
                newArray.put(storedArray.getJSONObject(i));
            }

            return prefs.edit().putString(type, newArray.toString()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveTrainingSession(
            Context context,
            String sessionId,
            String type,
            long durationMs) {
        saveTrainingSession(context, sessionId, type, currentTimestamp(), durationMs);
    }

    public static void saveTrainingSession(
            Context context,
            String sessionId,
            String type,
            String sessionTimestamp,
            long durationMs) {
        if (sessionId == null || sessionId.trim().isEmpty() || durationMs <= 0) {
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(KEY_SESSIONS, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean updated = false;

        try {
            for (int i = 0; i < storedArray.length(); i++) {
                JSONObject stored = storedArray.getJSONObject(i);
                if (sessionId.equals(stored.optString("sessionId"))) {
                    long previousDuration = stored.optLong("durationMs", 0L);
                    stored.put("durationMs", Math.max(previousDuration, durationMs));
                    stored.put("workoutType", type);
                    stored.put("timestamp", normalizeTimestamp(sessionTimestamp));
                    stored.put("completed", true);
                    updated = true;
                }
                updatedArray.put(stored);
            }

            if (!updated) {
                JSONObject session = new JSONObject();
                session.put("sessionId", sessionId);
                session.put("workoutType", type);
                session.put("durationMs", durationMs);
                session.put("completed", true);
                session.put("timestamp", normalizeTimestamp(sessionTimestamp));
                updatedArray.put(session);
            }

            prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).apply();
            markSessionItemsCompleted(context, type, sessionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<TrainingSession> getTrainingSessions(Context context) {
        return getTrainingSessions(context, false);
    }

    public static List<TrainingSession> getAllTrainingSessions(Context context) {
        return getTrainingSessions(context, true);
    }

    private static List<TrainingSession> getTrainingSessions(
            Context context,
            boolean includeIncomplete) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(KEY_SESSIONS, "[]"));
        List<TrainingSession> sessions = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject session = array.getJSONObject(i);
                boolean completed = isCompleted(session);
                if (!includeIncomplete && !completed) {
                    continue;
                }
                sessions.add(new TrainingSession(
                        session.optString("sessionId"),
                        session.optString("workoutType"),
                        session.optString("timestamp"),
                        session.optLong("durationMs", 0L),
                        completed
                ));
            } catch (JSONException ignored) {
            }
        }
        return sessions;
    }

    // Letztes Training für eine spezifische Übung abrufen
    public static LastWorkout getLastWorkout(Context context, String type, String exercise) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject workout = array.getJSONObject(i);
                if (!isCompleted(workout)) {
                    continue;
                }
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
        return getDetailedWorkouts(context, type, false);
    }

    public static List<DetailedWorkout> getAllDetailedWorkouts(Context context, String type) {
        return getDetailedWorkouts(context, type, true);
    }

    private static List<DetailedWorkout> getDetailedWorkouts(
            Context context,
            String type,
            boolean includeIncomplete) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        List<DetailedWorkout> workouts = new ArrayList<>();
        
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject workout = array.getJSONObject(i);
                boolean completed = isCompleted(workout);
                if (!includeIncomplete && !completed) {
                    continue;
                }
                String exercise = workout.getString("exercise");
                String timestamp = workout.getString("timestamp");
                JSONArray setsArray = workout.getJSONArray("sets");
                List<WorkoutSet> sets = new ArrayList<>();
                
                for (int j = 0; j < setsArray.length(); j++) {
                    JSONObject setObj = setsArray.getJSONObject(j);
                    sets.add(new WorkoutSet(setObj.getDouble("weight"), setObj.getInt("reps")));
                }
                
                workouts.add(new DetailedWorkout(
                        exercise,
                        timestamp,
                        sets,
                        type,
                        workout.optString("sessionId", ""),
                        completed
                ));
            } catch (JSONException ignored) {
            }
        }
        return workouts;
    }

    // Cardio-Sessions abrufen
    public static List<CardioSession> getCardioSessions(Context context, String type) {
        return getCardioSessions(context, type, false);
    }

    public static List<CardioSession> getAllCardioSessions(Context context, String type) {
        return getCardioSessions(context, type, true);
    }

    private static List<CardioSession> getCardioSessions(
            Context context,
            String type,
            boolean includeIncomplete) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        JSONArray array = parseArray(prefs.getString(type, "[]"));
        List<CardioSession> sessions = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject session = array.getJSONObject(i);
                boolean completed = isCompleted(session);
                if (!includeIncomplete && !completed) {
                    continue;
                }
                sessions.add(new CardioSession(
                    session.getString("exercise"),
                    session.getInt("minutes"),
                    session.getString("timestamp"),
                    type,
                    session.optString("sessionId", ""),
                    completed
                ));
            } catch (JSONException ignored) {
            }
        }
        return sessions;
    }

    public static long getTrainingSessionDuration(Context context, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return 0L;
        }
        for (TrainingSession session : getTrainingSessions(context)) {
            if (sessionId.equals(session.sessionId)) {
                return session.durationMs;
            }
        }
        return 0L;
    }

    public static void discardIncompleteTrainingData(Context context, String workoutType) {
        if (workoutType == null || workoutType.trim().isEmpty()) {
            return;
        }
        String safeType = workoutType.trim();
        discardIncompleteStoredItems(
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE),
                safeType
        );
        discardIncompleteStoredItems(
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE),
                safeType
        );
        discardIncompleteTrainingSessions(context, safeType);
    }

    public static boolean deleteHistoryWorkout(Context context, String date, String workoutType) {
        return deleteHistoryWorkout(context, date, workoutType, "");
    }

    public static boolean deleteHistoryWorkout(
            Context context,
            String date,
            String workoutType,
            String sessionId) {
        if (date == null || date.trim().isEmpty()
                || workoutType == null || workoutType.trim().isEmpty()) {
            return false;
        }
        String safeDate = date.trim();
        String safeType = workoutType.trim();
        String safeSessionId = sessionId == null ? "" : sessionId.trim();
        if (!safeSessionId.isEmpty()) {
            boolean removedDetailed = deleteStoredItemsForSession(
                    context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE),
                    safeType,
                    safeSessionId
            );
            boolean removedCardio = deleteStoredItemsForSession(
                    context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE),
                    safeType,
                    safeSessionId
            );
            boolean removedSession = deleteTrainingSessionById(context, safeSessionId);
            return removedDetailed || removedCardio || removedSession;
        }
        boolean removedDetailed = deleteStoredItemsForDate(
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE),
                safeType,
                safeDate
        );
        boolean removedCardio = deleteStoredItemsForDate(
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE),
                safeType,
                safeDate
        );
        boolean removedSession = deleteTrainingSessionsForDate(context, safeType, safeDate);
        return removedDetailed || removedCardio || removedSession;
    }

    private static boolean discardIncompleteStoredItems(
            SharedPreferences prefs,
            String key) {
        JSONArray storedArray = parseArray(prefs.getString(key, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject item = storedArray.getJSONObject(i);
                if (!isCompleted(item) && !item.optString("sessionId").trim().isEmpty()) {
                    removed = true;
                    continue;
                }
                updatedArray.put(item);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(key, updatedArray.toString()).commit();
    }

    private static boolean discardIncompleteTrainingSessions(
            Context context,
            String workoutType) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(KEY_SESSIONS, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject session = storedArray.getJSONObject(i);
                if (workoutType.equals(session.optString("workoutType"))
                        && !isCompleted(session)) {
                    removed = true;
                    continue;
                }
                updatedArray.put(session);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).commit();
    }

    private static boolean deleteStoredItemsForSession(
            SharedPreferences prefs,
            String key,
            String sessionId) {
        JSONArray storedArray = parseArray(prefs.getString(key, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject item = storedArray.getJSONObject(i);
                if (sessionId.equals(item.optString("sessionId"))) {
                    removed = true;
                    continue;
                }
                updatedArray.put(item);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(key, updatedArray.toString()).commit();
    }

    private static boolean deleteStoredItemsForDate(
            SharedPreferences prefs,
            String key,
            String date) {
        JSONArray storedArray = parseArray(prefs.getString(key, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject item = storedArray.getJSONObject(i);
                if (date.equals(getDatePart(item.optString("timestamp")))) {
                    removed = true;
                    continue;
                }
                updatedArray.put(item);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(key, updatedArray.toString()).commit();
    }

    private static boolean deleteTrainingSessionsForDate(
            Context context,
            String workoutType,
            String date) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(KEY_SESSIONS, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject session = storedArray.getJSONObject(i);
                if (workoutType.equals(session.optString("workoutType"))
                        && date.equals(getDatePart(session.optString("timestamp")))) {
                    removed = true;
                    continue;
                }
                updatedArray.put(session);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).commit();
    }

    private static boolean deleteTrainingSessionById(Context context, String sessionId) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArray(prefs.getString(KEY_SESSIONS, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean removed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject session = storedArray.getJSONObject(i);
                if (sessionId.equals(session.optString("sessionId"))) {
                    removed = true;
                    continue;
                }
                updatedArray.put(session);
            } catch (JSONException ignored) {
            }
        }

        if (!removed) {
            return false;
        }
        return prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).commit();
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

    // Verlaufseinträge nach einzelner Session gruppieren.
    public static List<DailyWorkout> getHistoryWorkouts(Context context, String type) {
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
            allWorkouts.addAll(getDetailedWorkouts(context, type));
            cardioSessions.addAll(getCardioSessions(context, type));
        }

        java.util.Map<String, DailyWorkout> historyMap = new java.util.HashMap<>();
        for (DetailedWorkout workout : allWorkouts) {
            String dateOnly = getDatePart(workout.timestamp);
            if (dateOnly.isEmpty() || workout.workoutType == null || workout.workoutType.isEmpty()) {
                continue;
            }

            String key = historyKey(workout.sessionId, dateOnly, workout.workoutType);
            DailyWorkout historyEntry = historyMap.get(key);
            if (historyEntry == null) {
                historyEntry = new DailyWorkout(
                        dateOnly,
                        workout.workoutType,
                        workout.sessionId,
                        workout.timestamp,
                        new ArrayList<>(),
                        new ArrayList<>()
                );
                historyMap.put(key, historyEntry);
            }
            historyEntry.exercises.add(workout);
        }

        for (CardioSession session : cardioSessions) {
            String dateOnly = getDatePart(session.timestamp);
            if (dateOnly.isEmpty() || session.workoutType == null || session.workoutType.isEmpty()) {
                continue;
            }

            String key = historyKey(session.sessionId, dateOnly, session.workoutType);
            DailyWorkout historyEntry = historyMap.get(key);
            if (historyEntry == null) {
                historyEntry = new DailyWorkout(
                        dateOnly,
                        session.workoutType,
                        session.sessionId,
                        session.timestamp,
                        new ArrayList<>(),
                        new ArrayList<>()
                );
                historyMap.put(key, historyEntry);
            }
            historyEntry.cardioSessions.add(session);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        List<DailyWorkout> historyWorkouts = new ArrayList<>(historyMap.values());
        historyWorkouts.sort((a, b) -> {
            try {
                Date dateA = dateFormat.parse(a.date);
                Date dateB = dateFormat.parse(b.date);
                int dateComparison = dateB.compareTo(dateA);
                if (dateComparison != 0) {
                    return dateComparison;
                }
            } catch (Exception ignored) {
            }
            int timestampComparison = safe(b.timestamp).compareTo(safe(a.timestamp));
            if (timestampComparison != 0) {
                return timestampComparison;
            }
            return Integer.compare(
                    getWorkoutTypeOrder(a.workoutType),
                    getWorkoutTypeOrder(b.workoutType)
            );
        });
        return historyWorkouts;
    }

    private static String historyKey(String sessionId, String date, String workoutType) {
        String safeSession = sessionId == null ? "" : sessionId.trim();
        if (!safeSession.isEmpty()) {
            return "session|" + safeSession;
        }
        return "legacy|" + date + "|" + workoutType;
    }

    private static String getDatePart(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "";
        }
        return timestamp.trim().split("\\s+")[0];
    }

    private static boolean isCompleted(JSONObject object) {
        return !object.has("completed") || object.optBoolean("completed", false);
    }

    private static boolean isLegacySession(String sessionId) {
        return sessionId == null || sessionId.trim().isEmpty();
    }

    private static String currentTimestamp() {
        return new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.getDefault()).format(new Date());
    }

    private static String normalizeTimestamp(String timestamp) {
        return timestamp == null || timestamp.trim().isEmpty()
                ? currentTimestamp()
                : timestamp.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void markSessionItemsCompleted(
            Context context,
            String workoutType,
            String sessionId) {
        markStoredItemsCompleted(
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE),
                workoutType,
                sessionId
        );
        markStoredItemsCompleted(
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE),
                workoutType,
                sessionId
        );
    }

    private static boolean markStoredItemsCompleted(
            SharedPreferences prefs,
            String key,
            String sessionId) {
        JSONArray storedArray = parseArray(prefs.getString(key, "[]"));
        JSONArray updatedArray = new JSONArray();
        boolean changed = false;

        for (int i = 0; i < storedArray.length(); i++) {
            try {
                JSONObject item = storedArray.getJSONObject(i);
                if (sessionId.equals(item.optString("sessionId"))
                        && !item.optBoolean("completed", false)) {
                    item.put("completed", true);
                    changed = true;
                }
                updatedArray.put(item);
            } catch (JSONException ignored) {
            }
        }

        return changed && prefs.edit().putString(key, updatedArray.toString()).commit();
    }

    private static int getWorkoutTypeOrder(String workoutType) {
        if (TYPE_PUSH.equals(workoutType)) {
            return 0;
        }
        if (TYPE_PULL.equals(workoutType)) {
            return 1;
        }
        if (TYPE_LEG.equals(workoutType)) {
            return 2;
        }
        return 3;
    }

    private static JSONArray parseArray(String data) {
        try {
            return new JSONArray(data);
        } catch (JSONException e) {
            return new JSONArray();
        }
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
        public String workoutType;
        public String sessionId;
        public boolean completed;

        public DetailedWorkout(String exercise, String timestamp, List<WorkoutSet> sets) {
            this(exercise, timestamp, sets, "");
        }

        public DetailedWorkout(String exercise, String timestamp, List<WorkoutSet> sets, String workoutType) {
            this(exercise, timestamp, sets, workoutType, "");
        }

        public DetailedWorkout(
                String exercise,
                String timestamp,
                List<WorkoutSet> sets,
                String workoutType,
                String sessionId) {
            this(exercise, timestamp, sets, workoutType, sessionId, true);
        }

        public DetailedWorkout(
                String exercise,
                String timestamp,
                List<WorkoutSet> sets,
                String workoutType,
                String sessionId,
                boolean completed) {
            this.exercise = exercise;
            this.timestamp = timestamp;
            this.sets = sets;
            this.workoutType = workoutType;
            this.sessionId = sessionId;
            this.completed = completed;
        }
    }

    public static class DailyWorkout {
        public String date;
        public String workoutType;
        public String sessionId;
        public String timestamp;
        public List<DetailedWorkout> exercises;
        public List<CardioSession> cardioSessions;
        
        public DailyWorkout(String date, List<DetailedWorkout> exercises) {
            this(date, "", exercises, new ArrayList<>());
        }

        public DailyWorkout(String date, List<DetailedWorkout> exercises, List<CardioSession> cardioSessions) {
            this(date, "", exercises, cardioSessions);
        }

        public DailyWorkout(
                String date,
                String workoutType,
                List<DetailedWorkout> exercises,
                List<CardioSession> cardioSessions) {
            this(date, workoutType, "", "", exercises, cardioSessions);
        }

        public DailyWorkout(
                String date,
                String workoutType,
                String sessionId,
                String timestamp,
                List<DetailedWorkout> exercises,
                List<CardioSession> cardioSessions) {
            this.date = date;
            this.workoutType = workoutType;
            this.sessionId = sessionId == null ? "" : sessionId;
            this.timestamp = timestamp == null ? "" : timestamp;
            this.exercises = exercises;
            this.cardioSessions = cardioSessions;
        }
    }

    public static class CardioSession {
        public String exercise;
        public int minutes;
        public String timestamp;
        public String workoutType;
        public String sessionId;
        public boolean completed;

        public CardioSession(String exercise, int minutes, String timestamp) {
            this(exercise, minutes, timestamp, "", "");
        }

        public CardioSession(String exercise, int minutes, String timestamp, String workoutType) {
            this(exercise, minutes, timestamp, workoutType, "");
        }

        public CardioSession(
                String exercise,
                int minutes,
                String timestamp,
                String workoutType,
                String sessionId) {
            this(exercise, minutes, timestamp, workoutType, sessionId, true);
        }

        public CardioSession(
                String exercise,
                int minutes,
                String timestamp,
                String workoutType,
                String sessionId,
                boolean completed) {
            this.exercise = exercise;
            this.minutes = minutes;
            this.timestamp = timestamp;
            this.workoutType = workoutType;
            this.sessionId = sessionId;
            this.completed = completed;
        }
    }

    public static class TrainingSession {
        public String sessionId;
        public String workoutType;
        public String timestamp;
        public long durationMs;
        public boolean completed;

        public TrainingSession(
                String sessionId,
                String workoutType,
                String timestamp,
                long durationMs,
                boolean completed) {
            this.sessionId = sessionId;
            this.workoutType = workoutType;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.completed = completed;
        }
    }
}

