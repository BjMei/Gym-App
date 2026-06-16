package com.example.gym_app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileGoalsActivity extends IronxActivity {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);

    private EditText etProfileName;
    private EditText etProfileWeight;
    private EditText etProfileHeight;
    private EditText etProfileBirthYear;
    private EditText etProfileBodyFat;
    private EditText etTargetWeight;
    private EditText etTargetDate;
    private EditText etTrainingGoalPerWeek;
    private EditText etWeightMeasurement;
    private EditText etWeightMeasurementDate;
    private EditText etBodyFatMeasurement;
    private EditText etBodyFatMeasurementDate;
    private EditText etStrengthGoalTarget;
    private EditText etVolumeGoal;
    private Spinner spinnerGoal;
    private Spinner spinnerActivityLevel;
    private Spinner spinnerExperience;
    private Spinner spinnerStrengthGoalExercise;
    private TextView tvLegacyStrengthGoal;
    private TextView btnToggleWeightMeasurements;
    private TextView btnToggleBodyFatMeasurements;
    private LinearLayout llWeightMeasurements;
    private LinearLayout llBodyFatMeasurements;
    private LinearLayout llStrengthGoals;
    private ProfileRepository repository;
    private LocalDate selectedTargetDate;
    private LocalDate selectedBirthDate;
    private LocalDate selectedMeasurementDate = LocalDate.now();
    private LocalDate selectedBodyFatMeasurementDate = LocalDate.now();
    private List<StrengthExerciseOption> strengthExerciseOptions = new ArrayList<>();
    private boolean weightMeasurementsExpanded = true;
    private boolean weightMeasurementsDropdownTouched;
    private boolean bodyFatMeasurementsExpanded = true;
    private boolean bodyFatMeasurementsDropdownTouched;

    private final List<MaterialButton> dayButtons = new ArrayList<>();
    private final List<DayOfWeek> dayValues = Arrays.asList(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_goals);
        applyWindowInsets();

        repository = new ProfileRepository(this);
        bindViews();
        setupSpinners();
        setupStrengthGoalExercises();
        setupDayButtons();
        setupBirthDate();
        setupTargetDate();
        setupMeasurementDate();
        setupBodyFatMeasurementDate();
        setupUnitHints();
        setupRecommendationUpdates();
        setupWeightMeasurementsDropdown();
        setupBodyFatMeasurementsDropdown();
        setupInfoButtons();
        loadProfile();

        findViewById(R.id.btnBackProfileGoals).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddWeightMeasurement)
                .setOnClickListener(v -> addWeightMeasurement());
        findViewById(R.id.btnAddBodyFatMeasurement)
                .setOnClickListener(v -> addBodyFatMeasurement());
        findViewById(R.id.btnAddStrengthGoal)
                .setOnClickListener(v -> addStrengthGoal());
        findViewById(R.id.btnSaveProfileGoals)
                .setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        etProfileName = findViewById(R.id.etProfileName);
        etProfileWeight = findViewById(R.id.etProfileWeight);
        etProfileHeight = findViewById(R.id.etProfileHeight);
        etProfileBirthYear = findViewById(R.id.etProfileBirthYear);
        etProfileBodyFat = findViewById(R.id.etProfileBodyFat);
        etTargetWeight = findViewById(R.id.etTargetWeight);
        etTargetDate = findViewById(R.id.etTargetDate);
        etTrainingGoalPerWeek = findViewById(R.id.etTrainingGoalPerWeek);
        etWeightMeasurement = findViewById(R.id.etWeightMeasurement);
        etWeightMeasurementDate = findViewById(R.id.etWeightMeasurementDate);
        etBodyFatMeasurement = findViewById(R.id.etBodyFatMeasurement);
        etBodyFatMeasurementDate = findViewById(R.id.etBodyFatMeasurementDate);
        etStrengthGoalTarget = findViewById(R.id.etStrengthGoalTarget);
        etVolumeGoal = findViewById(R.id.etVolumeGoal);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel);
        spinnerExperience = findViewById(R.id.spinnerExperience);
        spinnerStrengthGoalExercise = findViewById(R.id.spinnerStrengthGoalExercise);
        tvLegacyStrengthGoal = findViewById(R.id.tvLegacyStrengthGoal);
        btnToggleWeightMeasurements = findViewById(R.id.btnToggleWeightMeasurements);
        btnToggleBodyFatMeasurements = findViewById(R.id.btnToggleBodyFatMeasurements);
        llWeightMeasurements = findViewById(R.id.llWeightMeasurements);
        llBodyFatMeasurements = findViewById(R.id.llBodyFatMeasurements);
        llStrengthGoals = findViewById(R.id.llStrengthGoals);

        dayButtons.add(findViewById(R.id.btnDayMonday));
        dayButtons.add(findViewById(R.id.btnDayTuesday));
        dayButtons.add(findViewById(R.id.btnDayWednesday));
        dayButtons.add(findViewById(R.id.btnDayThursday));
        dayButtons.add(findViewById(R.id.btnDayFriday));
        dayButtons.add(findViewById(R.id.btnDaySaturday));
        dayButtons.add(findViewById(R.id.btnDaySunday));
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootProfileGoalsLayout);
        int left = rootLayout.getPaddingLeft();
        int top = rootLayout.getPaddingTop();
        int right = rootLayout.getPaddingRight();
        int bottom = rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + systemBars.left,
                    top + systemBars.top,
                    right + systemBars.right,
                    bottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(rootLayout);
    }

    private void setupSpinners() {
        setOptions(
                spinnerGoal,
                Arrays.asList(
                        new Option(
                                ProfileRepository.GOAL_MUSCLE,
                                getString(R.string.settings_goal_muscle)
                        ),
                        new Option(
                                ProfileRepository.GOAL_STRENGTH,
                                getString(R.string.settings_goal_strength)
                        ),
                        new Option(
                                ProfileRepository.GOAL_WEIGHT_LOSS,
                                getString(R.string.settings_goal_weight_loss)
                        ),
                        new Option(
                                ProfileRepository.GOAL_FITNESS,
                                getString(R.string.settings_goal_fitness)
                        )
                )
        );
        setOptions(
                spinnerActivityLevel,
                Arrays.asList(
                        new Option(
                                ProfileRepository.ACTIVITY_LOW,
                                getString(R.string.profile_activity_low)
                        ),
                        new Option(
                                ProfileRepository.ACTIVITY_MODERATE,
                                getString(R.string.profile_activity_moderate)
                        ),
                        new Option(
                                ProfileRepository.ACTIVITY_HIGH,
                                getString(R.string.profile_activity_high)
                        ),
                        new Option(
                                ProfileRepository.ACTIVITY_VERY_HIGH,
                                getString(R.string.profile_activity_very_high)
                        )
                )
        );
        setOptions(
                spinnerExperience,
                Arrays.asList(
                        new Option(
                                ProfileRepository.EXPERIENCE_BEGINNER,
                                getString(R.string.profile_experience_beginner)
                        ),
                        new Option(
                                ProfileRepository.EXPERIENCE_INTERMEDIATE,
                                getString(R.string.profile_experience_intermediate)
                        ),
                        new Option(
                                ProfileRepository.EXPERIENCE_EXPERIENCED,
                                getString(R.string.profile_experience_experienced)
                        )
                )
        );
        AdapterView.OnItemSelectedListener impactListener =
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                updateImpactInfo(getSelectedOptionId(spinnerGoal));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerGoal.setOnItemSelectedListener(impactListener);
        spinnerActivityLevel.setOnItemSelectedListener(impactListener);
        spinnerExperience.setOnItemSelectedListener(impactListener);
    }

    private void setOptions(Spinner spinner, List<Option> options) {
        ArrayAdapter<Option> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                options
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinner.setAdapter(adapter);
    }

    private void setupStrengthGoalExercises() {
        Map<String, StrengthExerciseOption> options = new LinkedHashMap<>();
        addStrengthExercises(
                options,
                WorkoutStorage.TYPE_PUSH
        );
        addStrengthExercises(
                options,
                WorkoutStorage.TYPE_PULL
        );
        addStrengthExercises(
                options,
                WorkoutStorage.TYPE_LEG
        );
        strengthExerciseOptions = new ArrayList<>(options.values());
        ArrayAdapter<StrengthExerciseOption> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                strengthExerciseOptions
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerStrengthGoalExercise.setAdapter(adapter);
        spinnerStrengthGoalExercise.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        updateStrengthGoalInput();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void addStrengthExercises(
            Map<String, StrengthExerciseOption> options,
            String workoutType) {
        for (String exercise : ExerciseCatalog.getExercises(this, workoutType)) {
            StrengthExerciseOption option =
                    new StrengthExerciseOption(workoutType, exercise);
            options.putIfAbsent(option.key(), option);
        }
    }

    private void setupDayButtons() {
        for (MaterialButton button : dayButtons) {
            button.setCheckable(true);
        }
    }

    private void setupBirthDate() {
        etProfileBirthYear.setOnClickListener(v -> {
            LocalDate initial = selectedBirthDate == null
                    ? LocalDate.now().minusYears(30)
                    : selectedBirthDate;
            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        selectedBirthDate = LocalDate.of(year, month + 1, day);
                        etProfileBirthYear.setText(selectedBirthDate.format(DISPLAY_DATE));
                        etProfileBirthYear.setError(null);
                    },
                    initial.getYear(),
                    initial.getMonthValue() - 1,
                    initial.getDayOfMonth()
            ).show();
        });
    }

    private void setupTargetDate() {
        etTargetDate.setOnClickListener(v -> {
            LocalDate initial = selectedTargetDate == null
                    ? LocalDate.now().plusMonths(3)
                    : selectedTargetDate;
            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        selectedTargetDate = LocalDate.of(year, month + 1, day);
                        etTargetDate.setText(selectedTargetDate.format(DISPLAY_DATE));
                    },
                    initial.getYear(),
                    initial.getMonthValue() - 1,
                    initial.getDayOfMonth()
            ).show();
        });
        etTargetDate.setOnLongClickListener(v -> {
            selectedTargetDate = null;
            etTargetDate.setText("");
            return true;
        });
    }

    private void setupMeasurementDate() {
        etWeightMeasurementDate.setText(selectedMeasurementDate.format(DISPLAY_DATE));
        etWeightMeasurementDate.setOnClickListener(v -> {
            LocalDate initial = selectedMeasurementDate == null
                    ? LocalDate.now()
                    : selectedMeasurementDate;
            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        selectedMeasurementDate = LocalDate.of(year, month + 1, day);
                        etWeightMeasurementDate.setText(
                                selectedMeasurementDate.format(DISPLAY_DATE)
                        );
                    },
                    initial.getYear(),
                    initial.getMonthValue() - 1,
                    initial.getDayOfMonth()
            ).show();
        });
    }

    private void setupBodyFatMeasurementDate() {
        etBodyFatMeasurementDate.setText(
                selectedBodyFatMeasurementDate.format(DISPLAY_DATE)
        );
        etBodyFatMeasurementDate.setOnClickListener(v -> {
            LocalDate initial = selectedBodyFatMeasurementDate == null
                    ? LocalDate.now()
                    : selectedBodyFatMeasurementDate;
            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        selectedBodyFatMeasurementDate = LocalDate.of(year, month + 1, day);
                        etBodyFatMeasurementDate.setText(
                                selectedBodyFatMeasurementDate.format(DISPLAY_DATE)
                        );
                    },
                    initial.getYear(),
                    initial.getMonthValue() - 1,
                    initial.getDayOfMonth()
            ).show();
        });
    }

    private void setupUnitHints() {
        String unit = AppSettings.getWeightUnit(this);
        etProfileWeight.setHint(getString(R.string.profile_weight_hint_unit, unit));
        etWeightMeasurement.setHint(
                getString(R.string.profile_measurement_weight_hint, unit)
        );
        etTargetWeight.setHint(getString(R.string.profile_target_weight_hint, unit));
        etStrengthGoalTarget.setHint(
                getString(R.string.profile_strength_goal_hint, unit)
        );
        etVolumeGoal.setHint(getString(R.string.profile_volume_goal_hint, unit));
    }

    private void setupRecommendationUpdates() {
        etTrainingGoalPerWeek.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value,
                    int start,
                    int before,
                    int count) {
                updateImpactInfo(getSelectedOptionId(spinnerGoal));
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    private void setupWeightMeasurementsDropdown() {
        if (btnToggleWeightMeasurements == null) {
            return;
        }
        btnToggleWeightMeasurements.setOnClickListener(v -> {
            weightMeasurementsDropdownTouched = true;
            weightMeasurementsExpanded = !weightMeasurementsExpanded;
            refreshWeightMeasurements();
        });
    }

    private void setupBodyFatMeasurementsDropdown() {
        if (btnToggleBodyFatMeasurements == null) {
            return;
        }
        btnToggleBodyFatMeasurements.setOnClickListener(v -> {
            bodyFatMeasurementsDropdownTouched = true;
            bodyFatMeasurementsExpanded = !bodyFatMeasurementsExpanded;
            refreshBodyFatMeasurements();
        });
    }

    private void setupInfoButtons() {
        InfoDialogHelper.bind(
                findViewById(R.id.infoProfilePersonalData),
                "Körperdaten",
                InfoDialogHelper.Texts.profileBodyData()
        );
        InfoDialogHelper.bind(
                findViewById(R.id.infoProfileWeightMeasurements),
                "Gewichtsmessungen",
                InfoDialogHelper.Texts.profileWeight()
        );
        updateImpactInfo(getSelectedOptionId(spinnerGoal));
        InfoDialogHelper.bind(
                findViewById(R.id.infoProfileTrainingProfile),
                "Empfehlungsprofil",
                infoMessage(
                        InfoDialogHelper.Texts.profileRecommendationBasis(),
                        getString(R.string.profile_preferred_days_hint)
                )
        );
        InfoDialogHelper.bind(
                findViewById(R.id.infoProfilePerformanceGoals),
                "Leistungsziele",
                infoMessage(
                        "Wähle eine Übung und speichere den Zielwert als geschätztes 1RM. Die Fortschritt-Seite vergleicht später dein bestes berechnetes 1RM mit diesem Ziel.",
                        InfoDialogHelper.Texts.profilePerformanceGoals(),
                        getString(R.string.profile_performance_goals_hint)
                )
        );
    }

    private String infoMessage(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append("\n\n");
            }
            result.append(part.trim());
        }
        return result.toString();
    }

    private void loadProfile() {
        ProfileRepository.Profile profile = repository.load();
        etProfileName.setText(profile.name);
        setDisplayedWeight(etProfileWeight, profile.currentWeightKg);
        setDisplayedWeight(etTargetWeight, profile.targetWeightKg);
        etProfileHeight.setText(optionalInt(profile.heightCm));
        selectedBirthDate = profile.birthDate;
        etProfileBirthYear.setText(
                selectedBirthDate == null ? "" : selectedBirthDate.format(DISPLAY_DATE)
        );
        etProfileBodyFat.setText(optionalNumber(profile.bodyFatPercent));
        etTrainingGoalPerWeek.setText(String.valueOf(profile.weeklyTrainingGoal));
        setDisplayedWeight(etVolumeGoal, profile.weeklyVolumeGoalKg);
        selectedTargetDate = profile.targetDate;
        etTargetDate.setText(
                selectedTargetDate == null
                        ? ""
                        : selectedTargetDate.format(DISPLAY_DATE)
        );

        selectOption(spinnerGoal, profile.goalId);
        selectOption(spinnerActivityLevel, profile.activityLevelId);
        selectOption(spinnerExperience, profile.experienceId);
        for (int i = 0; i < dayButtons.size(); i++) {
            dayButtons.get(i).setChecked(profile.preferredDays.contains(dayValues.get(i)));
        }
        updateImpactInfo(profile.goalId);
        refreshWeightMeasurements();
        refreshBodyFatMeasurements();
        refreshStrengthGoals();
        if (profile.legacyStrengthGoalKg > 0
                && profile.strengthGoalsKg.isEmpty()) {
            tvLegacyStrengthGoal.setVisibility(View.VISIBLE);
            tvLegacyStrengthGoal.setText(getString(
                    R.string.profile_legacy_strength_goal,
                    AppSettings.formatWeight(this, profile.legacyStrengthGoalKg, 1)
            ));
            setDisplayedWeight(etStrengthGoalTarget, profile.legacyStrengthGoalKg);
        } else {
            tvLegacyStrengthGoal.setVisibility(View.GONE);
        }
    }

    private void saveProfile() {
        clearErrors();
        ProfileRepository.Profile profile = new ProfileRepository.Profile();
        profile.name = etProfileName.getText().toString().trim();
        profile.currentWeightKg = repository.load().currentWeightKg;
        profile.heightCm = parseInt(etProfileHeight, 100, 250, false);
        profile.birthDate = selectedBirthDate;
        profile.birthYear = selectedBirthDate == null ? 0 : selectedBirthDate.getYear();
        profile.bodyFatPercent = repository.load().bodyFatPercent;
        profile.goalId = getSelectedOptionId(spinnerGoal);
        profile.targetWeightKg = parseDisplayedWeight(
                etTargetWeight,
                25,
                400,
                false
        );
        profile.weeklyTrainingGoal = parseInt(
                etTrainingGoalPerWeek,
                1,
                7,
                true
        );
        profile.activityLevelId = getSelectedOptionId(spinnerActivityLevel);
        profile.experienceId = getSelectedOptionId(spinnerExperience);
        profile.preferredDays = getSelectedDays();
        profile.targetDate = selectedTargetDate;
        profile.weeklyVolumeGoalKg = parseDisplayedWeight(
                etVolumeGoal,
                1,
                1_000_000,
                false
        );

        if (hasErrors()) {
            return;
        }
        if (profile.birthDate == null) {
            etProfileBirthYear.setError(getString(R.string.profile_required));
            return;
        }
        if (profile.birthDate.isAfter(LocalDate.now())) {
            etProfileBirthYear.setError(
                    getString(R.string.profile_measurement_date_future)
            );
            return;
        }
        if (!profile.preferredDays.isEmpty()
                && profile.preferredDays.size() < profile.weeklyTrainingGoal) {
            Toast.makeText(
                    this,
                    R.string.profile_preferred_days_too_few,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        if (profile.targetDate != null
                && profile.targetDate.isBefore(LocalDate.now())) {
            etTargetDate.setError(getString(R.string.profile_target_date_past));
            return;
        }

        repository.save(profile);
        Toast.makeText(this, R.string.profile_goals_saved, Toast.LENGTH_SHORT).show();
        updateImpactInfo(profile.goalId);
    }

    private void addWeightMeasurement() {
        etWeightMeasurement.setError(null);
        double weightKg = parseDisplayedWeight(
                etWeightMeasurement,
                25,
                400,
                true
        );
        if (etWeightMeasurement.getError() != null) {
            return;
        }
        if (selectedMeasurementDate == null) {
            etWeightMeasurementDate.setError(getString(R.string.profile_required));
            return;
        }
        if (selectedMeasurementDate.isAfter(LocalDate.now())) {
            etWeightMeasurementDate.setError(
                    getString(R.string.profile_measurement_date_future)
            );
            return;
        }

        repository.addWeightMeasurement(selectedMeasurementDate, weightKg);
        etWeightMeasurement.setText("");
        selectedMeasurementDate = LocalDate.now();
        etWeightMeasurementDate.setText(selectedMeasurementDate.format(DISPLAY_DATE));
        etWeightMeasurementDate.setError(null);
        setDisplayedWeight(etProfileWeight, repository.load().currentWeightKg);
        refreshWeightMeasurements();
        Toast.makeText(
                this,
                R.string.profile_measurement_saved,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void addBodyFatMeasurement() {
        etBodyFatMeasurement.setError(null);
        double bodyFatPercent = parseNumber(
                etBodyFatMeasurement,
                2,
                70,
                true
        );
        if (etBodyFatMeasurement.getError() != null) {
            return;
        }
        if (selectedBodyFatMeasurementDate == null) {
            etBodyFatMeasurementDate.setError(getString(R.string.profile_required));
            return;
        }
        if (selectedBodyFatMeasurementDate.isAfter(LocalDate.now())) {
            etBodyFatMeasurementDate.setError(
                    getString(R.string.profile_measurement_date_future)
            );
            return;
        }

        repository.addBodyFatMeasurement(
                selectedBodyFatMeasurementDate,
                bodyFatPercent
        );
        etBodyFatMeasurement.setText("");
        selectedBodyFatMeasurementDate = LocalDate.now();
        etBodyFatMeasurementDate.setText(
                selectedBodyFatMeasurementDate.format(DISPLAY_DATE)
        );
        etBodyFatMeasurementDate.setError(null);
        etProfileBodyFat.setText(optionalNumber(repository.load().bodyFatPercent));
        refreshBodyFatMeasurements();
        Toast.makeText(
                this,
                R.string.profile_body_fat_saved,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void addStrengthGoal() {
        etStrengthGoalTarget.setError(null);
        Object selected = spinnerStrengthGoalExercise.getSelectedItem();
        if (!(selected instanceof StrengthExerciseOption)) {
            return;
        }
        double targetKg = parseDisplayedWeight(
                etStrengthGoalTarget,
                1,
                1000,
                true
        );
        if (etStrengthGoalTarget.getError() != null) {
            return;
        }
        StrengthExerciseOption option = (StrengthExerciseOption) selected;
        repository.setStrengthGoal(option.workoutType, option.exercise, targetKg);
        tvLegacyStrengthGoal.setVisibility(View.GONE);
        refreshStrengthGoals();
        updateStrengthGoalInput();
        Toast.makeText(
                this,
                R.string.profile_strength_goal_saved,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateStrengthGoalInput() {
        Object selected = spinnerStrengthGoalExercise.getSelectedItem();
        if (!(selected instanceof StrengthExerciseOption) || repository == null) {
            return;
        }
        StrengthExerciseOption option = (StrengthExerciseOption) selected;
        double targetKg = repository.getStrengthGoalKg(
                option.workoutType,
                option.exercise
        );
        if (targetKg > 0) {
            setDisplayedWeight(etStrengthGoalTarget, targetKg);
        } else if (tvLegacyStrengthGoal == null
                || tvLegacyStrengthGoal.getVisibility() != View.VISIBLE) {
            etStrengthGoalTarget.setText("");
        }
    }

    private void refreshWeightMeasurements() {
        llWeightMeasurements.removeAllViews();
        List<ProfileRepository.WeightEntry> entries =
                new ArrayList<>(repository.getWeightHistory());
        entries.sort((left, right) -> right.date.compareTo(left.date));
        if (!weightMeasurementsDropdownTouched) {
            weightMeasurementsExpanded = entries.size() <= 3;
        }
        if (entries.isEmpty()) {
            weightMeasurementsExpanded = true;
        }
        updateWeightMeasurementsDropdown(entries.size());
        llWeightMeasurements.setVisibility(
                weightMeasurementsExpanded ? View.VISIBLE : View.GONE
        );
        if (entries.isEmpty()) {
            addEmptyRow(llWeightMeasurements, R.string.profile_measurement_empty);
            return;
        }
        if (!weightMeasurementsExpanded) {
            return;
        }
        for (int index = 0; index < entries.size(); index++) {
            ProfileRepository.WeightEntry entry = entries.get(index);
            addManageRow(
                    llWeightMeasurements,
                    entry.date.format(DISPLAY_DATE),
                    AppSettings.formatWeight(this, entry.weightKg, 1),
                    () -> confirmDeleteWeightMeasurement(entry.date)
            );
        }
    }

    private void updateWeightMeasurementsDropdown(int count) {
        if (btnToggleWeightMeasurements == null) {
            return;
        }
        String arrow = weightMeasurementsExpanded ? "▲" : "▼";
        String text = getString(R.string.profile_measurement_history_label)
                + " ("
                + count
                + ") "
                + arrow;
        btnToggleWeightMeasurements.setText(text);
        btnToggleWeightMeasurements.setContentDescription(text);
    }

    private void refreshBodyFatMeasurements() {
        llBodyFatMeasurements.removeAllViews();
        List<ProfileRepository.BodyFatEntry> entries =
                new ArrayList<>(repository.getBodyFatHistory());
        entries.sort((left, right) -> right.date.compareTo(left.date));
        if (!bodyFatMeasurementsDropdownTouched) {
            bodyFatMeasurementsExpanded = entries.size() <= 3;
        }
        if (entries.isEmpty()) {
            bodyFatMeasurementsExpanded = true;
        }
        updateBodyFatMeasurementsDropdown(entries.size());
        llBodyFatMeasurements.setVisibility(
                bodyFatMeasurementsExpanded ? View.VISIBLE : View.GONE
        );
        if (entries.isEmpty()) {
            addEmptyRow(llBodyFatMeasurements, R.string.profile_body_fat_empty);
            return;
        }
        if (!bodyFatMeasurementsExpanded) {
            return;
        }
        for (ProfileRepository.BodyFatEntry entry : entries) {
            addManageRow(
                    llBodyFatMeasurements,
                    entry.date.format(DISPLAY_DATE),
                    optionalNumber(entry.bodyFatPercent) + " %",
                    () -> confirmDeleteBodyFatMeasurement(entry.date)
            );
        }
    }

    private void updateBodyFatMeasurementsDropdown(int count) {
        if (btnToggleBodyFatMeasurements == null) {
            return;
        }
        String arrow = bodyFatMeasurementsExpanded ? "▲" : "▼";
        String text = getString(R.string.profile_body_fat_history_label)
                + " ("
                + count
                + ") "
                + arrow;
        btnToggleBodyFatMeasurements.setText(text);
        btnToggleBodyFatMeasurements.setContentDescription(text);
    }

    private void refreshStrengthGoals() {
        llStrengthGoals.removeAllViews();
        List<ProfileRepository.StrengthGoal> goals =
                new ArrayList<>(repository.getStrengthGoals().values());
        if (goals.isEmpty()) {
            addEmptyRow(llStrengthGoals, R.string.profile_strength_goal_empty);
            return;
        }
        for (ProfileRepository.StrengthGoal goal : goals) {
            addManageRow(
                    llStrengthGoals,
                    workoutLabel(goal.workoutType) + " · " + goal.exercise,
                    AppSettings.formatWeight(this, goal.targetKg, 1) + " 1RM",
                    () -> confirmDeleteStrengthGoal(goal)
            );
        }
    }

    private void addManageRow(
            LinearLayout container,
            String title,
            String value,
            Runnable deleteAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.setBackgroundResource(R.drawable.bg_training_input_compact);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        titleView.setTextSize(12);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.training_gold_highlight));
        valueView.setTextSize(11);
        valueView.setPadding(0, dp(3), 0, 0);
        texts.addView(titleView);
        texts.addView(valueView);
        row.addView(texts, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView delete = new TextView(this);
        delete.setText(R.string.delete);
        delete.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        delete.setTextSize(9);
        delete.setTypeface(null, android.graphics.Typeface.BOLD);
        delete.setGravity(android.view.Gravity.CENTER);
        delete.setPadding(dp(10), dp(10), dp(8), dp(10));
        delete.setOnClickListener(v -> deleteAction.run());
        row.addView(delete);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(8);
        container.addView(row, rowParams);
    }

    private void addEmptyRow(LinearLayout container, int textResource) {
        TextView empty = new TextView(this);
        empty.setText(textResource);
        empty.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        empty.setTextSize(11);
        empty.setPadding(0, dp(8), 0, 0);
        container.addView(empty);
    }

    private void confirmDeleteWeightMeasurement(LocalDate date) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_measurement_delete_title)
                .setMessage(getString(
                        R.string.profile_measurement_delete_message,
                        date.format(DISPLAY_DATE)
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.removeWeightMeasurement(date);
                    setDisplayedWeight(etProfileWeight, repository.load().currentWeightKg);
                    refreshWeightMeasurements();
                })
                .show();
    }

    private void confirmDeleteBodyFatMeasurement(LocalDate date) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_body_fat_delete_title)
                .setMessage(getString(
                        R.string.profile_body_fat_delete_message,
                        date.format(DISPLAY_DATE)
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.removeBodyFatMeasurement(date);
                    etProfileBodyFat.setText(
                            optionalNumber(repository.load().bodyFatPercent)
                    );
                    refreshBodyFatMeasurements();
                })
                .show();
    }

    private void confirmDeleteStrengthGoal(ProfileRepository.StrengthGoal goal) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_strength_goal_delete_title)
                .setMessage(getString(
                        R.string.profile_strength_goal_delete_message,
                        goal.exercise
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.removeStrengthGoal(goal.workoutType, goal.exercise);
                    refreshStrengthGoals();
                    updateStrengthGoalInput();
                })
                .show();
    }

    private String workoutLabel(String workoutType) {
        if (WorkoutStorage.TYPE_PULL.equals(workoutType)) {
            return "Pull";
        }
        if (WorkoutStorage.TYPE_LEG.equals(workoutType)) {
            return "Leg";
        }
        return "Push";
    }

    private void updateImpactInfo(String goalId) {
        int textRes;
        if (ProfileRepository.GOAL_STRENGTH.equals(goalId)) {
            textRes = R.string.profile_impact_strength;
        } else if (ProfileRepository.GOAL_WEIGHT_LOSS.equals(goalId)) {
            textRes = R.string.profile_impact_weight_loss;
        } else if (ProfileRepository.GOAL_FITNESS.equals(goalId)) {
            textRes = R.string.profile_impact_fitness;
        } else {
            textRes = R.string.profile_impact_muscle;
        }
        int configuredGoal = getConfiguredWeeklyGoal();
        ProfileInsights.SessionRecommendation recommendation =
                ProfileInsights.recommendSessions(
                        getSelectedOptionId(spinnerActivityLevel),
                        getSelectedOptionId(spinnerExperience),
                        configuredGoal
                );
        String range = recommendation.min == recommendation.max
                ? getString(
                        R.string.profile_recommendation_single,
                        recommendation.min
                )
                : getString(
                        R.string.profile_recommendation_range,
                        recommendation.min,
                        recommendation.max
                );
        int statusResource;
        if (recommendation.difference < 0) {
            statusResource = R.string.profile_recommendation_below;
        } else if (recommendation.difference > 0) {
            statusResource = R.string.profile_recommendation_above;
        } else {
            statusResource = R.string.profile_recommendation_suitable;
        }
        InfoDialogHelper.bind(
                findViewById(R.id.infoProfileTrainingGoal),
                "Trainingsziel",
                infoMessage(
                        getString(textRes),
                        InfoDialogHelper.Texts.profileTarget(),
                        InfoDialogHelper.Texts.profileTrainingGoal(),
                        getString(R.string.profile_recommendation_intro, range)
                                + " "
                                + getString(statusResource)
                )
        );
    }

    private int getConfiguredWeeklyGoal() {
        try {
            return Math.max(
                    1,
                    Math.min(
                            7,
                            Integer.parseInt(
                                    etTrainingGoalPerWeek.getText()
                                            .toString()
                                            .trim()
                            )
                    )
            );
        } catch (NumberFormatException ignored) {
            return 3;
        }
    }

    private Set<DayOfWeek> getSelectedDays() {
        Set<DayOfWeek> selected = new HashSet<>();
        for (int i = 0; i < dayButtons.size(); i++) {
            if (dayButtons.get(i).isChecked()) {
                selected.add(dayValues.get(i));
            }
        }
        return selected;
    }

    private double parseDisplayedWeight(
            EditText input,
            double minKg,
            double maxKg,
            boolean required) {
        double displayedMin = AppSettings.fromStoredKg(this, minKg);
        double displayedMax = AppSettings.fromStoredKg(this, maxKg);
        double displayed = parseNumber(
                input,
                displayedMin,
                displayedMax,
                required
        );
        return displayed <= 0 ? 0 : AppSettings.toStoredKg(this, displayed);
    }

    private double parseNumber(
            EditText input,
            double min,
            double max,
            boolean required) {
        String value = input.getText().toString().trim().replace(',', '.');
        if (value.isEmpty()) {
            if (required) {
                input.setError(getString(R.string.profile_required));
            }
            return 0;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                input.setError(getString(
                        R.string.profile_value_range,
                        optionalNumber(min),
                        optionalNumber(max)
                ));
                return 0;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            input.setError(getString(R.string.profile_invalid_number));
            return 0;
        }
    }

    private int parseInt(EditText input, int min, int max, boolean required) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            if (required) {
                input.setError(getString(R.string.profile_required));
            }
            return 0;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                input.setError(getString(
                        R.string.profile_value_range,
                        String.valueOf(min),
                        String.valueOf(max)
                ));
                return 0;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            input.setError(getString(R.string.profile_invalid_number));
            return 0;
        }
    }

    private boolean hasErrors() {
        return etProfileWeight.getError() != null
                || etProfileHeight.getError() != null
                || etProfileBirthYear.getError() != null
                || etProfileBodyFat.getError() != null
                || etBodyFatMeasurement.getError() != null
                || etBodyFatMeasurementDate.getError() != null
                || etTargetWeight.getError() != null
                || etTargetDate.getError() != null
                || etTrainingGoalPerWeek.getError() != null
                || etVolumeGoal.getError() != null;
    }

    private void clearErrors() {
        for (EditText input : Arrays.asList(
                etProfileWeight,
                etProfileHeight,
                etProfileBirthYear,
                etProfileBodyFat,
                etBodyFatMeasurement,
                etBodyFatMeasurementDate,
                etTargetWeight,
                etTargetDate,
                etTrainingGoalPerWeek,
                etVolumeGoal
        )) {
            input.setError(null);
        }
    }

    private void setDisplayedWeight(EditText input, double kilograms) {
        input.setText(
                kilograms > 0
                        ? optionalNumber(AppSettings.fromStoredKg(this, kilograms))
                        : ""
        );
    }

    private String optionalNumber(double value) {
        if (value <= 0) {
            return "";
        }
        return Math.abs(value - Math.rint(value)) < 0.01
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private String optionalInt(int value) {
        return value > 0 ? String.valueOf(value) : "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getSelectedOptionId(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected instanceof Option ? ((Option) selected).id : "";
    }

    private void selectOption(Spinner spinner, String id) {
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item instanceof Option && ((Option) item).id.equals(id)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private static final class Option {
        final String id;
        final String label;

        Option(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class StrengthExerciseOption {
        final String workoutType;
        final String exercise;

        StrengthExerciseOption(String workoutType, String exercise) {
            this.workoutType = workoutType;
            this.exercise = exercise == null ? "" : exercise.trim();
        }

        String key() {
            return ProfileRepository.strengthGoalKey(workoutType, exercise);
        }

        @Override
        public String toString() {
            String type = WorkoutStorage.TYPE_PULL.equals(workoutType)
                    ? "Pull"
                    : WorkoutStorage.TYPE_LEG.equals(workoutType) ? "Leg" : "Push";
            return type + " · " + exercise;
        }
    }
}
