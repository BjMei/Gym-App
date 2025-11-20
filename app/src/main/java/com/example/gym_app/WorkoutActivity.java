package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WorkoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // Push
        Button btnPush = findViewById(R.id.btnPush);
        btnPush.setOnClickListener(v -> {
            startActivity(new Intent(WorkoutActivity.this, PushActivity.class));
        });

        // Pull
        Button btnPull = findViewById(R.id.btnPull);
        btnPull.setOnClickListener(v ->
                startActivity(new Intent(this, PullActivity.class))
        );

        // Leg
        Button btnLegs = findViewById(R.id.btnLegs);
        btnLegs.setOnClickListener(v ->
                startActivity(new Intent(this, LegActivity.class))
        );

        // Zurueck
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
