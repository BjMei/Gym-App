package com.example.gym_app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PushActivity extends IronxActivity {

    public static final String EXTRA_WORKOUT_TYPE = "custom_workout_type";

    private Spinner spinnerExercise;
    private Spinner spinnerExerciseSecond;
    private LinearLayout btnToggleSecondExercise;
    private ImageView ivAccordionArrow;
    private ImageButton btnExerciseSettings;
    private ImageButton btnManageExercises;
    private TrainingSetInputController primarySetInputs;
    private TrainingSetInputController secondSetInputs;
    private MaterialButton btnSaveTraining;
    private ScrollView trainingScrollView;
    private Spinner spinnerCardio;
    private EditText etCardioMinutes;
    private MaterialButton btnSaveCardio;
    private View btnManageCardio;
    private LinearLayout llEntries;
    private LinearLayout llLastWorkout;
    private TextView tvLastWorkoutData;
    private TextView tvStopwatch;
    private View timerCard;
    private LinearLayout llSecondExerciseSection;
    private LinearLayout llLastWorkoutSecond;
    private TextView tvLastWorkoutDataSecond;
    private ImageButton btnTimerStartPause;
    private ImageButton btnTimerReset;
    private final Handler timerHandler = new Handler();
    private long timerBaseMs = 0L;
    private long elapsedWhenPausedMs = 0L;
    private boolean timerRunning = false;
    private boolean exitDialogVisible = false;
    private String trainingSessionId;
    private String trainingSessionStartedAt;
    private final ActiveDurationTracker activeDurationTracker =
            new ActiveDurationTracker();
    private static final String STATE_SESSION_ID = "training_session_id";
    private static final String STATE_SESSION_STARTED_AT = "training_session_started_at";
    private static final String STATE_ACTIVE_DURATION = "training_active_duration";
    private static final String STATE_TIMER_ELAPSED = "timer_elapsed";
    private static final String STATE_TIMER_RUNNING = "timer_running";
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) {
                return;
            }
            long elapsed = SystemClock.elapsedRealtime() - timerBaseMs;
            updateStopwatchText(elapsed);
            timerHandler.postDelayed(this, 100);
        }
    };

    private List<WorkoutEntry> workoutEntries;
    private List<CardioEntry> cardioEntries;
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> cardioAdapter;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ExerciseSettings";
    private String workoutType = WorkoutStorage.TYPE_PUSH;
    private String cardioType = WorkoutStorage.TYPE_PUSH + "_cardio";
    private WorkoutTypeRepository.WorkoutType workoutDefinition;

    public static Intent createCustomIntent(Context context, String workoutType) {
        return new Intent(context, PushActivity.class)
                .putExtra(EXTRA_WORKOUT_TYPE, workoutType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveWorkoutType();
        setContentView(R.layout.activity_push);
        configureWorkoutPresentation();
        configureSystemBars();
        applyWindowInsets();
        restoreSessionState(savedInstanceState);

        // Views initialisieren
        spinnerExercise = findViewById(R.id.spinnerExercise);
        spinnerExerciseSecond = findViewById(R.id.spinnerExercise2);
        btnToggleSecondExercise = findViewById(R.id.llToggleSecondExercise);
        ivAccordionArrow = findViewById(R.id.ivAccordionArrow);
        btnExerciseSettings = findViewById(R.id.btnExerciseSettings);
        btnManageExercises = findViewById(R.id.btnManageExercises);
        btnSaveTraining = findViewById(R.id.btnSaveTraining);
        trainingScrollView = findViewById(R.id.trainingScrollView);
        spinnerCardio = findViewById(R.id.spinnerCardio);
        etCardioMinutes = findViewById(R.id.etCardioMinutes);
        btnSaveCardio = findViewById(R.id.btnSaveCardio);
        btnManageCardio = findViewById(R.id.btnEditCardio);
        llEntries = findViewById(R.id.llEntries);
        llLastWorkout = findViewById(R.id.llLastWorkout);
        tvLastWorkoutData = findViewById(R.id.tvLastWorkoutData);
        tvStopwatch = findViewById(R.id.tvStopwatch);
        timerCard = findViewById(R.id.timerCard);
        btnTimerStartPause = findViewById(R.id.btnStartStop);
        btnTimerReset = findViewById(R.id.btnReset);
        llSecondExerciseSection = findViewById(R.id.llSecondExercise);
        llLastWorkoutSecond = findViewById(R.id.llLastWorkoutSecond);
        tvLastWorkoutDataSecond = findViewById(R.id.tvLastWorkoutDataSecond);

        // SharedPreferences initialisieren
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ExerciseCatalog.migrateLegacyExerciseSettings(this);

        setupStopwatch();

        setupExerciseSpinner();
        setupSpinnerPicker(spinnerExercise, adapter, getString(R.string.training_select_exercise));
        setupSpinnerPicker(spinnerExerciseSecond, adapter, getString(R.string.training_select_second_exercise));

        setupCardioSpinner();
        setupSpinnerPicker(spinnerCardio, cardioAdapter, getString(R.string.training_select_cardio));

        EditText[] initialWeights = new EditText[]{
            findViewById(R.id.etWeight1),
            findViewById(R.id.etWeight2),
            findViewById(R.id.etWeight3),
            findViewById(R.id.etWeight4)
        };

        EditText[] initialReps = new EditText[]{
            findViewById(R.id.etReps1),
            findViewById(R.id.etReps2),
            findViewById(R.id.etReps3),
            findViewById(R.id.etReps4)
        };

        EditText[] initialSecondWeights = new EditText[]{
            findViewById(R.id.etWeight1_2),
            findViewById(R.id.etWeight2_2),
            findViewById(R.id.etWeight3_2),
            findViewById(R.id.etWeight4_2)
        };

        EditText[] initialSecondReps = new EditText[]{
            findViewById(R.id.etReps1_2),
            findViewById(R.id.etReps2_2),
            findViewById(R.id.etReps3_2),
            findViewById(R.id.etReps4_2)
        };
        LinearLayout primarySetContainer =
                (LinearLayout) btnSaveTraining.getParent();
        primarySetInputs = new TrainingSetInputController(
                this,
                primarySetContainer,
                primarySetContainer.indexOfChild(btnSaveTraining),
                initialWeights,
                initialReps
        );
        secondSetInputs = new TrainingSetInputController(
                this,
                llSecondExerciseSection,
                llSecondExerciseSection.getChildCount(),
                initialSecondWeights,
                initialSecondReps
        );
        primarySetInputs.restoreState(savedInstanceState, "primary_sets");
        secondSetInputs.restoreState(savedInstanceState, "second_sets");
        configureWeightUnit();

        // Listen für Einträge initialisieren
        workoutEntries = new ArrayList<>();
        cardioEntries = new ArrayList<>();
        restoreSavedSessionEntries();
        setupBackNavigation();
        setupInfoButtons();

        // Button-Click-Listener
        btnToggleSecondExercise.setOnClickListener(v -> toggleSecondExerciseSection());
        btnSaveTraining.setOnClickListener(v -> {
            btnSaveTraining.setEnabled(false);
            if (saveEntry()) {
                SaveSuccessButtonAnimator.play(
                        btnSaveTraining,
                        this::returnToExerciseSelection
                );
            } else {
                btnSaveTraining.setEnabled(true);
            }
        });
        btnSaveCardio.setOnClickListener(v -> {
            btnSaveCardio.setEnabled(false);
            if (saveCardioEntry()) {
                SaveSuccessButtonAnimator.play(btnSaveCardio, null);
            } else {
                btnSaveCardio.setEnabled(true);
            }
        });
        btnManageCardio.setOnClickListener(v -> showManageCardioDialog());
        btnExerciseSettings.setOnClickListener(v -> showExerciseSettingsDialog());
        btnManageExercises.setOnClickListener(v -> showManageExercisesDialog());

        // Button für gespeicherte Trainings
        View btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(PushActivity.this, TrainingHistoryActivity.class);
            intent.putExtra(TrainingHistoryActivity.EXTRA_WORKOUT_TYPE, workoutType);
            intent.putExtra(
                    TrainingHistoryActivity.EXTRA_WORKOUT_TITLE,
                    workoutTitle() + " - Trainings"
            );
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

        spinnerExerciseSecond.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object selectedExerciseObj = spinnerExerciseSecond.getSelectedItem();
                if (selectedExerciseObj == null) {
                    llLastWorkoutSecond.setVisibility(View.GONE);
                    return;
                }
                loadLastWorkoutSecond(selectedExerciseObj.toString());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                llLastWorkoutSecond.setVisibility(View.GONE);
            }
        });

        llSecondExerciseSection.setVisibility(View.GONE);
        llLastWorkoutSecond.setVisibility(View.GONE);
        ivAccordionArrow.setImageResource(R.drawable.ic_ironx_chevron_down);

        // Initial die erste Übung laden
        if (spinnerExercise.getCount() > 0) {
            loadLastWorkout(spinnerExercise.getItemAtPosition(0).toString());
            if (spinnerExerciseSecond.getCount() > 0) {
                loadLastWorkoutSecond(spinnerExerciseSecond.getItemAtPosition(0).toString());
            }
        }
    }

    private void resolveWorkoutType() {
        String requested = getIntent().getStringExtra(EXTRA_WORKOUT_TYPE);
        WorkoutTypeRepository.WorkoutType requestedDefinition =
                WorkoutTypeRepository.find(this, requested);
        if (requestedDefinition != null && !requestedDefinition.builtIn) {
            workoutDefinition = requestedDefinition;
            workoutType = requestedDefinition.id;
        } else {
            workoutDefinition = WorkoutTypeRepository.find(
                    this,
                    WorkoutStorage.TYPE_PUSH
            );
            workoutType = WorkoutStorage.TYPE_PUSH;
        }
        cardioType = workoutType + "_cardio";
    }

    private void configureWorkoutPresentation() {
        if (workoutDefinition == null || workoutDefinition.builtIn) {
            return;
        }
        TextView badge = findViewById(R.id.tvWorkoutTypeBadge);
        TextView focus = findViewById(R.id.tvWorkoutFocus);
        TextView title = findViewById(R.id.tvWorkoutTitle);
        TextView description = findViewById(R.id.tvWorkoutDescription);
        badge.setText(R.string.custom_workout_chip);
        focus.setText(workoutDefinition.focusLabel);
        title.setText(workoutDefinition.name.toUpperCase(Locale.GERMANY));
        description.setText(workoutDefinition.description);
    }

    private String workoutTitle() {
        return workoutDefinition == null
                ? "PUSH DAY"
                : workoutDefinition.name.toUpperCase(Locale.GERMANY);
    }

    private void setupInfoButtons() {
        InfoDialogHelper.insertInfoRowAfter(
                llLastWorkout,
                "Sätze und Berechnungen",
                InfoDialogHelper.Texts.trainingSets()
        );
        InfoDialogHelper.insertInfoRowAfter(
                btnToggleSecondExercise,
                "Zweite Übung",
                "Klappe den Bereich auf, wenn du im selben Workout eine zweite Übung "
                        + "speichern möchtest. Beide Übungen werden getrennt ausgewertet.",
                17
        );
        InfoDialogHelper.insertInfoRowAfter(
                etCardioMinutes,
                "Cardio speichern",
                InfoDialogHelper.Texts.cardio()
        );
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootPushLayout);
        int basePaddingLeft = rootLayout.getPaddingLeft();
        int basePaddingTop = rootLayout.getPaddingTop();
        int basePaddingRight = rootLayout.getPaddingRight();
        int basePaddingBottom = rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + systemBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(rootLayout);
    }

    private void setupStopwatch() {
        updateStopwatchText(elapsedWhenPausedMs);
        if (timerRunning) {
            timerBaseMs = SystemClock.elapsedRealtime() - elapsedWhenPausedMs;
            btnTimerStartPause.setImageResource(R.drawable.ic_ironx_pause);
            btnTimerStartPause.setContentDescription(
                    getString(R.string.training_timer_pause)
            );
            TrainingTimerAnimator.setRunning(timerCard, true);
        } else {
            btnTimerStartPause.setImageResource(R.drawable.ic_ironx_play);
            btnTimerStartPause.setContentDescription(
                    getString(R.string.training_timer_start)
            );
        }
        btnTimerStartPause.setOnClickListener(v -> {
            if (timerRunning) {
                elapsedWhenPausedMs = SystemClock.elapsedRealtime() - timerBaseMs;
                timerRunning = false;
                timerHandler.removeCallbacks(timerRunnable);
                TrainingTimerAnimator.changeButtonState(
                        btnTimerStartPause,
                        R.drawable.ic_ironx_play
                );
                btnTimerStartPause.setContentDescription(
                        getString(R.string.training_timer_start)
                );
                TrainingTimerAnimator.setRunning(timerCard, false);
            } else {
                timerBaseMs = SystemClock.elapsedRealtime() - elapsedWhenPausedMs;
                timerRunning = true;
                TrainingTimerAnimator.changeButtonState(
                        btnTimerStartPause,
                        R.drawable.ic_ironx_pause
                );
                btnTimerStartPause.setContentDescription(
                        getString(R.string.training_timer_pause)
                );
                TrainingTimerAnimator.setRunning(timerCard, true);
                timerHandler.post(timerRunnable);
            }
        });

        btnTimerReset.setOnClickListener(v -> {
            boolean wasRunning = timerRunning;
            timerRunning = false;
            timerHandler.removeCallbacks(timerRunnable);
            timerBaseMs = 0L;
            elapsedWhenPausedMs = 0L;
            updateStopwatchText(0L);
            if (wasRunning) {
                TrainingTimerAnimator.changeButtonState(
                        btnTimerStartPause,
                        R.drawable.ic_ironx_play
                );
            } else {
                btnTimerStartPause.setImageResource(R.drawable.ic_ironx_play);
            }
            btnTimerStartPause.setContentDescription(
                    getString(R.string.training_timer_start)
            );
            TrainingTimerAnimator.setRunning(timerCard, false);
        });
    }

    private void updateStopwatchText(long elapsedMs) {
        long minutes = elapsedMs / 60000;
        long seconds = (elapsedMs % 60000) / 1000;
        long centiseconds = (elapsedMs % 1000) / 10;
        tvStopwatch.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centiseconds));
    }

    private void restoreSessionState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            WorkoutStorage.TrainingSession session =
                    WorkoutStorage.getOrCreateActiveTrainingSession(this, workoutType);
            if (session == null) {
                trainingSessionId = "";
                trainingSessionStartedAt = "";
                activeDurationTracker.restore(0L);
                Toast.makeText(this, R.string.training_session_start_failed, Toast.LENGTH_LONG)
                        .show();
                return;
            }
            trainingSessionId = session.sessionId;
            trainingSessionStartedAt = session.timestamp;
            activeDurationTracker.restore(session.durationMs);
            return;
        }
        trainingSessionId = savedInstanceState.getString(
                STATE_SESSION_ID,
                ""
        );
        trainingSessionStartedAt = savedInstanceState.getString(
                STATE_SESSION_STARTED_AT,
                ""
        );
        activeDurationTracker.restore(Math.max(
                savedInstanceState.getLong(STATE_ACTIVE_DURATION, 0L),
                WorkoutStorage.getTrainingSessionDuration(this, trainingSessionId)
        ));
        elapsedWhenPausedMs = savedInstanceState.getLong(STATE_TIMER_ELAPSED, 0L);
        timerRunning = savedInstanceState.getBoolean(STATE_TIMER_RUNNING, false);
    }

    private void restoreSavedSessionEntries() {
        if (trainingSessionId == null || trainingSessionId.isEmpty()) {
            return;
        }
        for (WorkoutStorage.DetailedWorkout stored :
                WorkoutStorage.getAllDetailedWorkouts(this, workoutType)) {
            if (!trainingSessionId.equals(stored.sessionId) || stored.sets == null) {
                continue;
            }
            List<Set> sets = new ArrayList<>();
            for (WorkoutStorage.WorkoutSet set : stored.sets) {
                sets.add(new Set(set.weight, set.reps));
            }
            WorkoutEntry entry = new WorkoutEntry(stored.exercise, sets);
            workoutEntries.add(entry);
            addEntryToView(entry);
        }
        for (WorkoutStorage.CardioSession stored :
                WorkoutStorage.getAllCardioSessions(this, workoutType)) {
            if (!trainingSessionId.equals(stored.sessionId)) {
                continue;
            }
            CardioEntry entry = new CardioEntry(stored.exercise, stored.minutes);
            cardioEntries.add(entry);
            addCardioEntryToView(entry);
        }
    }

    private long getCurrentElapsedMs() {
        return timerRunning
                ? SystemClock.elapsedRealtime() - timerBaseMs
                : elapsedWhenPausedMs;
    }

    private long pauseAndPersistActiveDuration() {
        long durationMs = activeDurationTracker.pause(SystemClock.elapsedRealtime());
        if (trainingSessionId != null && !trainingSessionId.trim().isEmpty()) {
            WorkoutStorage.updateActiveTrainingSessionDuration(
                    this,
                    trainingSessionId,
                    durationMs
            );
        }
        return durationMs;
    }

    private void toggleSecondExerciseSection() {
        boolean isVisible = llSecondExerciseSection.getVisibility() == View.VISIBLE;
        if (isVisible) {
            llSecondExerciseSection.setVisibility(View.GONE);
            ivAccordionArrow.setImageResource(R.drawable.ic_ironx_chevron_down);
        } else {
            llSecondExerciseSection.setVisibility(View.VISIBLE);
            ivAccordionArrow.setImageResource(R.drawable.ic_ironx_chevron_up);
            Object secondSelected = spinnerExerciseSecond.getSelectedItem();
            if (secondSelected != null) {
                loadLastWorkoutSecond(secondSelected.toString());
            }
        }
    }

    private void loadLastWorkout(String exercise) {
        WorkoutStorage.LastWorkout lastWorkout = WorkoutStorage.getLastWorkout(this, workoutType, exercise);
        if (lastWorkout != null && lastWorkout.sets != null && !lastWorkout.sets.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(lastWorkout.timestamp);
            for (int i = 0; i < lastWorkout.sets.size(); i++) {
                WorkoutStorage.WorkoutSet set = lastWorkout.sets.get(i);
                sb.append("\n")
                        .append(getString(
                                R.string.training_set_summary,
                                i + 1,
                                AppSettings.formatWeight(this, set.weight, 1),
                                set.reps
                        ));
            }
            tvLastWorkoutData.setText(sb.toString());
            llLastWorkout.setVisibility(View.VISIBLE);
        } else {
            llLastWorkout.setVisibility(View.GONE);
        }
    }

    private void loadLastWorkoutSecond(String exercise) {
        WorkoutStorage.LastWorkout lastWorkout = WorkoutStorage.getLastWorkout(this, workoutType, exercise);
        if (lastWorkout != null && lastWorkout.sets != null && !lastWorkout.sets.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(lastWorkout.timestamp);
            for (int i = 0; i < lastWorkout.sets.size(); i++) {
                WorkoutStorage.WorkoutSet set = lastWorkout.sets.get(i);
                sb.append("\n")
                        .append(getString(
                                R.string.training_set_summary,
                                i + 1,
                                AppSettings.formatWeight(this, set.weight, 1),
                                set.reps
                        ));
            }
            tvLastWorkoutDataSecond.setText(sb.toString());
            llLastWorkoutSecond.setVisibility(View.VISIBLE);
        } else {
            llLastWorkoutSecond.setVisibility(View.GONE);
        }
    }

    private void setupExerciseSpinner() {
        List<String> exercises = ExerciseCatalog.getExercises(this, workoutType);
        adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, exercises);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerExercise.setAdapter(adapter);
        spinnerExerciseSecond.setAdapter(adapter);
    }

    private void refreshExerciseSpinner(String preferredExercise) {
        List<String> exercises = ExerciseCatalog.getExercises(this, workoutType);
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
        if (spinnerExerciseSecond.getCount() > index) {
            spinnerExerciseSecond.setSelection(index);
        }
    }

    private void setupCardioSpinner() {
        List<String> cardioExercises = ExerciseCatalog.getExercises(this, cardioType);
        cardioAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, cardioExercises);
        cardioAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerCardio.setAdapter(cardioAdapter);
    }

    private void refreshCardioSpinner(String preferredExercise) {
        List<String> cardioExercises = ExerciseCatalog.getExercises(this, cardioType);
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

    private void setupSpinnerPicker(Spinner spinner, ArrayAdapter<String> spinnerAdapter, String title) {
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showSelectionDialog(title, spinner, spinnerAdapter);
            }
            return true;
        });

        spinner.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                showSelectionDialog(title, spinner, spinnerAdapter);
                return true;
            }
            return false;
        });
    }

    private void showSelectionDialog(String title, Spinner spinner, ArrayAdapter<String> spinnerAdapter) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_select_item);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setAttributes(params);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvSelectTitle);
        LinearLayout llItems = dialog.findViewById(R.id.llSelectItems);
        Button btnClose = dialog.findViewById(R.id.btnCloseSelectDialog);

        tvTitle.setText(title);
        llItems.removeAllViews();

        for (int i = 0; i < spinnerAdapter.getCount(); i++) {
            String item = spinnerAdapter.getItem(i);
            if (item == null) {
                continue;
            }
            llItems.addView(createSelectionRow(item, i, spinner, dialog));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private View createSelectionRow(String item, int index, Spinner spinner, Dialog dialog) {
        float density = getResources().getDisplayMetrics().density;
        int minHeight = Math.round(56 * density);
        int marginBottom = Math.round(10 * density);

        TextView tv = new TextView(this);
        tv.setText(item);
        tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setMinHeight(minHeight);
        tv.setBackground(getResources().getDrawable(R.drawable.bg_dialog_list_item, null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = marginBottom;
        tv.setLayoutParams(params);

        tv.setOnClickListener(v -> {
            spinner.setSelection(index);
            dialog.dismiss();
        });

        return tv;
    }

    private void showManageExercisesDialog() {
        showManageListDialog(
                "Übungen verwalten",
                workoutType,
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
                cardioType,
                null
        );
    }

    private void showManageListDialog(String title,
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
            List<String> items = ExerciseCatalog.getExercises(this, listKey);
            for (String item : items) {
                View row = createManageRow(dialog, item, listKey, afterUpdate);
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

            if (!ExerciseCatalog.addExercise(this, listKey, newItem)) {
                Toast.makeText(this, "Eintrag existiert bereits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listKey.equals(workoutType)) {
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
                                 String listKey,
                                 Runnable afterUpdate) {
        float density = getResources().getDisplayMetrics().density;
        int minHeight = Math.round(56 * density);
        int padStart = Math.round(18 * density);
        int padTopBottom = Math.round(12 * density);
        int padEnd = Math.round(10 * density);
        int marginBottom = Math.round(12 * density);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(minHeight);
        row.setPadding(padStart, padTopBottom, padEnd, padTopBottom);
        row.setBackground(getResources().getDrawable(R.drawable.bg_dialog_list_item, null));

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

        TextView btnDelete = new TextView(this);
        btnDelete.setText(R.string.delete);
        btnDelete.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        btnDelete.setTextSize(9);
        btnDelete.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setPadding(
                Math.round(10 * density),
                Math.round(10 * density),
                Math.round(8 * density),
                Math.round(10 * density)
        );
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);
        btnDelete.setContentDescription(getString(R.string.delete));
        row.addView(btnDelete);

        btnDelete.setOnClickListener(v -> DeleteConfirmationDialog.show(this, item, () -> {
            if (ExerciseCatalog.removeExercise(this, listKey, item)) {
                if (listKey.equals(workoutType)) {
                    sharedPreferences.edit()
                            .remove(ExerciseCatalog.settingsKey(workoutType, item))
                            .apply();
                    getSharedPreferences("ExerciseMuscleMappings", MODE_PRIVATE)
                            .edit()
                            .remove(workoutType + "|" + item)
                            .apply();
                    new ProfileRepository(this).removeStrengthGoal(workoutType, item);
                    refreshExerciseSpinner(null);
                } else {
                    refreshCardioSpinner(null);
                }
                if (afterUpdate != null) {
                    afterUpdate.run();
                }

                parentDialog.dismiss();
                if (listKey.equals(workoutType)) {
                    showManageExercisesDialog();
                } else {
                    showManageCardioDialog();
                }
            }
        }));

        return row;
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
        Button btnSaveSettings = dialog.findViewById(R.id.btnSaveSettings);

        // Gespeicherte Einstellungen laden
        String settingsKey = ExerciseCatalog.settingsKey(workoutType, selectedExercise);
        String savedPositions = sharedPreferences.getString(
                settingsKey,
                ""
        );
        etSeatPositions.setText(savedPositions);

        // Cancel-Button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save-Button
        btnSaveSettings.setOnClickListener(v -> {
            String positions = etSeatPositions.getText().toString().trim();
            
            // In SharedPreferences speichern
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(settingsKey, positions);
            editor.apply();

            Toast.makeText(this, String.format("Einstellungen für '%s' gespeichert", selectedExercise), 
                Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean saveEntry() {
        Object selectedExerciseObj = spinnerExercise.getSelectedItem();
        String exercise = selectedExerciseObj == null ? "" : selectedExerciseObj.toString();

        if (exercise == null || exercise.isEmpty()) {
            Toast.makeText(this, "Bitte Übung auswählen", Toast.LENGTH_SHORT).show();
            return false;
        }

        List<Set> primarySets = parseSetsFromInputs(
                primarySetInputs,
                "Hauptübung"
        );
        if (primarySets == null) {
            return false;
        }

        Object secondExerciseObj = spinnerExerciseSecond.getSelectedItem();
        String secondExercise = secondExerciseObj == null ? "" : secondExerciseObj.toString();
        boolean secondHasInput = secondSetInputs.hasAnyInput();
        List<Set> secondSets = null;

        if (secondHasInput) {
            if (secondExercise == null || secondExercise.isEmpty()) {
                Toast.makeText(this, "Bitte 2. Übung auswählen", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (exercise.equalsIgnoreCase(secondExercise)) {
                Toast.makeText(this, "Bitte eine andere 2. Übung wählen", Toast.LENGTH_SHORT).show();
                return false;
            }

            secondSets = parseSetsFromInputs(
                    secondSetInputs,
                    "2. Übung"
            );
            if (secondSets == null) {
                return false;
            }
        }

        List<WorkoutStorage.WorkoutExercise> exercisesToSave = new ArrayList<>();
        exercisesToSave.add(new WorkoutStorage.WorkoutExercise(
                exercise,
                toStorageSets(primarySets)
        ));
        if (secondHasInput && secondSets != null) {
            exercisesToSave.add(new WorkoutStorage.WorkoutExercise(
                    secondExercise,
                    toStorageSets(secondSets)
            ));
        }
        boolean saved = WorkoutStorage.saveDetailedWorkouts(
                this,
                workoutType,
                trainingSessionId,
                trainingSessionStartedAt,
                exercisesToSave
        );
        if (!saved) {
            Toast.makeText(this, R.string.training_save_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
        WorkoutEntry primaryEntry = new WorkoutEntry(exercise, primarySets);
        workoutEntries.add(primaryEntry);
        addEntryToView(primaryEntry);
        if (secondHasInput && secondSets != null) {
            WorkoutEntry secondEntry = new WorkoutEntry(secondExercise, secondSets);
            workoutEntries.add(secondEntry);
            addEntryToView(secondEntry);
        }
        clearInputFields();
        loadLastWorkout(exercise);
        return true;
    }

    private List<Set> parseSetsFromInputs(
            TrainingSetInputController inputs,
            String label
    ) {
        List<Set> sets = new ArrayList<>();
        for (int i = 0; i < inputs.getSetCount(); i++) {
            EditText weightInput = inputs.getWeightInput(i);
            EditText repsInput = inputs.getRepsInput(i);
            String weightStr = weightInput.getText().toString().trim();
            String repsStr = repsInput.getText().toString().trim();

            if (weightStr.isEmpty() || repsStr.isEmpty()) {
                weightInput.setError(getString(R.string.profile_required));
                repsInput.setError(getString(R.string.profile_required));
                Toast.makeText(this, String.format("%s: Bitte Satz %d vollständig ausfüllen", label, i + 1), Toast.LENGTH_SHORT).show();
                return null;
            }

            try {
                double displayedWeight = Double.parseDouble(weightStr.replace(',', '.'));
                double weight = AppSettings.toStoredKg(this, displayedWeight);
                int repsValue = Integer.parseInt(repsStr);
                if (!Double.isFinite(weight) || weight <= 0 || repsValue <= 0) {
                    throw new NumberFormatException();
                }
                sets.add(new Set(weight, repsValue));
            } catch (NumberFormatException e) {
                weightInput.setError(getString(R.string.profile_invalid_number));
                repsInput.setError(getString(R.string.profile_invalid_number));
                Toast.makeText(this, String.format("%s: Ungültige Eingabe in Satz %d", label, i + 1), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return sets;
    }

    private List<WorkoutStorage.WorkoutSet> toStorageSets(List<Set> sets) {
        List<WorkoutStorage.WorkoutSet> storageSets = new ArrayList<>();
        for (Set set : sets) {
            storageSets.add(new WorkoutStorage.WorkoutSet(set.getWeight(), set.getReps()));
        }
        return storageSets;
    }

    private void clearInputFields() {
        // Spinner auf erste Position zurücksetzen
        spinnerExercise.setSelection(0);
        primarySetInputs.clearInputs();
        secondSetInputs.clearInputs();
        spinnerExerciseSecond.setSelection(spinnerExercise.getSelectedItemPosition());
    }

    private void addEntryToView(WorkoutEntry entry) {
        // Container für die Übung erstellen mit Card-Design
        LinearLayout exerciseContainer = new LinearLayout(this);
        exerciseContainer.setOrientation(LinearLayout.VERTICAL);
        exerciseContainer.setPadding(20, 20, 20, 20);
        exerciseContainer.setBackground(getResources().getDrawable(R.drawable.rounded_card, null));
        
        exerciseContainer.setElevation(12f);

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
            setView.setText(getString(
                    R.string.training_set_detail,
                    i + 1,
                    AppSettings.formatWeight(this, set.getWeight(), 1),
                    set.getReps()
            ));
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

    private void returnToExerciseSelection() {
        trainingScrollView.post(() -> trainingScrollView.smoothScrollTo(0, 0));
    }

    private void configureWeightUnit() {
        String unit = AppSettings.getWeightUnit(this);
        primarySetInputs.setWeightUnit(unit);
        secondSetInputs.setWeightUnit(unit);
    }

    private boolean saveCardioEntry() {
        // Cardio-Übung aus Spinner auslesen
        Object selectedCardioObj = spinnerCardio.getSelectedItem();
        String cardioExercise = selectedCardioObj == null ? "" : selectedCardioObj.toString();
        String minutesStr = etCardioMinutes.getText().toString().trim();

        // Validierung
        if (cardioExercise == null || cardioExercise.isEmpty()) {
            Toast.makeText(this, "Bitte Cardio-Übung auswählen", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (minutesStr.isEmpty()) {
            Toast.makeText(this, "Bitte Zeit in Minuten eingeben", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int minutes = Integer.parseInt(minutesStr);
            if (minutes <= 0) {
                Toast.makeText(this, "Bitte eine gültige Zeit eingeben", Toast.LENGTH_SHORT).show();
                return false;
            }

            boolean saved = WorkoutStorage.saveCardioSession(
                    this,
                    workoutType,
                    trainingSessionId,
                    trainingSessionStartedAt,
                    cardioExercise,
                    minutes
            );
            if (!saved) {
                Toast.makeText(this, R.string.training_save_failed, Toast.LENGTH_SHORT).show();
                return false;
            }

            CardioEntry entry = new CardioEntry(cardioExercise, minutes);
            cardioEntries.add(entry);
            // Eintrag in der UI anzeigen
            addCardioEntryToView(entry);

            // Eingabefelder zurücksetzen
            spinnerCardio.setSelection(0);
            etCardioMinutes.setText("");

            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ungültige Eingabe für Minuten", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void addCardioEntryToView(CardioEntry entry) {
        // Container für die Cardio-Übung erstellen mit Card-Design
        LinearLayout cardioContainer = new LinearLayout(this);
        cardioContainer.setOrientation(LinearLayout.VERTICAL);
        cardioContainer.setPadding(20, 20, 20, 20);
        cardioContainer.setBackground(getResources().getDrawable(R.drawable.rounded_card, null));
        
        cardioContainer.setElevation(12f);

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
        timeView.setText(getString(R.string.training_minutes_value, entry.getMinutes()));
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

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showTrainingExitDialog();
            }
        });
    }

    private void showTrainingExitDialog() {
        if (exitDialogVisible) {
            return;
        }
        exitDialogVisible = true;
        boolean resumeTimer = pauseTimerForExitDialog();
        long activeDurationMs = pauseAndPersistActiveDuration();
        TrainingExitDialog.show(
                this,
                trainingSessionId,
                workoutType,
                trainingSessionStartedAt,
                activeDurationMs,
                hasUnsavedTrainingInput(),
                () -> {
                    exitDialogVisible = false;
                    if (resumeTimer) {
                        resumeTimerAfterExitDialog();
                    }
                    activeDurationTracker.start(SystemClock.elapsedRealtime());
                },
                () -> {
                    exitDialogVisible = false;
                    finish();
                }
        );
    }

    private boolean pauseTimerForExitDialog() {
        if (!timerRunning) {
            return false;
        }
        elapsedWhenPausedMs = SystemClock.elapsedRealtime() - timerBaseMs;
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        TrainingTimerAnimator.changeButtonState(
                btnTimerStartPause,
                R.drawable.ic_ironx_play
        );
        btnTimerStartPause.setContentDescription(
                getString(R.string.training_timer_start)
        );
        TrainingTimerAnimator.setRunning(timerCard, false);
        return true;
    }

    private void resumeTimerAfterExitDialog() {
        timerBaseMs = SystemClock.elapsedRealtime() - elapsedWhenPausedMs;
        timerRunning = true;
        TrainingTimerAnimator.changeButtonState(
                btnTimerStartPause,
                R.drawable.ic_ironx_pause
        );
        btnTimerStartPause.setContentDescription(
                getString(R.string.training_timer_pause)
        );
        TrainingTimerAnimator.setRunning(timerCard, true);
        timerHandler.post(timerRunnable);
    }

    private boolean hasUnsavedTrainingInput() {
        return primarySetInputs.hasAnyInput()
                || secondSetInputs.hasAnyInput()
                || !etCardioMinutes.getText().toString().trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeDurationTracker.start(SystemClock.elapsedRealtime());
        if (timerRunning) {
            timerBaseMs = SystemClock.elapsedRealtime() - elapsedWhenPausedMs;
            timerHandler.post(timerRunnable);
            TrainingTimerAnimator.setRunning(timerCard, true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SESSION_ID, trainingSessionId);
        outState.putString(STATE_SESSION_STARTED_AT, trainingSessionStartedAt);
        outState.putLong(
                STATE_ACTIVE_DURATION,
                activeDurationTracker.elapsed(SystemClock.elapsedRealtime())
        );
        outState.putLong(STATE_TIMER_ELAPSED, getCurrentElapsedMs());
        outState.putBoolean(STATE_TIMER_RUNNING, timerRunning);
        primarySetInputs.saveState(outState, "primary_sets");
        secondSetInputs.saveState(outState, "second_sets");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (timerRunning) {
            elapsedWhenPausedMs = SystemClock.elapsedRealtime() - timerBaseMs;
        }
        pauseAndPersistActiveDuration();
        timerHandler.removeCallbacks(timerRunnable);
        TrainingTimerAnimator.suspend(timerCard);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        TrainingTimerAnimator.release(timerCard);
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
