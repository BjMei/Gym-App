package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                             // verbindet Java mit deiner XML

        Button startWorkoutBtn = findViewById(R.id.btnStartWorkout);
        Button statsBtn = findViewById(R.id.btnStats);                      // holt Buttons aus dem Layout

        startWorkoutBtn.setOnClickListener(new View.OnClickListener() {     // reagiert auf Klicks
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class));

            }
        });

        statsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // später: StatsActivity starten
            }
        });
    }
}
