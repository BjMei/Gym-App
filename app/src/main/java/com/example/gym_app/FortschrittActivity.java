package com.example.gym_app;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class FortschrittActivity extends AppCompatActivity {

    private static final int[] RANGE_DAYS = {7, 30, 90, 180, 365};
    private static final String[] RANGE_LABELS = {"7T", "30T", "90T", "6M", "12M"};

    private static final String[] PUSH_EXERCISES = {"Flys", "Brustpresse", "Schrägbank", "Frontheben", "Seitheben", "Triceps"};
    private static final String[] PULL_EXERCISES = {"Latzug eng", "Latzug breit", "Rudern", "Unterer Rücken", "Bizeps", "Hammer Curls", "Bauch", "Butterfly Reverse"};
    private static final String[] LEG_EXERCISES = {"Beinbeuger", "Beine innen", "Waden sitzend", "Beinstrecker", "Beinpresse", "Hip Thrust", "Hyperextension"};

    private static final Set<String> PUSH_SET = new HashSet<>(Arrays.asList(PUSH_EXERCISES));
    private static final Set<String> LEG_SET = new HashSet<>(Arrays.asList(LEG_EXERCISES));

    private static final Map<String, String> MUSCLE_MAP = new HashMap<>();

    static {
        for (String e : new String[]{"Flys", "Brustpresse", "Schrägbank"}) MUSCLE_MAP.put(e, "Brust");
        for (String e : new String[]{"Frontheben", "Seitheben"}) MUSCLE_MAP.put(e, "Schultern");
        MUSCLE_MAP.put("Triceps", "Arme");
        for (String e : new String[]{"Latzug eng", "Latzug breit", "Rudern", "Butterfly Reverse"}) MUSCLE_MAP.put(e, "Rücken");
        for (String e : new String[]{"Unterer Rücken", "Bauch"}) MUSCLE_MAP.put(e, "Core");
        for (String e : new String[]{"Bizeps", "Hammer Curls"}) MUSCLE_MAP.put(e, "Arme");
        for (String e : LEG_EXERCISES) MUSCLE_MAP.put(e, "Beine");
    }

    private Spinner spinnerExercise;
    private Spinner spinnerRangeKraft;
    private Spinner spinnerRangeMuskeln;
    private Spinner spinnerRangeKonsistenz;

    private LineChart chart1RM;
    private LineChart chartMaxGewicht;
    private LineChart chartVolumen;
    private LineChart chartWiederholungen;

    private BarChart chartMuskelVolumen;
    private PieChart chartPushPull;
    private PieChart chartOberUnter;
    private TextView tvDysbalance;

    private TextView tvStreak;
    private TextView tvBestStreak;
    private TextView tvBestStreakZeitraum;
    private BarChart chartFreieTage;
    private PieChart chartQuote;
    private TextView tvQuoteProzent;
    private TextView tvQuoteStatus;

    private LinearLayout tabKraft;
    private LinearLayout tabMuskeln;
    private LinearLayout tabKonsistenz;
    private View sectionKraft;
    private View sectionMuskeln;
    private View sectionKonsistenz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortschritt);
        bindViews();
        setupTabs();
        setupExerciseSpinner();
        showSection("kraft");
    }

    private void bindViews() {
        spinnerExercise = findViewById(R.id.spinnerFortschrittExercise);
        spinnerRangeKraft = findViewById(R.id.spinnerRangeKraft);
        spinnerRangeMuskeln = findViewById(R.id.spinnerRangeMuskeln);
        spinnerRangeKonsistenz = findViewById(R.id.spinnerRangeKonsistenz);

        chart1RM = findViewById(R.id.chart1RM);
        chartMaxGewicht = findViewById(R.id.chartMaxGewicht);
        chartVolumen = findViewById(R.id.chartVolumen);
        chartWiederholungen = findViewById(R.id.chartWiederholungen);

        chartMuskelVolumen = findViewById(R.id.chartMuskelVolumen);
        chartPushPull = findViewById(R.id.chartPushPull);
        chartOberUnter = findViewById(R.id.chartOberUnter);
        tvDysbalance = findViewById(R.id.tvDysbalance);

        tvStreak = findViewById(R.id.tvStreak);
        tvBestStreak = findViewById(R.id.tvBestStreak);
        tvBestStreakZeitraum = findViewById(R.id.tvBestStreakZeitraum);
        chartFreieTage = findViewById(R.id.chartFreieTage);
        chartQuote = findViewById(R.id.chartQuote);
        tvQuoteProzent = findViewById(R.id.tvQuoteProzent);
        tvQuoteStatus = findViewById(R.id.tvQuoteStatus);

        tabKraft = findViewById(R.id.tabKraft);
        tabMuskeln = findViewById(R.id.tabMuskeln);
        tabKonsistenz = findViewById(R.id.tabKonsistenz);
        sectionKraft = findViewById(R.id.sectionKraft);
        sectionMuskeln = findViewById(R.id.sectionMuskeln);
        sectionKonsistenz = findViewById(R.id.sectionKonsistenz);
    }

    private void setupTabs() {
        tabKraft.setOnClickListener(v -> showSection("kraft"));
        tabMuskeln.setOnClickListener(v -> showSection("muskeln"));
        tabKonsistenz.setOnClickListener(v -> showSection("konsistenz"));
    }

    private void showSection(String tab) {
        sectionKraft.setVisibility("kraft".equals(tab) ? View.VISIBLE : View.GONE);
        sectionMuskeln.setVisibility("muskeln".equals(tab) ? View.VISIBLE : View.GONE);
        sectionKonsistenz.setVisibility("konsistenz".equals(tab) ? View.VISIBLE : View.GONE);

        int active = ContextCompat.getColor(this, R.color.gold_primary);
        int inactive = ContextCompat.getColor(this, R.color.text_tertiary);

        ((TextView) tabKraft.getChildAt(0)).setTextColor("kraft".equals(tab) ? active : inactive);
        ((TextView) tabMuskeln.getChildAt(0)).setTextColor("muskeln".equals(tab) ? active : inactive);
        ((TextView) tabKonsistenz.getChildAt(0)).setTextColor("konsistenz".equals(tab) ? active : inactive);

        tabKraft.getChildAt(1).setVisibility("kraft".equals(tab) ? View.VISIBLE : View.INVISIBLE);
        tabMuskeln.getChildAt(1).setVisibility("muskeln".equals(tab) ? View.VISIBLE : View.INVISIBLE);
        tabKonsistenz.getChildAt(1).setVisibility("konsistenz".equals(tab) ? View.VISIBLE : View.INVISIBLE);

        if ("kraft".equals(tab)) loadKraftData();
        if ("muskeln".equals(tab)) loadMuskelData();
        if ("konsistenz".equals(tab)) loadKonsistenzData();
    }

    private void setupExerciseSpinner() {
        List<String> all = new ArrayList<>();
        all.addAll(Arrays.asList(PUSH_EXERCISES));
        all.addAll(Arrays.asList(PULL_EXERCISES));
        all.addAll(Arrays.asList(LEG_EXERCISES));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, all);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerExercise.setAdapter(adapter);
        spinnerExercise.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadKraftData();
            }
        });

        setupRangeSpinner(spinnerRangeKraft, this::loadKraftData);
        setupRangeSpinner(spinnerRangeMuskeln, this::loadMuskelData);
        setupRangeSpinner(spinnerRangeKonsistenz, this::loadKonsistenzData);
    }

    private void setupRangeSpinner(Spinner spinner, Runnable onChanged) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, RANGE_LABELS);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinner.setAdapter(adapter);
        spinner.setSelection(1);
        spinner.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onChanged.run();
            }
        });
    }

    private int getRangeFromSpinner(Spinner spinner) {
        int pos = spinner.getSelectedItemPosition();
        return (pos >= 0 && pos < RANGE_DAYS.length) ? RANGE_DAYS[pos] : 30;
    }

    private void loadKraftData() {
        if (spinnerExercise.getSelectedItem() == null) return;
        String exercise = spinnerExercise.getSelectedItem().toString();
        int days = getRangeFromSpinner(spinnerRangeKraft);

        String type = PUSH_SET.contains(exercise) ? WorkoutStorage.TYPE_PUSH
                : LEG_SET.contains(exercise) ? WorkoutStorage.TYPE_LEG
                : WorkoutStorage.TYPE_PULL;

        List<WorkoutStorage.DetailedWorkout> raw = WorkoutStorage.getDetailedWorkouts(this, type);
        Date cutoff = getDaysAgo(days);

        Map<String, float[]> dayMap = new TreeMap<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd.MM.", Locale.getDefault());
        SimpleDateFormat parseFull = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        for (WorkoutStorage.DetailedWorkout workout : raw) {
            if (!exercise.equals(workout.exercise) || workout.sets == null) continue;
            try {
                Date date = parseFull.parse(workout.timestamp);
                if (date == null || date.before(cutoff)) continue;
                String key = labelFormat.format(date);

                double maxWeight = 0;
                double volume = 0;
                double reps = 0;
                double best1RM = 0;
                for (WorkoutStorage.WorkoutSet set : workout.sets) {
                    double estRm = set.weight * (1 + set.reps / 30.0);
                    best1RM = Math.max(best1RM, estRm);
                    maxWeight = Math.max(maxWeight, set.weight);
                    volume += set.weight * set.reps;
                    reps += set.reps;
                }
                float avgReps = workout.sets.isEmpty() ? 0 : (float) (reps / workout.sets.size());

                float[] existing = dayMap.get(key);
                if (existing == null) {
                    dayMap.put(key, new float[]{(float) best1RM, (float) maxWeight, (float) volume, avgReps});
                } else {
                    existing[0] = Math.max(existing[0], (float) best1RM);
                    existing[1] = Math.max(existing[1], (float) maxWeight);
                    existing[2] += (float) volume;
                    existing[3] = (existing[3] + avgReps) / 2f;
                }
            } catch (Exception ignored) {
            }
        }

        List<String> labels = new ArrayList<>(dayMap.keySet());
        List<Entry> e1RM = new ArrayList<>();
        List<Entry> eMax = new ArrayList<>();
        List<Entry> eVol = new ArrayList<>();
        List<Entry> eReps = new ArrayList<>();

        int i = 0;
        for (String key : labels) {
            float[] v = dayMap.get(key);
            e1RM.add(new Entry(i, v[0]));
            eMax.add(new Entry(i, v[1]));
            eVol.add(new Entry(i, v[2]));
            eReps.add(new Entry(i, v[3]));
            i++;
        }

        buildLineChart(chart1RM, e1RM, labels, "Gesch. 1RM", "kg");
        buildLineChart(chartMaxGewicht, eMax, labels, "Max Gewicht", "kg");
        buildLineChart(chartVolumen, eVol, labels, "Volumen", "kg");
        buildLineChart(chartWiederholungen, eReps, labels, "Ø Wdh.", "");
    }

    private void loadMuskelData() {
        int days = getRangeFromSpinner(spinnerRangeMuskeln);
        Date cutoff = getDaysAgo(days);
        SimpleDateFormat parseFull = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        Map<String, Float> muscleVol = new LinkedHashMap<>();
        float pushVol = 0, pullVol = 0, upperVol = 0, lowerVol = 0;

        for (String wType : new String[]{WorkoutStorage.TYPE_PUSH, WorkoutStorage.TYPE_PULL, WorkoutStorage.TYPE_LEG}) {
            for (WorkoutStorage.DetailedWorkout workout : WorkoutStorage.getDetailedWorkouts(this, wType)) {
                if (workout.sets == null) continue;
                try {
                    Date date = parseFull.parse(workout.timestamp);
                    if (date == null || date.before(cutoff)) continue;
                } catch (Exception e) {
                    continue;
                }

                float vol = 0;
                for (WorkoutStorage.WorkoutSet set : workout.sets) vol += (float) (set.weight * set.reps);

                String muscle = MUSCLE_MAP.getOrDefault(workout.exercise, "Sonstige");
                muscleVol.put(muscle, muscleVol.getOrDefault(muscle, 0f) + vol);

                boolean isPush = PUSH_SET.contains(workout.exercise);
                boolean isLeg = LEG_SET.contains(workout.exercise);
                if (isPush) {
                    pushVol += vol;
                    upperVol += vol;
                } else if (isLeg) {
                    lowerVol += vol;
                } else {
                    pullVol += vol;
                    upperVol += vol;
                }
            }
        }

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> barLabels = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, Float> entry : muscleVol.entrySet()) {
            barEntries.add(new BarEntry(idx++, entry.getValue()));
            barLabels.add(entry.getKey());
        }
        buildBarChart(chartMuskelVolumen, barEntries, barLabels, "Volumen (kg)");

        buildPieChart(chartPushPull,
                new String[]{"Push", "Pull"},
                new float[]{pushVol, pullVol},
                new int[]{R.color.gold_primary, R.color.card_background_light});

        buildPieChart(chartOberUnter,
                new String[]{"Oberkörper", "Unterkörper"},
                new float[]{upperVol, lowerVol},
                new int[]{R.color.gold_secondary, R.color.gold_dark});

        StringBuilder dysbalance = new StringBuilder();
        float totalPP = pushVol + pullVol;
        if (totalPP > 0) {
            float pushPct = (pushVol / totalPP) * 100f;
            float pullPct = 100f - pushPct;
            if (pushPct > 65f) dysbalance.append(String.format(Locale.getDefault(), "⚠️ Push überwiegt Pull um %.0f%%\n", pushPct - pullPct));
            if (pullPct > 65f) dysbalance.append(String.format(Locale.getDefault(), "⚠️ Pull überwiegt Push um %.0f%%\n", pullPct - pushPct));
        }
        float totalBody = upperVol + lowerVol;
        if (totalBody > 0) {
            float upperPct = (upperVol / totalBody) * 100f;
            float lowerPct = 100f - upperPct;
            if (upperPct > 65f) dysbalance.append(String.format(Locale.getDefault(), "⚠️ Oberkörper überwiegt Unterkörper um %.0f%%", upperPct - lowerPct));
            if (lowerPct > 65f) dysbalance.append(String.format(Locale.getDefault(), "⚠️ Unterkörper überwiegt Oberkörper um %.0f%%", lowerPct - upperPct));
        }

        if (dysbalance.length() > 0) {
            tvDysbalance.setText(dysbalance.toString().trim());
            tvDysbalance.setVisibility(View.VISIBLE);
        } else {
            tvDysbalance.setVisibility(View.GONE);
        }
    }

    private void loadKonsistenzData() {
        int days = getRangeFromSpinner(spinnerRangeKonsistenz);
        SimpleDateFormat parseFull = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        Set<String> trainedDays = new TreeSet<>();
        for (String type : new String[]{WorkoutStorage.TYPE_PUSH, WorkoutStorage.TYPE_PULL, WorkoutStorage.TYPE_LEG}) {
            for (WorkoutStorage.DetailedWorkout workout : WorkoutStorage.getDetailedWorkouts(this, type)) {
                try {
                    Date date = parseFull.parse(workout.timestamp);
                    if (date != null) trainedDays.add(dayFmt.format(date));
                } catch (Exception ignored) {
                }
            }
        }

        int streak = 0;
        Calendar cal = Calendar.getInstance();
        while (trainedDays.contains(dayFmt.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        tvStreak.setText(streak + " Tage");

        List<String> sorted = new ArrayList<>(trainedDays);
        Collections.sort(sorted);
        int bestStreak = sorted.isEmpty() ? 0 : 1;
        int curStreak = sorted.isEmpty() ? 0 : 1;
        String bestStart = sorted.isEmpty() ? "" : sorted.get(0);
        String bestEnd = bestStart;
        String curStart = bestStart;

        SimpleDateFormat sortParse = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        for (int i = 1; i < sorted.size(); i++) {
            try {
                Date prev = sortParse.parse(sorted.get(i - 1));
                Date curr = sortParse.parse(sorted.get(i));
                long diff = (curr.getTime() - prev.getTime()) / 86400000L;
                if (diff == 1) {
                    curStreak++;
                    if (curStreak > bestStreak) {
                        bestStreak = curStreak;
                        bestStart = curStart;
                        bestEnd = sorted.get(i);
                    }
                } else {
                    curStreak = 1;
                    curStart = sorted.get(i);
                }
            } catch (Exception ignored) {
            }
        }

        tvBestStreak.setText(bestStreak + " Tage");
        tvBestStreakZeitraum.setText(bestStart.isEmpty() ? "–" : bestStart + " – " + bestEnd);

        Map<String, int[]> weekMap = new LinkedHashMap<>();
        SimpleDateFormat weekFmt = new SimpleDateFormat("'KW'ww", Locale.getDefault());
        Calendar iter = Calendar.getInstance();
        iter.add(Calendar.DAY_OF_YEAR, -days);
        Calendar today = Calendar.getInstance();
        while (!iter.after(today)) {
            String week = weekFmt.format(iter.getTime());
            String dayKey = dayFmt.format(iter.getTime());
            if (!weekMap.containsKey(week)) weekMap.put(week, new int[]{0, 0});
            weekMap.get(week)[1]++;
            if (!trainedDays.contains(dayKey)) weekMap.get(week)[0]++;
            iter.add(Calendar.DAY_OF_YEAR, 1);
        }

        List<BarEntry> freeEntries = new ArrayList<>();
        List<String> weekLabels = new ArrayList<>();
        int w = 0;
        for (Map.Entry<String, int[]> e : weekMap.entrySet()) {
            freeEntries.add(new BarEntry(w++, e.getValue()[0]));
            weekLabels.add(e.getKey());
        }
        buildBarChart(chartFreieTage, freeEntries, weekLabels, "Freie Tage");

        Date cutoff = getDaysAgo(days);
        int trained = 0;
        for (String day : trainedDays) {
            try {
                Date date = sortParse.parse(day);
                if (date != null && !date.before(cutoff)) trained++;
            } catch (Exception ignored) {
            }
        }

        int quote = days > 0 ? Math.round((trained * 100f) / days) : 0;
        tvQuoteProzent.setText(quote + "%");

        int quoteColor;
        String status;
        if (quote > 80) {
            quoteColor = R.color.success;
            status = "🟢 Ausgezeichnet";
        } else if (quote >= 50) {
            quoteColor = R.color.gold_primary;
            status = "🟡 Gut – weiter so";
        } else {
            quoteColor = R.color.error;
            status = "🔴 Mehr Training nötig";
        }
        tvQuoteProzent.setTextColor(ContextCompat.getColor(this, quoteColor));
        tvQuoteStatus.setText(status);

        buildDonutChart(chartQuote, quote, quoteColor);
    }

    private void buildLineChart(LineChart chart, List<Entry> entries, List<String> labels, String label, String unit) {
        int gold = ContextCompat.getColor(this, R.color.gold_primary);
        int bg = ContextCompat.getColor(this, R.color.card_background);

        chart.setBackgroundColor(bg);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);

        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            chart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(gold);
        dataSet.setCircleColor(gold);
        dataSet.setCircleHoleColor(bg);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(gold);
        dataSet.setHighLightColor(ContextCompat.getColor(this, R.color.gold_light));

        chart.setData(new LineData(dataSet));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        xAxis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(labels.size(), 5), false);
        xAxis.setDrawAxisLine(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        left.setGridColor(ContextCompat.getColor(this, R.color.divider));
        left.setDrawAxisLine(false);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + unit;
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.animateX(600);
        chart.invalidate();
    }

    private void buildBarChart(BarChart chart, List<BarEntry> entries, List<String> labels, String yLabel) {
        int gold = ContextCompat.getColor(this, R.color.gold_primary);
        int bg = ContextCompat.getColor(this, R.color.card_background);

        chart.setBackgroundColor(bg);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);

        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            chart.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, yLabel);
        dataSet.setColor(gold);
        dataSet.setDrawValues(false);

        chart.setData(new BarData(dataSet));
        chart.getBarData().setBarWidth(0.6f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        xAxis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(labels.size(), 6), false);
        xAxis.setDrawAxisLine(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        left.setGridColor(ContextCompat.getColor(this, R.color.divider));
        left.setDrawAxisLine(false);
        chart.getAxisRight().setEnabled(false);
        chart.animateY(600);
        chart.invalidate();
    }

    private void buildPieChart(PieChart chart, String[] labels, float[] values, int[] colorRes) {
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(false);
        chart.setUsePercentValues(true);
        chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.black));
        chart.setEntryLabelTextSize(11f);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (values[i] > 0) {
                entries.add(new PieEntry(values[i], labels[i]));
                colors.add(ContextCompat.getColor(this, colorRes[i]));
            }
        }

        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            chart.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        Legend legend = chart.getLegend();
        legend.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        legend.setTextSize(11f);

        chart.setData(new PieData(dataSet));
        chart.animateY(700);
        chart.invalidate();
    }

    private void buildDonutChart(PieChart chart, int quotePct, int colorRes) {
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(60f);
        chart.setHoleColor(ContextCompat.getColor(this, R.color.card_background));
        chart.setDrawCenterText(false);
        chart.getLegend().setEnabled(false);
        chart.setEntryLabelColor(android.graphics.Color.TRANSPARENT);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(quotePct, ""));
        entries.add(new PieEntry(100 - quotePct, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                ContextCompat.getColor(this, colorRes),
                ContextCompat.getColor(this, R.color.divider)
        });
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(1f);

        chart.setData(new PieData(dataSet));
        chart.animateY(700);
        chart.invalidate();
    }

    private Date getDaysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return c.getTime();
    }

    private abstract static class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
