package com.example.gym_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

final class TrainingSetInputController {

    private final IronxActivity activity;
    private final LinearLayout container;
    private final List<SetInputRow> rows = new ArrayList<>();
    private final LinearLayout controls;
    private final TextView countText;
    private final TextView removeButton;
    private final TextView addButton;
    private String weightUnit;

    TrainingSetInputController(
            IronxActivity activity,
            LinearLayout container,
            int insertionIndex,
            EditText[] initialWeights,
            EditText[] initialReps
    ) {
        this.activity = activity;
        this.container = container;
        weightUnit = AppSettings.getWeightUnit(activity);

        for (int i = 0; i < TrainingSetConfiguration.MIN_SETS; i++) {
            rows.add(new SetInputRow(
                    initialWeights[i],
                    initialReps[i],
                    null
            ));
        }

        controls = createControls();
        countText = controls.findViewWithTag("set_count");
        removeButton = controls.findViewWithTag("remove_set");
        addButton = controls.findViewWithTag("add_set");
        container.addView(controls, insertionIndex);

        removeButton.setOnClickListener(view -> removeLastSet());
        addButton.setOnClickListener(view -> addSet());
        setWeightUnit(weightUnit);
        updateControls();
    }

    int getSetCount() {
        return rows.size();
    }

    EditText getWeightInput(int index) {
        return rows.get(index).weight;
    }

    EditText getRepsInput(int index) {
        return rows.get(index).reps;
    }

    boolean hasAnyInput() {
        for (SetInputRow row : rows) {
            if (!text(row.weight).isEmpty() || !text(row.reps).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    void clearInputs() {
        for (SetInputRow row : rows) {
            row.weight.setText("");
            row.weight.setError(null);
            row.reps.setText("");
            row.reps.setError(null);
        }
    }

    void setWeightUnit(String unit) {
        weightUnit = unit == null || unit.trim().isEmpty() ? "kg" : unit;
        for (SetInputRow row : rows) {
            row.weight.setHint(weightUnit);
        }
    }

    void saveState(Bundle outState, String keyPrefix) {
        outState.putInt(keyPrefix + "_count", rows.size());
        ArrayList<String> weights = new ArrayList<>();
        ArrayList<String> reps = new ArrayList<>();
        for (SetInputRow row : rows) {
            weights.add(text(row.weight));
            reps.add(text(row.reps));
        }
        outState.putStringArrayList(keyPrefix + "_weights", weights);
        outState.putStringArrayList(keyPrefix + "_reps", reps);
    }

    void restoreState(Bundle savedState, String keyPrefix) {
        if (savedState == null) {
            return;
        }
        int targetCount = TrainingSetConfiguration.clamp(
                savedState.getInt(
                        keyPrefix + "_count",
                        TrainingSetConfiguration.MIN_SETS
                )
        );
        while (rows.size() < targetCount) {
            addSetInternal();
        }

        ArrayList<String> weights =
                savedState.getStringArrayList(keyPrefix + "_weights");
        ArrayList<String> reps =
                savedState.getStringArrayList(keyPrefix + "_reps");
        for (int i = 0; i < rows.size(); i++) {
            if (weights != null && i < weights.size()) {
                rows.get(i).weight.setText(weights.get(i));
            }
            if (reps != null && i < reps.size()) {
                rows.get(i).reps.setText(reps.get(i));
            }
        }
        updateControls();
    }

    private void addSet() {
        if (rows.size() >= TrainingSetConfiguration.MAX_SETS) {
            return;
        }
        addSetInternal();
        updateControls();
    }

    private void addSetInternal() {
        int setNumber = rows.size() + 1;
        SetInputRow row = createSetRow(setNumber);
        int controlsIndex = container.indexOfChild(controls);
        container.addView(row.wrapper, controlsIndex);
        rows.add(row);
    }

    private void removeLastSet() {
        if (rows.size() <= TrainingSetConfiguration.MIN_SETS) {
            return;
        }
        SetInputRow last = rows.get(rows.size() - 1);
        if (!text(last.weight).isEmpty() || !text(last.reps).isEmpty()) {
            String message = activity.getString(
                    R.string.training_set_remove_non_empty,
                    rows.size()
            );
            last.weight.setError(message);
            last.reps.setError(message);
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            return;
        }
        container.removeView(last.wrapper);
        rows.remove(rows.size() - 1);
        updateControls();
    }

    private SetInputRow createSetRow(int setNumber) {
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(matchWrap());

        View divider = new View(activity);
        divider.setBackgroundColor(ContextCompat.getColor(
                activity,
                R.color.divider
        ));
        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
        dividerParams.setMargins(dp(52), 0, dp(14), 0);
        wrapper.addView(divider, dividerParams);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(8), dp(14), dp(8));
        wrapper.addView(row, matchWrap());

        TextView number = new TextView(activity);
        number.setText(String.valueOf(setNumber));
        number.setTextColor(ContextCompat.getColor(
                activity,
                R.color.training_gold
        ));
        number.setTextSize(22);
        number.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(28), wrap()));

        row.addView(createVerticalDivider(true));

        EditText weight = createInput(true);
        row.addView(createInputColumn(
                R.string.training_weight,
                weight,
                true
        ));

        row.addView(createVerticalDivider(false));

        EditText reps = createInput(false);
        row.addView(createInputColumn(
                R.string.training_reps,
                reps,
                false
        ));

        return new SetInputRow(weight, reps, wrapper);
    }

