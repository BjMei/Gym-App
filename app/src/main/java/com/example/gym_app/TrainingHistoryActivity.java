package com.example.gym_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.Locale;

public class TrainingHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_TYPE = "extra_workout_type";
    public static final String EXTRA_WORKOUT_TITLE = "extra_workout_title";

    private TextView tvEmptyState;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_history);

        TextView tvTitle = findViewById(R.id.tvHistoryTitle);
        View rootLayout = findViewById(R.id.rootHistoryLayout);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        listContainer = findViewById(R.id.historyListContainer);

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
            title = "Trainingsverlauf";
        }
        tvTitle.setText(title);

        populateHistory(workoutType);
    }

    private void populateHistory(String workoutType) {
        listContainer.removeAllViews();

        if (workoutType == null) {
            workoutType = "";
        }

        List<WorkoutStorage.DailyWorkout> dailyWorkouts = WorkoutStorage.getDailyWorkouts(this, workoutType);
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
        card.setBackgroundResource(R.drawable.rounded_card);
        card.setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(14);
        card.setLayoutParams(cardParams);

        TextView tvDate = new TextView(this);
        tvDate.setText(day.date);
        tvDate.setTextColor(ContextCompat.getColor(this, R.color.gold_primary));
        tvDate.setTextSize(15);
        tvDate.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvDate.setLetterSpacing(0.04f);
        card.addView(tvDate);

        View divider = new View(this);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        dividerParams.topMargin = dpToPx(10);
        dividerParams.bottomMargin = dpToPx(12);
        divider.setLayoutParams(dividerParams);
        card.addView(divider);

        if (day.exercises != null && !day.exercises.isEmpty()) {
            for (WorkoutStorage.DetailedWorkout workout : day.exercises) {
                card.addView(createExerciseLine(workout));
            }
        }

        if (day.cardioSessions != null && !day.cardioSessions.isEmpty()) {
            TextView cardioTitle = new TextView(this);
            cardioTitle.setText("Cardio");
            cardioTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            cardioTitle.setTextSize(14);
            cardioTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams cardioTitleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardioTitleParams.topMargin = dpToPx(6);
            cardioTitleParams.bottomMargin = dpToPx(6);
            cardioTitle.setLayoutParams(cardioTitleParams);
            card.addView(cardioTitle);

            for (WorkoutStorage.CardioSession cardioSession : day.cardioSessions) {
                card.addView(createCardioLine(cardioSession));
            }
        }

        return card;
    }

    private View createExerciseLine(WorkoutStorage.DetailedWorkout workout) {
        TextView line = new TextView(this);
        StringBuilder builder = new StringBuilder();
        builder.append(workout.exercise).append(": ");

        if (workout.sets != null && !workout.sets.isEmpty()) {
            for (int i = 0; i < workout.sets.size(); i++) {
                WorkoutStorage.WorkoutSet set = workout.sets.get(i);
                builder.append(String.format(Locale.getDefault(), "%.1f kg × %d", set.weight, set.reps));
                if (i < workout.sets.size() - 1) {
                    builder.append(" | ");
                }
            }
        } else {
            builder.append("ohne Satzdaten");
        }

        line.setText(builder.toString());
        line.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        line.setTextSize(15);
        line.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        line.setPadding(0, dpToPx(4), 0, dpToPx(4));
        return line;
    }

    private View createCardioLine(WorkoutStorage.CardioSession cardioSession) {
        TextView line = new TextView(this);
        String text = String.format(Locale.getDefault(), "%s: %d Minuten", cardioSession.exercise, cardioSession.minutes);
        line.setText(text);
        line.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        line.setTextSize(14);
        line.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        line.setPadding(0, dpToPx(2), 0, dpToPx(4));
        return line;
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
