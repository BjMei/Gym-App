package com.example.gym_app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PushActivity extends AppCompatActivity {

    private Spinner spinnerExercise;
    private ImageButton btnExerciseSettings;
    private ImageButton btnManageExercises;
    private EditText[] etWeights;
    private EditText[] etReps;
    private ImageButton btnSave;
    private Spinner spinnerCardio;
    private EditText etCardioMinutes;
    private ImageButton btnSaveCardio;
    private ImageButton btnManageCardio;
    private LinearLayout llEntries;
    private LinearLayout llLastWorkout;
    private TextView tvLastWorkoutData;
    private List<WorkoutEntry> workoutEntries;
    private List<CardioEntry> cardioEntries;
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> cardioAdapter;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ExerciseSettings";
    private static final int EXERCISE_ARRAY_RES = R.array.push_exercises;
    private static final String WORKOUT_TYPE = WorkoutStorage.TYPE_PUSH;
    private static final int CARDIO_ARRAY_RES = R.array.cardio_exercises;
    private static final String CARDIO_TYPE = WORKOUT_TYPE + "_cardio";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        // Views initialisieren
        spinnerExercise = findViewById(R.id.spinnerExercise);
        btnExerciseSettings = findViewById(R.id.btnExerciseSettings);
        btnManageExercises = findViewById(R.id.btnManageExercises);
        btnSave = findViewById(R.id.btnSave);
        spinnerCardio = findViewById(R.id.spinnerCardio);
        etCardioMinutes = findViewById(R.id.etCardioMinutes);
        btnSaveCardio = findViewById(R.id.btnSaveCardio);
        btnManageCardio = findViewById(R.id.btnManageCardio);
        llEntries = findViewById(R.id.llEntries);
        llLastWorkout = findViewById(R.id.llLastWorkout);
        tvLastWorkoutData = findViewById(R.id.tvLastWorkoutData);

        // SharedPreferences initialisieren
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupExerciseSpinner();

        setupCardioSpinner();

        // Arrays für die 4 Sätze initialisieren
        etWeights = new EditText[]{
            findViewById(R.id.etWeight1),
            findViewById(R.id.etWeight2),
            findViewById(R.id.etWeight3),
            findViewById(R.id.etWeight4)
        };

        etReps = new EditText[]{
            findViewById(R.id.etReps1),
            findViewById(R.id.etReps2),
            findViewById(R.id.etReps3),
            findViewById(R.id.etReps4)
        };

        // Listen für Einträge initialisieren
        workoutEntries = new ArrayList<>();
        cardioEntries = new ArrayList<>();

        // Button-Click-Listener
        btnSave.setOnClickListener(v -> saveEntry());
        btnSaveCardio.setOnClickListener(v -> saveCardioEntry());
        btnManageCardio.setOnClickListener(v -> showManageCardioDialog());
        btnExerciseSettings.setOnClickListener(v -> showExerciseSettingsDialog());
        btnManageExercises.setOnClickListener(v -> showManageExercisesDialog());

        // Button für gespeicherte Trainings
        ImageButton btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(PushActivity.this, TrainingHistoryActivity.class);
            intent.putExtra(TrainingHistoryActivity.EXTRA_WORKOUT_TYPE, WORKOUT_TYPE);
            intent.putExtra(TrainingHistoryActivity.EXTRA_WORKOUT_TITLE, "PUSH DAY - Trainings");
            startActivity(intent);
        });

        // Spinner-Listener für Anzeige der letzten Trainingsdaten
        spinnerExercise.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object selectedExerciseObj = spinnerExercise.getSelectedItem();
                if (selectedExerciseObj == null) {
                    llLastWorkout.setVisibility(View.GONE);
                    return;
                }
                String selectedExercise = selectedExerciseObj.toString();
                loadLastWorkout(selectedExercise);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                llLastWorkout.setVisibility(View.GONE);
            }
        });

        // Initial die erste Übung laden
        if (spinnerExercise.getCount() > 0) {
            loadLastWorkout(spinnerExercise.getItemAtPosition(0).toString());
        }
    }

    private void loadLastWorkout(String exercise) {
        WorkoutStorage.LastWorkout lastWorkout = WorkoutStorage.getLastWorkout(this, WORKOUT_TYPE, exercise);
        if (lastWorkout != null && lastWorkout.sets != null && !lastWorkout.sets.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(lastWorkout.timestamp).append("\n");
            for (int i = 0; i < lastWorkout.sets.size(); i++) {
                WorkoutStorage.WorkoutSet set = lastWorkout.sets.get(i);
                sb.append(String.format(Locale.getDefault(), "Satz %d: %.1f kg × %d", 
                    i + 1, set.weight, set.reps));
                if (i < lastWorkout.sets.size() - 1) {
                    sb.append(" | ");
                }
            }
            tvLastWorkoutData.setText(sb.toString());
            llLastWorkout.setVisibility(View.VISIBLE);
        } else {
            llLastWorkout.setVisibility(View.GONE);
        }
    }

    private void setupExerciseSpinner() {
        List<String> exercises = ExerciseCatalog.getExercises(this, EXERCISE_ARRAY_RES, WORKOUT_TYPE);
        adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, exercises);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerExercise.setAdapter(adapter);
    }

    private void refreshExerciseSpinner(String preferredExercise) {
        List<String> exercises = ExerciseCatalog.getExercises(this, EXERCISE_ARRAY_RES, WORKOUT_TYPE);
        adapter.clear();
        adapter.addAll(exercises);
        adapter.notifyDataSetChanged();

        if (adapter.isEmpty()) {
            llLastWorkout.setVisibility(View.GONE);
            return;
        }

        int index = 0;
        if (preferredExercise != null && !preferredExercise.trim().isEmpty()) {
            for (int i = 0; i < adapter.getCount(); i++) {
                String item = adapter.getItem(i);
                if (item != null && item.equalsIgnoreCase(preferredExercise.trim())) {
                    index = i;
                    break;
                }
            }
        }
        spinnerExercise.setSelection(index);
    }

    private void setupCardioSpinner() {
        List<String> cardioExercises = ExerciseCatalog.getExercises(this, CARDIO_ARRAY_RES, CARDIO_TYPE);
        cardioAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, cardioExercises);
        cardioAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerCardio.setAdapter(cardioAdapter);
    }

    private void refreshCardioSpinner(String preferredExercise) {
        List<String> cardioExercises = ExerciseCatalog.getExercises(this, CARDIO_ARRAY_RES, CARDIO_TYPE);
        cardioAdapter.clear();
        cardioAdapter.addAll(cardioExercises);
        cardioAdapter.notifyDataSetChanged();

        if (cardioAdapter.isEmpty()) {
            return;
        }

        int index = 0;
        if (preferredExercise != null && !preferredExercise.trim().isEmpty()) {
            for (int i = 0; i < cardioAdapter.getCount(); i++) {
                String item = cardioAdapter.getItem(i);
                if (item != null && item.equalsIgnoreCase(preferredExercise.trim())) {
                    index = i;
                    break;
                }
            }
        }
        spinnerCardio.setSelection(index);
    }

    private void showManageExercisesDialog() {
        showManageListDialog(
                "Übungen verwalten",
                EXERCISE_ARRAY_RES,
                WORKOUT_TYPE,
                () -> {
                    Object selected = spinnerExercise.getSelectedItem();
                    if (selected != null) {
                        loadLastWorkout(selected.toString());
                    } else {
                        llLastWorkout.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void showManageCardioDialog() {
        showManageListDialog(
                "Cardio verwalten",
                CARDIO_ARRAY_RES,
                CARDIO_TYPE,
                () -> {
                }
        );
    }

    private void showManageListDialog(String title,
                                      int defaultArrayRes,
                                      String listKey,
                                      Runnable afterUpdate) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_manage_items);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setAttributes(params);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvManageTitle);
        EditText etNewItem = dialog.findViewById(R.id.etNewItem);
        ImageButton btnAdd = dialog.findViewById(R.id.btnAddItem);
        Button btnClose = dialog.findViewById(R.id.btnCloseManageDialog);
        LinearLayout llItems = dialog.findViewById(R.id.llManageItems);
        ScrollView svItems = dialog.findViewById(R.id.svManageItems);

        tvTitle.setText(title);

        Runnable renderList = () -> {
            llItems.removeAllViews();
            List<String> items = ExerciseCatalog.getExercises(this, defaultArrayRes, listKey);
            for (String item : items) {
                View row = createManageRow(dialog, item, defaultArrayRes, listKey, afterUpdate);
                llItems.addView(row);
            }
            svItems.post(() -> svItems.scrollTo(0, 0));
        };

        btnAdd.setOnClickListener(v -> {
            String newItem = etNewItem.getText().toString().trim();
            if (newItem.isEmpty()) {
                Toast.makeText(this, "Bitte Namen eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ExerciseCatalog.addExercise(this, defaultArrayRes, listKey, newItem)) {
                Toast.makeText(this, "Eintrag existiert bereits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listKey.equals(WORKOUT_TYPE)) {
                refreshExerciseSpinner(newItem);
            } else {
                refreshCardioSpinner(newItem);
            }
            if (afterUpdate != null) {
                afterUpdate.run();
            }

            etNewItem.setText("");
            renderList.run();
            Toast.makeText(this, "Hinzugefügt", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        renderList.run();
        dialog.show();
    }

    private View createManageRow(Dialog parentDialog,
                                 String item,
                                 int defaultArrayRes,
                                 String listKey,
                                 Runnable afterUpdate) {
        float density = getResources().getDisplayMetrics().density;
        int minHeight = Math.round(56 * density);
        int padStart = Math.round(18 * density);
        int padTopBottom = Math.round(12 * density);
        int padEnd = Math.round(10 * density);
        int marginBottom = Math.round(12 * density);
        int deleteSize = Math.round(44 * density);
        int deletePad = Math.round(10 * density);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(minHeight);
        row.setPadding(padStart, padTopBottom, padEnd, padTopBottom);
        row.setBackground(getResources().getDrawable(R.drawable.rounded_input, null));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = marginBottom;
        row.setLayoutParams(rowParams);

        TextView tvName = new TextView(this);
        tvName.setText(item);
        tvName.setTextColor(getResources().getColor(R.color.text_primary, null));
        tvName.setTextSize(16);
        tvName.setGravity(Gravity.CENTER_VERTICAL);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);
        row.addView(tvName);

        ImageButton btnDelete = new ImageButton(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(deleteSize, deleteSize);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setBackground(getResources().getDrawable(R.drawable.rounded_input, null));
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        btnDelete.setPadding(deletePad, deletePad, deletePad, deletePad);
        btnDelete.setColorFilter(getResources().getColor(R.color.gold_primary, null));
        btnDelete.setContentDescription("Löschen");
        row.addView(btnDelete);

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(item, () -> {
            if (ExerciseCatalog.removeExercise(this, defaultArrayRes, listKey, item)) {
                if (listKey.equals(WORKOUT_TYPE)) {
                    refreshExerciseSpinner(null);
                } else {
                    refreshCardioSpinner(null);
                }
                if (afterUpdate != null) {
                    afterUpdate.run();
                }

                parentDialog.dismiss();
                if (listKey.equals(WORKOUT_TYPE)) {
                    showManageExercisesDialog();
                } else {
                    showManageCardioDialog();
                }
            }
        }));

        return row;
    }

    private void showDeleteConfirmDialog(String itemName, Runnable onConfirm) {
        Dialog confirmDialog = new Dialog(this);
        confirmDialog.setContentView(R.layout.dialog_confirm_delete);

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = confirmDialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            confirmDialog.getWindow().setAttributes(params);
        }

        TextView tvMessage = confirmDialog.findViewById(R.id.tvDeleteMessage);
        Button btnCancel = confirmDialog.findViewById(R.id.btnDeleteCancel);
        Button btnConfirm = confirmDialog.findViewById(R.id.btnDeleteConfirm);

        tvMessage.setText("'" + itemName + "' wirklich löschen?");
        btnCancel.setOnClickListener(v -> confirmDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
            confirmDialog.dismiss();
        });

        confirmDialog.show();
    }

    private void showExerciseSettingsDialog() {
        Object selectedExerciseObj = spinnerExercise.getSelectedItem();
        if (selectedExerciseObj == null) {
            Toast.makeText(this, "Keine Übung verfügbar", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedExercise = selectedExerciseObj.toString();
        
        // Dialog erstellen
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_exercise_settings);
        
        // Dialog-Größe und Hintergrund anpassen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setAttributes(params);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(String.format("Einstellungen: %s", selectedExercise));

        EditText etSeatPositions = dialog.findViewById(R.id.etSeatPositions);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        ImageButton btnSaveSettings = dialog.findViewById(R.id.btnSaveSettings);

        // Gespeicherte Einstellungen laden
        String savedPositions = sharedPreferences.getString(selectedExercise, "");
        etSeatPositions.setText(savedPositions);

        // Cancel-Button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save-Button
        btnSaveSettings.setOnClickListener(v -> {
            String positions = etSeatPositions.getText().toString().trim();
            
            // In SharedPreferences speichern
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(selectedExercise, positions);
            editor.apply();

            Toast.makeText(this, String.format("Einstellungen für '%s' gespeichert", selectedExercise), 
                Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveEntry() {
        // Übung aus Spinner auslesen
        Object selectedExerciseObj = spinnerExercise.getSelectedItem();
        String exercise = selectedExerciseObj == null ? "" : selectedExerciseObj.toString();

        // Validierung: Übung muss ausgewählt sein (Spinner sollte immer einen Wert haben, aber zur Sicherheit)
        if (exercise == null || exercise.isEmpty()) {
            Toast.makeText(this, "Bitte Übung auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Arrays für die 4 Sätze
        List<Set> sets = new ArrayList<>();

        // Alle 4 Sätze durchgehen - alle müssen ausgefüllt sein
        for (int i = 0; i < 4; i++) {
            String weightStr = etWeights[i].getText().toString().trim();
            String repsStr = etReps[i].getText().toString().trim();

            // Alle 4 Sätze müssen ausgefüllt sein
            if (weightStr.isEmpty() || repsStr.isEmpty()) {
                Toast.makeText(this, String.format("Bitte Satz %d vollständig ausfüllen", i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                int reps = Integer.parseInt(repsStr);
                sets.add(new Set(weight, reps));
            } catch (NumberFormatException e) {
                Toast.makeText(this, String.format("Ungültige Eingabe für Satz %d", i + 1), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Eintrag erstellen und zur Liste hinzufügen
        WorkoutEntry entry = new WorkoutEntry(exercise, sets);
        workoutEntries.add(entry);

        // Eintrag in der UI anzeigen
        addEntryToView(entry);
        saveWorkoutSummary(exercise, sets);

        // Strukturierte Daten speichern
        List<WorkoutStorage.WorkoutSet> storageSets = new ArrayList<>();
        for (Set set : sets) {
            storageSets.add(new WorkoutStorage.WorkoutSet(set.getWeight(), set.getReps()));
        }
        WorkoutStorage.saveDetailedWorkout(this, WORKOUT_TYPE, exercise, storageSets);

        // Eingabefelder zurücksetzen
        clearInputFields();

        // Letztes Training aktualisieren
        loadLastWorkout(exercise);

        Toast.makeText(this, String.format("Übung '%s' gespeichert (%d Sätze)", 
            exercise, sets.size()), Toast.LENGTH_SHORT).show();
    }

    private void saveWorkoutSummary(String exercise, List<Set> sets) {
        StringBuilder builder = new StringBuilder();
        builder.append(exercise).append(": ");
        for (int i = 0; i < sets.size(); i++) {
            Set set = sets.get(i);
            builder.append(String.format(Locale.getDefault(), "%.1f kg × %d", set.getWeight(), set.getReps()));
            if (i < sets.size() - 1) {
                builder.append(" | ");
            }
        }
        String timestamp = new SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(new Date());
        builder.append(" • ").append(timestamp);
        WorkoutStorage.addWorkout(this, WORKOUT_TYPE, builder.toString());
    }

    private void clearInputFields() {
        // Spinner auf erste Position zurücksetzen
        spinnerExercise.setSelection(0);
        // Gewichts- und Wiederholungsfelder zurücksetzen
        for (int i = 0; i < 4; i++) {
            etWeights[i].setText("");
            etReps[i].setText("");
        }
    }

    private void addEntryToView(WorkoutEntry entry) {
        // Container für die Übung erstellen mit Card-Design
        LinearLayout exerciseContainer = new LinearLayout(this);
        exerciseContainer.setOrientation(LinearLayout.VERTICAL);
        exerciseContainer.setPadding(20, 20, 20, 20);
        exerciseContainer.setBackground(getResources().getDrawable(R.drawable.rounded_card, null));
        
        // Elevation für Schatten-Effekt
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            exerciseContainer.setElevation(12f);
        }

        // Übungsname mit Nummer
        int exerciseNumber = workoutEntries.size();
        TextView exerciseName = new TextView(this);
        exerciseName.setText(String.format("%d. %s", exerciseNumber, entry.getExercise()));
        exerciseName.setTextColor(getResources().getColor(R.color.text_primary, null));
        exerciseName.setTextSize(24);
        exerciseName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        exerciseName.setPadding(0, 0, 0, 16);
        exerciseName.setLetterSpacing(0.05f);
        exerciseContainer.addView(exerciseName);

        // Divider-Linie
        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        );
        dividerParams.setMargins(0, 0, 0, 12);
        divider.setLayoutParams(dividerParams);
        exerciseContainer.addView(divider);

        // Sätze anzeigen
        for (int i = 0; i < entry.getSets().size(); i++) {
            Set set = entry.getSets().get(i);
            TextView setView = new TextView(this);
            setView.setText(String.format("Satz %d: %.1f kg × %d Wiederholungen", 
                i + 1, set.getWeight(), set.getReps()));
            setView.setTextColor(getResources().getColor(R.color.text_secondary, null));
            setView.setTextSize(17);
            setView.setPadding(0, 10, 0, 10);
            setView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            exerciseContainer.addView(setView);
        }

        // Margin hinzufügen
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        exerciseContainer.setLayoutParams(params);

        // Zur LinearLayout hinzufügen
        llEntries.addView(exerciseContainer);
    }

    private void saveCardioEntry() {
        // Cardio-Übung aus Spinner auslesen
        Object selectedCardioObj = spinnerCardio.getSelectedItem();
        String cardioExercise = selectedCardioObj == null ? "" : selectedCardioObj.toString();
        String minutesStr = etCardioMinutes.getText().toString().trim();

        // Validierung
        if (cardioExercise == null || cardioExercise.isEmpty()) {
            Toast.makeText(this, "Bitte Cardio-Übung auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minutesStr.isEmpty()) {
            Toast.makeText(this, "Bitte Zeit in Minuten eingeben", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int minutes = Integer.parseInt(minutesStr);
            if (minutes <= 0) {
                Toast.makeText(this, "Bitte eine gültige Zeit eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cardio-Eintrag erstellen, anzeigen und speichern
            CardioEntry entry = new CardioEntry(cardioExercise, minutes);
            cardioEntries.add(entry);
            WorkoutStorage.saveCardioSession(this, WORKOUT_TYPE, cardioExercise, minutes);

            // Eintrag in der UI anzeigen
            addCardioEntryToView(entry);

            // Eingabefelder zurücksetzen
            spinnerCardio.setSelection(0);
            etCardioMinutes.setText("");

            Toast.makeText(this, String.format("Cardio '%s' gespeichert (%d Minuten)", 
                cardioExercise, minutes), Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ungültige Eingabe für Minuten", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCardioEntryToView(CardioEntry entry) {
        // Container für die Cardio-Übung erstellen mit Card-Design
        LinearLayout cardioContainer = new LinearLayout(this);
        cardioContainer.setOrientation(LinearLayout.VERTICAL);
        cardioContainer.setPadding(20, 20, 20, 20);
        cardioContainer.setBackground(getResources().getDrawable(R.drawable.rounded_card, null));
        
        // Elevation für Schatten-Effekt
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cardioContainer.setElevation(12f);
        }

        // Cardio-Übungsname
        TextView cardioName = new TextView(this);
        cardioName.setText(entry.getExercise().toUpperCase());
        cardioName.setTextColor(getResources().getColor(R.color.text_primary, null));
        cardioName.setTextSize(24);
        cardioName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        cardioName.setLetterSpacing(0.1f);
        cardioName.setPadding(0, 0, 0, 16);
        cardioContainer.addView(cardioName);

        // Divider-Linie
        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        );
        dividerParams.setMargins(0, 0, 0, 8);
        divider.setLayoutParams(dividerParams);
        cardioContainer.addView(divider);

        // Zeit anzeigen
        TextView timeView = new TextView(this);
        timeView.setText(String.format("%d Minuten", entry.getMinutes()));
        timeView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        timeView.setTextSize(17);
        timeView.setPadding(0, 10, 0, 10);
        timeView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        cardioContainer.addView(timeView);

        // Margin hinzufügen
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        cardioContainer.setLayoutParams(params);

        // Zur LinearLayout hinzufügen
        llEntries.addView(cardioContainer);
    }

    // Innere Klasse für einen Satz
    private static class Set {
        private double weight;
        private int reps;

        public Set(double weight, int reps) {
            this.weight = weight;
            this.reps = reps;
        }

        public double getWeight() {
            return weight;
        }

        public int getReps() {
            return reps;
        }
    }

    // Innere Klasse für Workout-Einträge
    private static class WorkoutEntry {
        private String exercise;
        private List<Set> sets;

        public WorkoutEntry(String exercise, List<Set> sets) {
            this.exercise = exercise;
            this.sets = sets;
        }

        public String getExercise() {
            return exercise;
        }

        public List<Set> getSets() {
            return sets;
        }
    }

    // Innere Klasse für Cardio-Einträge
    private static class CardioEntry {
        private String exercise;
        private int minutes;

        public CardioEntry(String exercise, int minutes) {
            this.exercise = exercise;
            this.minutes = minutes;
        }

        public String getExercise() {
            return exercise;
        }

        public int getMinutes() {
            return minutes;
        }
    }
}
