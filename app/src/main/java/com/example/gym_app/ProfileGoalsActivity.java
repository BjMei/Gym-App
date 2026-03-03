package com.example.gym_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileGoalsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_PROFILE_WEIGHT = "profile_weight";
    private static final String KEY_GOAL = "goal";
    private static final String KEY_CALORIE_GOAL = "calorie_goal";

    private EditText etProfileName;
    private EditText etProfileWeight;
    private Spinner spinnerGoal;
    private EditText etCalorieGoal;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_goals);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etProfileName = findViewById(R.id.etProfileName);
        etProfileWeight = findViewById(R.id.etProfileWeight);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        etCalorieGoal = findViewById(R.id.etCalorieGoal);
        Button btnSaveProfileGoals = findViewById(R.id.btnSaveProfileGoals);

        setupGoalSpinner();
        loadProfileGoals();

        btnSaveProfileGoals.setOnClickListener(v -> saveProfileGoals());
    }

    private void setupGoalSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.settings_goals,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoal.setAdapter(adapter);
    }

    private void loadProfileGoals() {
        etProfileName.setText(prefs.getString(KEY_PROFILE_NAME, ""));
        etProfileWeight.setText(prefs.getString(KEY_PROFILE_WEIGHT, ""));
        etCalorieGoal.setText(prefs.getString(KEY_CALORIE_GOAL, ""));
        setSpinnerByValue(spinnerGoal, prefs.getString(KEY_GOAL, getString(R.string.settings_goal_muscle)));
    }

    private void saveProfileGoals() {
        String name = etProfileName.getText().toString().trim();
        String weight = etProfileWeight.getText().toString().trim();
        String goal = spinnerGoal.getSelectedItem() != null ? spinnerGoal.getSelectedItem().toString() : "";
        String calories = etCalorieGoal.getText().toString().trim();

        prefs.edit()
                .putString(KEY_PROFILE_NAME, name)
                .putString(KEY_PROFILE_WEIGHT, weight)
                .putString(KEY_GOAL, goal)
                .putString(KEY_CALORIE_GOAL, calories)
                .apply();

        Toast.makeText(this, R.string.profile_goals_saved, Toast.LENGTH_SHORT).show();
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
