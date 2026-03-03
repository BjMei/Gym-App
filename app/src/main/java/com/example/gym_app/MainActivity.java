package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

        LinearLayout workoutCard = findViewById(R.id.workoutCard);
        LinearLayout statsCard = findViewById(R.id.statsCard);
        LinearLayout fortschrittCard = findViewById(R.id.fortschrittCard);
        ImageButton btnBurgerMenu = findViewById(R.id.btnBurgerMenu);

        workoutCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
            }
        });

        statsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StatistikActivity.class));
            }
        });

        fortschrittCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FortschrittActivity.class));
            }
        });

        btnBurgerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        setupDrawerMenuItems();
    }

    private void setupDrawerMenuItems() {
        TextView drawerHistory = findViewById(R.id.drawerHistory);
        TextView drawerProfileGoals = findViewById(R.id.drawerProfileGoals);
        TextView drawerSettings = findViewById(R.id.drawerSettings);

        drawerHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(MainActivity.this, TrainingHistoryActivity.class));
            }
        });

        drawerProfileGoals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(MainActivity.this, ProfileGoalsActivity.class));
            }
        });

        drawerSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                openSettingsScreen();
            }
        });
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
        View rootLayout = findViewById(R.id.rootMainLayout);
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
