package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class AppSettings {

    public static final String PREFS_NAME = "AppSettings";

    public static final String KEY_THEME = "theme";
    public static final String KEY_UNITS = "units";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_ANIMATIONS = "animations";
    public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String KEY_HISTORY_EXPANDED = "history_expanded";
    public static final String KEY_HAPTICS = "haptics";

    public static final String THEME_STANDARD = "standard";
    public static final String THEME_OLED = "oled";
    public static final String UNIT_KG = "kg";
    public static final String UNIT_LBS = "lbs";
    public static final String LANGUAGE_DE = "de";
    public static final String LANGUAGE_EN = "en";
    public static final String TEXT_COMPACT = "compact";
    public static final String TEXT_STANDARD = "standard";
    public static final String TEXT_LARGE = "large";

    private static final double KG_TO_LBS = 2.2046226218;

    private AppSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Context wrapContext(Context context) {
        SharedPreferences preferences = preferences(context);
        String language = normalizeLanguage(preferences.getString(KEY_LANGUAGE, LANGUAGE_DE));
        Locale locale = Locale.forLanguageTag(language);
        Locale.setDefault(locale);

        Configuration configuration =
                new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        configuration.fontScale = getTextScale(preferences);
        return context.createConfigurationContext(configuration);
    }

    public static boolean isOledMode(Context context) {
        String value = preferences(context).getString(KEY_THEME, THEME_STANDARD);
        return THEME_OLED.equals(value) || "Light".equalsIgnoreCase(value);
    }

    public static boolean usesLbs(Context context) {
        String value = preferences(context).getString(KEY_UNITS, UNIT_KG);
        return UNIT_LBS.equalsIgnoreCase(value);
    }

    public static String getWeightUnit(Context context) {
        return usesLbs(context) ? UNIT_LBS : UNIT_KG;
    }

    public static double fromStoredKg(Context context, double kilograms) {
        return usesLbs(context) ? kilograms * KG_TO_LBS : kilograms;
    }

    public static double toStoredKg(Context context, double displayedWeight) {
        return usesLbs(context) ? displayedWeight / KG_TO_LBS : displayedWeight;
    }

    public static String formatWeight(Context context, double kilograms, int decimals) {
        String pattern = "%." + Math.max(0, decimals) + "f %s";
        return String.format(
                Locale.getDefault(),
                pattern,
                fromStoredKg(context, kilograms),
                getWeightUnit(context)
        );
    }

    public static String formatVolume(Context context, double kilogramVolume, int decimals) {
        return formatWeight(context, kilogramVolume, decimals);
    }

    public static boolean animationsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ANIMATIONS, true);
    }

    public static boolean keepScreenOn(Context context) {
        return preferences(context).getBoolean(KEY_KEEP_SCREEN_ON, true);
    }

    public static boolean historyExpanded(Context context) {
        return preferences(context).getBoolean(KEY_HISTORY_EXPANDED, false);
    }

    public static boolean hapticsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_HAPTICS, true);
    }

    public static String normalizeLanguage(String value) {
        if (value == null) {
            return LANGUAGE_DE;
        }
        if (LANGUAGE_EN.equalsIgnoreCase(value) || "English".equalsIgnoreCase(value)) {
            return LANGUAGE_EN;
        }
        return LANGUAGE_DE;
    }

    public static float getTextScale(Context context) {
        return getTextScale(preferences(context));
    }

    private static float getTextScale(SharedPreferences preferences) {
        String value = preferences.getString(KEY_TEXT_SIZE, TEXT_STANDARD);
        if (TEXT_COMPACT.equals(value)) {
            return 0.90f;
        }
        if (TEXT_LARGE.equals(value)) {
            return 1.15f;
        }
        return 1.0f;
    }
}
