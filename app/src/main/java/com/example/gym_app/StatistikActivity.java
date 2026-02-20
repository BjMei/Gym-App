package com.example.gym_app;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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

public class StatistikActivity extends AppCompatActivity {

    private static final int[] RANGE_DAYS = {7, 30, 90, 180, 365, Integer.MAX_VALUE};
    private static final String[] RANGE_LABELS = {"7T", "30T", "90T", "6M", "12M", "Gesamt"};

    private static final String[] PUSH_EXERCISES = {"Flys", "Brustpresse", "Schrägbank", "Frontheben", "Seitheben", "Triceps"};
    private static final String[] LEG_EXERCISES = {"Beinbeuger", "Beine innen", "Waden sitzend", "Beinstrecker", "Beinpresse", "Hip Thrust", "Hyperextension"};
    private static final String[] CARDIO_EXERCISES = {"Fahrrad", "Seil springen"};
    private static final Set<String> PUSH_SET = new HashSet<>(Arrays.asList(PUSH_EXERCISES));
    private static final Set<String> LEG_SET = new HashSet<>(Arrays.asList(LEG_EXERCISES));

    private LinearLayout tabUebersicht, tabVolumen, tabStruktur, tabCardio, tabPR;
    private View secUebersicht, secVolumen, secStruktur, secCardio, secPR;

    private Spinner spRangeUebersicht, spRangeVolumen, spRangeStruktur, spRangeCardio, spRangePR;

    private TextView tvBestWeek;
    private TextView tvGesamtVolumen, tvAvgVolumen, tvAvgSatzVolumen;
    private BarChart chartVolPerType;
    private HorizontalBarChart chartVolPerExercise;
    private TextView tvSchwerstesDatum, tvSchwerstesTyp, tvSchwerstesGesamt, tvSchwerstesEinzelsatz;

    private PieChart chartPPLVerteilung;
    private BarChart chartWochentag;
    private TextView tvAvgPause;
    private BarChart chartPausenHistogramm;
    private TextView tvAvgZwischen;

    private TextView tvCardioMinuten, tvCardioAvg, tvCardioLaengste, tvCardioSessions;
    private PieChart chartCardioArt;
    private BarChart chartCardioMinuten;

    private TableLayout tableSchwersterSatz;
    private TableLayout tableVolumenRekord;
    private TextView tvVolRekordDatum, tvVolRekordTyp, tvVolRekordGesamt;

