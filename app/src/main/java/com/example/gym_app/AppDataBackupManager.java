package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.core.content.pm.PackageInfoCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

public final class AppDataBackupManager {

    private static final String FORMAT_ID = "IRONX_APP_BACKUP";
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_JSON_BYTES = 100 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 10_000;
    private static final long MAX_DECODED_FILE_BYTES = 100L * 1024L * 1024L;
    private static final Set<String> EXCLUDED_PREFERENCE_NAMES =
            Collections.singleton("WorkoutHistory");

    private static final List<String> KNOWN_PREFERENCE_NAMES = Arrays.asList(
            AppSettings.PREFS_NAME,
            "CustomExercises",
            "ExerciseMuscleMappings",
            "ExerciseSettings",
            "WorkoutHistoryDetailed",
            "WorkoutCardio",
            "WorkoutSessions"
    );

    private final Context context;

    public AppDataBackupManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public ExportSummary exportTo(OutputStream outputStream) throws IOException {
        BackupSnapshot snapshot = captureSnapshot();
        String jsonText;
        try {
            jsonText = toJson(snapshot).toString(2);
        } catch (JSONException exception) {
            throw new IOException("Die Sicherung konnte nicht als JSON erstellt werden.", exception);
        }

        byte[] bytes = jsonText.getBytes(StandardCharsets.UTF_8);
        try (BufferedOutputStream output = new BufferedOutputStream(outputStream)) {
            output.write(bytes);
            output.flush();
        }
        return new ExportSummary(
                snapshot.preferenceCount(),
                snapshot.preferenceValueCount(),
                snapshot.fileCount(),
                bytes.length
        );
    }

    public PreparedBackup readAndValidate(InputStream inputStream) throws IOException {
        byte[] bytes = readWithLimit(inputStream, MAX_JSON_BYTES);
        try {
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            BackupSnapshot snapshot = fromJson(root);
            return new PreparedBackup(
                    snapshot,
                    snapshot.preferenceCount(),
                    snapshot.preferenceValueCount(),
                    snapshot.fileCount(),
                    bytes.length,
                    root.optString("createdAt", "")
            );
        } catch (JSONException | IllegalArgumentException exception) {
            throw new IOException("Die ausgewählte Datei ist keine gültige IRONX-Sicherung.", exception);
        }
    }

    public void restore(PreparedBackup preparedBackup) throws IOException {
        if (preparedBackup == null || preparedBackup.snapshot == null) {
            throw new IOException("Es wurden keine gültigen Sicherungsdaten übergeben.");
        }

        BackupSnapshot currentSnapshot = captureSnapshot();
        try {
            applySnapshot(preparedBackup.snapshot);
        } catch (IOException | RuntimeException restoreError) {
            try {
                applySnapshot(currentSnapshot);
            } catch (Exception rollbackError) {
                restoreError.addSuppressed(rollbackError);
            }
            if (restoreError instanceof IOException) {
                throw (IOException) restoreError;
            }
            throw new IOException("Die Sicherung konnte nicht vollständig wiederhergestellt werden.",
                    restoreError);
        }
    }

