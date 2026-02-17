package com.example.gym_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

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
        tvEmptyState = findViewById(R.id.tvEmptyState);
        listContainer = findViewById(R.id.historyListContainer);

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

        List<String> workouts = WorkoutStorage.getWorkouts(this, workoutType);
        if (workouts.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        for (int i = 0; i < workouts.size(); i++) {
            TextView entry = new TextView(this);
            entry.setText(workouts.get(i));
            entry.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            entry.setTextSize(16);

            int horizontalPadding = dpToPx(20);
            int verticalPadding = dpToPx(16);
            entry.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            entry.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            entry.setBackgroundResource(R.drawable.rounded_card);

            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            entryParams.bottomMargin = dpToPx(12);
            entry.setLayoutParams(entryParams);
            listContainer.addView(entry);

            if (i < workouts.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                );
                divider.setLayoutParams(dividerParams);
                listContainer.addView(divider);
            }
        }
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
