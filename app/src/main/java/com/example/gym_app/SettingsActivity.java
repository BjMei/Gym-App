package com.example.gym_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_UNITS = "units";
    private static final String KEY_LANGUAGE = "language";

    private Spinner spinnerTheme;
    private Spinner spinnerUnits;
    private Spinner spinnerLanguage;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        spinnerTheme = findViewById(R.id.spinnerTheme);
        spinnerUnits = findViewById(R.id.spinnerUnits);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);

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
        String theme = spinnerTheme.getSelectedItem() != null ? spinnerTheme.getSelectedItem().toString() : "";
        String units = spinnerUnits.getSelectedItem() != null ? spinnerUnits.getSelectedItem().toString() : "";
        String language = spinnerLanguage.getSelectedItem() != null ? spinnerLanguage.getSelectedItem().toString() : "";

        prefs.edit()
                .putString(KEY_THEME, theme)
                .putString(KEY_UNITS, units)
                .putString(KEY_LANGUAGE, language)
                .apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
}
