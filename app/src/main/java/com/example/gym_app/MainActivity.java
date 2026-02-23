package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout workoutCard;
    private LinearLayout statsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        workoutCard = findViewById(R.id.workoutCard);
        statsCard = findViewById(R.id.statsCard);
        LinearLayout fortschrittCard = findViewById(R.id.fortschrittCard);

        workoutCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));

        statsCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatistikActivity.class)));

        fortschrittCard.setOnClickListener(v -> {
            // später: FortschrittActivity starten
        });
    }

    private void applyWindowInsets() {
        android.view.View rootLayout = findViewById(R.id.rootMainLayout);
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
}
