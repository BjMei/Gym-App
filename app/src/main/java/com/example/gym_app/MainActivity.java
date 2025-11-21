package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout workoutCard;
    private LinearLayout statsCard;
    private LinearLayout pushStatsList;
    private LinearLayout pullStatsList;
    private LinearLayout legStatsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workoutCard = findViewById(R.id.workoutCard);
        statsCard = findViewById(R.id.statsCard);
        pushStatsList = findViewById(R.id.pushStatsList);
        pullStatsList = findViewById(R.id.pullStatsList);
        legStatsList = findViewById(R.id.legStatsList);

        // Workout-Button und Card
        View.OnClickListener workoutClickListener = v -> {
            startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
        };
        workoutCard.setOnClickListener(workoutClickListener);

        // Statistik-Card
        statsCard.setOnClickListener(v -> {
            // später: StatsActivity starten
        });

        populateSavedTrainings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateSavedTrainings();
    }

    private void populateSavedTrainings() {
        populateList(pushStatsList, WorkoutStorage.getWorkouts(this, WorkoutStorage.TYPE_PUSH));
        populateList(pullStatsList, WorkoutStorage.getWorkouts(this, WorkoutStorage.TYPE_PULL));
        populateList(legStatsList, WorkoutStorage.getWorkouts(this, WorkoutStorage.TYPE_LEG));
    }

    private void populateList(LinearLayout container, List<String> workouts) {
        container.removeAllViews();
        if (workouts == null || workouts.isEmpty()) {
            TextView placeholder = createEntryTextView("Noch keine Trainings gespeichert");
            placeholder.setAlpha(0.7f);
            container.addView(placeholder);
            return;
        }

        for (String workout : workouts) {
            TextView entryView = createEntryTextView(workout);
            container.addView(entryView);
        }
    }

    private TextView createEntryTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getResources().getColor(R.color.text_primary, null));
        textView.setTextSize(15);
        textView.setPadding(0, 6, 0, 6);
        textView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        return textView;
    }
}
