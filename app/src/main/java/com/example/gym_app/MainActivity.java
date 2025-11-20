package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton startWorkoutBtn = findViewById(R.id.btnStartWorkout);
        ImageButton statsBtn = findViewById(R.id.btnStats);
        LinearLayout workoutCard = findViewById(R.id.workoutCard);

        // Workout-Button und Card
        View.OnClickListener workoutClickListener = v -> {
            startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
        };
        
        startWorkoutBtn.setOnClickListener(workoutClickListener);
        workoutCard.setOnClickListener(workoutClickListener);

        // Statistik-Button
        statsBtn.setOnClickListener(v -> {
            // später: StatsActivity starten
        });
    }
}
