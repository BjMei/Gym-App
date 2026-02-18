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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {

    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat weekLabelFormat = new SimpleDateFormat("ww/yy", Locale.getDefault());
    private final SimpleDateFormat dayLabelFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private LinearLayout metricsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        View rootLayout = findViewById(R.id.rootProgressLayout);
        metricsContainer = findViewById(R.id.progressMetricsContainer);

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

        renderProgress();
    }

    private void renderProgress() {
        List<SessionRecord> sessions = getAllSessions();
        if (sessions.isEmpty()) {
            addMetricCard("Fortschritt", "Noch keine Trainingsdaten vorhanden.");
            return;
        }

        Collections.sort(sessions, Comparator.comparingLong(s -> s.timestampMs));

        addMetricCard("Leistungssteigerung über Zeit (Diagramm)", buildProgressChart(sessions));
        addMetricCard("Vergleich Woche / Monat", buildWeekMonthComparison(sessions));
        addMetricCard("Plateaus", buildPlateauInfo(sessions));
        addMetricCard("Persönliche Rekorde (PRs)", buildPrInfo(sessions));
        addMetricCard("Training nach Wochentagen", buildWeekdayStrength(sessions));
        addMetricCard("Trainingsvolumen pro Muskelgruppe", buildMuscleVolume(sessions));
        addMetricCard("Verhältnis Push / Pull / Legs", buildPplRatio(sessions));
        addMetricCard("Vernachlässigte Muskelgruppen", buildNeglectedMuscles(sessions));
        addMetricCard("Überbelastung", buildOverloadInfo(sessions));
        addMetricCard("Training nach Tageszeit", buildTimeOfDayInfo(sessions));
        addMetricCard("Pausentage / Regenerationstage", buildRecoveryInfo(sessions));
        addMetricCard("Regelmäßigkeit", buildConsistencyInfo(sessions));
    }

    private List<SessionRecord> getAllSessions() {
        List<SessionRecord> all = new ArrayList<>();
        collectTypeSessions(WorkoutStorage.TYPE_PUSH, all);
        collectTypeSessions(WorkoutStorage.TYPE_PULL, all);
        collectTypeSessions(WorkoutStorage.TYPE_LEG, all);
        return all;
    }

    private void collectTypeSessions(String type, List<SessionRecord> target) {
        List<WorkoutStorage.DetailedWorkout> list = WorkoutStorage.getDetailedWorkouts(this, type);
        for (WorkoutStorage.DetailedWorkout workout : list) {
            long timestamp = parseTimestamp(workout.timestamp);
            if (timestamp <= 0) {
                continue;
            }

            double volume = 0;
            int totalReps = 0;
            double maxWeight = 0;
            int maxSetReps = 0;

            if (workout.sets != null) {
                for (WorkoutStorage.WorkoutSet set : workout.sets) {
                    volume += set.weight * set.reps;
                    totalReps += set.reps;
                    maxWeight = Math.max(maxWeight, set.weight);
                    maxSetReps = Math.max(maxSetReps, set.reps);
                }
            }

            target.add(new SessionRecord(
                    type,
                    workout.exercise,
                    workout.timestamp,
                    timestamp,
                    volume,
                    totalReps,
                    maxWeight,
                    maxSetReps,
                    guessMuscleGroup(workout.exercise, type)
            ));
        }
    }

    private String buildProgressChart(List<SessionRecord> sessions) {
        Map<String, Double> weeklyVolume = new LinkedHashMap<>();
        for (SessionRecord s : sessions) {
            String key = buildWeekKey(s.timestampMs);
            weeklyVolume.put(key, weeklyVolume.getOrDefault(key, 0.0) + s.volume);
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(weeklyVolume.entrySet());
        if (entries.size() > 8) {
            entries = entries.subList(entries.size() - 8, entries.size());
        }

        double max = 0;
        for (Map.Entry<String, Double> e : entries) {
            max = Math.max(max, e.getValue());
        }

        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Double> e : entries) {
            int bars = max <= 0 ? 0 : (int) Math.round((e.getValue() / max) * 12);
            b.append(e.getKey())
                    .append(" | ")
                    .append(repeat("▮", bars))
                    .append(" ")
                    .append(String.format(Locale.getDefault(), "%.0f", e.getValue()))
                    .append(" Vol")
                    .append("\n");
        }
        return b.toString().trim();
    }

    private String buildWeekMonthComparison(List<SessionRecord> sessions) {
        Calendar now = Calendar.getInstance();
        int curWeek = now.get(Calendar.WEEK_OF_YEAR);
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH);

        Calendar prev = (Calendar) now.clone();
        prev.add(Calendar.WEEK_OF_YEAR, -1);
        int prevWeek = prev.get(Calendar.WEEK_OF_YEAR);
        int prevWeekYear = prev.get(Calendar.YEAR);

        Calendar prevMonthCal = (Calendar) now.clone();
        prevMonthCal.add(Calendar.MONTH, -1);
        int prevMonth = prevMonthCal.get(Calendar.MONTH);
        int prevMonthYear = prevMonthCal.get(Calendar.YEAR);

        double weekCurrent = 0, weekPrev = 0, monthCurrent = 0, monthPrev = 0;
        for (SessionRecord s : sessions) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(s.timestampMs);
            if (c.get(Calendar.WEEK_OF_YEAR) == curWeek && c.get(Calendar.YEAR) == curYear) {
                weekCurrent += s.volume;
            }
            if (c.get(Calendar.WEEK_OF_YEAR) == prevWeek && c.get(Calendar.YEAR) == prevWeekYear) {
                weekPrev += s.volume;
            }
            if (c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear) {
                monthCurrent += s.volume;
            }
            if (c.get(Calendar.MONTH) == prevMonth && c.get(Calendar.YEAR) == prevMonthYear) {
                monthPrev += s.volume;
            }
        }

        return String.format(Locale.getDefault(),
                "Woche aktuell: %.0f Vol\nWoche davor: %.0f Vol\nΔ Woche: %+.1f%%\n\nMonat aktuell: %.0f Vol\nMonat davor: %.0f Vol\nΔ Monat: %+.1f%%",
                weekCurrent,
                weekPrev,
                percentDiff(weekCurrent, weekPrev),
                monthCurrent,
                monthPrev,
                percentDiff(monthCurrent, monthPrev));
    }

    private String buildPlateauInfo(List<SessionRecord> sessions) {
        Map<String, ExerciseTrend> trends = new HashMap<>();
        long now = System.currentTimeMillis();
        long plateauDays = 21L * 24 * 60 * 60 * 1000;
        List<String> plateauExercises = new ArrayList<>();

        for (SessionRecord s : sessions) {
            ExerciseTrend t = trends.getOrDefault(s.exercise, new ExerciseTrend());
            if (s.maxWeight > t.bestWeight) {
                t.bestWeight = s.maxWeight;
                t.lastImprovementMs = s.timestampMs;
            }
            trends.put(s.exercise, t);
        }

        for (Map.Entry<String, ExerciseTrend> e : trends.entrySet()) {
            long since = now - e.getValue().lastImprovementMs;
            if (e.getValue().lastImprovementMs > 0 && since >= plateauDays) {
                plateauExercises.add(String.format(Locale.getDefault(),
                        "• %s: seit %.0f Tagen kein neues Max-Gewicht",
                        e.getKey(),
                        since / (1000d * 60 * 60 * 24)));
            }
        }

        if (plateauExercises.isEmpty()) {
            return "Kein deutliches Plateau erkannt.";
        }

        StringBuilder b = new StringBuilder();
        for (String line : plateauExercises) {
            if (b.length() > 0) {
                b.append("\n");
            }
            b.append(line);
        }
        return b.toString();
    }

    private String buildPrInfo(List<SessionRecord> sessions) {
        Map<String, Double> maxWeight = new LinkedHashMap<>();
        Map<String, Integer> maxReps = new LinkedHashMap<>();

        for (SessionRecord s : sessions) {
            maxWeight.put(s.exercise, Math.max(maxWeight.getOrDefault(s.exercise, 0.0), s.maxWeight));
            maxReps.put(s.exercise, Math.max(maxReps.getOrDefault(s.exercise, 0), s.maxSetReps));
        }

        StringBuilder b = new StringBuilder();
        int count = 0;
        for (String exercise : maxWeight.keySet()) {
            if (count >= 8) {
                break;
            }
            b.append("• ")
                    .append(exercise)
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.1f kg", maxWeight.get(exercise)))
                    .append(" | ")
                    .append(maxReps.getOrDefault(exercise, 0))
                    .append(" Reps")
                    .append("\n");
            count++;
        }
        return b.toString().trim();
    }

    private String buildWeekdayStrength(List<SessionRecord> sessions) {
        String[] labels = {"So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"};
        double[] volume = new double[7];

        for (SessionRecord s : sessions) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(s.timestampMs);
            int day = c.get(Calendar.DAY_OF_WEEK) - 1;
            volume[day] += s.volume;
        }

        int bestDay = 0;
        for (int i = 1; i < 7; i++) {
            if (volume[i] > volume[bestDay]) {
                bestDay = i;
            }
        }

        StringBuilder b = new StringBuilder();
        b.append("Stärkster Tag: ").append(labels[bestDay]).append("\n");
        for (int i = 0; i < 7; i++) {
            b.append(labels[i])
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.0f", volume[i]))
                    .append(" Vol")
                    .append("\n");
        }
        return b.toString().trim();
    }

    private String buildMuscleVolume(List<SessionRecord> sessions) {
        Map<String, Double> byGroup = new LinkedHashMap<>();
        for (SessionRecord s : sessions) {
            byGroup.put(s.muscleGroup, byGroup.getOrDefault(s.muscleGroup, 0.0) + s.volume);
        }
        return formatDoubleMap(byGroup, " Vol");
    }

    private String buildPplRatio(List<SessionRecord> sessions) {
        int push = 0;
        int pull = 0;
        int leg = 0;

        for (SessionRecord s : sessions) {
            if (WorkoutStorage.TYPE_PUSH.equals(s.type)) {
                push++;
            } else if (WorkoutStorage.TYPE_PULL.equals(s.type)) {
                pull++;
            } else if (WorkoutStorage.TYPE_LEG.equals(s.type)) {
                leg++;
            }
        }

        int total = Math.max(1, push + pull + leg);
        return String.format(Locale.getDefault(),
                "Push: %d (%.0f%%)\nPull: %d (%.0f%%)\nLegs: %d (%.0f%%)",
                push, push * 100f / total,
                pull, pull * 100f / total,
                leg, leg * 100f / total);
    }

    private String buildNeglectedMuscles(List<SessionRecord> sessions) {
        long cutoff = System.currentTimeMillis() - (21L * 24 * 60 * 60 * 1000);
        Map<String, Long> lastByGroup = new HashMap<>();

        for (SessionRecord s : sessions) {
            long current = lastByGroup.getOrDefault(s.muscleGroup, 0L);
            if (s.timestampMs > current) {
                lastByGroup.put(s.muscleGroup, s.timestampMs);
            }
        }

        List<String> neglected = new ArrayList<>();
        for (Map.Entry<String, Long> e : lastByGroup.entrySet()) {
            if (e.getValue() < cutoff) {
                neglected.add("• " + e.getKey() + " (letzte Einheit: " + dayLabelFormat.format(e.getValue()) + ")");
            }
        }

        return neglected.isEmpty() ? "Keine vernachlässigten Muskelgruppen erkannt." : joinLines(neglected);
    }

    private String buildOverloadInfo(List<SessionRecord> sessions) {
        Map<String, Double> weekly = new LinkedHashMap<>();
        for (SessionRecord s : sessions) {
            String week = buildWeekKey(s.timestampMs);
            weekly.put(week, weekly.getOrDefault(week, 0.0) + s.volume);
        }

        List<Double> vals = new ArrayList<>(weekly.values());
        if (vals.size() < 2) {
            return "Zu wenige Daten für Überbelastungsanalyse.";
        }

        double current = vals.get(vals.size() - 1);
        int start = Math.max(0, vals.size() - 5);
        double sum = 0;
        int count = 0;
        for (int i = start; i < vals.size() - 1; i++) {
            sum += vals.get(i);
            count++;
        }
        double avgPrev = count == 0 ? current : sum / count;

        if (current > avgPrev * 1.6) {
            return String.format(Locale.getDefault(),
                    "Warnung: Aktuelles Wochenvolumen (%.0f) liegt >60%% über dem Schnitt der Vorwochen (%.0f).",
                    current, avgPrev);
        }

        return String.format(Locale.getDefault(),
                "Kein Überbelastungs-Signal. Woche: %.0f | Vorwochen-Schnitt: %.0f", current, avgPrev);
    }

    private String buildTimeOfDayInfo(List<SessionRecord> sessions) {
        int morning = 0, afternoon = 0, evening = 0, night = 0;
        for (SessionRecord s : sessions) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(s.timestampMs);
            int h = c.get(Calendar.HOUR_OF_DAY);
            if (h >= 5 && h < 12) {
                morning++;
            } else if (h < 17) {
                afternoon++;
            } else if (h < 22) {
                evening++;
            } else {
                night++;
            }
        }

        return String.format(Locale.getDefault(),
                "Morgen: %d\nNachmittag: %d\nAbend: %d\nNacht: %d",
                morning, afternoon, evening, night);
    }

    private String buildRecoveryInfo(List<SessionRecord> sessions) {
        List<Long> uniqueDays = new ArrayList<>();
        long lastDay = -1;
        for (SessionRecord s : sessions) {
            long dayStart = toDayStart(s.timestampMs);
            if (dayStart != lastDay) {
                uniqueDays.add(dayStart);
                lastDay = dayStart;
            }
        }

        if (uniqueDays.size() < 2) {
            return "Zu wenige Daten für Regenerationsanalyse.";
        }

        int longestGap = 0;
        int totalGap = 0;
        int gaps = 0;
        for (int i = 1; i < uniqueDays.size(); i++) {
            int diffDays = (int) ((uniqueDays.get(i) - uniqueDays.get(i - 1)) / (1000 * 60 * 60 * 24));
            int restDays = Math.max(0, diffDays - 1);
            longestGap = Math.max(longestGap, restDays);
            totalGap += restDays;
            gaps++;
        }

        double avgGap = gaps == 0 ? 0 : (double) totalGap / gaps;
        return String.format(Locale.getDefault(),
                "Längste Pause: %d Tage\nØ Regeneration zwischen Sessions: %.1f Tage",
                longestGap, avgGap);
    }

    private String buildConsistencyInfo(List<SessionRecord> sessions) {
        Map<String, Integer> weeklyCounts = new LinkedHashMap<>();
        for (SessionRecord s : sessions) {
            String week = buildWeekKey(s.timestampMs);
            weeklyCounts.put(week, weeklyCounts.getOrDefault(week, 0) + 1);
        }

        List<Integer> counts = new ArrayList<>(weeklyCounts.values());
        int considered = Math.min(8, counts.size());
        if (considered == 0) {
            return "Keine Daten";
        }

        int consistentWeeks = 0;
        for (int i = counts.size() - considered; i < counts.size(); i++) {
            if (counts.get(i) >= 2) {
                consistentWeeks++;
            }
        }

        int streak = 0;
        for (int i = counts.size() - 1; i >= 0; i--) {
            if (counts.get(i) >= 2) {
                streak++;
            } else {
                break;
            }
        }

        return String.format(Locale.getDefault(),
                "Konstante Wochen (letzte %d): %d/%d\nAktuelle Streak (>=2 Sessions/Woche): %d",
                considered, consistentWeeks, considered, streak);
    }

    private String buildWeekKey(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        return "KW " + weekLabelFormat.format(c.getTime());
    }

    private long parseTimestamp(String raw) {
        try {
            return timestampFormat.parse(raw).getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    private long toDayStart(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private double percentDiff(double current, double previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return ((current - previous) / previous) * 100;
    }

    private String repeat(String value, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < count; i++) {
            b.append(value);
        }
        return b.toString();
    }

    private String joinLines(List<String> lines) {
        StringBuilder b = new StringBuilder();
        for (String line : lines) {
            if (b.length() > 0) {
                b.append("\n");
            }
            b.append(line);
        }
        return b.toString();
    }

    private String formatDoubleMap(Map<String, Double> map, String suffix) {
        if (map.isEmpty()) {
            return "Noch keine Daten";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            if (b.length() > 0) {
                b.append("\n");
            }
            b.append("• ")
                    .append(e.getKey())
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.0f", e.getValue()))
                    .append(suffix);
        }
        return b.toString();
    }

    private String guessMuscleGroup(String exercise, String type) {
        String ex = exercise == null ? "" : exercise.toLowerCase(Locale.getDefault());

        if (ex.contains("tricep")) {
            return "Trizeps";
        }
        if (ex.contains("bizeps") || ex.contains("curl")) {
            return "Bizeps";
        }
        if (ex.contains("brust") || ex.contains("fly") || ex.contains("bank")) {
            return "Brust";
        }
        if (ex.contains("lat") || ex.contains("rudern") || ex.contains("rücken") || ex.contains("back")) {
            return "Rücken";
        }
        if (ex.contains("bein") || ex.contains("quad") || ex.contains("ham") || ex.contains("waden") || ex.contains("hip")) {
            return "Beine";
        }
        if (ex.contains("schulter") || ex.contains("frontheben") || ex.contains("seitheben") || ex.contains("shoulder")) {
            return "Schultern";
        }
        if (ex.contains("bauch") || ex.contains("core")) {
            return "Core";
        }

        if (WorkoutStorage.TYPE_PUSH.equals(type)) {
            return "Push-Mix";
        }
        if (WorkoutStorage.TYPE_PULL.equals(type)) {
            return "Pull-Mix";
        }
        return "Legs-Mix";
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
        tvValue.setTextSize(15);
        tvValue.setLineSpacing(0f, 1.25f);
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

    private static class ExerciseTrend {
        double bestWeight;
        long lastImprovementMs;
    }

    private static class SessionRecord {
        final String type;
        final String exercise;
        final String timestampRaw;
        final long timestampMs;
        final double volume;
        final int totalReps;
        final double maxWeight;
        final int maxSetReps;
        final String muscleGroup;

        SessionRecord(String type,
                      String exercise,
                      String timestampRaw,
                      long timestampMs,
                      double volume,
                      int totalReps,
                      double maxWeight,
                      int maxSetReps,
                      String muscleGroup) {
            this.type = type;
            this.exercise = exercise;
            this.timestampRaw = timestampRaw;
            this.timestampMs = timestampMs;
            this.volume = volume;
            this.totalReps = totalReps;
            this.maxWeight = maxWeight;
            this.maxSetReps = maxSetReps;
            this.muscleGroup = muscleGroup;
        }
    }
}
