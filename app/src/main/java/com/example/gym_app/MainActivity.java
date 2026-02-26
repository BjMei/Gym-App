package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout workoutCard;
    private LinearLayout statsCard;
    private LinearLayout fortschrittCard;
    private ImageButton btnBurgerMenu;

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
        fortschrittCard = findViewById(R.id.fortschrittCard);
        btnBurgerMenu = findViewById(R.id.btnBurgerMenu);

        workoutCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));

        statsCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatistikActivity.class)));

        fortschrittCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FortschrittActivity.class)));

        btnBurgerMenu.setOnClickListener(v -> showBurgerMenu());
    }

    private void showBurgerMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnBurgerMenu);
        popupMenu.getMenuInflater().inflate(R.menu.home_burger_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_workout) {
                startActivity(new Intent(MainActivity.this, WorkoutActivity.class));
                return true;
            } else if (itemId == R.id.menu_history) {
                startActivity(new Intent(MainActivity.this, TrainingHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_stats) {
                startActivity(new Intent(MainActivity.this, StatistikActivity.class));
                return true;
            } else if (itemId == R.id.menu_progress) {
                startActivity(new Intent(MainActivity.this, FortschrittActivity.class));
                return true;
            }
            return false;
        });

        popupMenu.show();
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
