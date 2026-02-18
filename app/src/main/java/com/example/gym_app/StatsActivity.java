package com.example.gym_app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {

    private LinearLayout metricsContainer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        View rootLayout = findViewById(R.id.rootStatsLayout);
        metricsContainer = findViewById(R.id.statsMetricsContainer);

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

        renderStats();
    }

    private void renderStats() {
        List<WorkoutStorage.DetailedWorkout> detailedWorkouts = getAllDetailedWorkouts();
        List<WorkoutStorage.CardioSession> cardioSessions = getAllCardioSessions();

        Set<String> trainingDates = getAllTrainingDates(detailedWorkouts, cardioSessions);
        int trainingDaysWeek = countDatesInCurrentWeek(trainingDates);
        int trainingDaysMonth = countDatesInCurrentMonth(trainingDates);

        int totalWorkouts = detailedWorkouts.size() + cardioSessions.size();

        Map<String, Integer> repsPerExercise = new LinkedHashMap<>();
        Map<String, Double> maxWeightPerExercise = new LinkedHashMap<>();
        Map<String, Integer> totalRepsPerExercise = new LinkedHashMap<>();
        Map<String, Integer> totalSetsPerExercise = new LinkedHashMap<>();

        double globalMaxWeight = 0;
        String globalMaxWeightExercise = "-";
        int globalMaxReps = 0;
        String globalMaxRepsExercise = "-";

        for (WorkoutStorage.DetailedWorkout workout : detailedWorkouts) {
            int exerciseTotalReps = 0;
            double exerciseMaxWeight = 0;

            if (workout.sets != null) {
                for (WorkoutStorage.WorkoutSet set : workout.sets) {
                    exerciseTotalReps += set.reps;

                    if (set.weight > exerciseMaxWeight) {
                        exerciseMaxWeight = set.weight;
                    }
                    if (set.weight > globalMaxWeight) {
                        globalMaxWeight = set.weight;
                        globalMaxWeightExercise = workout.exercise;
                    }
                    if (set.reps > globalMaxReps) {
                        globalMaxReps = set.reps;
                        globalMaxRepsExercise = workout.exercise;
                    }

                    totalRepsPerExercise.put(workout.exercise,
                            totalRepsPerExercise.getOrDefault(workout.exercise, 0) + set.reps);
                    totalSetsPerExercise.put(workout.exercise,
                            totalSetsPerExercise.getOrDefault(workout.exercise, 0) + 1);
                }
            }

            repsPerExercise.put(workout.exercise,
                    repsPerExercise.getOrDefault(workout.exercise, 0) + exerciseTotalReps);

            double currentMax = maxWeightPerExercise.getOrDefault(workout.exercise, 0.0);
            maxWeightPerExercise.put(workout.exercise, Math.max(currentMax, exerciseMaxWeight));
        }

        Map<String, Integer> cardioMinutesPerDate = new HashMap<>();
        for (WorkoutStorage.CardioSession session : cardioSessions) {
            String date = extractDate(session.timestamp);
            cardioMinutesPerDate.put(date, cardioMinutesPerDate.getOrDefault(date, 0) + session.minutes);
        }

        int totalCardioMinutes = 0;
        for (int minutes : cardioMinutesPerDate.values()) {
            totalCardioMinutes += minutes;
        }
        double avgCardioMinutesPerUnit = cardioMinutesPerDate.isEmpty() ? 0 :
                (double) totalCardioMinutes / cardioMinutesPerDate.size();

        metricsContainer.removeAllViews();

        addMetricCard("Trainingstage", String.format(Locale.getDefault(),
                "Woche: %d\nMonat: %d", trainingDaysWeek, trainingDaysMonth));

        addMetricCard("Trainingsdauer pro Einheit (Cardio)", String.format(Locale.getDefault(),
                "Durchschnitt: %.1f Min\nGesamt: %d Min", avgCardioMinutesPerUnit, totalCardioMinutes));

        addMetricCard("Gesamtanzahl absolvierte Workouts", String.valueOf(totalWorkouts));

        addMetricCard("Gesamtwiederholungen pro Übung", formatIntMap(repsPerExercise, " Reps"));

        addMetricCard("Maximalgewicht pro Übung", formatDoubleMap(maxWeightPerExercise, " kg"));

        addMetricCard("Durchschnittliche Wiederholungen pro Satz", formatAverageMap(totalRepsPerExercise, totalSetsPerExercise));

        String bestPerformance = String.format(Locale.getDefault(),
                "Höchstes Gewicht: %.1f kg (%s)\nMeiste Reps in einem Satz: %d (%s)",
                globalMaxWeight, globalMaxWeightExercise, globalMaxReps, globalMaxRepsExercise);
        addMetricCard("Beste Leistung", bestPerformance);
    }

    private List<WorkoutStorage.DetailedWorkout> getAllDetailedWorkouts() {
        List<WorkoutStorage.DetailedWorkout> all = new ArrayList<>();
        all.addAll(WorkoutStorage.getDetailedWorkouts(this, WorkoutStorage.TYPE_PUSH));
        all.addAll(WorkoutStorage.getDetailedWorkouts(this, WorkoutStorage.TYPE_PULL));
        all.addAll(WorkoutStorage.getDetailedWorkouts(this, WorkoutStorage.TYPE_LEG));
        return all;
    }

    private List<WorkoutStorage.CardioSession> getAllCardioSessions() {
        List<WorkoutStorage.CardioSession> all = new ArrayList<>();
        all.addAll(WorkoutStorage.getCardioSessions(this, WorkoutStorage.TYPE_PUSH));
        all.addAll(WorkoutStorage.getCardioSessions(this, WorkoutStorage.TYPE_PULL));
        all.addAll(WorkoutStorage.getCardioSessions(this, WorkoutStorage.TYPE_LEG));
        return all;
    }

    private Set<String> getAllTrainingDates(List<WorkoutStorage.DetailedWorkout> workouts,
                                            List<WorkoutStorage.CardioSession> sessions) {
        Set<String> dates = new HashSet<>();
        for (WorkoutStorage.DetailedWorkout workout : workouts) {
            dates.add(extractDate(workout.timestamp));
        }
        for (WorkoutStorage.CardioSession session : sessions) {
            dates.add(extractDate(session.timestamp));
        }
        dates.remove("");
        return dates;
    }

    private int countDatesInCurrentWeek(Set<String> dates) {
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(Calendar.YEAR);

        int count = 0;
        for (String date : dates) {
            Calendar cal = parseDateToCalendar(date);
            if (cal != null && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                    && cal.get(Calendar.YEAR) == currentYear) {
                count++;
            }
        }
        return count;
    }

    private int countDatesInCurrentMonth(Set<String> dates) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        int count = 0;
        for (String date : dates) {
            Calendar cal = parseDateToCalendar(date);
            if (cal != null && cal.get(Calendar.MONTH) == currentMonth
                    && cal.get(Calendar.YEAR) == currentYear) {
                count++;
            }
        }
        return count;
    }

    private Calendar parseDateToCalendar(String date) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(date));
            return calendar;
        } catch (ParseException e) {
            return null;
        }
    }

    private String extractDate(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "";
        }
        String[] parts = timestamp.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    private String formatIntMap(Map<String, Integer> map, String unitSuffix) {
        if (map.isEmpty()) {
            return "Noch keine Daten";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("• ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(unitSuffix);
        }
        return builder.toString();
    }

    private String formatDoubleMap(Map<String, Double> map, String unitSuffix) {
        if (map.isEmpty()) {
            return "Noch keine Daten";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("• ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.1f", entry.getValue()))
                    .append(unitSuffix);
        }
        return builder.toString();
    }

    private String formatAverageMap(Map<String, Integer> repsMap, Map<String, Integer> setsMap) {
        if (repsMap.isEmpty() || setsMap.isEmpty()) {
            return "Noch keine Daten";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : repsMap.entrySet()) {
            String exercise = entry.getKey();
            int totalReps = entry.getValue();
            int totalSets = setsMap.getOrDefault(exercise, 0);
            if (totalSets == 0) {
                continue;
            }
            double avg = (double) totalReps / totalSets;
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("• ")
                    .append(exercise)
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.1f Reps/Satz", avg));
        }
        return builder.length() == 0 ? "Noch keine Daten" : builder.toString();
    }

    private void addMetricCard(String title, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.rounded_card);
        card.setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(14);
        card.setLayoutParams(cardParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title.toUpperCase(Locale.getDefault()));
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.gold_primary));
        tvTitle.setTextSize(13);
        tvTitle.setLetterSpacing(0.08f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        card.addView(tvTitle);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvValue.setTextSize(16);
        tvValue.setLineSpacing(0f, 1.2f);
        tvValue.setTypeface(Typeface.create("monospace", Typeface.NORMAL));

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueParams.topMargin = dpToPx(10);
        tvValue.setLayoutParams(valueParams);
        card.addView(tvValue);

        metricsContainer.addView(card);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }
}
