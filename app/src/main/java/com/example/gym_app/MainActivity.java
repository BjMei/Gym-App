package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        drawerLayout = findViewById(R.id.drawerLayout);

        LinearLayout workoutCard = findViewById(R.id.workoutCard);
        LinearLayout statsCard = findViewById(R.id.statsCard);
        LinearLayout fortschrittCard = findViewById(R.id.fortschrittCard);
        ImageButton btnBurgerMenu = findViewById(R.id.btnBurgerMenu);

        workoutCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));
        statsCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatistikActivity.class)));
        fortschrittCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FortschrittActivity.class)));
        btnBurgerMenu.setOnClickListener(v -> toggleDrawer());

        setupDrawerMenuItems();
    }

    private void setupDrawerMenuItems() {
        TextView drawerHistory = findViewById(R.id.drawerHistory);
        TextView drawerProfileGoals = findViewById(R.id.drawerProfileGoals);
        TextView drawerSettings = findViewById(R.id.drawerSettings);

        drawerHistory.setOnClickListener(v -> {
            closeDrawerIfOpen();
            startActivity(new Intent(MainActivity.this, TrainingHistoryActivity.class));
        });

        drawerProfileGoals.setOnClickListener(v -> {
            closeDrawerIfOpen();
            startActivity(new Intent(MainActivity.this, ProfileGoalsActivity.class));
        });

        drawerSettings.setOnClickListener(v -> {
            closeDrawerIfOpen();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void closeDrawerIfOpen() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void toggleDrawer() {
        if (drawerLayout == null) {
            return;
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }
}
