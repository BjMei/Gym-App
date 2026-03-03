package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private LinearLayout workoutCard;
    private LinearLayout statsCard;
    private LinearLayout fortschrittCard;
    private ImageButton btnBurgerMenu;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        drawerLayout = findViewById(R.id.drawerLayout);
        workoutCard = findViewById(R.id.workoutCard);
        statsCard = findViewById(R.id.statsCard);
        fortschrittCard = findViewById(R.id.fortschrittCard);
        btnBurgerMenu = findViewById(R.id.btnBurgerMenu);

        workoutCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));

        statsCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatistikActivity.class)));

        fortschrittCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FortschrittActivity.class)));

        btnBurgerMenu.setOnClickListener(v -> toggleDrawer());
        setupDrawerMenuItems();
    }

    private void setupDrawerMenuItems() {
        TextView drawerWorkout = findViewById(R.id.drawerWorkout);
        TextView drawerHistory = findViewById(R.id.drawerHistory);
        TextView drawerStats = findViewById(R.id.drawerStats);
        TextView drawerProgress = findViewById(R.id.drawerProgress);
        TextView drawerSettings = findViewById(R.id.drawerSettings);

        drawerWorkout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
        });

        drawerHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, TrainingHistoryActivity.class));
        });

        drawerStats.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, StatistikActivity.class));
        });

        drawerProgress.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(MainActivity.this, FortschrittActivity.class));
        });

        drawerSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            openSettingsScreen();
        });
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void openSettingsScreen() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
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
