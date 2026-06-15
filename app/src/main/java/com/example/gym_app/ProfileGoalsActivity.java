package com.example.gym_app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;
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
    private EditText etStrengthGoal;
    private EditText etVolumeGoal;
    private Spinner spinnerGoal;
    private Spinner spinnerActivityLevel;
    private Spinner spinnerExperience;
    private TextView tvProfileImpact;
    private ProfileRepository repository;
    private LocalDate selectedTargetDate;

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
        setupDayButtons();
        setupTargetDate();
        setupUnitHints();
        loadProfile();

        findViewById(R.id.btnBackProfileGoals).setOnClickListener(v -> finish());
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
        etStrengthGoal = findViewById(R.id.etStrengthGoal);
        etVolumeGoal = findViewById(R.id.etVolumeGoal);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel);
        spinnerExperience = findViewById(R.id.spinnerExperience);
        tvProfileImpact = findViewById(R.id.tvProfileImpact);

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
        spinnerGoal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                updateImpactText(getSelectedOptionId(spinnerGoal));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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

    private void setupDayButtons() {
        for (MaterialButton button : dayButtons) {
            button.setCheckable(true);
        }
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

    private void setupUnitHints() {
        String unit = AppSettings.getWeightUnit(this);
        etProfileWeight.setHint(getString(R.string.profile_weight_hint_unit, unit));
        etTargetWeight.setHint(getString(R.string.profile_target_weight_hint, unit));
        etStrengthGoal.setHint(getString(R.string.profile_strength_goal_hint, unit));
        etVolumeGoal.setHint(getString(R.string.profile_volume_goal_hint, unit));
    }

    private void loadProfile() {
        ProfileRepository.Profile profile = repository.load();
        etProfileName.setText(profile.name);
        setDisplayedWeight(etProfileWeight, profile.currentWeightKg);
        setDisplayedWeight(etTargetWeight, profile.targetWeightKg);
        etProfileHeight.setText(optionalInt(profile.heightCm));
        etProfileBirthYear.setText(optionalInt(profile.birthYear));
        etProfileBodyFat.setText(optionalNumber(profile.bodyFatPercent));
        etTrainingGoalPerWeek.setText(String.valueOf(profile.weeklyTrainingGoal));
        setDisplayedWeight(etStrengthGoal, profile.strengthGoalKg);
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
        updateImpactText(profile.goalId);
    }

    private void saveProfile() {
        clearErrors();
        ProfileRepository.Profile profile = new ProfileRepository.Profile();
        profile.name = etProfileName.getText().toString().trim();
        profile.currentWeightKg = parseDisplayedWeight(
                etProfileWeight,
                25,
                400,
                false
        );
        profile.heightCm = parseInt(etProfileHeight, 100, 250, false);
        profile.birthYear = parseInt(
                etProfileBirthYear,
                1900,
                LocalDate.now().getYear(),
                false
        );
        profile.bodyFatPercent = parseNumber(
                etProfileBodyFat,
                2,
                70,
                false
        );
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
        profile.strengthGoalKg = parseDisplayedWeight(
                etStrengthGoal,
                1,
                1000,
                false
        );
        profile.weeklyVolumeGoalKg = parseDisplayedWeight(
                etVolumeGoal,
                1,
                1_000_000,
                false
        );

        if (hasErrors()) {
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

        repository.save(profile, LocalDate.now());
        Toast.makeText(this, R.string.profile_goals_saved, Toast.LENGTH_SHORT).show();
        updateImpactText(profile.goalId);
    }

    private void updateImpactText(String goalId) {
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
        tvProfileImpact.setText(textRes);
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
                || etTargetWeight.getError() != null
                || etTargetDate.getError() != null
                || etTrainingGoalPerWeek.getError() != null
                || etStrengthGoal.getError() != null
                || etVolumeGoal.getError() != null;
    }

    private void clearErrors() {
        for (EditText input : Arrays.asList(
                etProfileWeight,
                etProfileHeight,
                etProfileBirthYear,
                etProfileBodyFat,
                etTargetWeight,
                etTargetDate,
                etTrainingGoalPerWeek,
                etStrengthGoal,
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
}
