package com.example.gym_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.Locale;

public class TrainingHistoryActivity extends IronxActivity {

    public static final String EXTRA_WORKOUT_TYPE = "extra_workout_type";
    public static final String EXTRA_WORKOUT_TITLE = "extra_workout_title";
    private TextView tvEmptyState;
    private TextView tvSessionCount;
    private LinearLayout listContainer;
    private boolean showingAllWorkoutTypes;
    private String currentWorkoutType = "";
    private String weightUnit;
    private double weightFactor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_history);

        TextView tvTitle = findViewById(R.id.tvHistoryTitle);
        View rootLayout = findViewById(R.id.rootHistoryLayout);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvSessionCount = findViewById(R.id.tvHistorySessionCount);
        listContainer = findViewById(R.id.historyListContainer);
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

        weightUnit = AppSettings.getWeightUnit(this);
        weightFactor = AppSettings.fromStoredKg(this, 1.0);

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

        String workoutType = getIntent().getStringExtra(EXTRA_WORKOUT_TYPE);
        String title = getIntent().getStringExtra(EXTRA_WORKOUT_TITLE);

        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.history_default_title);
        }
        tvTitle.setText(normalizeTitle(title));

        populateHistory(workoutType);
    }

    private void populateHistory(String workoutType) {
        listContainer.removeAllViews();

        if (workoutType == null) {
            workoutType = "";
        }
        currentWorkoutType = workoutType;
        showingAllWorkoutTypes = workoutType.trim().isEmpty();

        List<WorkoutStorage.DailyWorkout> dailyWorkouts =
                WorkoutStorage.getHistoryWorkouts(this, workoutType);
        tvSessionCount.setText(getResources().getQuantityString(
                R.plurals.history_session_count,
                dailyWorkouts.size(),
                dailyWorkouts.size()
        ));
        if (dailyWorkouts.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        for (WorkoutStorage.DailyWorkout day : dailyWorkouts) {
            View dayCard = createDayCard(day);
            listContainer.addView(dayCard);
        }
    }

    private View createDayCard(WorkoutStorage.DailyWorkout day) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_history_session);
        card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClickable(true);
        header.setFocusable(true);
        header.setMinimumHeight(dpToPx(56));
        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        header.addView(heading, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView tvDate = createTextView(
                day.date,
                16,
                R.color.text_primary,
                Typeface.BOLD
        );
        tvDate.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvDate.setLetterSpacing(0.05f);
        heading.addView(tvDate);

        TextView tvSummary = createTextView(
                buildDaySummary(day),
                10,
                R.color.text_tertiary,
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        summaryParams.topMargin = dpToPx(3);
        tvSummary.setLayoutParams(summaryParams);
        heading.addView(tvSummary);

        LinearLayout headerActions = new LinearLayout(this);
        headerActions.setOrientation(LinearLayout.HORIZONTAL);
        headerActions.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerActions);

        String dayWorkoutLabel = getDayWorkoutLabel(day);
        if (!dayWorkoutLabel.isEmpty()) {
            TextView typeChip = createTextView(
                    dayWorkoutLabel.replace("-", " ").toUpperCase(Locale.GERMAN),
                    8,
                    R.color.training_gold_highlight,
                    Typeface.BOLD
            );
            typeChip.setBackgroundResource(R.drawable.bg_home_chip);
            typeChip.setGravity(Gravity.CENTER);
            typeChip.setLetterSpacing(0.07f);
            typeChip.setPadding(dpToPx(10), 0, dpToPx(10), 0);
            headerActions.addView(typeChip, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(28)
            ));
        }

        TextView delete = createDeleteTextButton();
        delete.setOnClickListener(v -> confirmDeleteHistoryEntry(day));
        headerActions.addView(delete);

        ImageView expandIcon = new ImageView(this);
        boolean expandedByDefault = AppSettings.historyExpanded(this);
        expandIcon.setImageResource(
                expandedByDefault
                        ? R.drawable.ic_ironx_chevron_up
                        : R.drawable.ic_ironx_chevron_down
        );
        expandIcon.setContentDescription(
                expandedByDefault
                        ? getString(R.string.history_collapse_session)
                        : getString(R.string.history_expand_session)
        );
        LinearLayout.LayoutParams expandParams = new LinearLayout.LayoutParams(
                dpToPx(28),
                dpToPx(28)
        );
        expandParams.leftMargin = dpToPx(8);
        headerActions.addView(expandIcon, expandParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(expandedByDefault ? View.VISIBLE : View.GONE);
        card.addView(details, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        dividerParams.topMargin = dpToPx(13);
        dividerParams.bottomMargin = dpToPx(2);
        divider.setLayoutParams(dividerParams);
        details.addView(divider);

        if (day.exercises != null && !day.exercises.isEmpty()) {
            for (WorkoutStorage.DetailedWorkout workout : day.exercises) {
                details.addView(createExerciseCard(workout));
            }
        }

        if (day.cardioSessions != null && !day.cardioSessions.isEmpty()) {
            for (WorkoutStorage.CardioSession cardioSession : day.cardioSessions) {
                details.addView(createCardioCard(cardioSession));
            }
        }

        header.setContentDescription(getString(
                R.string.history_header_show_details,
                day.date,
                dayWorkoutLabel,
                buildDaySummary(day)
        ));
        header.setOnClickListener(view -> {
            boolean expanding = details.getVisibility() != View.VISIBLE;
            setSessionDetailsVisible(details, expanding);
            expandIcon.setImageResource(
                    expanding
                            ? R.drawable.ic_ironx_chevron_up
                            : R.drawable.ic_ironx_chevron_down
            );
            expandIcon.setContentDescription(
                    expanding
                            ? getString(R.string.history_collapse_session)
                            : getString(R.string.history_expand_session)
            );
            header.setContentDescription(getString(
                    expanding
                            ? R.string.history_header_hide_details
                            : R.string.history_header_show_details,
                    day.date,
                    dayWorkoutLabel,
                    buildDaySummary(day)
            ));
        });

        return card;
    }

    private TextView createDeleteTextButton() {
        TextView delete = createTextView(
                getString(R.string.delete),
                9,
                R.color.text_tertiary,
                Typeface.BOLD
        );
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(dpToPx(10), dpToPx(10), dpToPx(8), dpToPx(10));
        delete.setClickable(true);
        delete.setFocusable(true);
        delete.setContentDescription(getString(R.string.delete));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = dpToPx(8);
        delete.setLayoutParams(params);
        return delete;
    }

    private void confirmDeleteHistoryEntry(WorkoutStorage.DailyWorkout day) {
        String workoutType = resolveWorkoutType(day);
        if (workoutType.isEmpty()) {
            return;
        }
        String label = getDayWorkoutLabel(day);
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_delete_session_title)
                .setMessage(getString(
                        R.string.history_delete_session_message,
                        day.date,
                        label
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (WorkoutStorage.deleteHistoryWorkout(
                            this,
                            day.date,
                            workoutType,
                            day.sessionId
                    )) {
                        populateHistory(currentWorkoutType);
                    }
                })
                .show();
    }

    private void setSessionDetailsVisible(View details, boolean visible) {
        details.animate().cancel();
        details.setAlpha(1f);
        details.setTranslationY(0f);
        details.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private View createExerciseCard(WorkoutStorage.DetailedWorkout workout) {
        LinearLayout exerciseCard = new LinearLayout(this);
        exerciseCard.setOrientation(LinearLayout.VERTICAL);
        exerciseCard.setBackgroundResource(R.drawable.bg_history_exercise);
        exerciseCard.setPadding(dpToPx(13), dpToPx(12), dpToPx(13), dpToPx(12));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(10);
        exerciseCard.setLayoutParams(cardParams);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        exerciseCard.addView(titleRow);

        TextView title = createTextView(
                workout.exercise,
                15,
                R.color.text_primary,
                Typeface.BOLD
        );
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        int setCount = workout.sets == null ? 0 : workout.sets.size();
        String meta = getResources().getQuantityString(
                R.plurals.history_set_count_upper,
                setCount,
                setCount
        );
        if (showingAllWorkoutTypes) {
            String workoutLabel = getWorkoutTypeLabel(workout.workoutType);
            if (!workoutLabel.isEmpty()) {
                meta += " · " + workoutLabel.replace("-Day", "").toUpperCase(Locale.GERMAN);
            }
        }
        TextView setCountView = createTextView(
                meta,
                8,
                R.color.training_gold,
                Typeface.BOLD
        );
        setCountView.setLetterSpacing(0.06f);
        titleRow.addView(setCountView);

        if (setCount == 0) {
            TextView emptySets = createTextView(
                    getString(R.string.history_no_set_data),
                    12,
                    R.color.text_tertiary,
                    Typeface.NORMAL
            );
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emptyParams.topMargin = dpToPx(10);
            emptySets.setLayoutParams(emptyParams);
            exerciseCard.addView(emptySets);
            return exerciseCard;
        }

        LinearLayout labels = createSetColumns();
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dpToPx(11);
        labels.setLayoutParams(labelParams);
        labels.addView(createColumnLabel(getString(R.string.history_set_label), dpToPx(46), 0));
        labels.addView(createColumnLabel(getString(R.string.history_weight_label), 0, 1f));
        labels.addView(createColumnLabel(getString(R.string.history_reps_label), dpToPx(72), 0));
        exerciseCard.addView(labels);

        for (int i = 0; i < workout.sets.size(); i++) {
            exerciseCard.addView(createSetRow(i + 1, workout.sets.get(i)));
        }

        return exerciseCard;
    }

    private View createSetRow(int setNumber, WorkoutStorage.WorkoutSet set) {
        LinearLayout row = createSetColumns();
        row.setBackgroundResource(R.drawable.bg_history_set_row);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(4), 0, dpToPx(4), 0);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(46)
        );
        rowParams.topMargin = dpToPx(6);
        row.setLayoutParams(rowParams);

        TextView index = createValueText(
                String.format(Locale.GERMAN, "%02d", setNumber),
                R.color.training_gold_highlight,
                Gravity.CENTER
        );
        row.addView(index, new LinearLayout.LayoutParams(dpToPx(42), dpToPx(46)));

        TextView weight = createValueText(
                formatWeight(set.weight),
                R.color.text_primary,
                Gravity.START | Gravity.CENTER_VERTICAL
        );
        styleWeightUnit(weight);
        row.addView(weight, new LinearLayout.LayoutParams(
                0,
                dpToPx(46),
                1f
        ));

        TextView reps = createValueText(
                String.valueOf(set.reps),
                R.color.text_primary,
                Gravity.CENTER
        );
        row.addView(reps, new LinearLayout.LayoutParams(dpToPx(68), dpToPx(46)));
        return row;
    }

    private View createCardioCard(WorkoutStorage.CardioSession cardioSession) {
        LinearLayout cardioCard = new LinearLayout(this);
        cardioCard.setOrientation(LinearLayout.HORIZONTAL);
        cardioCard.setGravity(Gravity.CENTER_VERTICAL);
        cardioCard.setBackgroundResource(R.drawable.bg_history_exercise);
        cardioCard.setPadding(dpToPx(13), dpToPx(11), dpToPx(13), dpToPx(11));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(10);
        cardioCard.setLayoutParams(cardParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        cardioCard.addView(labels, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView eyebrow = createTextView(
                "CARDIO",
                8,
                R.color.training_gold,
                Typeface.BOLD
        );
        eyebrow.setLetterSpacing(0.08f);
        labels.addView(eyebrow);

        TextView title = createTextView(
                cardioSession.exercise,
                15,
                R.color.text_primary,
                Typeface.BOLD
        );
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dpToPx(3);
        title.setLayoutParams(titleParams);
        labels.addView(title);

        TextView minutes = createTextView(
                getString(R.string.history_minutes_short, cardioSession.minutes),
                15,
                R.color.training_gold_highlight,
                Typeface.BOLD
        );
        cardioCard.addView(minutes);
        return cardioCard;
    }

    private LinearLayout createSetColumns() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private TextView createColumnLabel(String text, int width, float weight) {
        TextView label = createTextView(
                text,
                8,
                R.color.text_tertiary,
                Typeface.BOLD
        );
        label.setLetterSpacing(0.08f);
        label.setGravity(width == dpToPx(72) ? Gravity.CENTER : Gravity.START);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                width,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        ));
        return label;
    }

    private TextView createValueText(String text, int colorRes, int gravity) {
        TextView view = createTextView(text, 17, colorRes, Typeface.BOLD);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        view.setGravity(gravity);
        return view;
    }

    private TextView createTextView(String text, float sizeSp, int colorRes, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, colorRes));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTypeface(Typeface.create("sans-serif", style));
        return view;
    }

    private String buildDaySummary(WorkoutStorage.DailyWorkout day) {
        int exerciseCount = day.exercises == null ? 0 : day.exercises.size();
        int setCount = 0;
        if (day.exercises != null) {
            for (WorkoutStorage.DetailedWorkout workout : day.exercises) {
                if (workout.sets != null) {
                    setCount += workout.sets.size();
                }
            }
        }

        String exerciseSummary = getResources().getQuantityString(
                R.plurals.history_exercise_count,
                exerciseCount,
                exerciseCount
        );
        String setSummary = getResources().getQuantityString(
                R.plurals.history_set_count,
                setCount,
                setCount
        );
        String summary = getString(
                R.string.history_day_summary,
                exerciseSummary,
                setSummary
        );
        if (day.timestamp != null && day.timestamp.length() >= 16) {
            summary = day.timestamp.substring(11) + " · " + summary;
        }
        if (day.cardioSessions != null && !day.cardioSessions.isEmpty()) {
            summary += getString(
                    R.string.history_cardio_count,
                    day.cardioSessions.size()
            );
        }
        return summary;
    }

    private String formatWeight(double weightKg) {
        return String.format(Locale.getDefault(), "%.1f %s", weightKg * weightFactor, weightUnit);
    }

    private void styleWeightUnit(TextView view) {
        String text = view.getText().toString();
        int unitStart = text.lastIndexOf(' ') + 1;
        if (unitStart <= 0 || unitStart >= text.length()) {
            return;
        }

        SpannableString styled = new SpannableString(text);
        styled.setSpan(
                new RelativeSizeSpan(0.62f),
                unitStart,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        styled.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_tertiary)),
                unitStart,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        view.setText(styled);
    }

    private String normalizeTitle(String title) {
        return title
                .replace(" - Trainings", "")
                .replace(" – Trainings", "")
                .trim();
    }


    private String getDayWorkoutLabel(WorkoutStorage.DailyWorkout day) {
        String storedLabel = getWorkoutTypeLabel(resolveWorkoutType(day));
        if (!storedLabel.isEmpty()) {
            return storedLabel;
        }

        if (day.exercises != null && !day.exercises.isEmpty()) {
            return getWorkoutTypeLabel(day.exercises.get(0).workoutType);
        }
        if (day.cardioSessions != null && !day.cardioSessions.isEmpty()) {
            return getWorkoutTypeLabel(day.cardioSessions.get(0).workoutType);
        }
        return "";
    }

    private String resolveWorkoutType(WorkoutStorage.DailyWorkout day) {
        if (day.workoutType != null && !day.workoutType.trim().isEmpty()) {
            return day.workoutType.trim();
        }
        if (day.exercises != null && !day.exercises.isEmpty()) {
            String workoutType = day.exercises.get(0).workoutType;
            return workoutType == null ? "" : workoutType.trim();
        }
        if (day.cardioSessions != null && !day.cardioSessions.isEmpty()) {
            String workoutType = day.cardioSessions.get(0).workoutType;
            return workoutType == null ? "" : workoutType.trim();
        }
        return "";
    }

    private String getWorkoutTypeLabel(String workoutType) {
        return WorkoutTypeRepository.label(this, workoutType);
    }

    private int dpToPx(int dp) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        getResources().getDisplayMetrics()
                )
        );
    }
}
