package com.example.gym_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends IronxActivity {

    private Spinner spinnerTheme;
    private Spinner spinnerUnits;
    private Spinner spinnerTextSize;
    private MaterialSwitch switchAnimations;
    private MaterialSwitch switchKeepScreenOn;
    private MaterialSwitch switchHistoryExpanded;
    private MaterialSwitch switchHaptics;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        applyWindowInsets();

        prefs = AppSettings.preferences(this);

        spinnerTheme = findViewById(R.id.spinnerTheme);
        spinnerUnits = findViewById(R.id.spinnerUnits);
        spinnerTextSize = findViewById(R.id.spinnerTextSize);
        switchAnimations = findViewById(R.id.switchAnimations);
        switchKeepScreenOn = findViewById(R.id.switchKeepScreenOn);
        switchHistoryExpanded = findViewById(R.id.switchHistoryExpanded);
        switchHaptics = findViewById(R.id.switchHaptics);
        View btnSaveSettings = findViewById(R.id.btnSaveSettings);

        setupSpinner(spinnerTheme, R.array.settings_theme_options);
        setupSpinner(spinnerUnits, R.array.settings_units_options);
        setupSpinner(spinnerTextSize, R.array.settings_text_size_options);

        loadSettings();
        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootSettingsLayout);
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

    private void setupSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayRes,
                R.layout.spinner_item_white
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinner.setAdapter(adapter);
    }

    private void loadSettings() {
        String theme = prefs.getString(AppSettings.KEY_THEME, AppSettings.THEME_STANDARD);
        spinnerTheme.setSelection(
                AppSettings.THEME_OLED.equals(theme) || "Light".equalsIgnoreCase(theme) ? 1 : 0
        );
        spinnerUnits.setSelection(AppSettings.usesLbs(this) ? 1 : 0);
        String textSize = prefs.getString(
                AppSettings.KEY_TEXT_SIZE,
                AppSettings.TEXT_STANDARD
        );
        if (AppSettings.TEXT_COMPACT.equals(textSize)) {
            spinnerTextSize.setSelection(0);
        } else if (AppSettings.TEXT_LARGE.equals(textSize)) {
            spinnerTextSize.setSelection(2);
        } else {
            spinnerTextSize.setSelection(1);
        }

        switchAnimations.setChecked(AppSettings.animationsEnabled(this));
        switchKeepScreenOn.setChecked(AppSettings.keepScreenOn(this));
        switchHistoryExpanded.setChecked(AppSettings.historyExpanded(this));
        switchHaptics.setChecked(AppSettings.hapticsEnabled(this));
    }

    private String selectedTextSize() {
        if (spinnerTextSize.getSelectedItemPosition() == 0) {
            return AppSettings.TEXT_COMPACT;
        }
        if (spinnerTextSize.getSelectedItemPosition() == 2) {
            return AppSettings.TEXT_LARGE;
        }
        return AppSettings.TEXT_STANDARD;
    }

    private void saveSettings() {
        String theme = spinnerTheme.getSelectedItemPosition() == 1
                ? AppSettings.THEME_OLED
                : AppSettings.THEME_STANDARD;
        String units = spinnerUnits.getSelectedItemPosition() == 1
                ? AppSettings.UNIT_LBS
                : AppSettings.UNIT_KG;
        prefs.edit()
                .putString(AppSettings.KEY_THEME, theme)
                .putString(AppSettings.KEY_UNITS, units)
                .putString(AppSettings.KEY_TEXT_SIZE, selectedTextSize())
                .putBoolean(AppSettings.KEY_ANIMATIONS, switchAnimations.isChecked())
                .putBoolean(AppSettings.KEY_KEEP_SCREEN_ON, switchKeepScreenOn.isChecked())
                .putBoolean(AppSettings.KEY_HISTORY_EXPANDED, switchHistoryExpanded.isChecked())
                .putBoolean(AppSettings.KEY_HAPTICS, switchHaptics.isChecked())
                .commit();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        Intent restart = new Intent(this, MainActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
        if (!switchAnimations.isChecked()) {
            overridePendingTransition(0, 0);
        }
    }
}
