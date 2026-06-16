package com.example.gym_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends IronxActivity {

    private DrawerLayout drawerLayout;
    private String nextWorkoutType = WorkoutStorage.TYPE_PUSH;
    private static final DateTimeFormatter STORAGE_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter STORAGE_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        configureSystemBars();

        LinearLayout workoutCard = findViewById(R.id.workoutCard);
        LinearLayout statsCard = findViewById(R.id.statsCard);
        LinearLayout fortschrittCard = findViewById(R.id.fortschrittCard);
        LinearLayout nextWorkoutCard = findViewById(R.id.nextWorkoutCard);
        ImageButton btnBurgerMenu = findViewById(R.id.btnBurgerMenu);

        workoutCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, WorkoutActivity.class)));
        statsCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatistikActivity.class)));
        fortschrittCard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FortschrittActivity.class)));
        nextWorkoutCard.setOnClickListener(v -> openNextWorkout());
        btnBurgerMenu.setOnClickListener(v -> toggleDrawer());

        setupDrawerMenuItems();
        setupBackNavigation();
        updateDashboard();
    }

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        LinearLayout rootMainLayout = findViewById(R.id.rootMainLayout);
        LinearLayout drawerMenuPanel = findViewById(R.id.drawerMenuPanel);
        int rootLeft = rootMainLayout.getPaddingLeft();
        int rootTop = rootMainLayout.getPaddingTop();
        int rootRight = rootMainLayout.getPaddingRight();
        int rootBottom = rootMainLayout.getPaddingBottom();
        int drawerLeft = drawerMenuPanel.getPaddingLeft();
        int drawerTop = drawerMenuPanel.getPaddingTop();
        int drawerRight = drawerMenuPanel.getPaddingRight();
        int drawerBottom = drawerMenuPanel.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            rootMainLayout.setPadding(
                    rootLeft,
                    rootTop + systemBars.top,
                    rootRight,
                    rootBottom + systemBars.bottom);
            drawerMenuPanel.setPadding(
                    drawerLeft,
                    drawerTop + systemBars.top,
                    drawerRight,
                    drawerBottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(drawerLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard();
    }

    private void setupDrawerMenuItems() {
        TextView drawerHistory = findViewById(R.id.drawerHistory);
        TextView drawerProfileGoals = findViewById(R.id.drawerProfileGoals);
        TextView drawerSettings = findViewById(R.id.drawerSettings);
        TextView drawerAppInfo = findViewById(R.id.drawerAppInfo);
        TextView drawerResetData = findViewById(R.id.drawerResetData);

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

        drawerAppInfo.setOnClickListener(v -> {
            closeDrawerIfOpen();
            startActivity(new Intent(MainActivity.this, AppInfoActivity.class));
        });

        drawerResetData.setOnClickListener(v -> {
            closeDrawerIfOpen();
            showResetConfirmation();
        });
    }

    private void showResetConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.drawer_reset_confirm_title)
                .setMessage(R.string.drawer_reset_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.drawer_reset_confirm_continue,
                        (dialogInterface, which) -> showFinalResetConfirmation()
                )
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(Color.parseColor("#EF4444"))
        );
        dialog.show();
    }

    private void showFinalResetConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.drawer_reset_final_title)
                .setMessage(R.string.drawer_reset_final_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.drawer_reset_final_action,
                        (dialogInterface, which) -> resetAppData()
                )
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(Color.parseColor("#EF4444"))
        );
        dialog.show();
    }

    private void resetAppData() {
        try {
            AppDataResetManager.resetAllData(this);
            Toast.makeText(this, R.string.drawer_reset_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } catch (Exception exception) {
            Toast.makeText(this, R.string.drawer_reset_error, Toast.LENGTH_LONG).show();
        }
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

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void updateDashboard() {
        TextView tvHomeDate = findViewById(R.id.tvHomeDate);
        TextView tvHomeGreeting = findViewById(R.id.tvHomeGreeting);
        TextView tvWeekSessions = findViewById(R.id.tvWeekSessions);
        TextView tvWeekProgress = findViewById(R.id.tvWeekProgress);
        ProgressBar progressWeekGoal = findViewById(R.id.progressWeekGoal);
        TextView tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        TextView tvWeekVolume = findViewById(R.id.tvWeekVolume);
        TextView tvNextWorkoutSummary = findViewById(R.id.tvNextWorkoutSummary);

        Locale displayLocale = getResources().getConfiguration().getLocales().get(0);
        LocalDate today = LocalDate.now();
        tvHomeDate.setText(today.format(
                DateTimeFormatter.ofPattern("EEEE · d. MMMM", displayLocale))
                .toUpperCase(displayLocale));

        ProfileRepository.Profile profile = new ProfileRepository(this).load();
        String profileName = profile.name;
        if (profileName != null && !profileName.trim().isEmpty()) {
            String firstName = profileName.trim().split("\\s+")[0];
            tvHomeGreeting.setText(getString(R.string.home_greeting, firstName));
        } else {
            tvHomeGreeting.setText(R.string.home_ready);
        }

        List<WorkoutStorage.DailyWorkout> dailyWorkouts =
                WorkoutStorage.getDailyWorkouts(this, "");
        Set<LocalDate> trainedDays = collectTrainedDays(dailyWorkouts);

        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long sessionsThisWeek = trainedDays.stream()
                .filter(date -> !date.isBefore(weekStart) && !date.isAfter(today))
                .count();

        int weeklyGoal = profile.weeklyTrainingGoal;
        int weeklyProgress = TrainingGoalPlanner.progressPercent(
                (int) sessionsThisWeek,
                weeklyGoal
        );
        tvWeekSessions.setText(getString(
                R.string.home_week_goal_value,
                sessionsThisWeek,
                weeklyGoal
        ));
        tvWeekProgress.setText(getString(
                R.string.home_week_goal_percent,
                weeklyProgress
        ));
        progressWeekGoal.setProgress(weeklyProgress);
        tvCurrentStreak.setText(String.valueOf(calculateCurrentStreak(trainedDays, today)));
        tvWeekVolume.setText(formatVolume(calculateWeekVolume(weekStart, today)));
        nextWorkoutType = NextWorkoutPlanner.findNextWorkoutType(collectWorkoutEvents());
        LocalDate nextPreferredDay = TrainingGoalPlanner.nextPreferredTrainingDate(
                today,
                profile.preferredDays,
                trainedDays,
                weeklyGoal
        );
        tvNextWorkoutSummary.setText(buildNextWorkoutSummary(
                nextWorkoutType,
                nextPreferredDay,
                today
        ));
    }

    private Set<LocalDate> collectTrainedDays(
            List<WorkoutStorage.DailyWorkout> dailyWorkouts) {
        Set<LocalDate> trainedDays = new HashSet<>();
        for (WorkoutStorage.DailyWorkout day : dailyWorkouts) {
            LocalDate parsedDate = parseStorageDate(day.date);
            if (parsedDate != null) {
                trainedDays.add(parsedDate);
            }
        }
        return trainedDays;
    }

    private int calculateCurrentStreak(Set<LocalDate> trainedDays, LocalDate today) {
        LocalDate cursor = today;
        if (!trainedDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!trainedDays.contains(cursor)) {
                return 0;
            }
        }

        int streak = 0;
        while (trainedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private double calculateWeekVolume(LocalDate weekStart, LocalDate today) {
        double volume = 0;
        String[] workoutTypes = {
                WorkoutStorage.TYPE_PUSH,
                WorkoutStorage.TYPE_PULL,
                WorkoutStorage.TYPE_LEG
        };

        for (String type : workoutTypes) {
            for (WorkoutStorage.DetailedWorkout workout :
                    WorkoutStorage.getDetailedWorkouts(this, type)) {
                LocalDate workoutDate = parseTimestampDate(workout.timestamp);
                if (workoutDate == null
                        || workoutDate.isBefore(weekStart)
                        || workoutDate.isAfter(today)
                        || workout.sets == null) {
                    continue;
                }
                for (WorkoutStorage.WorkoutSet set : workout.sets) {
                    volume += set.weight * set.reps;
                }
            }
        }
        return volume;
    }

    private String formatVolume(double volume) {
        double displayedVolume = AppSettings.fromStoredKg(this, volume);
        if (displayedVolume >= 1000) {
            return String.format(Locale.getDefault(), "%.1fk", displayedVolume / 1000.0);
        }
        return String.format(Locale.getDefault(), "%.0f", displayedVolume);
    }

    private List<NextWorkoutPlanner.WorkoutEvent> collectWorkoutEvents() {
        List<NextWorkoutPlanner.WorkoutEvent> events = new ArrayList<>();
        String[] workoutTypes = {
                WorkoutStorage.TYPE_PUSH,
                WorkoutStorage.TYPE_PULL,
                WorkoutStorage.TYPE_LEG
        };

        for (String type : workoutTypes) {
            for (WorkoutStorage.DetailedWorkout workout :
                    WorkoutStorage.getDetailedWorkouts(this, type)) {
                LocalDateTime timestamp = parseStorageTimestamp(workout.timestamp);
                if (timestamp != null) {
                    events.add(new NextWorkoutPlanner.WorkoutEvent(type, timestamp));
                }
            }
        }
        return events;
    }

    private String buildNextWorkoutSummary(
            String type,
            LocalDate nextPreferredDay,
            LocalDate today) {
        Locale displayLocale = getResources().getConfiguration().getLocales().get(0);
        String workoutLabel = getWorkoutTypeLabel(type).toUpperCase(displayLocale);
        if (nextPreferredDay == null) {
            return getString(R.string.home_next_workout_goal_reached, workoutLabel);
        }
        if (nextPreferredDay.equals(today)) {
            return getString(R.string.home_next_workout_today, workoutLabel);
        }
        String weekday = nextPreferredDay.format(
                DateTimeFormatter.ofPattern("EEEE", displayLocale)
        ).toUpperCase(displayLocale);
        return getString(
                R.string.home_next_workout_planned,
                workoutLabel,
                weekday
        );
    }

    private void openNextWorkout() {
        Class<?> targetActivity;
        if (WorkoutStorage.TYPE_PULL.equals(nextWorkoutType)) {
            targetActivity = PullActivity.class;
        } else if (WorkoutStorage.TYPE_LEG.equals(nextWorkoutType)) {
            targetActivity = LegActivity.class;
        } else {
            targetActivity = PushActivity.class;
        }
        startActivity(new Intent(MainActivity.this, targetActivity));
    }

    private String getWorkoutTypeLabel(String type) {
        if (WorkoutStorage.TYPE_PUSH.equals(type)) {
            return "Push";
        }
        if (WorkoutStorage.TYPE_PULL.equals(type)) {
            return "Pull";
        }
        if (WorkoutStorage.TYPE_LEG.equals(type)) {
            return "Leg";
        }
        return "Training";
    }

    private LocalDate parseTimestampDate(String timestamp) {
        if (timestamp == null || timestamp.length() < 10) {
            return null;
        }
        return parseStorageDate(timestamp.substring(0, 10));
    }

    private LocalDateTime parseStorageTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestamp, STORAGE_TIMESTAMP);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDate parseStorageDate(String date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date, STORAGE_DATE);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