    private String currentTab = "uebersicht";
    private final Map<String, LinearLayout> tabMap = new LinkedHashMap<>();
    private final Map<String, View> secMap = new LinkedHashMap<>();
    private final Map<String, Spinner> rangeMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistik);
        applyWindowInsets();
        bindViews();
        setupTabs();
        setupRangeSpinners();
        showSection("uebersicht");
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootStatistikLayout);
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

    private void bindViews() {
        tabUebersicht = findViewById(R.id.tabUebersicht);
        tabVolumen = findViewById(R.id.tabVolumen);
        tabStruktur = findViewById(R.id.tabStruktur);
        tabCardio = findViewById(R.id.tabCardio);
        tabPR = findViewById(R.id.tabPR);

        secUebersicht = findViewById(R.id.secUebersicht);
        secVolumen = findViewById(R.id.secVolumen);
        secStruktur = findViewById(R.id.secStruktur);
        secCardio = findViewById(R.id.secCardio);
        secPR = findViewById(R.id.secPR);

        spRangeUebersicht = findViewById(R.id.spRangeUebersicht);
        spRangeVolumen = findViewById(R.id.spRangeVolumen);
        spRangeStruktur = findViewById(R.id.spRangeStruktur);
        spRangeCardio = findViewById(R.id.spRangeCardio);
        spRangePR = findViewById(R.id.spRangePR);

        tvBestWeek = findViewById(R.id.tvBestWeek);

        tvGesamtVolumen = findViewById(R.id.tvGesamtVolumen);
        tvAvgVolumen = findViewById(R.id.tvAvgVolumen);
        tvAvgSatzVolumen = findViewById(R.id.tvAvgSatzVolumen);
        chartVolPerType = findViewById(R.id.chartVolPerType);
        chartVolPerExercise = findViewById(R.id.chartVolPerExercise);
        tvSchwerstesDatum = findViewById(R.id.tvSchwerstesDatum);
        tvSchwerstesTyp = findViewById(R.id.tvSchwerstesTyp);
        tvSchwerstesGesamt = findViewById(R.id.tvSchwerstesGesamt);
        tvSchwerstesEinzelsatz = findViewById(R.id.tvSchwerstesEinzelsatz);

        chartPPLVerteilung = findViewById(R.id.chartPPLVerteilung);
        chartWochentag = findViewById(R.id.chartWochentag);
        tvAvgPause = findViewById(R.id.tvAvgPause);
        chartPausenHistogramm = findViewById(R.id.chartPausenHistogramm);
        tvAvgZwischen = findViewById(R.id.tvAvgZwischen);

        tvCardioMinuten = findViewById(R.id.tvCardioMinuten);
        tvCardioAvg = findViewById(R.id.tvCardioAvg);
        tvCardioLaengste = findViewById(R.id.tvCardioLaengste);
        tvCardioSessions = findViewById(R.id.tvCardioSessions);
        chartCardioArt = findViewById(R.id.chartCardioArt);
        chartCardioMinuten = findViewById(R.id.chartCardioMinuten);

        tableSchwersterSatz = findViewById(R.id.tableSchwersterSatz);
        tableVolumenRekord = findViewById(R.id.tableVolumenRekord);
        tvVolRekordDatum = findViewById(R.id.tvVolRekordDatum);
        tvVolRekordTyp = findViewById(R.id.tvVolRekordTyp);
        tvVolRekordGesamt = findViewById(R.id.tvVolRekordGesamt);
    }

    private void setupTabs() {
        tabMap.put("uebersicht", tabUebersicht);
        tabMap.put("volumen", tabVolumen);
        tabMap.put("struktur", tabStruktur);
        tabMap.put("cardio", tabCardio);
        tabMap.put("pr", tabPR);

        secMap.put("uebersicht", secUebersicht);
        secMap.put("volumen", secVolumen);
        secMap.put("struktur", secStruktur);
        secMap.put("cardio", secCardio);
        secMap.put("pr", secPR);

        rangeMap.put("uebersicht", spRangeUebersicht);
        rangeMap.put("volumen", spRangeVolumen);
        rangeMap.put("struktur", spRangeStruktur);
        rangeMap.put("cardio", spRangeCardio);
        rangeMap.put("pr", spRangePR);

        for (Map.Entry<String, LinearLayout> e : tabMap.entrySet()) {
            final String key = e.getKey();
            e.getValue().setOnClickListener(v -> showSection(key));
        }

        ((TextView) tabUebersicht.getChildAt(0)).setText("Übersicht");
        ((TextView) tabVolumen.getChildAt(0)).setText("🏋Volumen");
        ((TextView) tabStruktur.getChildAt(0)).setText("Struktur");
        ((TextView) tabCardio.getChildAt(0)).setText("Cardio");
        ((TextView) tabPR.getChildAt(0)).setText("Rekorde");
    }

    private void showSection(String tab) {
        currentTab = tab;
        int activeColor = ContextCompat.getColor(this, R.color.gold_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary);

        for (Map.Entry<String, LinearLayout> e : tabMap.entrySet()) {
            boolean active = e.getKey().equals(tab);
            TextView label = (TextView) e.getValue().getChildAt(0);
            View indicator = e.getValue().getChildAt(1);
            label.setTextColor(active ? activeColor : inactiveColor);
            indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        }

        for (Map.Entry<String, View> e : secMap.entrySet()) {
            e.getValue().setVisibility(e.getKey().equals(tab) ? View.VISIBLE : View.GONE);
        }

        loadSection(tab);
    }

    private void setupRangeSpinners() {
        for (Map.Entry<String, Spinner> e : rangeMap.entrySet()) {
            final String key = e.getKey();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, RANGE_LABELS);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
            e.getValue().setAdapter(adapter);
            e.getValue().setSelection(1);
            e.getValue().setOnItemSelectedListener(new SimpleItemSelected() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (key.equals(currentTab)) {
                        loadSection(key);
                    }
                }
            });
        }
    }

    private int getRange(String tab) {
        Spinner sp = rangeMap.get(tab);
        if (sp == null) return 30;
        int pos = sp.getSelectedItemPosition();
        return (pos >= 0 && pos < RANGE_DAYS.length) ? RANGE_DAYS[pos] : 30;
    }

    private void loadSection(String tab) {
        switch (tab) {
            case "uebersicht": loadUebersicht(); break;
            case "volumen": loadVolumen(); break;
            case "struktur": loadStruktur(); break;
            case "cardio": loadCardio(); break;
            case "pr": loadPR(); break;
        }
    }

    private List<WorkoutStorage.DetailedWorkout> getAllWorkoutsInRange(int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        List<WorkoutStorage.DetailedWorkout> result = new ArrayList<>();
        for (String t : new String[]{WorkoutStorage.TYPE_PUSH, WorkoutStorage.TYPE_PULL, WorkoutStorage.TYPE_LEG}) {
            for (WorkoutStorage.DetailedWorkout w : WorkoutStorage.getDetailedWorkouts(this, t)) {
                try {
                    Date d = sdf.parse(w.timestamp);
                    if (d != null && !d.before(cutoff)) result.add(w);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private Set<String> getTrainedDays(List<WorkoutStorage.DetailedWorkout> workouts) {
        Set<String> days = new TreeSet<>();
        for (WorkoutStorage.DetailedWorkout w : workouts) {
            if (w.timestamp != null) days.add(w.timestamp.split(" ")[0]);
        }
        return days;
    }

    private String getType(WorkoutStorage.DetailedWorkout w) {
        if (PUSH_SET.contains(w.exercise)) return "Push";
        if (LEG_SET.contains(w.exercise)) return "Leg";
        return "Pull";
    }

    private double calcVolume(List<WorkoutStorage.WorkoutSet> sets) {
        double v = 0;
        for (WorkoutStorage.WorkoutSet s : sets) v += s.weight * s.reps;
        return v;
    }

    private Date getDaysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return c.getTime();
    }

    private void loadUebersicht() {
        int days = getRange("uebersicht");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);
        Set<String> trainedDays = getTrainedDays(all);
        int totalSessions = trainedDays.size();

        int totalCardioMin = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        for (String t : new String[]{WorkoutStorage.TYPE_PUSH, WorkoutStorage.TYPE_PULL, WorkoutStorage.TYPE_LEG}) {
            for (WorkoutStorage.CardioSession cs : WorkoutStorage.getCardioSessions(this, t)) {
                try {
                    Date d = sdf.parse(cs.timestamp);
                    if (d != null && !d.before(cutoff)) totalCardioMin += cs.minutes;
                } catch (Exception ignored) {
                }
            }
        }

        Map<String, Integer> setsPerDay = new HashMap<>();
        for (WorkoutStorage.DetailedWorkout w : all) {
            String day = w.timestamp.split(" ")[0];
            setsPerDay.put(day, setsPerDay.getOrDefault(day, 0) + w.sets.size());
        }
        int totalEstMin = 0;
        for (int s : setsPerDay.values()) totalEstMin += s * 3;
        totalEstMin += totalCardioMin;

        double totalHours = totalEstMin / 60.0;
        double avgDuration = totalSessions > 0 ? (double) totalEstMin / totalSessions : 0;
        int rangeWeeks = Math.max(1, days == Integer.MAX_VALUE ? Math.max(1, totalSessions) : days / 7);
        double frequency = (double) totalSessions / rangeWeeks;

        SimpleDateFormat weekFmt = new SimpleDateFormat("'KW'ww/yyyy", Locale.getDefault());
        Map<String, Integer> weekCount = new HashMap<>();
        SimpleDateFormat dayParse = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        for (String day : trainedDays) {
            try {
                Date d = dayParse.parse(day);
                if (d != null) {
                    String wk = weekFmt.format(d);
                    weekCount.put(wk, weekCount.getOrDefault(wk, 0) + 1);
                }
            } catch (Exception ignored) {
            }
        }
        String bestWeek = "–";
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : weekCount.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                bestWeek = e.getKey();
            }
        }

        View kpiWorkouts = findViewById(R.id.kpiWorkouts);
        View kpiHours = findViewById(R.id.kpiHours);
        View kpiDuration = findViewById(R.id.kpiDuration);
        View kpiFrequency = findViewById(R.id.kpiFrequency);

        ((TextView) kpiWorkouts.findViewById(R.id.kpiLabel)).setText("TRAININGS");
        ((TextView) kpiWorkouts.findViewById(R.id.kpiValue)).setText(String.valueOf(totalSessions));

        ((TextView) kpiHours.findViewById(R.id.kpiLabel)).setText("STUNDEN");
        ((TextView) kpiHours.findViewById(R.id.kpiValue)).setText(String.format(Locale.getDefault(), "%.1f", totalHours));

        ((TextView) kpiDuration.findViewById(R.id.kpiLabel)).setText("∅ DAUER");
        ((TextView) kpiDuration.findViewById(R.id.kpiValue)).setText(String.format(Locale.getDefault(), "%.0f min", avgDuration));

        ((TextView) kpiFrequency.findViewById(R.id.kpiLabel)).setText("PRO WOCHE");
        ((TextView) kpiFrequency.findViewById(R.id.kpiValue)).setText(String.format(Locale.getDefault(), "%.1fx", frequency));

        tvBestWeek.setText(bestWeek + " (" + bestCount + " Sessions)");
    }

    private void loadVolumen() {
        int days = getRange("volumen");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);

        double totalVol = 0;
        int totalSaetze = 0;
        Map<String, Double> volPerType = new LinkedHashMap<>();
        volPerType.put("Push", 0.0);
        volPerType.put("Pull", 0.0);
        volPerType.put("Leg", 0.0);
        Map<String, Double> volPerExercise = new HashMap<>();

        Map<String, double[]> dayVolMap = new HashMap<>();

        for (WorkoutStorage.DetailedWorkout w : all) {
            double vol = calcVolume(w.sets);
            totalVol += vol;
            totalSaetze += w.sets.size();
            String type = getType(w);
            volPerType.put(type, volPerType.get(type) + vol);
            volPerExercise.put(w.exercise, volPerExercise.getOrDefault(w.exercise, 0.0) + vol);

            String day = w.timestamp.split(" ")[0];
            double maxSet = 0;
            for (WorkoutStorage.WorkoutSet s : w.sets) {
                double sv = s.weight * s.reps;
                if (sv > maxSet) maxSet = sv;
            }
            if (!dayVolMap.containsKey(day)) dayVolMap.put(day, new double[]{0, 0});
            dayVolMap.get(day)[0] += vol;
            if (maxSet > dayVolMap.get(day)[1]) dayVolMap.get(day)[1] = maxSet;
        }

        int sessionCount = getTrainedDays(all).size();
        tvGesamtVolumen.setText(String.format(Locale.getDefault(), "%.0f kg", totalVol));
        tvAvgVolumen.setText(String.format(Locale.getDefault(), "%.0f kg", sessionCount > 0 ? totalVol / sessionCount : 0));
        tvAvgSatzVolumen.setText(String.format(Locale.getDefault(), "%.0f kg", totalSaetze > 0 ? totalVol / totalSaetze : 0));

        List<BarEntry> typeEntries = new ArrayList<>();
        List<String> typeLabels = new ArrayList<>(volPerType.keySet());
        for (int i = 0; i < typeLabels.size(); i++) typeEntries.add(new BarEntry(i, volPerType.get(typeLabels.get(i)).floatValue()));
        buildBarChart(chartVolPerType, typeEntries, typeLabels, "kg", false);

        List<Map.Entry<String, Double>> sortedEx = new ArrayList<>(volPerExercise.entrySet());
        sortedEx.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
        List<BarEntry> exEntries = new ArrayList<>();
        List<String> exLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(sortedEx.size(), 10); i++) {
            exEntries.add(new BarEntry(i, sortedEx.get(i).getValue().floatValue()));
            exLabels.add(sortedEx.get(i).getKey());
        }
        buildHorizontalBarChart(chartVolPerExercise, exEntries, exLabels);

        String schwerstDatum = "–";
        double schwerstVol = 0;
        String schwerstTyp = "–";
        double schwerstEinzel = 0;
        for (Map.Entry<String, double[]> e : dayVolMap.entrySet()) {
            if (e.getValue()[0] > schwerstVol) {
                schwerstVol = e.getValue()[0];
                schwerstDatum = e.getKey();
                schwerstEinzel = e.getValue()[1];
                String td = e.getKey();
                Map<String, Double> typeOnDay = new HashMap<>();
                for (WorkoutStorage.DetailedWorkout w : all) {
                    if (w.timestamp.startsWith(td)) {
                        String t = getType(w);
                        typeOnDay.put(t, typeOnDay.getOrDefault(t, 0.0) + calcVolume(w.sets));
                    }
                }
                schwerstTyp = typeOnDay.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("–");
            }
        }
        tvSchwerstesDatum.setText(schwerstDatum);
        tvSchwerstesTyp.setText(schwerstTyp);
        tvSchwerstesGesamt.setText(String.format(Locale.getDefault(), "%.0f kg Gesamtvolumen", schwerstVol));
        tvSchwerstesEinzelsatz.setText(String.format(Locale.getDefault(), "%.0f kg Einzelsatz", schwerstEinzel));
    }

    private void loadStruktur() {
        int days = getRange("struktur");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);
        Set<String> trainedDays = getTrainedDays(all);

        Map<String, Set<String>> typeDays = new HashMap<>();
        typeDays.put("Push", new HashSet<>());
        typeDays.put("Pull", new HashSet<>());
        typeDays.put("Leg", new HashSet<>());
        for (WorkoutStorage.DetailedWorkout w : all) {
            typeDays.get(getType(w)).add(w.timestamp.split(" ")[0]);
        }
        float pushC = typeDays.get("Push").size();
        float pullC = typeDays.get("Pull").size();
        float legC = typeDays.get("Leg").size();

        buildPieChart(chartPPLVerteilung,
                new String[]{"Push", "Pull", "Leg"},
                new float[]{pushC, pullC, legC},
                new int[]{R.color.gold_primary, R.color.gold_secondary, R.color.gold_dark});

        SimpleDateFormat dayParse = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        int[] weekdays = new int[7];
        String[] wdLabels = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        for (String day : trainedDays) {
            try {
                Date d = dayParse.parse(day);
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    int dow = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
                    weekdays[dow]++;
                }
            } catch (Exception ignored) {
            }
        }
        List<BarEntry> wdEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) wdEntries.add(new BarEntry(i, weekdays[i]));
        buildBarChart(chartWochentag, wdEntries, Arrays.asList(wdLabels), "", false);

        List<Date> sortedDates = new ArrayList<>();
        for (String day : trainedDays) {
            try {
                sortedDates.add(dayParse.parse(day));
            } catch (Exception ignored) {
            }
        }
        sortedDates.sort(Date::compareTo);

        int[] pauseBins = new int[4];
        long totalPause = 0;
        for (int i = 1; i < sortedDates.size(); i++) {
            long diff = (sortedDates.get(i).getTime() - sortedDates.get(i - 1).getTime()) / 86400000L - 1;
            if (diff < 0) diff = 0;
            totalPause += diff;
            if (diff <= 1) pauseBins[0]++;
            else if (diff <= 3) pauseBins[1]++;
            else if (diff <= 6) pauseBins[2]++;
            else pauseBins[3]++;
        }

        double avgPause = sortedDates.size() > 1 ? (double) totalPause / (sortedDates.size() - 1) : 0;
        tvAvgPause.setText(String.format(Locale.getDefault(), "%.1f Tage", avgPause));
        tvAvgZwischen.setText(String.format(Locale.getDefault(), "%.1f Tage", avgPause));

        List<BarEntry> pauseEntries = new ArrayList<>();
        String[] pauseLabels = {"0–1T", "2–3T", "4–6T", "7+T"};
        for (int i = 0; i < 4; i++) pauseEntries.add(new BarEntry(i, pauseBins[i]));
        buildBarChart(chartPausenHistogramm, pauseEntries, Arrays.asList(pauseLabels), "x", false);
    }

    private void loadCardio() {
        int days = getRange("cardio");
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);

        List<WorkoutStorage.CardioSession> allCardio = new ArrayList<>();
        for (String t : new String[]{WorkoutStorage.TYPE_PUSH, WorkoutStorage.TYPE_PULL, WorkoutStorage.TYPE_LEG}) {
            for (WorkoutStorage.CardioSession cs : WorkoutStorage.getCardioSessions(this, t)) {
                try {
                    Date d = sdf.parse(cs.timestamp);
                    if (d != null && !d.before(cutoff)) allCardio.add(cs);
                } catch (Exception ignored) {
                }
            }
        }

        int totalMin = 0;
        int maxMin = 0;
        String maxDatum = "–";
        Map<String, Integer> artMin = new LinkedHashMap<>();
        Map<String, Integer> artCount = new LinkedHashMap<>();
        for (String e : CARDIO_EXERCISES) {
            artMin.put(e, 0);
            artCount.put(e, 0);
        }

        for (WorkoutStorage.CardioSession cs : allCardio) {
            totalMin += cs.minutes;
            if (cs.minutes > maxMin) {
                maxMin = cs.minutes;
                maxDatum = cs.timestamp.split(" ")[0];
            }
            artMin.put(cs.exercise, artMin.getOrDefault(cs.exercise, 0) + cs.minutes);
            artCount.put(cs.exercise, artCount.getOrDefault(cs.exercise, 0) + 1);
        }

        tvCardioMinuten.setText(totalMin + " min");
        tvCardioAvg.setText(allCardio.isEmpty() ? "–" : String.format(Locale.getDefault(), "%.0f min", (double) totalMin / allCardio.size()));
        tvCardioLaengste.setText(maxMin + " min (" + maxDatum + ")");
        tvCardioSessions.setText(String.valueOf(allCardio.size()));

        List<String> artLabels = new ArrayList<>(artCount.keySet());
        float[] artValues = new float[artLabels.size()];
        for (int i = 0; i < artLabels.size(); i++) artValues[i] = artCount.get(artLabels.get(i));
        buildPieChart(chartCardioArt, artLabels.toArray(new String[0]), artValues, new int[]{R.color.gold_primary, R.color.gold_dark});

        List<BarEntry> minEntries = new ArrayList<>();
        List<String> minLabels = new ArrayList<>(artMin.keySet());
        for (int i = 0; i < minLabels.size(); i++) minEntries.add(new BarEntry(i, artMin.get(minLabels.get(i))));
        buildBarChart(chartCardioMinuten, minEntries, minLabels, "min", false);
    }

    private void loadPR() {
        int days = getRange("pr");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);

        Map<String, double[]> prMap = new HashMap<>();
        Map<String, String> prDate = new HashMap<>();

        Map<String, Double> dayVol = new HashMap<>();
        Map<String, String> dayType = new HashMap<>();

        for (WorkoutStorage.DetailedWorkout w : all) {
            String day = w.timestamp.split(" ")[0];
            double dayV = calcVolume(w.sets);
            dayVol.put(day, dayVol.getOrDefault(day, 0.0) + dayV);
            dayType.put(day, getType(w));

            for (WorkoutStorage.WorkoutSet s : w.sets) {
                double[] best = prMap.get(w.exercise);
                if (best == null || s.weight > best[0] || (s.weight == best[0] && s.reps > best[1])) {
                    prMap.put(w.exercise, new double[]{s.weight, s.reps, s.weight * s.reps});
                    prDate.put(w.exercise, day);
                }
            }
        }

        tableSchwersterSatz.removeAllViews();
        addTableHeader(tableSchwersterSatz, "Übung", "Gewicht", "Wdh.", "Datum");
        List<String> exSorted = new ArrayList<>(prMap.keySet());
        Collections.sort(exSorted);
        for (String ex : exSorted) {
            double[] v = prMap.get(ex);
            addTableRow(tableSchwersterSatz,
                    ex,
                    String.format(Locale.getDefault(), "%.1f kg", v[0]),
                    String.format(Locale.getDefault(), "%.0f", v[1]),
                    prDate.getOrDefault(ex, "–"));
        }

        String rekordDatum = "–";
        double rekordVol = 0;
        String rekordTyp = "–";
        for (Map.Entry<String, Double> e : dayVol.entrySet()) {
            if (e.getValue() > rekordVol) {
                rekordVol = e.getValue();
                rekordDatum = e.getKey();
                rekordTyp = dayType.getOrDefault(e.getKey(), "–");
            }
        }
        tvVolRekordDatum.setText(rekordDatum);
        tvVolRekordTyp.setText(rekordTyp);
        tvVolRekordGesamt.setText(String.format(Locale.getDefault(), "%.0f kg Gesamtvolumen", rekordVol));

        tableVolumenRekord.removeAllViews();
        addTableHeader(tableVolumenRekord, "Übung", "Gewicht", "Max Wdh.", "Datum");

        Map<String, Map<Double, double[]>> repRecord = new HashMap<>();
        Map<String, Map<Double, String>> repDate = new HashMap<>();
        for (WorkoutStorage.DetailedWorkout w : all) {
            repRecord.putIfAbsent(w.exercise, new TreeMap<>());
            repDate.putIfAbsent(w.exercise, new TreeMap<>());
            for (WorkoutStorage.WorkoutSet s : w.sets) {
                Double wKey = s.weight;
                double[] cur = repRecord.get(w.exercise).get(wKey);
                if (cur == null || s.reps > cur[0]) {
                    repRecord.get(w.exercise).put(wKey, new double[]{s.reps});
                    repDate.get(w.exercise).put(wKey, w.timestamp.split(" ")[0]);
                }
            }
        }

        for (String ex : exSorted) {
            if (!repRecord.containsKey(ex)) continue;
            Map<Double, double[]> wMap = repRecord.get(ex);
            double bestW = 0;
            int bestR = 0;
            String bDate = "–";
            for (Map.Entry<Double, double[]> e : wMap.entrySet()) {
                if (e.getValue()[0] > bestR || (e.getValue()[0] == bestR && e.getKey() > bestW)) {
                    bestR = (int) e.getValue()[0];
                    bestW = e.getKey();
                    bDate = repDate.get(ex).get(e.getKey());
                }
            }
            addTableRow(tableVolumenRekord, ex,
                    String.format(Locale.getDefault(), "%.1f kg", bestW),
                    String.valueOf(bestR), bDate);
        }
    }

    private void buildBarChart(BarChart chart, List<BarEntry> entries, List<String> labels, String unit, boolean hasLegend) {
        int gold = ContextCompat.getColor(this, R.color.gold_primary);
        int bg = ContextCompat.getColor(this, R.color.card_background);
        chart.setBackgroundColor(bg);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(hasLegend);
        if (entries.isEmpty()) {
            setNoData(chart);
            return;
        }

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(gold);
        ds.setDrawValues(false);
        chart.setData(new BarData(ds));
        chart.getBarData().setBarWidth(0.6f);
        styleXAxis(chart.getXAxis(), labels);
        styleYAxis(chart.getAxisLeft(), unit);
        chart.getAxisRight().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.animateY(500);
        chart.invalidate();
    }

    private void buildHorizontalBarChart(HorizontalBarChart chart, List<BarEntry> entries, List<String> labels) {
        int gold = ContextCompat.getColor(this, R.color.gold_primary);
        int bg = ContextCompat.getColor(this, R.color.card_background);
        chart.setBackgroundColor(bg);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.clear();
            return;
        }

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(gold);
        ds.setDrawValues(false);
        chart.setData(new BarData(ds));
        chart.getBarData().setBarWidth(0.5f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        xAxis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        xAxis.setDrawAxisLine(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(labels.size(), 10), false);

        styleYAxis(chart.getAxisLeft(), "kg");
        chart.getAxisRight().setEnabled(false);

        chart.setTouchEnabled(false);
        chart.animateX(500);
        chart.invalidate();
    }

    private void buildPieChart(PieChart chart, String[] labels, float[] values, int[] colorRes) {
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.card_background));
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(false);
        chart.setUsePercentValues(true);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.black));

        List<PieEntry> ents = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (values[i] > 0) {
                ents.add(new PieEntry(values[i], labels[i]));
                colors.add(ContextCompat.getColor(this, colorRes[Math.min(i, colorRes.length - 1)]));
            }
        }
        if (ents.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.clear();
            return;
        }

        PieDataSet ds = new PieDataSet(ents, "");
        ds.setColors(colors);
        ds.setSliceSpace(2f);
        ds.setValueTextSize(11f);
        ds.setValueTextColor(ContextCompat.getColor(this, R.color.black));
        ds.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                return String.format(Locale.getDefault(), "%.0f%%", v);
            }
        });
        chart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        chart.getLegend().setTextSize(11f);
        chart.setData(new PieData(ds));
        chart.animateY(600);
        chart.invalidate();
    }

    private void styleXAxis(XAxis x, List<String> labels) {
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        x.setGridColor(ContextCompat.getColor(this, R.color.divider));
        x.setDrawAxisLine(false);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setGranularity(1f);
        x.setLabelCount(Math.min(labels.size(), 6), false);
    }

    private void styleYAxis(com.github.mikephil.charting.components.YAxis y, String unit) {
        y.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        y.setGridColor(ContextCompat.getColor(this, R.color.divider));
        y.setDrawAxisLine(false);
        if (!unit.isEmpty()) {
            y.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float v) {
                    return (int) v + unit;
                }
            });
        }
    }

    private void setNoData(BarChart chart) {
        chart.setNoDataText("Noch keine Daten");
        chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        chart.clear();
    }

    private void addTableHeader(TableLayout table, String... cols) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        for (String col : cols) {
            row.addView(makeCell(col, true));
        }
        table.addView(row);
    }

    private void addTableRow(TableLayout table, String... cols) {
        TableRow row = new TableRow(this);
        boolean odd = table.getChildCount() % 2 == 0;
        row.setBackgroundColor(ContextCompat.getColor(this, odd ? R.color.card_background : R.color.card_background_light));
        for (String col : cols) row.addView(makeCell(col, false));
        table.addView(row);
    }

    private TextView makeCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 10, 12, 10);
        tv.setTextSize(isHeader ? 11f : 12f);
        tv.setTextColor(ContextCompat.getColor(this, isHeader ? R.color.gold_primary : R.color.text_secondary));
        if (isHeader) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        return tv;
    }

    private abstract static class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> p) {
        }
    }
}
