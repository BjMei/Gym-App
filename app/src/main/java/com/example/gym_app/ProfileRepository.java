package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProfileRepository {

    public static final String GOAL_MUSCLE = "muscle";
    public static final String GOAL_STRENGTH = "strength";
    public static final String GOAL_WEIGHT_LOSS = "weight_loss";
    public static final String GOAL_FITNESS = "fitness";

    public static final String ACTIVITY_LOW = "low";
    public static final String ACTIVITY_MODERATE = "moderate";
    public static final String ACTIVITY_HIGH = "high";
    public static final String ACTIVITY_VERY_HIGH = "very_high";

    public static final String EXPERIENCE_BEGINNER = "beginner";
    public static final String EXPERIENCE_INTERMEDIATE = "intermediate";
    public static final String EXPERIENCE_EXPERIENCED = "experienced";

    private static final String KEY_NAME = "profile_name";
    private static final String KEY_WEIGHT = "profile_weight";
    private static final String KEY_LEGACY_GOAL = "goal";
    private static final String KEY_GOAL_ID = "profile_goal_id";
    private static final String KEY_WEEKLY_GOAL = "training_goal_per_week";
    private static final String KEY_HEIGHT = "profile_height_cm";
    private static final String KEY_BIRTH_YEAR = "profile_birth_year";
    private static final String KEY_BIRTH_DATE = "profile_birth_date";
    private static final String KEY_TARGET_WEIGHT = "profile_target_weight";
    private static final String KEY_ACTIVITY_LEVEL = "profile_activity_level";
    private static final String KEY_EXPERIENCE = "profile_experience";
    private static final String KEY_PREFERRED_DAYS = "profile_preferred_days";
    private static final String KEY_TARGET_DATE = "profile_target_date";
    private static final String KEY_BODY_FAT = "profile_body_fat";
    private static final String KEY_LEGACY_STRENGTH_GOAL = "profile_strength_goal";
    private static final String KEY_STRENGTH_GOALS = "profile_strength_goals";
    private static final String KEY_VOLUME_GOAL = "profile_volume_goal";
    private static final String KEY_WEIGHT_HISTORY = "profile_weight_history";
    private static final String KEY_BODY_FAT_HISTORY = "profile_body_fat_history";
    private static final String KEY_LEGACY_CALORIES = "calorie_goal";

    private final SharedPreferences preferences;

    public ProfileRepository(Context context) {
        preferences = AppSettings.preferences(context);
    }

    public Profile load() {
        Profile profile = new Profile();
        profile.name = preferences.getString(KEY_NAME, "");
        profile.currentWeightKg = parseDouble(preferences.getString(KEY_WEIGHT, ""));
        profile.goalId = getGoalId();
        profile.weeklyTrainingGoal = clamp(
                preferences.getInt(KEY_WEEKLY_GOAL, 3),
                1,
                7
        );
        profile.heightCm = preferences.getInt(KEY_HEIGHT, 0);
        profile.birthDate = parseDate(preferences.getString(KEY_BIRTH_DATE, ""));
        profile.birthYear = profile.birthDate == null
                ? preferences.getInt(KEY_BIRTH_YEAR, 0)
                : profile.birthDate.getYear();
        profile.targetWeightKg =
                parseDouble(preferences.getString(KEY_TARGET_WEIGHT, ""));
        profile.activityLevelId = preferences.getString(
                KEY_ACTIVITY_LEVEL,
                ACTIVITY_MODERATE
        );
        profile.experienceId = preferences.getString(
                KEY_EXPERIENCE,
                EXPERIENCE_BEGINNER
        );
        profile.preferredDays = parsePreferredDays(
                preferences.getStringSet(KEY_PREFERRED_DAYS, Collections.emptySet())
        );
        profile.targetDate = parseDate(
                preferences.getString(KEY_TARGET_DATE, "")
        );
        profile.bodyFatPercent = latestBodyFatPercent();
        profile.legacyStrengthGoalKg =
                parseDouble(preferences.getString(KEY_LEGACY_STRENGTH_GOAL, ""));
        profile.strengthGoalsKg = getStrengthGoals();
        profile.weeklyVolumeGoalKg =
                parseDouble(preferences.getString(KEY_VOLUME_GOAL, ""));
        return profile;
    }

    public boolean save(Profile profile) {
        if (profile == null) {
            return false;
        }
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_NAME, safe(profile.name))
                .putString(KEY_WEIGHT, formatNumber(profile.currentWeightKg))
                .putString(KEY_GOAL_ID, normalizeGoalId(profile.goalId))
                .putString(KEY_LEGACY_GOAL, normalizeGoalId(profile.goalId))
                .putInt(KEY_WEEKLY_GOAL, clamp(profile.weeklyTrainingGoal, 1, 7))
                .putInt(KEY_HEIGHT, Math.max(0, profile.heightCm))
                .putInt(
                        KEY_BIRTH_YEAR,
                        profile.birthDate == null ? Math.max(0, profile.birthYear) : profile.birthDate.getYear()
                )
                .putString(
                        KEY_BIRTH_DATE,
                        profile.birthDate == null ? "" : profile.birthDate.toString()
                )
                .putString(KEY_TARGET_WEIGHT, formatNumber(profile.targetWeightKg))
                .putString(
                        KEY_ACTIVITY_LEVEL,
                        normalizeActivityId(profile.activityLevelId)
                )
                .putString(
                        KEY_EXPERIENCE,
                        normalizeExperienceId(profile.experienceId)
                )
                .putStringSet(
                        KEY_PREFERRED_DAYS,
                        serializePreferredDays(profile.preferredDays)
                )
                .putString(
                        KEY_TARGET_DATE,
                        profile.targetDate == null ? "" : profile.targetDate.toString()
                )
                .putString(KEY_BODY_FAT, formatNumber(profile.bodyFatPercent))
                .putString(
                        KEY_VOLUME_GOAL,
                        formatNumber(profile.weeklyVolumeGoalKg)
                )
                .remove(KEY_LEGACY_CALORIES);

        return editor.commit();
    }

    public boolean addWeightMeasurement(LocalDate date, double weightKg) {
        if (date == null || weightKg <= 0) {
            return false;
        }
        List<WeightEntry> history = getWeightHistoryForWrite();
        if (history == null) {
            return false;
        }
        upsertWeightEntry(history, date, weightKg);
        history.sort(Comparator.comparing(entry -> entry.date));
        double latestWeight = history.get(history.size() - 1).weightKg;
        return preferences.edit()
                .putString(KEY_WEIGHT, formatNumber(latestWeight))
                .putString(KEY_WEIGHT_HISTORY, serializeWeightHistory(history))
                .commit();
    }

    public boolean removeWeightMeasurement(LocalDate date) {
        if (date == null) {
            return false;
        }
        List<WeightEntry> history = getWeightHistoryForWrite();
        if (history == null) {
            return false;
        }
        boolean removed = history.removeIf(entry -> entry.date.equals(date));
        if (!removed) {
            return false;
        }
        history.sort(Comparator.comparing(entry -> entry.date));
        double latestWeight = history.isEmpty()
                ? 0
                : history.get(history.size() - 1).weightKg;
        return preferences.edit()
                .putString(KEY_WEIGHT, formatNumber(latestWeight))
                .putString(KEY_WEIGHT_HISTORY, serializeWeightHistory(history))
                .commit();
    }

    public boolean addBodyFatMeasurement(LocalDate date, double bodyFatPercent) {
        if (date == null || bodyFatPercent <= 0) {
            return false;
        }
        List<BodyFatEntry> history = getBodyFatHistoryForWrite();
        if (history == null) {
            return false;
        }
        upsertBodyFatEntry(history, date, bodyFatPercent);
        history.sort(Comparator.comparing(entry -> entry.date));
        double latestBodyFat = history.get(history.size() - 1).bodyFatPercent;
        return preferences.edit()
                .putString(KEY_BODY_FAT, formatNumber(latestBodyFat))
                .putString(KEY_BODY_FAT_HISTORY, serializeBodyFatHistory(history))
                .commit();
    }

    public boolean removeBodyFatMeasurement(LocalDate date) {
        if (date == null) {
            return false;
        }
        List<BodyFatEntry> history = getBodyFatHistoryForWrite();
        if (history == null) {
            return false;
        }
        boolean removed = history.removeIf(entry -> entry.date.equals(date));
        if (!removed) {
            return false;
        }
        history.sort(Comparator.comparing(entry -> entry.date));
        double latestBodyFat = history.isEmpty()
                ? 0
                : history.get(history.size() - 1).bodyFatPercent;
        return preferences.edit()
                .putString(KEY_BODY_FAT, formatNumber(latestBodyFat))
                .putString(KEY_BODY_FAT_HISTORY, serializeBodyFatHistory(history))
                .commit();
    }

    public boolean setStrengthGoal(
            String workoutType,
            String exercise,
            double targetKg
    ) {
        String key = strengthGoalKey(workoutType, exercise);
        if (key.isEmpty() || !WorkoutTypeRepository.isSafeTypeId(workoutType)) {
            return false;
        }
        Map<String, StrengthGoal> goals = getStrengthGoalsForWrite();
        if (goals == null) {
            return false;
        }
        if (targetKg > 0) {
            goals.put(key, new StrengthGoal(workoutType, exercise.trim(), targetKg));
        } else {
            goals.remove(key);
        }
        return preferences.edit()
                .putString(KEY_STRENGTH_GOALS, serializeStrengthGoals(goals))
                .remove(KEY_LEGACY_STRENGTH_GOAL)
                .commit();
    }

    public boolean removeStrengthGoal(String workoutType, String exercise) {
        return setStrengthGoal(workoutType, exercise, 0);
    }

    public Map<String, StrengthGoal> getStrengthGoals() {
        Map<String, StrengthGoal> goals = getStrengthGoalsForWrite();
        return goals == null ? new LinkedHashMap<>() : goals;
    }

    public double getStrengthGoalKg(String workoutType, String exercise) {
        StrengthGoal goal = getStrengthGoals().get(strengthGoalKey(workoutType, exercise));
        return goal == null ? 0 : goal.targetKg;
    }

    public List<WeightEntry> getWeightHistory() {
        List<WeightEntry> result = getWeightHistoryForWrite();
        return result == null ? new ArrayList<>() : result;
    }

    private List<WeightEntry> getWeightHistoryForWrite() {
        List<WeightEntry> result;
        try {
            result = parseWeightHistory(
                    preferences.getString(KEY_WEIGHT_HISTORY, "[]")
            );
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(
                    AppSettings.PREFS_NAME + "/" + KEY_WEIGHT_HISTORY,
                    exception
            );
            return null;
        }
        if (result.isEmpty()) {
            double legacyWeight = parseDouble(preferences.getString(KEY_WEIGHT, ""));
            if (legacyWeight > 0) {
                result.add(new WeightEntry(LocalDate.now(), legacyWeight));
            }
        }
        result.sort(Comparator.comparing(entry -> entry.date));
        return result;
    }

    public List<BodyFatEntry> getBodyFatHistory() {
        List<BodyFatEntry> result = getBodyFatHistoryForWrite();
        return result == null ? new ArrayList<>() : result;
    }

    private List<BodyFatEntry> getBodyFatHistoryForWrite() {
        List<BodyFatEntry> result;
        try {
            result = parseBodyFatHistory(
                    preferences.getString(KEY_BODY_FAT_HISTORY, "[]")
            );
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(
                    AppSettings.PREFS_NAME + "/" + KEY_BODY_FAT_HISTORY,
                    exception
            );
            return null;
        }
        if (result.isEmpty()) {
            double legacyBodyFat = parseDouble(preferences.getString(KEY_BODY_FAT, ""));
            if (legacyBodyFat > 0) {
                result.add(new BodyFatEntry(LocalDate.now(), legacyBodyFat));
            }
        }
        result.sort(Comparator.comparing(entry -> entry.date));
        return result;
    }

    private Map<String, StrengthGoal> getStrengthGoalsForWrite() {
        try {
            return parseStrengthGoals(
                    preferences.getString(KEY_STRENGTH_GOALS, "[]")
            );
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(
                    AppSettings.PREFS_NAME + "/" + KEY_STRENGTH_GOALS,
                    exception
            );
            return null;
        }
    }

    public String getGoalId() {
        String stored = preferences.getString(KEY_GOAL_ID, "");
        if (stored != null && !stored.trim().isEmpty()) {
            return normalizeGoalId(stored);
        }

        String legacy = preferences.getString(KEY_LEGACY_GOAL, "");
        String normalized = legacy == null
                ? ""
                : legacy.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("kraft") || normalized.contains("strength")) {
            return GOAL_STRENGTH;
        }
        if (normalized.contains("abnehm")
                || normalized.contains("lose")
                || normalized.contains("weight")) {
            return GOAL_WEIGHT_LOSS;
        }
        if (normalized.contains("fitness")) {
            return GOAL_FITNESS;
        }
        return GOAL_MUSCLE;
    }

    private static void upsertWeightEntry(
            List<WeightEntry> history,
            LocalDate date,
            double weightKg) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).date.equals(date)) {
                history.set(i, new WeightEntry(date, weightKg));
                return;
            }
        }
        history.add(new WeightEntry(date, weightKg));
    }

    private static void upsertBodyFatEntry(
            List<BodyFatEntry> history,
            LocalDate date,
            double bodyFatPercent) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).date.equals(date)) {
                history.set(i, new BodyFatEntry(date, bodyFatPercent));
                return;
            }
        }
        history.add(new BodyFatEntry(date, bodyFatPercent));
    }

    private static List<WeightEntry> parseWeightHistory(String json)
            throws JSONException {
        List<WeightEntry> result = new ArrayList<>();
        JSONArray array = new JSONArray(json == null ? "[]" : json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            LocalDate date = parseDate(object.getString("date"));
            double weight = object.getDouble("weightKg");
            if (date == null || !Double.isFinite(weight) || weight <= 0) {
                throw new JSONException("Ungültiger Gewichtseintrag an Position " + i + ".");
            }
            result.add(new WeightEntry(date, weight));
        }
        return result;
    }

    private static String serializeWeightHistory(List<WeightEntry> history) {
        JSONArray array = new JSONArray();
        history.sort(Comparator.comparing(entry -> entry.date));
        for (WeightEntry entry : history) {
            try {
                JSONObject object = new JSONObject();
                object.put("date", entry.date.toString());
                object.put("weightKg", entry.weightKg);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    private static List<BodyFatEntry> parseBodyFatHistory(String json)
            throws JSONException {
        List<BodyFatEntry> result = new ArrayList<>();
        JSONArray array = new JSONArray(json == null ? "[]" : json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            LocalDate date = parseDate(object.getString("date"));
            double bodyFatPercent = object.getDouble("bodyFatPercent");
            if (date == null
                    || !Double.isFinite(bodyFatPercent)
                    || bodyFatPercent <= 0
                    || bodyFatPercent > 100) {
                throw new JSONException("Ungültiger Körperfetteintrag an Position " + i + ".");
            }
            result.add(new BodyFatEntry(date, bodyFatPercent));
        }
        return result;
    }

    private static String serializeBodyFatHistory(List<BodyFatEntry> history) {
        JSONArray array = new JSONArray();
        history.sort(Comparator.comparing(entry -> entry.date));
        for (BodyFatEntry entry : history) {
            try {
                JSONObject object = new JSONObject();
                object.put("date", entry.date.toString());
                object.put("bodyFatPercent", entry.bodyFatPercent);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    private double latestBodyFatPercent() {
        List<BodyFatEntry> history = getBodyFatHistory();
        if (!history.isEmpty()) {
            return history.get(history.size() - 1).bodyFatPercent;
        }
        return parseDouble(preferences.getString(KEY_BODY_FAT, ""));
    }

    private static Map<String, StrengthGoal> parseStrengthGoals(String json)
            throws JSONException {
        Map<String, StrengthGoal> result = new LinkedHashMap<>();
        JSONArray array = new JSONArray(json == null ? "[]" : json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String workoutType = object.getString("workoutType");
            String exercise = object.getString("exercise").trim();
            double targetKg = object.getDouble("targetKg");
            String key = strengthGoalKey(workoutType, exercise);
            if (key.isEmpty()
                    || !isKnownWorkoutType(workoutType)
                    || !Double.isFinite(targetKg)
                    || targetKg <= 0) {
                throw new JSONException("Ungültiges Kraftziel an Position " + i + ".");
            }
            result.put(key, new StrengthGoal(workoutType, exercise, targetKg));
        }
        return result;
    }

    static void validateStructuredPreference(String key, String json)
            throws JSONException {
        if (KEY_WEIGHT_HISTORY.equals(key)) {
            parseWeightHistory(json);
        } else if (KEY_BODY_FAT_HISTORY.equals(key)) {
            parseBodyFatHistory(json);
        } else if (KEY_STRENGTH_GOALS.equals(key)) {
            parseStrengthGoals(json);
        }
    }

    static boolean isStructuredPreferenceKey(String key) {
        return KEY_WEIGHT_HISTORY.equals(key)
                || KEY_BODY_FAT_HISTORY.equals(key)
                || KEY_STRENGTH_GOALS.equals(key);
    }

    private static boolean isKnownWorkoutType(String workoutType) {
        return WorkoutTypeRepository.isSafeTypeId(workoutType);
    }

    private static String serializeStrengthGoals(Map<String, StrengthGoal> goals) {
        JSONArray array = new JSONArray();
        if (goals == null) {
            return array.toString();
        }
        List<StrengthGoal> sorted = new ArrayList<>(goals.values());
        sorted.sort(Comparator
                .comparing((StrengthGoal goal) -> goal.workoutType)
                .thenComparing(goal -> goal.exercise.toLowerCase(Locale.ROOT)));
        for (StrengthGoal goal : sorted) {
            if (goal == null || goal.targetKg <= 0) {
                continue;
            }
            try {
                JSONObject object = new JSONObject();
                object.put("workoutType", goal.workoutType);
                object.put("exercise", goal.exercise);
                object.put("targetKg", goal.targetKg);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    public static String strengthGoalKey(String workoutType, String exercise) {
        String safeType = workoutType == null ? "" : workoutType.trim().toLowerCase(Locale.ROOT);
        String safeExercise = exercise == null ? "" : exercise.trim().toLowerCase(Locale.ROOT);
        if (safeType.isEmpty() || safeExercise.isEmpty()) {
            return "";
        }
        return safeType + "|" + safeExercise;
    }

    private static Set<DayOfWeek> parsePreferredDays(Set<String> stored) {
        Set<DayOfWeek> result = new HashSet<>();
        if (stored == null) {
            return result;
        }
        for (String value : stored) {
            try {
                result.add(DayOfWeek.of(Integer.parseInt(value)));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static Set<String> serializePreferredDays(Set<DayOfWeek> days) {
        Set<String> result = new HashSet<>();
        if (days != null) {
            for (DayOfWeek day : days) {
                result.add(String.valueOf(day.getValue()));
            }
        }
        return result;
    }

    private static String normalizeGoalId(String value) {
        if (GOAL_STRENGTH.equals(value)
                || GOAL_WEIGHT_LOSS.equals(value)
                || GOAL_FITNESS.equals(value)) {
            return value;
        }
        return GOAL_MUSCLE;
    }

    private static String normalizeActivityId(String value) {
        if (ACTIVITY_LOW.equals(value)
                || ACTIVITY_HIGH.equals(value)
                || ACTIVITY_VERY_HIGH.equals(value)) {
            return value;
        }
        return ACTIVITY_MODERATE;
    }

    private static String normalizeExperienceId(String value) {
        if (EXPERIENCE_INTERMEDIATE.equals(value)
                || EXPERIENCE_EXPERIENCED.equals(value)) {
            return value;
        }
        return EXPERIENCE_BEGINNER;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static double parseDouble(String value) {
        try {
            return value == null || value.trim().isEmpty()
                    ? 0
                    : Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String formatNumber(double value) {
        return value > 0
                ? String.format(Locale.ROOT, "%.2f", value)
                : "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Profile {
        public String name = "";
        public double currentWeightKg;
        public double targetWeightKg;
        public int heightCm;
        public int birthYear;
        public LocalDate birthDate;
        public double bodyFatPercent;
        public String goalId = GOAL_MUSCLE;
        public int weeklyTrainingGoal = 3;
        public String activityLevelId = ACTIVITY_MODERATE;
        public String experienceId = EXPERIENCE_BEGINNER;
        public Set<DayOfWeek> preferredDays = new HashSet<>();
        public LocalDate targetDate;
        public double legacyStrengthGoalKg;
        public Map<String, StrengthGoal> strengthGoalsKg = new LinkedHashMap<>();
        public double weeklyVolumeGoalKg;

        public double getStrengthGoalKg(String workoutType, String exercise) {
            StrengthGoal goal = strengthGoalsKg.get(
                    strengthGoalKey(workoutType, exercise)
            );
            return goal == null ? 0 : goal.targetKg;
        }
    }

    public static final class WeightEntry {
        public final LocalDate date;
        public final double weightKg;

        public WeightEntry(LocalDate date, double weightKg) {
            this.date = date;
            this.weightKg = weightKg;
        }
    }

    public static final class BodyFatEntry {
        public final LocalDate date;
        public final double bodyFatPercent;

        public BodyFatEntry(LocalDate date, double bodyFatPercent) {
            this.date = date;
            this.bodyFatPercent = bodyFatPercent;
        }
    }

    public static final class StrengthGoal {
        public final String workoutType;
        public final String exercise;
        public final double targetKg;

        public StrengthGoal(String workoutType, String exercise, double targetKg) {
            this.workoutType = workoutType == null ? "" : workoutType;
            this.exercise = exercise == null ? "" : exercise;
            this.targetKg = targetKg;
        }
    }
}
