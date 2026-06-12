package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WorkoutActivity extends IronxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        applyWindowInsets();

        findViewById(R.id.btnBackWorkout).setOnClickListener(v -> finish());

        View pushCard = findViewById(R.id.pushCard);
        View pullCard = findViewById(R.id.pullCard);
        View legCard = findViewById(R.id.legCard);

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

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootWorkoutLayout);
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
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
