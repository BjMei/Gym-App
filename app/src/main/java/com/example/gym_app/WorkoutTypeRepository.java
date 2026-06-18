package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WorkoutTypeRepository {

    public static final String PREFS_NAME = "WorkoutTypes";

    public static final String FOCUS_UPPER = "upper";
    public static final String FOCUS_LOWER = "lower";
    public static final String FOCUS_FULL = "full";
    public static final String FOCUS_PUSH = "push_focus";
    public static final String FOCUS_PULL = "pull_focus";
    public static final String FOCUS_CORE = "core";
    public static final String FOCUS_OTHER = "other";

    private static final String KEY_CUSTOM_TYPES = "custom_types";
    private static final String CUSTOM_PREFIX = "custom_";
    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_DESCRIPTION_LENGTH = 120;

    private static final List<WorkoutType> BUILT_INS = Collections.unmodifiableList(
            Arrays.asList(
                    new WorkoutType(
                            WorkoutStorage.TYPE_PUSH,
                            "Push",
                            "BRUST · SCHULTER · TRIZEPS",
                            "Drückbewegungen und Oberkörperkraft",
                            FOCUS_PUSH,
                            true,
                            true
                    ),
                    new WorkoutType(
                            WorkoutStorage.TYPE_PULL,
                            "Pull",
                            "RÜCKEN · BIZEPS · CORE",
                            "Zugbewegungen und starke Rückenkette",
                            FOCUS_PULL,
                            true,
                            true
                    ),
                    new WorkoutType(
                            WorkoutStorage.TYPE_LEG,
                            "Leg",
                            "BEINE · GLUTEUS · WADEN",
                            "Unterkörperkraft und stabile Basis",
                            FOCUS_LOWER,
                            true,
                            true
                    )
            )
    );

    private WorkoutTypeRepository() {
    }

    public static List<WorkoutType> getActiveTypes(Context context) {
        List<WorkoutType> result = new ArrayList<>(BUILT_INS);
        for (WorkoutType type : readCustomTypes(context)) {
            if (type.active) {
                result.add(type);
            }
        }
        return result;
    }

    public static List<WorkoutType> getAllTypes(Context context) {
        List<WorkoutType> result = new ArrayList<>(BUILT_INS);
        result.addAll(readCustomTypes(context));
        return result;
    }

    public static List<String> getActiveTypeIds(Context context) {
        return ids(getActiveTypes(context));
    }

    public static List<String> getAllTypeIds(Context context) {
        return ids(getAllTypes(context));
    }

    public static WorkoutType find(Context context, String id) {
        String safeId = normalizeId(id);
        if (safeId.isEmpty()) {
            return null;
        }
        for (WorkoutType type : getAllTypes(context)) {
            if (type.id.equals(safeId)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isKnownType(Context context, String id) {
        return find(context, id) != null;
    }

    public static boolean isSafeTypeId(String id) {
        String safeId = normalizeId(id);
        if (WorkoutStorage.TYPE_PUSH.equals(safeId)
                || WorkoutStorage.TYPE_PULL.equals(safeId)
                || WorkoutStorage.TYPE_LEG.equals(safeId)) {
            return true;
        }
        if (!safeId.startsWith(CUSTOM_PREFIX) || safeId.length() > 80) {
            return false;
        }
        for (int i = 0; i < safeId.length(); i++) {
            char value = safeId.charAt(i);
            if (!(value >= 'a' && value <= 'z')
                    && !(value >= '0' && value <= '9')
                    && value != '_'
                    && value != '-') {
                return false;
            }
        }
        return true;
    }

    public static String label(Context context, String id) {
        WorkoutType type = find(context, id);
        if (type != null) {
            return disambiguatedLabel(context, type);
        }
        String safeId = normalizeId(id);
        if (safeId.isEmpty()) {
            return "Training";
        }
        return safeId.replace(CUSTOM_PREFIX, "")
                .replace('_', ' ')
                .trim();
    }

    private static String disambiguatedLabel(Context context, WorkoutType selected) {
        List<WorkoutType> matching = new ArrayList<>();
        for (WorkoutType type : getAllTypes(context)) {
            if (type.name.equalsIgnoreCase(selected.name)) {
                matching.add(type);
            }
        }
        if (matching.size() <= 1) {
            return selected.name;
        }
        if (selected.active) {
            return selected.name + " (aktiv)";
        }
        int archivedIndex = 0;
        int archivedCount = 0;
        for (WorkoutType type : matching) {
            if (type.active) {
                continue;
            }
            archivedCount++;
            if (type.id.equals(selected.id)) {
                archivedIndex = archivedCount;
            }
        }
        return selected.name + " (archiviert "
                + Math.max(1, archivedIndex)
                + "/" + archivedCount + ")";
    }

    public static WorkoutType create(
            Context context,
            String name,
            String description,
            String focus
    ) {
        String safeName = normalizeName(name);
        if (safeName.isEmpty() || containsActiveName(context, safeName, null)) {
            return null;
        }
        WorkoutType created = new WorkoutType(
                CUSTOM_PREFIX + UUID.randomUUID().toString().replace("-", ""),
                safeName,
                focusLabel(focus).toUpperCase(Locale.GERMANY),
                normalizeDescription(description, focus),
                normalizeFocus(focus),
                false,
                true
        );
        List<WorkoutType> customTypes = readCustomTypes(context);
        customTypes.add(created);
        return writeCustomTypes(context, customTypes) ? created : null;
    }

    public static boolean update(
            Context context,
            String id,
            String name,
            String description,
            String focus
    ) {
        String safeId = normalizeId(id);
        String safeName = normalizeName(name);
        if (!isSafeTypeId(safeId)
                || !safeId.startsWith(CUSTOM_PREFIX)
                || safeName.isEmpty()
                || containsActiveName(context, safeName, safeId)) {
            return false;
        }
        List<WorkoutType> customTypes = readCustomTypes(context);
        boolean changed = false;
        for (int i = 0; i < customTypes.size(); i++) {
            WorkoutType current = customTypes.get(i);
            if (!current.id.equals(safeId)) {
                continue;
            }
            customTypes.set(i, new WorkoutType(
                    current.id,
                    safeName,
                    focusLabel(focus).toUpperCase(Locale.GERMANY),
                    normalizeDescription(description, focus),
                    normalizeFocus(focus),
                    false,
                    current.active
            ));
            changed = true;
            break;
        }
        return changed && writeCustomTypes(context, customTypes);
    }

    public static boolean archive(Context context, String id) {
        String safeId = normalizeId(id);
        List<WorkoutType> customTypes = readCustomTypes(context);
        boolean changed = false;
        for (int i = 0; i < customTypes.size(); i++) {
            WorkoutType current = customTypes.get(i);
            if (!current.id.equals(safeId) || !current.active) {
                continue;
            }
            customTypes.set(i, current.withActive(false));
            changed = true;
            break;
        }
        return changed && writeCustomTypes(context, customTypes);
    }

    public static String focusLabel(String focus) {
        switch (normalizeFocus(focus)) {
            case FOCUS_UPPER:
                return "Oberkörper";
            case FOCUS_LOWER:
                return "Unterkörper";
            case FOCUS_FULL:
                return "Ganzkörper";
            case FOCUS_PUSH:
                return "Push-orientiert";
            case FOCUS_PULL:
                return "Pull-orientiert";
            case FOCUS_CORE:
                return "Core";
            default:
                return "Freier Fokus";
        }
    }

    public static String[] focusValues() {
        return new String[]{
                FOCUS_UPPER,
                FOCUS_LOWER,
                FOCUS_FULL,
                FOCUS_PUSH,
                FOCUS_PULL,
                FOCUS_CORE,
                FOCUS_OTHER
        };
    }

    public static String[] focusLabels() {
        String[] values = focusValues();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = focusLabel(values[i]);
        }
        return labels;
    }

    public static int focusIndex(String focus) {
        String normalized = normalizeFocus(focus);
        String[] values = focusValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(normalized)) {
                return i;
            }
        }
        return values.length - 1;
    }

    private static boolean containsActiveName(
            Context context,
            String name,
            String excludedId
    ) {
        for (WorkoutType type : getActiveTypes(context)) {
            if (type.id.equals(excludedId)) {
                continue;
            }
            if (type.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> ids(List<WorkoutType> types) {
        List<String> result = new ArrayList<>();
        for (WorkoutType type : types) {
            result.add(type.id);
        }
        return result;
    }

    private static List<WorkoutType> readCustomTypes(Context context) {
        List<WorkoutType> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = preferences.getString(KEY_CUSTOM_TYPES, "[]");
        try {
            JSONArray array = new JSONArray(stored == null ? "[]" : stored);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                WorkoutType type = new WorkoutType(
                        normalizeId(object.getString("id")),
                        normalizeName(object.getString("name")),
                        object.optString("focusLabel", ""),
                        object.optString("description", ""),
                        normalizeFocus(object.optString("focus", FOCUS_OTHER)),
                        false,
                        object.optBoolean("active", true)
                );
                if (isSafeTypeId(type.id)
                        && type.id.startsWith(CUSTOM_PREFIX)
                        && !type.name.isEmpty()) {
                    result.add(type);
                }
            }
        } catch (JSONException exception) {
            AppDataCorruptionTracker.record(PREFS_NAME + "/" + KEY_CUSTOM_TYPES, exception);
        }
        return result;
    }

    private static boolean writeCustomTypes(
            Context context,
            List<WorkoutType> types
    ) {
        JSONArray array = new JSONArray();
        for (WorkoutType type : types) {
            try {
                JSONObject object = new JSONObject();
                object.put("id", type.id);
                object.put("name", type.name);
                object.put("focusLabel", type.focusLabel);
                object.put("description", type.description);
                object.put("focus", type.focus);
                object.put("active", type.active);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_TYPES, array.toString())
                .commit();
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= MAX_NAME_LENGTH
                ? normalized
                : normalized.substring(0, MAX_NAME_LENGTH).trim();
    }

    private static String normalizeDescription(String value, String focus) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            normalized = defaultDescription(focus);
        }
        return normalized.length() <= MAX_DESCRIPTION_LENGTH
                ? normalized
                : normalized.substring(0, MAX_DESCRIPTION_LENGTH).trim();
    }

    private static String normalizeFocus(String value) {
        if (FOCUS_UPPER.equals(value)
                || FOCUS_LOWER.equals(value)
                || FOCUS_FULL.equals(value)
                || FOCUS_PUSH.equals(value)
                || FOCUS_PULL.equals(value)
                || FOCUS_CORE.equals(value)) {
            return value;
        }
        return FOCUS_OTHER;
    }

    private static String defaultDescription(String focus) {
        switch (normalizeFocus(focus)) {
            case FOCUS_UPPER:
                return "Dein individueller Oberkörpertag";
            case FOCUS_LOWER:
                return "Dein individueller Unterkörpertag";
            case FOCUS_FULL:
                return "Kraft und Bewegung für den ganzen Körper";
            case FOCUS_PUSH:
                return "Individuelle Drückbewegungen und Kraft";
            case FOCUS_PULL:
                return "Individuelle Zugbewegungen und Kraft";
            case FOCUS_CORE:
                return "Rumpfkraft, Stabilität und Kontrolle";
            default:
                return "Dein individuell zusammengestelltes Training";
        }
    }

    public static final class WorkoutType {
        public final String id;
        public final String name;
        public final String focusLabel;
        public final String description;
        public final String focus;
        public final boolean builtIn;
        public final boolean active;

        WorkoutType(
                String id,
                String name,
                String focusLabel,
                String description,
                String focus,
                boolean builtIn,
                boolean active
        ) {
            this.id = id;
            this.name = name;
            this.focusLabel = focusLabel == null || focusLabel.trim().isEmpty()
                    ? WorkoutTypeRepository.focusLabel(focus).toUpperCase(Locale.GERMANY)
                    : focusLabel.trim();
            this.description = description == null ? "" : description.trim();
            this.focus = normalizeFocus(focus);
            this.builtIn = builtIn;
            this.active = active;
        }

        WorkoutType withActive(boolean active) {
            return new WorkoutType(
                    id,
                    name,
                    focusLabel,
                    description,
                    focus,
                    builtIn,
                    active
            );
        }
    }
}
