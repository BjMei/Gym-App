package com.example.gym_app;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class TrainingHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_TYPE = "extra_workout_type";
    public static final String EXTRA_WORKOUT_TITLE = "extra_workout_title";

    private TextView tvTitle;
    private TextView tvEmptyState;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_history);

        tvTitle = findViewById(R.id.tvHistoryTitle);
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

        List<String> workouts = WorkoutStorage.getWorkouts(this, workoutType);
        if (workouts == null || workouts.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        for (String workout : workouts) {
            TextView entry = new TextView(this);
            entry.setText(workout);
            entry.setTextColor(getResources().getColor(R.color.text_primary, null));
            entry.setTextSize(16);
            entry.setPadding(0, 12, 0, 12);
            entry.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            listContainer.addView(entry);

            // Divider line
            android.view.View divider = new android.view.View(this);
            divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            );
            params.setMargins(0, 0, 0, 0);
            divider.setLayoutParams(params);
            listContainer.addView(divider);
        }
    }
}