    private LinearLayout createInputColumn(
            int labelResource,
            EditText input,
            boolean addEndMargin
    ) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, wrap(), 1f);
        if (addEndMargin) {
            params.setMarginEnd(dp(8));
        }
        column.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText(labelResource);
        label.setTextColor(ContextCompat.getColor(
                activity,
                R.color.text_tertiary
        ));
        label.setTextSize(8);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        label.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams labelParams = wrapWrap();
        labelParams.bottomMargin = dp(3);
        column.addView(label, labelParams);
        column.addView(input);
        return column;
    }

    private EditText createInput(boolean weight) {
        EditText input = new EditText(activity);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
        ));
        input.setHint(weight ? weightUnit : "—");
        input.setTextColor(ContextCompat.getColor(
                activity,
                R.color.text_primary
        ));
        input.setHintTextColor(ContextCompat.getColor(
                activity,
                R.color.text_tertiary
        ));
        input.setBackgroundResource(R.drawable.bg_training_input_compact);
        input.setPadding(dp(10), 0, dp(10), 0);
        input.setTextSize(14);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setSingleLine(true);
        input.setInputType(weight
                ? InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_NUMBER);
        return input;
    }

    private View createVerticalDivider(boolean addStartMargin) {
        View divider = new View(activity);
        divider.setBackgroundColor(ContextCompat.getColor(
                activity,
                R.color.divider
        ));
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(dp(1), dp(36));
        if (addStartMargin) {
            params.setMarginStart(dp(10));
        }
        params.setMarginEnd(dp(10));
        divider.setLayoutParams(params);
        return divider;
    }

    private LinearLayout createControls() {
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = matchWrap();
        wrapperParams.setMargins(dp(14), dp(4), dp(14), dp(12));
        wrapper.setLayoutParams(wrapperParams);

        View divider = new View(activity);
        divider.setBackgroundColor(ContextCompat.getColor(
                activity,
                R.color.divider
        ));
        wrapper.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);
        wrapper.addView(row, matchWrap());

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, wrap(), 1f));

        TextView title = new TextView(activity);
        title.setText(R.string.training_set_count);
        title.setTextColor(ContextCompat.getColor(
                activity,
                R.color.text_secondary
        ));
        title.setTextSize(11);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        title.setLetterSpacing(0.1f);
        labels.addView(title);

        TextView count = new TextView(activity);
        count.setTag("set_count");
        count.setTextColor(ContextCompat.getColor(
                activity,
                R.color.text_tertiary
        ));
        count.setTextSize(10);
        labels.addView(count);

        TextView remove = createControlButton(
                "−",
                R.drawable.bg_circle_dark,
                R.color.training_gold,
                R.string.training_remove_set
        );
        remove.setTag("remove_set");
        row.addView(remove);

        TextView add = createControlButton(
                "+",
                R.drawable.bg_training_primary_action,
                R.color.black,
                R.string.training_add_set
        );
        add.setTag("add_set");
        LinearLayout.LayoutParams addParams =
                (LinearLayout.LayoutParams) add.getLayoutParams();
        addParams.setMarginStart(dp(8));
        add.setLayoutParams(addParams);
        row.addView(add);
        return wrapper;
    }

    private TextView createControlButton(
            String text,
            int backgroundResource,
            int textColorResource,
            int contentDescriptionResource
    ) {
        TextView button = new TextView(activity);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        button.setBackgroundResource(backgroundResource);
        button.setGravity(Gravity.CENTER);
        button.setText(text);
        button.setTextColor(ContextCompat.getColor(
                activity,
                textColorResource
        ));
        button.setTextSize(24);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setContentDescription(activity.getString(
                contentDescriptionResource
        ));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void updateControls() {
        countText.setText(activity.getString(
                R.string.training_set_count_value,
                rows.size(),
                TrainingSetConfiguration.MAX_SETS
        ));
        setControlEnabled(
                removeButton,
                rows.size() > TrainingSetConfiguration.MIN_SETS
        );
        setControlEnabled(
                addButton,
                rows.size() < TrainingSetConfiguration.MAX_SETS
        );
    }

    private void setControlEnabled(TextView view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.35f);
    }

    private String text(EditText input) {
        return input.getText().toString().trim();
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources()
                .getDisplayMetrics().density);
    }

    private int wrap() {
        return LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private static final class SetInputRow {
        final EditText weight;
        final EditText reps;
        final LinearLayout wrapper;

        SetInputRow(EditText weight, EditText reps, LinearLayout wrapper) {
            this.weight = weight;
            this.reps = reps;
            this.wrapper = wrapper;
        }
    }
}
