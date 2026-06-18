package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WorkoutStorage {

    private static final String TAG = "WorkoutStorage";
    private static final String PREFS_DETAILED = "WorkoutHistoryDetailed";
    private static final String PREFS_CARDIO = "WorkoutCardio";
    private static final String PREFS_SESSIONS = "WorkoutSessions";
    private static final String PREFS_TRANSACTIONS = "WorkoutStorageTransactions";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_PENDING_TRANSACTION = "pendingTransaction";
    private static final String TIMESTAMP_PATTERN = "dd.MM.yyyy HH:mm";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_PULL = "pull";
    public static final String TYPE_LEG = "leg";

    public static boolean saveDetailedWorkouts(
            Context context,
            String type,
            String sessionId,
            String sessionTimestamp,
            List<WorkoutExercise> exercises) {
        if (!isKnownWorkoutType(context, type)
                || sessionId == null
                || sessionId.trim().isEmpty()
                || exercises == null
                || exercises.isEmpty()) {
            return false;
        }
        for (WorkoutExercise exercise : exercises) {
            if (exercise == null
                    || exercise.exercise.trim().isEmpty()
                    || exercise.sets == null
                    || exercise.sets.isEmpty()) {
                return false;
            }
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArrayForWrite(
                context,
                PREFS_DETAILED + "/" + type,
                prefs.getString(type, "[]")
        );
        if (storedArray == null) {
            return false;
        }
        JSONArray newArray = new JSONArray();
        
        try {
            for (WorkoutExercise exercise : exercises) {
                JSONObject workout = new JSONObject();
                workout.put("exercise", exercise.exercise);
                workout.put("sessionId", sessionId);
                workout.put("timestamp", normalizeTimestamp(sessionTimestamp));
                workout.put("completed", false);

                JSONArray setsArray = new JSONArray();
                for (WorkoutSet set : exercise.sets) {
                    JSONObject setObject = new JSONObject();
                    setObject.put("weight", set.weight);
                    setObject.put("reps", set.reps);
                    setsArray.put(setObject);
                }
                workout.put("sets", setsArray);
                newArray.put(workout);
            }
            
            // Alle bisherigen Einträge beibehalten (Langzeitdaten)
            for (int i = 0; i < storedArray.length(); i++) {
                newArray.put(storedArray.getJSONObject(i));
            }
            
            return prefs.edit().putString(type, newArray.toString()).commit();
        } catch (JSONException e) {
            Log.e(TAG, "Krafttraining konnte nicht gespeichert werden.", e);
            return false;
        }
    }

    public static boolean saveCardioSession(
            Context context,
            String type,
            String sessionId,
            String sessionTimestamp,
            String exercise,
            int minutes) {
        if (!isKnownWorkoutType(context, type)
                || sessionId == null
                || sessionId.trim().isEmpty()
                || exercise == null
                || exercise.trim().isEmpty()
                || minutes <= 0) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArrayForWrite(
                context,
                PREFS_CARDIO + "/" + type,
                prefs.getString(type, "[]")
        );
        if (storedArray == null) {
            return false;
        }
        JSONArray newArray = new JSONArray();

        JSONObject session = new JSONObject();
        try {
            session.put("exercise", exercise);
            session.put("minutes", minutes);
            session.put("sessionId", sessionId == null ? "" : sessionId);
            session.put("timestamp", normalizeTimestamp(sessionTimestamp));
            session.put("completed", false);

            newArray.put(session);

            // Alle bisherigen Einträge beibehalten (Langzeitdaten)
            for (int i = 0; i < storedArray.length(); i++) {
                newArray.put(storedArray.getJSONObject(i));
            }

            return prefs.edit().putString(type, newArray.toString()).commit();
        } catch (JSONException e) {
            Log.e(TAG, "Cardio-Eintrag konnte nicht gespeichert werden.", e);
            return false;
        }
    }

    public static TrainingSession getOrCreateActiveTrainingSession(
            Context context,
            String workoutType) {
        if (!isKnownWorkoutType(context, workoutType)) {
            return null;
        }
        TrainingSession active = findActiveTrainingSession(context, workoutType);
        if (active != null) {
            return active;
        }

        String sessionId = UUID.randomUUID().toString();
        String timestamp = currentTimestamp();
        long startedAtEpochMs = System.currentTimeMillis();
        TrainingSession created = new TrainingSession(
                sessionId,
                workoutType,
                timestamp,
                0L,
                false,
                startedAtEpochMs
        );
        return persistTrainingSession(context, created) ? created : null;
    }

    public static boolean saveTrainingSession(
            Context context,
            String sessionId,
            String type,
            String sessionTimestamp,
            long durationMs) {
        if (!isKnownWorkoutType(context, type)
                || sessionId == null
                || sessionId.trim().isEmpty()
                || durationMs <= 0) {
            return false;
        }
        if (!hasTrainingSessionItems(context, type, sessionId)) {
            return false;
        }

        SharedPreferences detailedPrefs =
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        SharedPreferences cardioPrefs =
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        SharedPreferences sessionPrefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        String detailedOriginal = detailedPrefs.getString(type, "[]");
        String cardioOriginal = cardioPrefs.getString(type, "[]");
        String sessionsOriginal = sessionPrefs.getString(KEY_SESSIONS, "[]");

        JSONArray detailed = parseArrayForWrite(
                context,
                PREFS_DETAILED + "/" + type,
                detailedOriginal
        );
        JSONArray cardio = parseArrayForWrite(
                context,
                PREFS_CARDIO + "/" + type,
                cardioOriginal
        );
        JSONArray sessions = parseArrayForWrite(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                sessionsOriginal
        );
        if (detailed == null || cardio == null || sessions == null) {
            return false;
        }

        try {
            String detailedUpdated =
                    markSessionItemsCompleted(detailed, sessionId).toString();
            String cardioUpdated =
                    markSessionItemsCompleted(cardio, sessionId).toString();
            String sessionsUpdated = completeTrainingSession(
                    sessions,
                    sessionId,
                    type,
                    sessionTimestamp,
                    durationMs
            ).toString();

            return commitWorkoutTransaction(
                    context,
                    type,
                    detailedOriginal,
                    detailedUpdated,
                    cardioOriginal,
                    cardioUpdated,
                    sessionsOriginal,
                    sessionsUpdated
            );
        } catch (JSONException e) {
            Log.e(TAG, "Trainingssession konnte nicht abgeschlossen werden.", e);
            return false;
        }
    }

    public static boolean updateActiveTrainingSessionDuration(
            Context context,
            String sessionId,
            long durationMs) {
        if (sessionId == null || sessionId.trim().isEmpty() || durationMs < 0L) {
            return false;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArrayForWrite(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                prefs.getString(KEY_SESSIONS, "[]")
        );
        if (storedArray == null) {
            return false;
        }

        JSONArray updatedArray = new JSONArray();
        boolean found = false;
        boolean changed = false;
        try {
            for (int i = 0; i < storedArray.length(); i++) {
                JSONObject stored = storedArray.getJSONObject(i);
                if (sessionId.equals(stored.optString("sessionId"))) {
                    found = true;
                    if (!isCompleted(stored)) {
                        long storedDuration = stored.optLong("durationMs", 0L);
                        long updatedDuration = Math.max(storedDuration, durationMs);
                        if (updatedDuration != storedDuration) {
                            stored.put("durationMs", updatedDuration);
                            changed = true;
                        }
                    }
                }
                updatedArray.put(stored);
            }
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(
                    PREFS_SESSIONS + "/" + KEY_SESSIONS,
                    exception
            );
            return false;
        }

        if (!found || !changed) {
            return found;
        }
        return prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).commit();
    }

    public static List<TrainingSession> getTrainingSessions(Context context) {
        return getTrainingSessions(context, false);
    }

    private static List<TrainingSession> getTrainingSessions(
            Context context,
            boolean includeIncomplete) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray array = parseArray(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                prefs.getString(KEY_SESSIONS, "[]")
        );
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
                        completed,
                        session.optLong(
                                "startedAtEpochMs",
                                parseTimestampEpoch(session.optString("timestamp"))
                        )
                ));
            } catch (JSONException exception) {
                AppDataCorruptionTracker.record(
                        PREFS_SESSIONS + "/" + KEY_SESSIONS,
                        exception
                );
            }
        }
        return sessions;
    }

    // Letztes Training für eine spezifische Übung abrufen
    public static LastWorkout getLastWorkout(Context context, String type, String exercise) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        JSONArray array = parseArray(
                context,
                PREFS_DETAILED + "/" + type,
                prefs.getString(type, "[]")
        );
        
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
            } catch (JSONException exception) {
                AppDataCorruptionTracker.record(PREFS_DETAILED + "/" + type, exception);
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
        JSONArray array = parseArray(
                context,
                PREFS_DETAILED + "/" + type,
                prefs.getString(type, "[]")
        );
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
            } catch (JSONException exception) {
                AppDataCorruptionTracker.record(PREFS_DETAILED + "/" + type, exception);
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
        JSONArray array = parseArray(
                context,
                PREFS_CARDIO + "/" + type,
                prefs.getString(type, "[]")
        );
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
            } catch (JSONException exception) {
                AppDataCorruptionTracker.record(PREFS_CARDIO + "/" + type, exception);
            }
        }
        return sessions;
    }

    public static long getTrainingSessionDuration(Context context, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return 0L;
        }
        for (TrainingSession session : getTrainingSessions(context, true)) {
            if (sessionId.equals(session.sessionId)) {
                return session.durationMs;
            }
        }
        return 0L;
    }

    public static boolean hasTrainingSessionItems(
            Context context,
            String workoutType,
            String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        if (!recoverPendingTransaction(context)) {
            return false;
        }
        return hasStoredItemsForSession(
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE),
                workoutType,
                sessionId
        ) || hasStoredItemsForSession(
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE),
                workoutType,
                sessionId
        );
    }

    public static boolean discardTrainingSession(
            Context context,
            String workoutType,
            String sessionId) {
        if (workoutType == null || workoutType.trim().isEmpty()
                || sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        String safeType = workoutType.trim();
        String safeSessionId = sessionId.trim();
        SharedPreferences detailedPrefs =
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        SharedPreferences cardioPrefs =
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        SharedPreferences sessionPrefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        String detailedOriginal = detailedPrefs.getString(safeType, "[]");
        String cardioOriginal = cardioPrefs.getString(safeType, "[]");
        String sessionsOriginal = sessionPrefs.getString(KEY_SESSIONS, "[]");
        JSONArray detailed = parseArrayForWrite(
                context,
                PREFS_DETAILED + "/" + safeType,
                detailedOriginal
        );
        JSONArray cardio = parseArrayForWrite(
                context,
                PREFS_CARDIO + "/" + safeType,
                cardioOriginal
        );
        JSONArray sessions = parseArrayForWrite(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                sessionsOriginal
        );
        if (detailed == null || cardio == null || sessions == null) {
            return false;
        }

        JSONArray detailedUpdated = removeSessionItems(detailed, safeSessionId);
        JSONArray cardioUpdated = removeSessionItems(cardio, safeSessionId);
        JSONArray sessionsUpdated = removeSessionItems(sessions, safeSessionId);
        boolean removed = detailedUpdated.length() != detailed.length()
                || cardioUpdated.length() != cardio.length()
                || sessionsUpdated.length() != sessions.length();
        if (!removed) {
            return false;
        }

        return commitWorkoutTransaction(
                context,
                safeType,
                detailedOriginal,
                detailedUpdated.toString(),
                cardioOriginal,
                cardioUpdated.toString(),
                sessionsOriginal,
                sessionsUpdated.toString()
        );
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
        SharedPreferences detailedPrefs =
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        SharedPreferences cardioPrefs =
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        SharedPreferences sessionPrefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        String detailedOriginal = detailedPrefs.getString(safeType, "[]");
        String cardioOriginal = cardioPrefs.getString(safeType, "[]");
        String sessionsOriginal = sessionPrefs.getString(KEY_SESSIONS, "[]");

        JSONArray detailed = parseArrayForWrite(
                context,
                PREFS_DETAILED + "/" + safeType,
                detailedOriginal
        );
        JSONArray cardio = parseArrayForWrite(
                context,
                PREFS_CARDIO + "/" + safeType,
                cardioOriginal
        );
        JSONArray sessions = parseArrayForWrite(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                sessionsOriginal
        );
        if (detailed == null || cardio == null || sessions == null) {
            return false;
        }

        JSONArray detailedUpdated = safeSessionId.isEmpty()
                ? removeItemsForDate(detailed, safeDate)
                : removeSessionItems(detailed, safeSessionId);
        JSONArray cardioUpdated = safeSessionId.isEmpty()
                ? removeItemsForDate(cardio, safeDate)
                : removeSessionItems(cardio, safeSessionId);
        JSONArray sessionsUpdated = safeSessionId.isEmpty()
                ? removeSessionsForDate(sessions, safeType, safeDate)
                : removeSessionItems(sessions, safeSessionId);
        boolean removed = detailedUpdated.length() != detailed.length()
                || cardioUpdated.length() != cardio.length()
                || sessionsUpdated.length() != sessions.length();
        if (!removed) {
            return false;
        }

        return commitWorkoutTransaction(
                context,
                safeType,
                detailedOriginal,
                detailedUpdated.toString(),
                cardioOriginal,
                cardioUpdated.toString(),
                sessionsOriginal,
                sessionsUpdated.toString()
        );
    }

    private static JSONArray removeItemsForDate(JSONArray storedArray, String date) {
        JSONArray updatedArray = new JSONArray();
        for (int i = 0; i < storedArray.length(); i++) {
            JSONObject item = storedArray.optJSONObject(i);
            if (item == null || date.equals(getDatePart(item.optString("timestamp")))) {
                continue;
            }
            updatedArray.put(item);
        }
        return updatedArray;
    }

    private static JSONArray removeSessionsForDate(
            JSONArray storedArray,
            String workoutType,
            String date) {
        JSONArray updatedArray = new JSONArray();
        for (int i = 0; i < storedArray.length(); i++) {
            JSONObject session = storedArray.optJSONObject(i);
            if (session == null
                    || (workoutType.equals(session.optString("workoutType"))
                    && date.equals(getDatePart(session.optString("timestamp"))))) {
                continue;
            }
            updatedArray.put(session);
        }
        return updatedArray;
    }

    // Trainings nach Tagen gruppieren
    public static List<DailyWorkout> getDailyWorkouts(Context context, String type) {
        List<DetailedWorkout> allWorkouts = new ArrayList<>();
        List<CardioSession> cardioSessions = new ArrayList<>();

        boolean allTypes = type == null || type.trim().isEmpty();
        if (allTypes) {
            for (String workoutType : WorkoutTypeRepository.getAllTypeIds(context)) {
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
            for (String workoutType : WorkoutTypeRepository.getAllTypeIds(context)) {
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

    private static boolean isKnownWorkoutType(Context context, String workoutType) {
        return WorkoutTypeRepository.isKnownType(context, workoutType);
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

    private static JSONArray markSessionItemsCompleted(
            JSONArray storedArray,
            String sessionId) throws JSONException {
        JSONArray updatedArray = new JSONArray();

        for (int i = 0; i < storedArray.length(); i++) {
            JSONObject item = storedArray.getJSONObject(i);
            if (sessionId.equals(item.optString("sessionId"))) {
                item.put("completed", true);
            }
            updatedArray.put(item);
        }
        return updatedArray;
    }

    private static JSONArray removeSessionItems(
            JSONArray storedArray,
            String sessionId) {
        JSONArray updatedArray = new JSONArray();
        for (int i = 0; i < storedArray.length(); i++) {
            JSONObject item = storedArray.optJSONObject(i);
            if (item == null || sessionId.equals(item.optString("sessionId"))) {
                continue;
            }
            updatedArray.put(item);
        }
        return updatedArray;
    }

    private static JSONArray completeTrainingSession(
            JSONArray storedArray,
            String sessionId,
            String workoutType,
            String sessionTimestamp,
            long durationMs) throws JSONException {
        JSONArray updatedArray = new JSONArray();
        boolean updated = false;

        for (int i = 0; i < storedArray.length(); i++) {
            JSONObject stored = storedArray.getJSONObject(i);
            if (sessionId.equals(stored.optString("sessionId"))) {
                long previousDuration = stored.optLong("durationMs", 0L);
                stored.put("durationMs", Math.max(previousDuration, durationMs));
                stored.put("workoutType", workoutType);
                stored.put("timestamp", normalizeTimestamp(sessionTimestamp));
                stored.put("completed", true);
                if (stored.optLong("startedAtEpochMs", 0L) <= 0L) {
                    stored.put(
                            "startedAtEpochMs",
                            parseTimestampEpoch(sessionTimestamp)
                    );
                }
                updated = true;
            }
            updatedArray.put(stored);
        }

        if (!updated) {
            JSONObject session = new JSONObject();
            session.put("sessionId", sessionId);
            session.put("workoutType", workoutType);
            session.put("durationMs", durationMs);
            session.put("completed", true);
            session.put("timestamp", normalizeTimestamp(sessionTimestamp));
            session.put("startedAtEpochMs", parseTimestampEpoch(sessionTimestamp));
            updatedArray.put(session);
        }
        return updatedArray;
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

    private static JSONArray parseArray(
            Context context,
            String source,
            String data) {
        if (context != null && !recoverPendingTransaction(context)) {
            return new JSONArray();
        }
        try {
            JSONArray array = new JSONArray(data);
            validateObjectArray(array);
            return array;
        } catch (JSONException e) {
            AppDataCorruptionTracker.record(source, e);
            return new JSONArray();
        }
    }

    private static JSONArray parseArray(String data) {
        return parseArray(null, "WorkoutStorage", data);
    }

    private static JSONArray parseArrayForWrite(
            Context context,
            String source,
            String data) {
        if (context != null && !recoverPendingTransaction(context)) {
            return null;
        }
        try {
            JSONArray array = new JSONArray(data);
            validateObjectArray(array);
            return array;
        } catch (JSONException e) {
            AppDataCorruptionTracker.record(source, e);
            return null;
        }
    }

    private static JSONArray parseArrayForWrite(String data) {
        return parseArrayForWrite(null, "WorkoutStorage", data);
    }

    private static void validateObjectArray(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONObject(i) == null) {
                throw new JSONException("Eintrag " + i + " ist kein JSON-Objekt.");
            }
        }
    }

    private static TrainingSession findActiveTrainingSession(
            Context context,
            String workoutType) {
        TrainingSession latest = null;
        for (TrainingSession session : getTrainingSessions(context, true)) {
            if (session.completed || !safe(workoutType).equals(session.workoutType)) {
                continue;
            }
            if (latest == null || session.startedAtEpochMs > latest.startedAtEpochMs) {
                latest = session;
            }
        }
        if (latest != null) {
            return latest;
        }

        String fallbackSessionId = "";
        String fallbackTimestamp = "";
        long fallbackStartedAt = 0L;
        for (DetailedWorkout workout : getAllDetailedWorkouts(context, workoutType)) {
            if (workout.completed || safe(workout.sessionId).isEmpty()) {
                continue;
            }
            long startedAt = parseTimestampEpoch(workout.timestamp);
            if (fallbackSessionId.isEmpty() || startedAt > fallbackStartedAt) {
                fallbackSessionId = workout.sessionId;
                fallbackTimestamp = workout.timestamp;
                fallbackStartedAt = startedAt;
            }
        }
        for (CardioSession cardio : getAllCardioSessions(context, workoutType)) {
            if (cardio.completed || safe(cardio.sessionId).isEmpty()) {
                continue;
            }
            long startedAt = parseTimestampEpoch(cardio.timestamp);
            if (fallbackSessionId.isEmpty() || startedAt > fallbackStartedAt) {
                fallbackSessionId = cardio.sessionId;
                fallbackTimestamp = cardio.timestamp;
                fallbackStartedAt = startedAt;
            }
        }
        if (fallbackSessionId.isEmpty()) {
            return null;
        }

        TrainingSession recovered = new TrainingSession(
                fallbackSessionId,
                workoutType,
                fallbackTimestamp,
                0L,
                false,
                fallbackStartedAt
        );
        persistTrainingSession(context, recovered);
        return recovered;
    }

    private static boolean persistTrainingSession(
            Context context,
            TrainingSession trainingSession) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        JSONArray storedArray = parseArrayForWrite(
                context,
                PREFS_SESSIONS + "/" + KEY_SESSIONS,
                prefs.getString(KEY_SESSIONS, "[]")
        );
        if (storedArray == null) {
            return false;
        }
        JSONArray updatedArray = new JSONArray();
        boolean replaced = false;
        try {
            for (int i = 0; i < storedArray.length(); i++) {
                JSONObject stored = storedArray.getJSONObject(i);
                if (trainingSession.sessionId.equals(stored.optString("sessionId"))) {
                    updatedArray.put(trainingSessionToJson(trainingSession));
                    replaced = true;
                } else {
                    updatedArray.put(stored);
                }
            }
            if (!replaced) {
                updatedArray.put(trainingSessionToJson(trainingSession));
            }
            return prefs.edit().putString(KEY_SESSIONS, updatedArray.toString()).commit();
        } catch (JSONException e) {
            Log.e(TAG, "Aktive Trainingssession konnte nicht gespeichert werden.", e);
            return false;
        }
    }

    private static JSONObject trainingSessionToJson(TrainingSession session)
            throws JSONException {
        JSONObject object = new JSONObject();
        object.put("sessionId", session.sessionId);
        object.put("workoutType", session.workoutType);
        object.put("timestamp", normalizeTimestamp(session.timestamp));
        object.put("durationMs", session.durationMs);
        object.put("completed", session.completed);
        object.put("startedAtEpochMs", session.startedAtEpochMs);
        return object;
    }

    private static boolean hasStoredItemsForSession(
            SharedPreferences preferences,
            String key,
            String sessionId) {
        JSONArray array = parseArray(preferences.getString(key, "[]"));
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object != null && sessionId.equals(object.optString("sessionId"))) {
                return true;
            }
        }
        return false;
    }

    private static synchronized boolean commitWorkoutTransaction(
            Context context,
            String workoutType,
            String detailedOriginal,
            String detailedUpdated,
            String cardioOriginal,
            String cardioUpdated,
            String sessionsOriginal,
            String sessionsUpdated
    ) {
        if (!recoverPendingTransaction(context)) {
            return false;
        }

        SharedPreferences journal =
                context.getSharedPreferences(PREFS_TRANSACTIONS, Context.MODE_PRIVATE);
        JSONObject transaction = new JSONObject();
        try {
            transaction.put("workoutType", workoutType);
            transaction.put("detailedOriginal", detailedOriginal);
            transaction.put("cardioOriginal", cardioOriginal);
            transaction.put("sessionsOriginal", sessionsOriginal);
        } catch (JSONException exception) {
            Log.e(TAG, "Transaktionsjournal konnte nicht erstellt werden.", exception);
            return false;
        }
        if (!journal.edit()
                .putString(KEY_PENDING_TRANSACTION, transaction.toString())
                .commit()) {
            return false;
        }

        SharedPreferences detailedPrefs =
                context.getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE);
        SharedPreferences cardioPrefs =
                context.getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE);
        SharedPreferences sessionPrefs =
                context.getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE);
        boolean committed = detailedPrefs.edit()
                .putString(workoutType, detailedUpdated)
                .commit()
                && cardioPrefs.edit()
                .putString(workoutType, cardioUpdated)
                .commit()
                && sessionPrefs.edit()
                .putString(KEY_SESSIONS, sessionsUpdated)
                .commit();
        if (!committed) {
            recoverPendingTransaction(context);
            return false;
        }
        if (!journal.edit().remove(KEY_PENDING_TRANSACTION).commit()) {
            recoverPendingTransaction(context);
            return false;
        }
        return true;
    }

    private static synchronized boolean recoverPendingTransaction(Context context) {
        SharedPreferences journal =
                context.getSharedPreferences(PREFS_TRANSACTIONS, Context.MODE_PRIVATE);
        String stored = journal.getString(KEY_PENDING_TRANSACTION, "");
        if (stored == null || stored.isEmpty()) {
            return true;
        }

        try {
            JSONObject transaction = new JSONObject(stored);
            String workoutType = transaction.getString("workoutType");
            if (!isKnownWorkoutType(context, workoutType)) {
                throw new JSONException("Ungültiger Trainingstyp im Transaktionsjournal.");
            }
            boolean restored = context
                    .getSharedPreferences(PREFS_DETAILED, Context.MODE_PRIVATE)
                    .edit()
                    .putString(
                            workoutType,
                            transaction.getString("detailedOriginal")
                    )
                    .commit();
            restored = context
                    .getSharedPreferences(PREFS_CARDIO, Context.MODE_PRIVATE)
                    .edit()
                    .putString(
                            workoutType,
                            transaction.getString("cardioOriginal")
                    )
                    .commit() && restored;
            restored = context
                    .getSharedPreferences(PREFS_SESSIONS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(
                            KEY_SESSIONS,
                            transaction.getString("sessionsOriginal")
                    )
                    .commit() && restored;
            if (!restored) {
                return false;
            }
            return journal.edit().remove(KEY_PENDING_TRANSACTION).commit();
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(
                    PREFS_TRANSACTIONS + "/" + KEY_PENDING_TRANSACTION,
                    exception
            );
            return false;
        }
    }

    private static long parseTimestampEpoch(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return System.currentTimeMillis();
        }
        SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.getDefault());
        format.setLenient(false);
        try {
            Date parsed = format.parse(timestamp.trim());
            return parsed == null ? System.currentTimeMillis() : parsed.getTime();
        } catch (ParseException ignored) {
            return System.currentTimeMillis();
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

    public static class WorkoutExercise {
        public final String exercise;
        public final List<WorkoutSet> sets;

        public WorkoutExercise(String exercise, List<WorkoutSet> sets) {
            this.exercise = exercise == null ? "" : exercise.trim();
            this.sets = sets;
        }
    }

    public static class DailyWorkout {
        public String date;
        public String workoutType;
        public String sessionId;
        public String timestamp;
        public List<DetailedWorkout> exercises;
        public List<CardioSession> cardioSessions;
        
        public DailyWorkout(String date, List<DetailedWorkout> exercises, List<CardioSession> cardioSessions) {
            this(date, "", "", "", exercises, cardioSessions);
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
        public long startedAtEpochMs;

        public TrainingSession(
                String sessionId,
                String workoutType,
                String timestamp,
                long durationMs,
                boolean completed,
                long startedAtEpochMs) {
            this.sessionId = sessionId;
            this.workoutType = workoutType;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.completed = completed;
            this.startedAtEpochMs = startedAtEpochMs;
        }
    }
}

