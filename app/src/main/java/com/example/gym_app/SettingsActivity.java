package com.example.gym_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_PROFILE_WEIGHT = "profile_weight";
    private static final String KEY_GOAL = "goal";
    private static final String KEY_CALORIE_GOAL = "calorie_goal";
    private static final String KEY_THEME = "theme";
    private static final String KEY_UNITS = "units";
    private static final String KEY_LANGUAGE = "language";

    private EditText etProfileName;
    private EditText etProfileWeight;
    private Spinner spinnerGoal;
    private EditText etCalorieGoal;
    private Spinner spinnerTheme;
    private Spinner spinnerUnits;
    private Spinner spinnerLanguage;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etProfileName = findViewById(R.id.etProfileName);
        etProfileWeight = findViewById(R.id.etProfileWeight);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        etCalorieGoal = findViewById(R.id.etCalorieGoal);
        spinnerTheme = findViewById(R.id.spinnerTheme);
        spinnerUnits = findViewById(R.id.spinnerUnits);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);

        setupSpinner(spinnerGoal, R.array.settings_goals);
        setupSpinner(spinnerTheme, R.array.settings_theme_options);
        setupSpinner(spinnerUnits, R.array.settings_units_options);
        setupSpinner(spinnerLanguage, R.array.settings_language_options);

        loadSettings();
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void setupSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayRes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadSettings() {
        etProfileName.setText(prefs.getString(KEY_PROFILE_NAME, ""));
        etProfileWeight.setText(prefs.getString(KEY_PROFILE_WEIGHT, ""));
        etCalorieGoal.setText(prefs.getString(KEY_CALORIE_GOAL, ""));

        setSpinnerByValue(spinnerGoal, prefs.getString(KEY_GOAL, getString(R.string.settings_goal_muscle)));
        setSpinnerByValue(spinnerTheme, prefs.getString(KEY_THEME, getString(R.string.settings_theme_dark)));
        setSpinnerByValue(spinnerUnits, prefs.getString(KEY_UNITS, getString(R.string.settings_unit_kg)));
        setSpinnerByValue(spinnerLanguage, prefs.getString(KEY_LANGUAGE, getString(R.string.settings_language_german)));
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

    private void saveSettings() {
        String name = etProfileName.getText().toString().trim();
        String weight = etProfileWeight.getText().toString().trim();
        String goal = spinnerGoal.getSelectedItem() != null ? spinnerGoal.getSelectedItem().toString() : "";
        String calories = etCalorieGoal.getText().toString().trim();
        String theme = spinnerTheme.getSelectedItem() != null ? spinnerTheme.getSelectedItem().toString() : "";
        String units = spinnerUnits.getSelectedItem() != null ? spinnerUnits.getSelectedItem().toString() : "";
        String language = spinnerLanguage.getSelectedItem() != null ? spinnerLanguage.getSelectedItem().toString() : "";

        prefs.edit()
                .putString(KEY_PROFILE_NAME, name)
                .putString(KEY_PROFILE_WEIGHT, weight)
                .putString(KEY_GOAL, goal)
                .putString(KEY_CALORIE_GOAL, calories)
                .putString(KEY_THEME, theme)
                .putString(KEY_UNITS, units)
                .putString(KEY_LANGUAGE, language)
                .apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
}
