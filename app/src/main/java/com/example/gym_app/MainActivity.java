package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout workoutCard;
    private LinearLayout statsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workoutCard = findViewById(R.id.workoutCard);
        statsCard = findViewById(R.id.statsCard);

        View.OnClickListener workoutClickListener = v ->
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
        workoutCard.setOnClickListener(workoutClickListener);

        statsCard.setOnClickListener(v -> {
            // später: StatsActivity starten
        });
    }
}
