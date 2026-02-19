package com.example.gym_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout workoutCard;
    private LinearLayout statsCard;
    private LinearLayout progressCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        workoutCard = findViewById(R.id.workoutCard);
        statsCard = findViewById(R.id.statsCard);
        progressCard = findViewById(R.id.progressCard);

        ImageButton btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        TextView drawerWorkout = findViewById(R.id.drawerWorkout);
        TextView drawerHistory = findViewById(R.id.drawerHistory);
        TextView drawerStats = findViewById(R.id.drawerStats);
        TextView drawerProgress = findViewById(R.id.drawerProgress);
        TextView drawerReset = findViewById(R.id.drawerReset);
        TextView drawerSettings = findViewById(R.id.drawerSettings);

        workoutCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));
        statsCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatistikActivity.class)));
        progressCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FortschrittActivity.class)));

        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

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

        drawerReset.setOnClickListener(v -> showResetDialog());

        drawerSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Einstellungen folgen bald.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Alle Daten zurücksetzen?")
                .setMessage("Möchtest du wirklich alle Trainingsdaten und Einstellungen löschen?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) -> {
                    WorkoutStorage.resetAllData(MainActivity.this);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(MainActivity.this, "Alle Daten wurden gelöscht.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
