package com.example.gym_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProfileGoalsActivity extends IronxActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_PROFILE_WEIGHT = "profile_weight";
    private static final String KEY_GOAL = "goal";
    private static final String KEY_CALORIE_GOAL = "calorie_goal";
    private static final String KEY_TRAINING_GOAL_PER_WEEK = "training_goal_per_week";

    private EditText etProfileName;
    private EditText etProfileWeight;
    private Spinner spinnerGoal;
    private EditText etCalorieGoal;
    private EditText etTrainingGoalPerWeek;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_goals);
        applyWindowInsets();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etProfileName = findViewById(R.id.etProfileName);
        etProfileWeight = findViewById(R.id.etProfileWeight);
        etProfileWeight.setHint(
                getString(R.string.profile_weight_label)
                        + " (" + AppSettings.getWeightUnit(this) + ")"
        );
        spinnerGoal = findViewById(R.id.spinnerGoal);
        etCalorieGoal = findViewById(R.id.etCalorieGoal);
        etTrainingGoalPerWeek = findViewById(R.id.etTrainingGoalPerWeek);
        View btnSaveProfileGoals = findViewById(R.id.btnSaveProfileGoals);

        setupGoalSpinner();
        loadProfileGoals();

        findViewById(R.id.btnBackProfileGoals).setOnClickListener(v -> finish());
        btnSaveProfileGoals.setOnClickListener(v -> saveProfileGoals());
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootProfileGoalsLayout);
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

    private void setupGoalSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.settings_goals,
                R.layout.spinner_item_white
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerGoal.setAdapter(adapter);
    }

    private void loadProfileGoals() {
        etProfileName.setText(prefs.getString(KEY_PROFILE_NAME, ""));
        String storedWeight = prefs.getString(KEY_PROFILE_WEIGHT, "");
        if (storedWeight == null || storedWeight.trim().isEmpty()) {
            etProfileWeight.setText("");
        } else {
            try {
                double kilograms = Double.parseDouble(storedWeight.replace(',', '.'));
                etProfileWeight.setText(String.format(
                        java.util.Locale.getDefault(),
                        "%.1f",
                        AppSettings.fromStoredKg(this, kilograms)
                ));
            } catch (NumberFormatException ignored) {
                etProfileWeight.setText(storedWeight);
            }
        }
        etCalorieGoal.setText(prefs.getString(KEY_CALORIE_GOAL, ""));
        etTrainingGoalPerWeek.setText(String.valueOf(
                prefs.getInt(KEY_TRAINING_GOAL_PER_WEEK, 3)
        ));
        setSpinnerByValue(spinnerGoal, prefs.getString(KEY_GOAL, getString(R.string.settings_goal_muscle)));
    }

    private void saveProfileGoals() {
        String name = etProfileName.getText().toString().trim();
        String weight = normalizeStoredWeight(
                etProfileWeight.getText().toString().trim()
        );
        String goal = spinnerGoal.getSelectedItem() != null ? spinnerGoal.getSelectedItem().toString() : "";
        String calories = etCalorieGoal.getText().toString().trim();
        int weeklyGoal = parseWeeklyGoal(
                etTrainingGoalPerWeek.getText().toString().trim()
        );

        prefs.edit()
                .putString(KEY_PROFILE_NAME, name)
                .putString(KEY_PROFILE_WEIGHT, weight)
                .putString(KEY_GOAL, goal)
                .putString(KEY_CALORIE_GOAL, calories)
                .putInt(KEY_TRAINING_GOAL_PER_WEEK, weeklyGoal)
                .apply();

        etTrainingGoalPerWeek.setText(String.valueOf(weeklyGoal));
        Toast.makeText(this, R.string.profile_goals_saved, Toast.LENGTH_SHORT).show();
    }

    private String normalizeStoredWeight(String displayedWeight) {
        if (displayedWeight.isEmpty()) {
            return "";
        }
        try {
            double value = Double.parseDouble(displayedWeight.replace(',', '.'));
            return String.format(
                    java.util.Locale.ROOT,
                    "%.2f",
                    AppSettings.toStoredKg(this, value)
            );
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private int parseWeeklyGoal(String value) {
        try {
            return Math.max(1, Math.min(7, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return 3;
        }
    }

    private void setSpinnerByValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && item.toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }
}
