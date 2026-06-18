package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppDataResetManager {

    private AppDataResetManager() {
    }

    public static void resetAllData(Context context) throws IOException {
        Context appContext = context.getApplicationContext();
        clearSharedPreferences(appContext);
        clearDirectory(appContext.getFilesDir());
        clearDirectory(appContext.getNoBackupFilesDir());
        clearDirectory(appContext.getCacheDir());
        clearDirectory(appContext.getCodeCacheDir());

        File database = appContext.getDatabasePath("ironx-placeholder");
        clearDirectory(database == null ? null : database.getParentFile());
    }

    private static void clearSharedPreferences(Context context) throws IOException {
        for (String preferenceName : discoverPreferenceNames(context)) {
            SharedPreferences preferences =
                    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
            if (!preferences.edit().clear().commit()) {
                throw new IOException("App-Daten konnten nicht vollständig gelöscht werden.");
            }
        }
    }

    private static Set<String> discoverPreferenceNames(Context context) {
        Set<String> names = new LinkedHashSet<>();
        names.add(AppSettings.PREFS_NAME);
        names.add("CustomExercises");
        names.add(WorkoutTypeRepository.PREFS_NAME);
        names.add("ExerciseMuscleMappings");
        names.add("ExerciseSettings");
        names.add("WorkoutHistory");
        names.add("WorkoutHistoryDetailed");
        names.add("WorkoutCardio");
        names.add("WorkoutSessions");

        File directory = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        if (files == null) {
            return names;
        }
        for (File file : files) {
            String fileName = file.getName();
            names.add(fileName.substring(0, fileName.length() - 4));
        }
        return names;
    }

    private static void clearDirectory(File directory) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            deleteRecursively(child);
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Eine App-Datei konnte nicht gelöscht werden.");
        }
    }
}
