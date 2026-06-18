package com.example.gym_app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.util.Locale;

public class WorkoutActivity extends IronxActivity {

    private static final String[] TEMPLATE_NAMES = {
            "Keine Vorlage",
            "Oberkörper",
            "Unterkörper",
            "Ganzkörper",
            "Brust und Rücken",
            "Arme",
            "Schultern und Arme",
            "Core"
    };

    private static final String[] TEMPLATE_FOCUS = {
            WorkoutTypeRepository.FOCUS_OTHER,
            WorkoutTypeRepository.FOCUS_UPPER,
            WorkoutTypeRepository.FOCUS_LOWER,
            WorkoutTypeRepository.FOCUS_FULL,
            WorkoutTypeRepository.FOCUS_UPPER,
            WorkoutTypeRepository.FOCUS_UPPER,
            WorkoutTypeRepository.FOCUS_UPPER,
            WorkoutTypeRepository.FOCUS_CORE
    };

    private static final String[] TEMPLATE_DESCRIPTIONS = {
            "",
            "Brust, Rücken, Schultern und Arme",
            "Beine, Gluteus und Waden",
            "Kraft und Bewegung für den ganzen Körper",
            "Drück- und Zugbewegungen für den Oberkörper",
            "Bizeps, Trizeps und Unterarme",
            "Schulterkraft und gezieltes Armtraining",
            "Rumpfkraft, Stabilität und Kontrolle"
    };

    private LinearLayout customWorkoutContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        applyWindowInsets();

        findViewById(R.id.btnBackWorkout).setOnClickListener(v -> finish());
        findViewById(R.id.pushCard).setOnClickListener(v ->
                startActivity(new Intent(this, PushActivity.class)));
        findViewById(R.id.pullCard).setOnClickListener(v ->
                startActivity(new Intent(this, PullActivity.class)));
        findViewById(R.id.legCard).setOnClickListener(v ->
                startActivity(new Intent(this, LegActivity.class)));

