package com.example.gym_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WorkoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        Button btnPush = findViewById(R.id.btnPush);
        Button btnPull = findViewById(R.id.btnPull);
        Button btnLegs = findViewById(R.id.btnLegs);
        Button btnBack = findViewById(R.id.btnBack);

        btnPush.setOnClickListener(v -> showMessage("Push-Day ausgewählt!"));
        btnPull.setOnClickListener(v -> showMessage("Pull-Day ausgewählt!"));
        btnLegs.setOnClickListener(v -> showMessage("Leg-Day ausgewählt!"));

        btnBack.setOnClickListener(v -> finish());
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
