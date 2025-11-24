package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WorkoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        LinearLayout pushCard = findViewById(R.id.pushCard);
        LinearLayout pullCard = findViewById(R.id.pullCard);
        LinearLayout legCard = findViewById(R.id.legCard);

        // Push - Button und Card
        View.OnClickListener pushClickListener = v ->
                startActivity(new Intent(WorkoutActivity.this, PushActivity.class));
        pushCard.setOnClickListener(pushClickListener);

        // Pull - Button und Card
        View.OnClickListener pullClickListener = v ->
                startActivity(new Intent(WorkoutActivity.this, PullActivity.class));
        pullCard.setOnClickListener(pullClickListener);

        // Leg - Button und Card
        View.OnClickListener legClickListener = v ->
                startActivity(new Intent(WorkoutActivity.this, LegActivity.class));
        legCard.setOnClickListener(legClickListener);
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