    private BackupSnapshot captureSnapshot() throws IOException {
        Map<String, Map<String, PreferenceValue>> preferences = new TreeMap<>();
        for (String preferenceName : discoverPreferenceNames()) {
            Map<String, ?> values =
                    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).getAll();
            Map<String, PreferenceValue> encodedValues = new TreeMap<>();
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                encodedValues.put(entry.getKey(), PreferenceValue.from(entry.getValue()));
            }
            preferences.put(preferenceName, encodedValues);
        }

        Map<String, List<StoredFile>> storage = new LinkedHashMap<>();
        for (Map.Entry<String, File> root : storageRoots().entrySet()) {
            List<StoredFile> files = new ArrayList<>();
            captureFiles(root.getValue(), root.getValue(), files);
            storage.put(root.getKey(), files);
        }

        return new BackupSnapshot(preferences, storage);
    }

    private Set<String> discoverPreferenceNames() {
        Set<String> names = new LinkedHashSet<>(KNOWN_PREFERENCE_NAMES);
        File directory = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                String fileName = file.getName();
                String preferenceName = fileName.substring(0, fileName.length() - 4);
                if (!EXCLUDED_PREFERENCE_NAMES.contains(preferenceName)) {
                    names.add(preferenceName);
                }
            }
        }
        names.removeAll(EXCLUDED_PREFERENCE_NAMES);
        return names;
    }

    private Map<String, File> storageRoots() {
        Map<String, File> roots = new LinkedHashMap<>();
        roots.put("files", context.getFilesDir());
        roots.put("no_backup", context.getNoBackupFilesDir());
        File databaseDirectory = context.getDatabasePath("ironx-placeholder").getParentFile();
        roots.put("databases", databaseDirectory);
        return roots;
    }

    private void captureFiles(File root, File current, List<StoredFile> files) throws IOException {
        if (root == null || current == null || !current.exists()) {
            return;
        }
        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                captureFiles(root, child, files);
            } else if (child.isFile()) {
                if (files.size() >= MAX_FILE_COUNT) {
                    throw new IOException("Die App besitzt zu viele interne Dateien für eine Sicherung.");
                }
                String relativePath = relativePath(root, child);
                files.add(new StoredFile(
                        relativePath,
                        readFile(child),
                        child.lastModified()
                ));
            }
        }
    }

    private JSONObject toJson(BackupSnapshot snapshot) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT_ID);
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("createdAt", utcTimestamp());

        JSONObject app = new JSONObject();
        app.put("packageName", context.getPackageName());
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            app.put("versionName", packageInfo.versionName != null ? packageInfo.versionName : "");
            app.put("versionCode", PackageInfoCompat.getLongVersionCode(packageInfo));
        } catch (PackageManager.NameNotFoundException exception) {
            app.put("versionName", "");
            app.put("versionCode", 0);
        }
        root.put("app", app);

        JSONObject preferencesJson = new JSONObject();
        for (Map.Entry<String, Map<String, PreferenceValue>> preference :
                snapshot.preferences.entrySet()) {
            JSONObject valuesJson = new JSONObject();
            for (Map.Entry<String, PreferenceValue> value : preference.getValue().entrySet()) {
                valuesJson.put(value.getKey(), value.getValue().toJson());
            }
            preferencesJson.put(preference.getKey(), valuesJson);
        }
        root.put("sharedPreferences", preferencesJson);

        JSONObject storageJson = new JSONObject();
        for (Map.Entry<String, List<StoredFile>> rootFiles : snapshot.storage.entrySet()) {
            JSONArray filesJson = new JSONArray();
            for (StoredFile file : rootFiles.getValue()) {
                JSONObject fileJson = new JSONObject();
                fileJson.put("path", file.relativePath);
                fileJson.put("lastModified", file.lastModified);
                fileJson.put("encoding", "base64");
                fileJson.put("data", Base64.encodeToString(file.data, Base64.NO_WRAP));
                filesJson.put(fileJson);
            }
            storageJson.put(rootFiles.getKey(), filesJson);
        }
        root.put("storage", storageJson);

        JSONObject statistics = new JSONObject();
        statistics.put("preferenceFiles", snapshot.preferenceCount());
        statistics.put("preferenceValues", snapshot.preferenceValueCount());
        statistics.put("internalFiles", snapshot.fileCount());
        root.put("statistics", statistics);
        return root;
    }

    private BackupSnapshot fromJson(JSONObject root) throws JSONException, IOException {
        if (!FORMAT_ID.equals(root.optString("format"))) {
            throw new JSONException("Unbekanntes Sicherungsformat.");
        }
        int schemaVersion = root.optInt("schemaVersion", -1);
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            throw new JSONException("Nicht unterstützte Sicherungsversion.");
        }

        JSONObject app = root.getJSONObject("app");
        if (!context.getPackageName().equals(app.optString("packageName"))) {
            throw new JSONException("Die Sicherung gehört nicht zu dieser App.");
        }

        JSONObject preferencesJson = root.getJSONObject("sharedPreferences");
        Map<String, Map<String, PreferenceValue>> preferences = new TreeMap<>();
        Iterator<String> preferenceNames = preferencesJson.keys();
        while (preferenceNames.hasNext()) {
            String preferenceName = validatePreferenceName(preferenceNames.next());
            JSONObject valuesJson = preferencesJson.getJSONObject(preferenceName);
            Map<String, PreferenceValue> values = new TreeMap<>();
            Iterator<String> keys = valuesJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                values.put(key, PreferenceValue.fromJson(valuesJson.getJSONObject(key)));
            }
            preferences.put(preferenceName, values);
        }

        JSONObject storageJson = root.getJSONObject("storage");
        Map<String, List<StoredFile>> storage = new LinkedHashMap<>();
        long decodedBytes = 0;
        int fileCount = 0;
        for (String rootName : storageRoots().keySet()) {
            JSONArray filesJson = storageJson.optJSONArray(rootName);
            List<StoredFile> files = new ArrayList<>();
            if (filesJson != null) {
                for (int index = 0; index < filesJson.length(); index++) {
                    if (++fileCount > MAX_FILE_COUNT) {
                        throw new JSONException("Die Sicherung enthält zu viele Dateien.");
                    }
                    JSONObject fileJson = filesJson.getJSONObject(index);
                    if (!"base64".equals(fileJson.optString("encoding"))) {
                        throw new JSONException("Unbekannte Dateikodierung.");
                    }
                    String relativePath = validateRelativePath(fileJson.getString("path"));
                    byte[] data = Base64.decode(fileJson.getString("data"), Base64.DEFAULT);
                    decodedBytes += data.length;
                    if (decodedBytes > MAX_DECODED_FILE_BYTES) {
                        throw new JSONException("Die Sicherung ist zu groß.");
                    }
                    files.add(new StoredFile(
                            relativePath,
                            data,
                            fileJson.optLong("lastModified", 0)
                    ));
                }
            }
            storage.put(rootName, files);
        }
        return new BackupSnapshot(preferences, storage);
    }

    private void applySnapshot(BackupSnapshot snapshot) throws IOException {
        restorePreferences(snapshot.preferences);
        restoreStorage(snapshot.storage);
    }

    private void restorePreferences(
            Map<String, Map<String, PreferenceValue>> importedPreferences
    ) throws IOException {
        Set<String> allPreferenceNames = new HashSet<>(discoverPreferenceNames());
        allPreferenceNames.addAll(importedPreferences.keySet());

        for (String preferenceName : allPreferenceNames) {
            SharedPreferences preferences =
                    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
            if (!preferences.edit().clear().commit()) {
                throw new IOException("Der Datenspeicher " + preferenceName
                        + " konnte nicht geleert werden.");
            }
        }

        for (Map.Entry<String, Map<String, PreferenceValue>> preference :
                importedPreferences.entrySet()) {
            if (EXCLUDED_PREFERENCE_NAMES.contains(preference.getKey())) {
                continue;
            }
            SharedPreferences.Editor editor = context
                    .getSharedPreferences(preference.getKey(), Context.MODE_PRIVATE)
                    .edit();
            for (Map.Entry<String, PreferenceValue> value : preference.getValue().entrySet()) {
                value.getValue().put(editor, value.getKey());
            }
            if (!editor.commit()) {
                throw new IOException("Der Datenspeicher " + preference.getKey()
                        + " konnte nicht wiederhergestellt werden.");
            }
        }
    }

    private void restoreStorage(Map<String, List<StoredFile>> storage) throws IOException {
        Map<String, File> roots = storageRoots();
        for (Map.Entry<String, File> root : roots.entrySet()) {
            File rootDirectory = root.getValue();
            if (rootDirectory == null) {
                continue;
            }
            if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
                throw new IOException("Ein interner Speicherordner konnte nicht erstellt werden.");
            }
            clearDirectory(rootDirectory);
            List<StoredFile> files = storage.getOrDefault(
                    root.getKey(),
                    Collections.emptyList()
            );
            for (StoredFile file : files) {
                writeStoredFile(rootDirectory, file);
            }
        }
    }

    private void clearDirectory(File directory) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            deleteRecursively(child);
        }
    }

    private void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Eine vorhandene App-Datei konnte nicht ersetzt werden.");
        }
    }

    private void writeStoredFile(File root, StoredFile storedFile) throws IOException {
        File target = new File(root, storedFile.relativePath);
        String rootPath = root.getCanonicalPath();
        String targetPath = target.getCanonicalPath();
        if (!targetPath.startsWith(rootPath + File.separator)) {
            throw new IOException("Ungültiger Dateipfad in der Sicherung.");
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Ein interner Unterordner konnte nicht erstellt werden.");
        }
        try (BufferedOutputStream output =
                     new BufferedOutputStream(new FileOutputStream(target))) {
            output.write(storedFile.data);
        }
        if (storedFile.lastModified > 0) {
            target.setLastModified(storedFile.lastModified);
        }
    }

    private byte[] readFile(File file) throws IOException {
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            return readWithLimit(input, (int) Math.min(MAX_DECODED_FILE_BYTES, Integer.MAX_VALUE));
        }
    }

    private byte[] readWithLimit(InputStream inputStream, int maxBytes) throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(inputStream);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Die ausgewählte Datei überschreitet die erlaubte Größe.");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private String relativePath(File root, File file) throws IOException {
        String rootPath = root.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (!filePath.startsWith(rootPath + File.separator)) {
            throw new IOException("Eine interne Datei liegt außerhalb des App-Speichers.");
        }
        return filePath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
    }

    private String validatePreferenceName(String value) throws JSONException {
        if (value == null || value.isEmpty()
                || value.contains("/") || value.contains("\\") || value.contains("..")) {
            throw new JSONException("Ungültiger Datenspeichername.");
        }
        return value;
    }

    private String validateRelativePath(String value) throws JSONException {
        if (value == null || value.isEmpty()
                || value.startsWith("/") || value.startsWith("\\")
                || value.contains("../") || value.contains("..\\")
                || value.contains(":")) {
            throw new JSONException("Ungültiger Dateipfad.");
        }
        return value.replace('\\', '/');
    }

    private String utcTimestamp() {
        SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    public static final class ExportSummary {
        public final int preferenceFiles;
        public final int preferenceValues;
        public final int internalFiles;
        public final int jsonBytes;

        private ExportSummary(
                int preferenceFiles,
                int preferenceValues,
                int internalFiles,
                int jsonBytes
        ) {
            this.preferenceFiles = preferenceFiles;
            this.preferenceValues = preferenceValues;
            this.internalFiles = internalFiles;
            this.jsonBytes = jsonBytes;
        }
    }

    public static final class PreparedBackup {
        private final BackupSnapshot snapshot;
        public final int preferenceFiles;
        public final int preferenceValues;
        public final int internalFiles;
        public final int jsonBytes;
        public final String createdAt;

        private PreparedBackup(
                BackupSnapshot snapshot,
                int preferenceFiles,
                int preferenceValues,
                int internalFiles,
                int jsonBytes,
                String createdAt
        ) {
            this.snapshot = snapshot;
            this.preferenceFiles = preferenceFiles;
            this.preferenceValues = preferenceValues;
            this.internalFiles = internalFiles;
            this.jsonBytes = jsonBytes;
            this.createdAt = createdAt;
        }
    }

    private static final class BackupSnapshot {
        final Map<String, Map<String, PreferenceValue>> preferences;
        final Map<String, List<StoredFile>> storage;

        BackupSnapshot(
                Map<String, Map<String, PreferenceValue>> preferences,
                Map<String, List<StoredFile>> storage
        ) {
            this.preferences = preferences;
            this.storage = storage;
        }

        int preferenceCount() {
            return preferences.size();
        }

        int preferenceValueCount() {
            int count = 0;
            for (Map<String, PreferenceValue> values : preferences.values()) {
                count += values.size();
            }
            return count;
        }

        int fileCount() {
            int count = 0;
            for (List<StoredFile> files : storage.values()) {
                count += files.size();
            }
            return count;
        }
    }

    private static final class StoredFile {
        final String relativePath;
        final byte[] data;
        final long lastModified;

        StoredFile(String relativePath, byte[] data, long lastModified) {
            this.relativePath = relativePath;
            this.data = data;
            this.lastModified = lastModified;
        }
    }

    private static final class PreferenceValue {
        private final String type;
        private final Object value;

        private PreferenceValue(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        static PreferenceValue from(Object value) throws IOException {
            if (value instanceof String) {
                return new PreferenceValue("string", value);
            }
            if (value instanceof Integer) {
                return new PreferenceValue("int", value);
            }
            if (value instanceof Long) {
                return new PreferenceValue("long", value);
            }
            if (value instanceof Float) {
                return new PreferenceValue("float", value);
            }
            if (value instanceof Boolean) {
                return new PreferenceValue("boolean", value);
            }
            if (value instanceof Set<?>) {
                Set<String> strings = new LinkedHashSet<>();
                for (Object item : (Set<?>) value) {
                    if (!(item instanceof String)) {
                        throw new IOException("Eine Einstellungsmenge enthält ungültige Werte.");
                    }
                    strings.add((String) item);
                }
                return new PreferenceValue("string_set", strings);
            }
            throw new IOException("Nicht unterstützter Einstellungsdatentyp.");
        }

        static PreferenceValue fromJson(JSONObject json) throws JSONException {
            String type = json.getString("type");
            switch (type) {
                case "string":
                    return new PreferenceValue(type, json.getString("value"));
                case "int":
                    return new PreferenceValue(type, json.getInt("value"));
                case "long":
                    return new PreferenceValue(type, json.getLong("value"));
                case "float":
                    return new PreferenceValue(type, (float) json.getDouble("value"));
                case "boolean":
                    return new PreferenceValue(type, json.getBoolean("value"));
                case "string_set":
                    JSONArray array = json.getJSONArray("value");
                    Set<String> values = new LinkedHashSet<>();
                    for (int index = 0; index < array.length(); index++) {
                        values.add(array.getString(index));
                    }
                    return new PreferenceValue(type, values);
                default:
                    throw new JSONException("Unbekannter Einstellungsdatentyp.");
            }
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            if ("string_set".equals(type)) {
                JSONArray array = new JSONArray();
                List<String> sorted = new ArrayList<>((Set<String>) value);
                Collections.sort(sorted);
                for (String item : sorted) {
                    array.put(item);
                }
                json.put("value", array);
            } else {
                json.put("value", value);
            }
            return json;
        }

        void put(SharedPreferences.Editor editor, String key) throws IOException {
            switch (type) {
                case "string":
                    editor.putString(key, (String) value);
                    break;
                case "int":
                    editor.putInt(key, (Integer) value);
                    break;
                case "long":
                    editor.putLong(key, (Long) value);
                    break;
                case "float":
                    editor.putFloat(key, (Float) value);
                    break;
                case "boolean":
                    editor.putBoolean(key, (Boolean) value);
                    break;
                case "string_set":
                    editor.putStringSet(key, new LinkedHashSet<>((Set<String>) value));
                    break;
                default:
                    throw new IOException("Unbekannter Einstellungsdatentyp.");
            }
        }
    }
}