        customWorkoutContainer = findViewById(R.id.customWorkoutContainer);
        findViewById(R.id.btnAddCustomWorkout)
                .setOnClickListener(v -> showCustomWorkoutDialog(null));
        renderCustomWorkouts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (customWorkoutContainer != null) {
            renderCustomWorkouts();
        }
    }

    private void renderCustomWorkouts() {
        customWorkoutContainer.removeAllViews();
        for (WorkoutTypeRepository.WorkoutType type :
                WorkoutTypeRepository.getActiveTypes(this)) {
            if (!type.builtIn) {
                customWorkoutContainer.addView(createCustomWorkoutCard(type));
            }
        }
    }

    private View createCustomWorkoutCard(WorkoutTypeRepository.WorkoutType type) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(12), dp(16));
        card.setMinimumHeight(dp(108));
        card.setBackgroundResource(R.drawable.bg_workout_card);
        card.setForeground(ContextCompat.getDrawable(
                this,
                android.R.drawable.list_selector_background
        ));
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(5));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        FrameLayout iconBackground = new FrameLayout(this);
        iconBackground.setBackgroundResource(R.drawable.bg_icon_circle);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(72), dp(72));
        card.addView(iconBackground, iconParams);

        TextView initial = new TextView(this);
        initial.setText(type.name.substring(0, 1).toUpperCase(Locale.GERMANY));
        initial.setTextColor(ContextCompat.getColor(this, R.color.gold_light));
        initial.setTextSize(28);
        initial.setGravity(Gravity.CENTER);
        initial.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        iconBackground.addView(initial, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(16));
        card.addView(texts, textParams);

        TextView focus = new TextView(this);
        focus.setText(type.focusLabel);
        focus.setTextColor(ContextCompat.getColor(this, R.color.gold_dark));
        focus.setTextSize(9);
        focus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        focus.setLetterSpacing(0.08f);
        texts.addView(focus);

        TextView name = new TextView(this);
        name.setText(type.name.toUpperCase(Locale.GERMANY));
        name.setTextColor(ContextCompat.getColor(this, R.color.gold_light));
        name.setTextSize(20);
        name.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        LinearLayout.LayoutParams nameParams = wrapWrap();
        nameParams.topMargin = dp(4);
        texts.addView(name, nameParams);

        TextView description = new TextView(this);
        description.setText(type.description);
        description.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        description.setTextSize(11);
        LinearLayout.LayoutParams descriptionParams = wrapWrap();
        descriptionParams.topMargin = dp(3);
        texts.addView(description, descriptionParams);

        ImageButton edit = new ImageButton(this);
        edit.setImageResource(R.drawable.ic_ironx_edit);
        edit.setBackgroundResource(R.drawable.bg_circle_dark);
        edit.setPadding(dp(10), dp(10), dp(10), dp(10));
        edit.setContentDescription(getString(R.string.custom_workout_edit_title));
        edit.setOnClickListener(v -> showCustomWorkoutDialog(type));
        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        editParams.setMarginStart(dp(8));
        card.addView(edit, editParams);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_home);
        chevron.setContentDescription(getString(R.string.open));
        LinearLayout.LayoutParams chevronParams =
                new LinearLayout.LayoutParams(dp(24), dp(24));
        chevronParams.setMarginStart(dp(4));
        card.addView(chevron, chevronParams);

        card.setContentDescription(type.name);
        card.setOnClickListener(v ->
                startActivity(PushActivity.createCustomIntent(this, type.id)));
        return card;
    }

    private void showCustomWorkoutDialog(
            WorkoutTypeRepository.WorkoutType existing
    ) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_custom_workout);

        TextView title = dialog.findViewById(R.id.tvCustomWorkoutDialogTitle);
        Spinner templateSpinner =
                dialog.findViewById(R.id.spinnerCustomWorkoutTemplate);
        Spinner focusSpinner = dialog.findViewById(R.id.spinnerCustomWorkoutFocus);
        EditText nameInput = dialog.findViewById(R.id.etCustomWorkoutName);
        EditText descriptionInput =
                dialog.findViewById(R.id.etCustomWorkoutDescription);
        MaterialButton cancel = dialog.findViewById(R.id.btnCustomWorkoutCancel);
        MaterialButton save = dialog.findViewById(R.id.btnCustomWorkoutSave);
        MaterialButton archive = dialog.findViewById(R.id.btnCustomWorkoutArchive);

        ArrayAdapter<String> templateAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                TEMPLATE_NAMES
        );
        templateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        templateSpinner.setAdapter(templateAdapter);

        ArrayAdapter<String> focusAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                WorkoutTypeRepository.focusLabels()
        );
        focusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        focusSpinner.setAdapter(focusAdapter);

        if (existing != null) {
            title.setText(R.string.custom_workout_edit_title);
            nameInput.setText(existing.name);
            descriptionInput.setText(existing.description);
            focusSpinner.setSelection(
                    WorkoutTypeRepository.focusIndex(existing.focus)
            );
            archive.setVisibility(View.VISIBLE);
        }

        templateSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position <= 0 || existing != null) {
                            return;
                        }
                        nameInput.setText(TEMPLATE_NAMES[position]);
                        descriptionInput.setText(TEMPLATE_DESCRIPTIONS[position]);
                        focusSpinner.setSelection(
                                WorkoutTypeRepository.focusIndex(
                                        TEMPLATE_FOCUS[position]
                                )
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError(getString(R.string.custom_workout_required));
                return;
            }
            int focusPosition = focusSpinner.getSelectedItemPosition();
            String[] focusValues = WorkoutTypeRepository.focusValues();
            String focus = focusPosition >= 0 && focusPosition < focusValues.length
                    ? focusValues[focusPosition]
                    : WorkoutTypeRepository.FOCUS_OTHER;
            boolean success;
            if (existing == null) {
                success = WorkoutTypeRepository.create(
                        this,
                        name,
                        descriptionInput.getText().toString(),
                        focus
                ) != null;
            } else {
                success = WorkoutTypeRepository.update(
                        this,
                        existing.id,
                        name,
                        descriptionInput.getText().toString(),
                        focus
                );
            }
            if (!success) {
                nameInput.setError(getString(R.string.custom_workout_duplicate));
                Toast.makeText(
                        this,
                        R.string.custom_workout_save_failed,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            dialog.dismiss();
            renderCustomWorkouts();
        });

        archive.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.custom_workout_archive_title)
                .setMessage(getString(
                        R.string.custom_workout_archive_message,
                        existing == null ? "" : existing.name
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.custom_workout_archive, (whichDialog, which) -> {
                    if (existing != null
                            && WorkoutTypeRepository.archive(this, existing.id)) {
                        dialog.dismiss();
                        renderCustomWorkouts();
                    }
                })
                .show());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setBackgroundDrawable(
                    new ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
            shownWindow.setLayout(
                    getResources().getDisplayMetrics().widthPixels - dp(44),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootWorkoutLayout);
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

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
