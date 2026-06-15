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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private static final String KEY_TARGET_WEIGHT = "profile_target_weight";
    private static final String KEY_ACTIVITY_LEVEL = "profile_activity_level";
    private static final String KEY_EXPERIENCE = "profile_experience";
    private static final String KEY_PREFERRED_DAYS = "profile_preferred_days";
    private static final String KEY_TARGET_DATE = "profile_target_date";
    private static final String KEY_BODY_FAT = "profile_body_fat";
    private static final String KEY_STRENGTH_GOAL = "profile_strength_goal";
    private static final String KEY_VOLUME_GOAL = "profile_volume_goal";
    private static final String KEY_WEIGHT_HISTORY = "profile_weight_history";
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
        profile.birthYear = preferences.getInt(KEY_BIRTH_YEAR, 0);
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
        profile.bodyFatPercent =
                parseDouble(preferences.getString(KEY_BODY_FAT, ""));
        profile.strengthGoalKg =
                parseDouble(preferences.getString(KEY_STRENGTH_GOAL, ""));
        profile.weeklyVolumeGoalKg =
                parseDouble(preferences.getString(KEY_VOLUME_GOAL, ""));
        return profile;
    }

    public void save(Profile profile, LocalDate measurementDate) {
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_NAME, safe(profile.name))
                .putString(KEY_WEIGHT, formatNumber(profile.currentWeightKg))
                .putString(KEY_GOAL_ID, normalizeGoalId(profile.goalId))
                .putString(KEY_LEGACY_GOAL, normalizeGoalId(profile.goalId))
                .putInt(KEY_WEEKLY_GOAL, clamp(profile.weeklyTrainingGoal, 1, 7))
                .putInt(KEY_HEIGHT, Math.max(0, profile.heightCm))
                .putInt(KEY_BIRTH_YEAR, Math.max(0, profile.birthYear))
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
                .putString(KEY_STRENGTH_GOAL, formatNumber(profile.strengthGoalKg))
                .putString(
                        KEY_VOLUME_GOAL,
                        formatNumber(profile.weeklyVolumeGoalKg)
                )
                .remove(KEY_LEGACY_CALORIES);

        if (profile.currentWeightKg > 0) {
            List<WeightEntry> history = getWeightHistory();
            upsertWeightEntry(
                    history,
                    measurementDate == null ? LocalDate.now() : measurementDate,
                    profile.currentWeightKg
            );
            editor.putString(KEY_WEIGHT_HISTORY, serializeWeightHistory(history));
        }
        editor.apply();
    }

    public List<WeightEntry> getWeightHistory() {
        List<WeightEntry> result = parseWeightHistory(
                preferences.getString(KEY_WEIGHT_HISTORY, "[]")
        );
        if (result.isEmpty()) {
            double legacyWeight = parseDouble(preferences.getString(KEY_WEIGHT, ""));
            if (legacyWeight > 0) {
                result.add(new WeightEntry(LocalDate.now(), legacyWeight));
            }
        }
        result.sort(Comparator.comparing(entry -> entry.date));
        return result;
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

    private static List<WeightEntry> parseWeightHistory(String json) {
        List<WeightEntry> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                LocalDate date = parseDate(object.optString("date", ""));
                double weight = object.optDouble("weightKg", 0);
                if (date != null && weight > 0) {
                    result.add(new WeightEntry(date, weight));
                }
            }
        } catch (JSONException ignored) {
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
        public double bodyFatPercent;
        public String goalId = GOAL_MUSCLE;
        public int weeklyTrainingGoal = 3;
        public String activityLevelId = ACTIVITY_MODERATE;
        public String experienceId = EXPERIENCE_BEGINNER;
        public Set<DayOfWeek> preferredDays = new HashSet<>();
        public LocalDate targetDate;
        public double strengthGoalKg;
        public double weeklyVolumeGoalKg;
    }

    public static final class WeightEntry {
        public final LocalDate date;
        public final double weightKg;

        public WeightEntry(LocalDate date, double weightKg) {
            this.date = date;
            this.weightKg = weightKg;
        }
    }
}
