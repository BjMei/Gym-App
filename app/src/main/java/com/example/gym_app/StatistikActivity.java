package com.example.gym_app;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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

public class StatistikActivity extends IronxActivity {

    private static final int[] RANGE_DAYS = {7, 30, 90, 180, 365, Integer.MAX_VALUE};
    private static final String[] RANGE_LABELS = {"7T", "30T", "90T", "6M", "12M", "Gesamt"};
    private final List<String> typeFilterValues = new ArrayList<>();

    private LinearLayout tabUebersicht, tabVolumen, tabStruktur, tabCardio, tabPR;
    private View secUebersicht, secVolumen, secStruktur, secCardio, secPR;
    private ScrollView scrollStatistikContent;

    private Spinner spRangeUebersicht, spRangeVolumen, spRangeStruktur, spRangeCardio, spRangePR;
    private Spinner spFilterType, spFilterExercise;
    private ArrayAdapter<String> exerciseFilterAdapter;
    private final List<ExerciseFilter> exerciseFilterValues = new ArrayList<>();
    private boolean updatingExerciseFilter;

    private TextView tvBestWeek;
    private TextView tvComparisonPeriod;
    private TextView tvCompareSessions, tvCompareVolume, tvCompareDuration;
    private TextView tvGesamtVolumen, tvAvgVolumen, tvAvgSatzVolumen;
    private BarChart chartVolPerType;
    private HorizontalBarChart chartVolPerExercise;
    private LineChart chartWeightTrend, chartVolumeTrend, chartRepsTrend;
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
        findViewById(R.id.btnBackStatistik).setOnClickListener(v -> finish());
        bindViews();
        setupTabs();
        setupRangeSpinners();
        setupFilters();
        setupInfoButtons();
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
        scrollStatistikContent = findViewById(R.id.scrollStatistikContent);

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
        spFilterType = findViewById(R.id.spFilterType);
        spFilterExercise = findViewById(R.id.spFilterExercise);

        tvBestWeek = findViewById(R.id.tvBestWeek);
        tvComparisonPeriod = findViewById(R.id.tvComparisonPeriod);
        tvCompareSessions = findViewById(R.id.tvCompareSessions);
        tvCompareVolume = findViewById(R.id.tvCompareVolume);
        tvCompareDuration = findViewById(R.id.tvCompareDuration);

        tvGesamtVolumen = findViewById(R.id.tvGesamtVolumen);
        tvAvgVolumen = findViewById(R.id.tvAvgVolumen);
        tvAvgSatzVolumen = findViewById(R.id.tvAvgSatzVolumen);
        chartVolPerType = findViewById(R.id.chartVolPerType);
        chartVolPerExercise = findViewById(R.id.chartVolPerExercise);
        chartWeightTrend = findViewById(R.id.chartWeightTrend);
        chartVolumeTrend = findViewById(R.id.chartVolumeTrend);
        chartRepsTrend = findViewById(R.id.chartRepsTrend);
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
        ((TextView) tabVolumen.getChildAt(0)).setText("Volumen");
        ((TextView) tabStruktur.getChildAt(0)).setText("Struktur");
        ((TextView) tabCardio.getChildAt(0)).setText("Cardio");
        ((TextView) tabPR.getChildAt(0)).setText("Rekorde");
    }

    private void showSection(String tab) {
        currentTab = tab;
        int activeColor = ContextCompat.getColor(this, R.color.training_gold_highlight);
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

        boolean exerciseFilterRelevant = !"cardio".equals(tab);
        spFilterExercise.setEnabled(exerciseFilterRelevant);
        spFilterExercise.setAlpha(exerciseFilterRelevant ? 1f : 0.45f);

        loadSection(tab);
        scrollToTop(scrollStatistikContent);
    }

    private void scrollToTop(ScrollView scrollView) {
        if (scrollView == null) {
            return;
        }
        scrollView.post(() -> scrollView.scrollTo(0, 0));
    }

    private void setupRangeSpinners() {
        for (Map.Entry<String, Spinner> e : rangeMap.entrySet()) {
            final String key = e.getKey();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_stats, RANGE_LABELS);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_stats);
            e.getValue().setAdapter(adapter);
            e.getValue().setSelection(RANGE_LABELS.length - 1);
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

    private void setupInfoButtons() {
        InfoDialogHelper.insertInfoRowAfter(
                spRangeUebersicht,
                "Übersicht verstehen",
                InfoDialogHelper.Texts.statisticsOverview()
        );
        InfoDialogHelper.insertInfoRowAfter(
                spRangeVolumen,
                "Volumen berechnen",
                InfoDialogHelper.Texts.statisticsVolume()
        );
        InfoDialogHelper.insertInfoRowAfter(
                spRangeStruktur,
                "Struktur & Pausen",
                InfoDialogHelper.Texts.statisticsStructure()
        );
        InfoDialogHelper.insertInfoRowAfter(
                spRangeCardio,
                "Cardio-Auswertung",
                InfoDialogHelper.Texts.cardio()
        );
        InfoDialogHelper.insertInfoRowAfter(
                spRangePR,
                "Rekorde verstehen",
                InfoDialogHelper.Texts.statisticsRecords()
        );
    }

    private int getRange(String tab) {
        Spinner sp = rangeMap.get(tab);
        if (sp == null) return 30;
        int pos = sp.getSelectedItemPosition();
        return (pos >= 0 && pos < RANGE_DAYS.length)
                ? RANGE_DAYS[pos]
                : Integer.MAX_VALUE;
    }

    private void setupFilters() {
        List<String> typeLabels = new ArrayList<>();
        typeLabels.add("Alle Typen");
        typeFilterValues.clear();
        typeFilterValues.add("");
        for (WorkoutTypeRepository.WorkoutType type :
                WorkoutTypeRepository.getAllTypes(this)) {
            typeLabels.add(WorkoutTypeRepository.label(this, type.id));
            typeFilterValues.add(type.id);
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_stats,
                typeLabels
        );
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_stats);
        spFilterType.setAdapter(typeAdapter);

        exerciseFilterAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_stats,
                new ArrayList<>()
        );
        exerciseFilterAdapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item_stats
        );
        spFilterExercise.setAdapter(exerciseFilterAdapter);

        spFilterType.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                refreshExerciseFilter();
                loadSection(currentTab);
            }
        });

        spFilterExercise.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                if (!updatingExerciseFilter) {
                    loadSection(currentTab);
                }
            }
        });

        refreshExerciseFilter();
    }

    private void refreshExerciseFilter() {
        updatingExerciseFilter = true;
        String selectedType = getSelectedTypeFilter();
        Map<String, ExerciseFilter> exercises = new LinkedHashMap<>();
        List<String> types = selectedType.isEmpty()
                ? WorkoutTypeRepository.getAllTypeIds(this)
                : Collections.singletonList(selectedType);

        for (String type : types) {
            for (WorkoutStorage.DetailedWorkout workout :
                    WorkoutStorage.getDetailedWorkouts(this, type)) {
                if (workout.exercise != null && !workout.exercise.trim().isEmpty()) {
                    String storedType = workout.workoutType == null
                            || workout.workoutType.trim().isEmpty()
                            ? type
                            : workout.workoutType;
                    ExerciseFilter filter =
                            new ExerciseFilter(storedType, workout.exercise.trim());
                    exercises.putIfAbsent(filter.key(), filter);
                }
            }
        }

        List<ExerciseFilter> sorted = new ArrayList<>(exercises.values());
        sorted.sort(Comparator.comparing(filter -> filter.label));
        exerciseFilterValues.clear();
        exerciseFilterValues.addAll(sorted);
        exerciseFilterAdapter.clear();
        exerciseFilterAdapter.add("Alle Übungen");
        for (ExerciseFilter filter : sorted) {
            exerciseFilterAdapter.add(filter.label);
        }
        exerciseFilterAdapter.notifyDataSetChanged();
        spFilterExercise.setSelection(0);
        updatingExerciseFilter = false;
    }

    private String getSelectedTypeFilter() {
        int position = spFilterType == null
                ? 0
                : spFilterType.getSelectedItemPosition();
        return position >= 0 && position < typeFilterValues.size()
                ? typeFilterValues.get(position)
                : "";
    }

    private ExerciseFilter getSelectedExerciseFilter() {
        if (spFilterExercise == null
                || spFilterExercise.getSelectedItemPosition() <= 0
                || spFilterExercise.getSelectedItem() == null) {
            return null;
        }
        int valueIndex = spFilterExercise.getSelectedItemPosition() - 1;
        return valueIndex >= 0 && valueIndex < exerciseFilterValues.size()
                ? exerciseFilterValues.get(valueIndex)
                : null;
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
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        return getWorkoutsBetween(cutoff, new Date());
    }

    private List<WorkoutStorage.DetailedWorkout> getWorkoutsBetween(
            Date startInclusive,
            Date endExclusive) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        List<WorkoutStorage.DetailedWorkout> result = new ArrayList<>();
        String selectedType = getSelectedTypeFilter();
        ExerciseFilter selectedExercise = getSelectedExerciseFilter();
        List<String> types = selectedType.isEmpty()
                ? WorkoutTypeRepository.getAllTypeIds(this)
                : Collections.singletonList(selectedType);

        for (String t : types) {
            for (WorkoutStorage.DetailedWorkout w : WorkoutStorage.getDetailedWorkouts(this, t)) {
                try {
                    Date d = sdf.parse(w.timestamp);
                    if (d != null
                            && !d.before(startInclusive)
                            && d.before(endExclusive)
                            && (selectedExercise == null
                            || (selectedExercise.exercise.equals(w.exercise)
                            && selectedExercise.workoutType.equals(w.workoutType)))) {
                        result.add(w);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private List<WorkoutStorage.CardioSession> getCardioBetween(
            Date startInclusive,
            Date endExclusive) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        List<WorkoutStorage.CardioSession> result = new ArrayList<>();
        String selectedType = getSelectedTypeFilter();
        List<String> types = selectedType.isEmpty()
                ? WorkoutTypeRepository.getAllTypeIds(this)
                : Collections.singletonList(selectedType);

        for (String type : types) {
            for (WorkoutStorage.CardioSession session :
                    WorkoutStorage.getCardioSessions(this, type)) {
                try {
                    Date date = sdf.parse(session.timestamp);
                    if (date != null
                            && !date.before(startInclusive)
                            && date.before(endExclusive)) {
                        result.add(session);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private List<WorkoutStorage.TrainingSession> getMeasuredSessionsBetween(
            Date startInclusive,
            Date endExclusive) {
        if (getSelectedExerciseFilter() != null) {
            return Collections.emptyList();
        }

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String selectedType = getSelectedTypeFilter();
        List<WorkoutStorage.TrainingSession> result = new ArrayList<>();

        for (WorkoutStorage.TrainingSession session :
                WorkoutStorage.getTrainingSessions(this)) {
            if (!selectedType.isEmpty()
                    && !selectedType.equals(session.workoutType)) {
                continue;
            }
            if (!WorkoutStorage.hasTrainingSessionItems(
                    this,
                    session.workoutType,
                    session.sessionId
            )) {
                continue;
            }
            try {
                Date date = sdf.parse(session.timestamp);
                if (date != null
                        && !date.before(startInclusive)
                        && date.before(endExclusive)
                        && session.durationMs > 0
                        && session.completed) {
                    result.add(session);
                }
            } catch (Exception ignored) {
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

    private Map<String, String> getSessionDates(
            List<WorkoutStorage.DetailedWorkout> workouts,
            List<WorkoutStorage.CardioSession> cardioSessions) {
        Map<String, String> sessionDates = new LinkedHashMap<>();
        for (WorkoutStorage.DetailedWorkout workout : workouts) {
            String date = datePart(workout.timestamp);
            if (!date.isEmpty()) {
                sessionDates.putIfAbsent(
                        sessionKey(
                                workout.sessionId,
                                date,
                                workout.workoutType
                        ),
                        date
                );
            }
        }
        for (WorkoutStorage.CardioSession cardio : cardioSessions) {
            String date = datePart(cardio.timestamp);
            if (!date.isEmpty()) {
                sessionDates.putIfAbsent(
                        sessionKey(
                                cardio.sessionId,
                                date,
                                cardio.workoutType
                        ),
                        date
                );
            }
        }
        return sessionDates;
    }

    private String sessionKey(
            String sessionId,
            String date,
            String workoutType) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            return "session|" + sessionId.trim();
        }
        return "legacy|"
                + date
                + "|"
                + (workoutType == null ? "" : workoutType);
    }

    private String datePart(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "";
        }
        return timestamp.trim().split("\\s+")[0];
    }

    private Set<String> getActiveDays(
            List<WorkoutStorage.DetailedWorkout> workouts,
            List<WorkoutStorage.CardioSession> cardioSessions) {
        Set<String> days = getTrainedDays(workouts);
        if (getSelectedExerciseFilter() == null) {
            for (WorkoutStorage.CardioSession session : cardioSessions) {
                if (session.timestamp != null) {
                    days.add(session.timestamp.split(" ")[0]);
                }
            }
        }
        return days;
    }

    private String getType(WorkoutStorage.DetailedWorkout w) {
        return workoutTypeLabel(w.workoutType);
    }

    private String workoutTypeLabel(String workoutType) {
        return WorkoutTypeRepository.label(this, workoutType);
    }

    private String exerciseLabel(WorkoutStorage.DetailedWorkout workout) {
        return workoutTypeLabel(workout.workoutType) + " · " + workout.exercise;
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
        Date end = new Date();
        Date start = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        OverviewMetrics metrics = calculateOverviewMetrics(
                start,
                end,
                days == Integer.MAX_VALUE ? null : days
        );

        Map<String, Integer> weekCount = new HashMap<>();
        DateTimeFormatter dayParse =
                DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY);
        for (String day : metrics.sessionDates.values()) {
            try {
                String week = StatisticsCalculator.isoWeekLabel(
                        LocalDate.parse(day, dayParse)
                );
                weekCount.put(week, weekCount.getOrDefault(week, 0) + 1);
            } catch (RuntimeException ignored) {
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
        ((TextView) kpiWorkouts.findViewById(R.id.kpiValue))
                .setText(String.valueOf(metrics.sessionCount));
        ((TextView) kpiWorkouts.findViewById(R.id.kpiTrend))
                .setText("Einzelne Kraft- und Cardioeinheiten");

        ((TextView) kpiHours.findViewById(R.id.kpiLabel)).setText("STUNDEN");
        ((TextView) kpiHours.findViewById(R.id.kpiValue)).setText(
                metrics.measuredSessionCount == 0
                        ? "–"
                        : String.format(Locale.getDefault(), "%.1f", metrics.totalMinutes / 60.0)
        );
        String durationQuality = metrics.measuredSessionCount == 0
                ? "Keine beendeten Workouts"
                : metrics.measuredSessionCount == 1
                ? "1 Workout fest erfasst"
                : metrics.measuredSessionCount + " Workouts fest erfasst";
        ((TextView) kpiHours.findViewById(R.id.kpiTrend)).setText(durationQuality);

        ((TextView) kpiDuration.findViewById(R.id.kpiLabel)).setText("∅ DAUER");
        ((TextView) kpiDuration.findViewById(R.id.kpiValue)).setText(
                metrics.measuredSessionCount == 0
                        ? "–"
                        : String.format(Locale.getDefault(), "%.0f min", metrics.averageMinutes)
        );
        ((TextView) kpiDuration.findViewById(R.id.kpiTrend)).setText(durationQuality);

        ((TextView) kpiFrequency.findViewById(R.id.kpiLabel)).setText("PRO WOCHE");
        ((TextView) kpiFrequency.findViewById(R.id.kpiValue)).setText(
                String.format(Locale.getDefault(), "%.1fx", metrics.weeklyFrequency)
        );
        ((TextView) kpiFrequency.findViewById(R.id.kpiTrend))
                .setText(days == Integer.MAX_VALUE ? "Seit erster Aktivität" : "Im Zeitraum");

        InfoDialogHelper.bindKpi(kpiWorkouts, "TRAININGS");
        InfoDialogHelper.bindKpi(kpiHours, "STUNDEN");
        InfoDialogHelper.bindKpi(kpiDuration, "Ø DAUER");
        InfoDialogHelper.bindKpi(kpiFrequency, "PRO WOCHE");

        tvBestWeek.setText(bestWeek + " (" + bestCount + " Sessions)");
        updatePeriodComparison(days, end);
    }

    private OverviewMetrics calculateOverviewMetrics(
            Date startInclusive,
            Date endExclusive,
            Integer frequencyDays) {
        List<WorkoutStorage.DetailedWorkout> workouts =
                getWorkoutsBetween(startInclusive, endExclusive);
        List<WorkoutStorage.CardioSession> cardio =
                getCardioBetween(startInclusive, endExclusive);
        Set<String> activeDays = getActiveDays(workouts, cardio);
        Map<String, String> sessionDates = getSessionDates(workouts, cardio);
        List<WorkoutStorage.TrainingSession> measuredSessions =
                getMeasuredSessionsBetween(startInclusive, endExclusive);
        for (WorkoutStorage.TrainingSession session : measuredSessions) {
            if (session.timestamp != null) {
                activeDays.add(session.timestamp.split(" ")[0]);
            }
        }

        double totalVolume = 0;
        for (WorkoutStorage.DetailedWorkout workout : workouts) {
            if (workout.sets != null) {
                totalVolume += calcVolume(workout.sets);
            }
        }

        double totalMinutes = 0;
        for (WorkoutStorage.TrainingSession session : measuredSessions) {
            totalMinutes += session.durationMs / 60_000.0;
        }

        LocalDate firstActivity = null;
        for (String day : activeDays) {
            try {
                LocalDate parsed = LocalDate.parse(
                        day,
                        java.time.format.DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy",
                                Locale.GERMAN
                        )
                );
                if (firstActivity == null || parsed.isBefore(firstActivity)) {
                    firstActivity = parsed;
                }
            } catch (Exception ignored) {
            }
        }

        int sessionCount = sessionDates.size();
        int measuredSessionCount = measuredSessions.size();
        double averageMinutes = measuredSessionCount == 0
                ? 0
                : totalMinutes / measuredSessionCount;
        double weeklyFrequency = StatisticsCalculator.weeklyFrequency(
                sessionCount,
                firstActivity,
                endExclusive.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                frequencyDays
        );

        return new OverviewMetrics(
                activeDays,
                sessionDates,
                sessionCount,
                totalMinutes,
                averageMinutes,
                weeklyFrequency,
                totalVolume,
                measuredSessionCount
        );
    }

    private void updatePeriodComparison(int days, Date now) {
        int comparisonDays = days == Integer.MAX_VALUE ? 30 : days;
        Calendar currentStartCalendar = Calendar.getInstance();
        currentStartCalendar.setTime(now);
        currentStartCalendar.add(Calendar.DAY_OF_YEAR, -comparisonDays);
        Date currentStart = currentStartCalendar.getTime();

        Calendar previousStartCalendar = Calendar.getInstance();
        previousStartCalendar.setTime(currentStart);
        previousStartCalendar.add(Calendar.DAY_OF_YEAR, -comparisonDays);
        Date previousStart = previousStartCalendar.getTime();

        OverviewMetrics current = calculateOverviewMetrics(
                currentStart,
                now,
                comparisonDays
        );
        OverviewMetrics previous = calculateOverviewMetrics(
                previousStart,
                currentStart,
                comparisonDays
        );

        tvComparisonPeriod.setText(days == Integer.MAX_VALUE
                ? "Letzte 30 Tage gegenüber den 30 Tagen davor"
                : RANGE_LABELS[rangeMap.get("uebersicht").getSelectedItemPosition()]
                + " gegenüber dem gleich langen Vorzeitraum");
        tvCompareSessions.setText(
                "TRAININGS " + StatisticsCalculator.formatChange(
                        current.sessionCount,
                        previous.sessionCount
                )
        );
        tvCompareVolume.setText(
                "VOLUMEN " + StatisticsCalculator.formatChange(
                        current.totalVolume,
                        previous.totalVolume
                )
        );
        if (current.measuredSessionCount == 0 || previous.measuredSessionCount == 0) {
            tvCompareDuration.setText("DAUER nicht vergleichbar");
        } else {
            tvCompareDuration.setText(
                    "DAUER " + StatisticsCalculator.formatChange(
                            current.averageMinutes,
                            previous.averageMinutes
                    )
            );
        }
    }

    private static final class OverviewMetrics {
        final Set<String> activeDays;
        final Map<String, String> sessionDates;
        final int sessionCount;
        final double totalMinutes;
        final double averageMinutes;
        final double weeklyFrequency;
        final double totalVolume;
        final int measuredSessionCount;

        OverviewMetrics(
                Set<String> activeDays,
                Map<String, String> sessionDates,
                int sessionCount,
                double totalMinutes,
                double averageMinutes,
                double weeklyFrequency,
                double totalVolume,
                int measuredSessionCount) {
            this.activeDays = activeDays;
            this.sessionDates = sessionDates;
            this.sessionCount = sessionCount;
            this.totalMinutes = totalMinutes;
            this.averageMinutes = averageMinutes;
            this.weeklyFrequency = weeklyFrequency;
            this.totalVolume = totalVolume;
            this.measuredSessionCount = measuredSessionCount;
        }
    }

    private static final class CardioAggregate {
        final String date;
        int minutes;

        CardioAggregate(String date) {
            this.date = date;
        }
    }

    private final class ExerciseFilter {
        final String workoutType;
        final String exercise;
        final String label;

        ExerciseFilter(String workoutType, String exercise) {
            this.workoutType = workoutType;
            this.exercise = exercise;
            this.label = workoutTypeLabel(workoutType) + " · " + exercise;
        }

        String key() {
            return workoutType + "|" + exercise.toLowerCase(Locale.ROOT);
        }
    }

    private void loadVolumen() {
        int days = getRange("volumen");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);

        double totalVol = 0;
        int totalSaetze = 0;
        Map<String, Double> volPerType = new LinkedHashMap<>();
        for (WorkoutTypeRepository.WorkoutType type :
                WorkoutTypeRepository.getAllTypes(this)) {
            volPerType.put(WorkoutTypeRepository.label(this, type.id), 0.0);
        }
        Map<String, Double> volPerExercise = new HashMap<>();

        List<StatisticsCalculator.VolumeEntry> sessionVolumeEntries =
                new ArrayList<>();

        for (WorkoutStorage.DetailedWorkout w : all) {
            double vol = calcVolume(w.sets);
            totalVol += vol;
            totalSaetze += w.sets.size();
            String type = getType(w);
            volPerType.put(type, volPerType.getOrDefault(type, 0.0) + vol);
            String exerciseLabel = exerciseLabel(w);
            volPerExercise.put(
                    exerciseLabel,
                    volPerExercise.getOrDefault(exerciseLabel, 0.0) + vol
            );

            String day = datePart(w.timestamp);
            double maxSet = 0;
            for (WorkoutStorage.WorkoutSet s : w.sets) {
                double sv = s.weight * s.reps;
                if (sv > maxSet) maxSet = sv;
            }
            sessionVolumeEntries.add(new StatisticsCalculator.VolumeEntry(
                    w.sessionId,
                    day,
                    type,
                    vol,
                    maxSet
            ));
        }

        int sessionCount = getSessionDates(all, Collections.emptyList()).size();
        tvGesamtVolumen.setText(AppSettings.formatVolume(this, totalVol, 0));
        tvAvgVolumen.setText(AppSettings.formatVolume(
                this,
                sessionCount > 0 ? totalVol / sessionCount : 0,
                0
        ));
        tvAvgSatzVolumen.setText(AppSettings.formatVolume(
                this,
                totalSaetze > 0 ? totalVol / totalSaetze : 0,
                0
        ));

        List<BarEntry> typeEntries = new ArrayList<>();
        List<String> typeLabels = new ArrayList<>(volPerType.keySet());
        for (int i = 0; i < typeLabels.size(); i++) {
            typeEntries.add(new BarEntry(
                    i,
                    (float) AppSettings.fromStoredKg(
                            this,
                            volPerType.get(typeLabels.get(i))
                    )
            ));
        }
        buildBarChart(
                chartVolPerType,
                typeEntries,
                typeLabels,
                AppSettings.getWeightUnit(this),
                false
        );

        List<Map.Entry<String, Double>> sortedEx =
                StatisticsCalculator.topDescending(volPerExercise, 10);
        List<BarEntry> exEntries = new ArrayList<>();
        List<String> exLabels = new ArrayList<>();
        for (int i = 0; i < sortedEx.size(); i++) {
            exEntries.add(new BarEntry(
                    i,
                    (float) AppSettings.fromStoredKg(this, sortedEx.get(i).getValue())
            ));
            exLabels.add(sortedEx.get(i).getKey());
        }
        buildHorizontalBarChart(chartVolPerExercise, exEntries, exLabels);
        buildTrendCharts(all);

        String schwerstDatum = "–";
        double schwerstVol = 0;
        String schwerstTyp = "–";
        double schwerstEinzel = 0;
        for (StatisticsCalculator.SessionVolume session :
                StatisticsCalculator.aggregateSessionVolumes(sessionVolumeEntries)) {
            if (session.totalVolume > schwerstVol) {
                schwerstVol = session.totalVolume;
                schwerstDatum = session.date;
                schwerstEinzel = session.maxSetVolume;
                schwerstTyp = session.workoutType;
            }
        }
        tvSchwerstesDatum.setText(schwerstDatum);
        tvSchwerstesTyp.setText(schwerstTyp);
        tvSchwerstesGesamt.setText(
                AppSettings.formatVolume(this, schwerstVol, 0) + " Gesamtvolumen"
        );
        tvSchwerstesEinzelsatz.setText(
                AppSettings.formatVolume(this, schwerstEinzel, 0) + " Einzelsatz"
        );
    }

    private void buildTrendCharts(List<WorkoutStorage.DetailedWorkout> workouts) {
        SimpleDateFormat parseFull =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat keyFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat =
                new SimpleDateFormat("dd.MM.", Locale.getDefault());

        Map<String, double[]> valuesByDay = new TreeMap<>();
        Map<String, String> labelsByDay = new HashMap<>();

        for (WorkoutStorage.DetailedWorkout workout : workouts) {
            if (workout.sets == null) {
                continue;
            }
            try {
                Date date = parseFull.parse(workout.timestamp);
                if (date == null) {
                    continue;
                }
                String key = keyFormat.format(date);
                labelsByDay.put(key, labelFormat.format(date));
                double[] values = valuesByDay.computeIfAbsent(
                        key,
                        ignored -> new double[]{0, 0, 0, 0}
                );
                for (WorkoutStorage.WorkoutSet set : workout.sets) {
                    values[0] = Math.max(values[0], set.weight);
                    values[1] += set.weight * set.reps;
                    values[2] += set.reps;
                    values[3]++;
                }
            } catch (Exception ignored) {
            }
        }

        List<Entry> weightEntries = new ArrayList<>();
        List<Entry> volumeEntries = new ArrayList<>();
        List<Entry> repsEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, double[]> entry : valuesByDay.entrySet()) {
            double[] values = entry.getValue();
            labels.add(labelsByDay.getOrDefault(entry.getKey(), entry.getKey()));
            weightEntries.add(new Entry(
                    index,
                    (float) AppSettings.fromStoredKg(this, values[0])
            ));
            volumeEntries.add(new Entry(
                    index,
                    (float) AppSettings.fromStoredKg(this, values[1])
            ));
            repsEntries.add(new Entry(
                    index,
                    values[3] == 0 ? 0 : (float) (values[2] / values[3])
            ));
            index++;
        }

        String weightUnit = AppSettings.getWeightUnit(this);
        buildLineChart(chartWeightTrend, weightEntries, labels, weightUnit);
        buildLineChart(chartVolumeTrend, volumeEntries, labels, weightUnit);
        buildLineChart(chartRepsTrend, repsEntries, labels, " Wdh.");
    }

    private void loadStruktur() {
        int days = getRange("struktur");
        List<WorkoutStorage.DetailedWorkout> all = getAllWorkoutsInRange(days);
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        List<WorkoutStorage.CardioSession> cardio =
                getCardioBetween(cutoff, new Date());
        Set<String> trainedDays = getActiveDays(all, cardio);

        Map<String, Set<String>> typeSessions = new LinkedHashMap<>();
        for (WorkoutTypeRepository.WorkoutType type :
                WorkoutTypeRepository.getAllTypes(this)) {
            typeSessions.put(
                    WorkoutTypeRepository.label(this, type.id),
                    new HashSet<>()
            );
        }
        for (WorkoutStorage.DetailedWorkout w : all) {
            String type = getType(w);
            typeSessions.computeIfAbsent(type, ignored -> new HashSet<>())
                    .add(sessionKey(
                            w.sessionId,
                            datePart(w.timestamp),
                            w.workoutType
                    ));
        }
        for (WorkoutStorage.CardioSession session : cardio) {
            String type = workoutTypeLabel(session.workoutType);
            typeSessions.computeIfAbsent(type, ignored -> new HashSet<>())
                    .add(sessionKey(
                            session.sessionId,
                            datePart(session.timestamp),
                            session.workoutType
                    ));
        }

        String[] typeLabels = typeSessions.keySet().toArray(new String[0]);
        float[] typeCounts = new float[typeLabels.length];
        for (int i = 0; i < typeLabels.length; i++) {
            typeCounts[i] = typeSessions.get(typeLabels[i]).size();
        }
        buildPieChart(chartPPLVerteilung,
                typeLabels,
                typeCounts,
                workoutPalette(typeLabels.length));

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

        List<LocalDate> sortedDates = new ArrayList<>();
        java.time.format.DateTimeFormatter storageDate =
                java.time.format.DateTimeFormatter.ofPattern(
                        "dd.MM.yyyy",
                        Locale.GERMAN
                );
        for (String day : trainedDays) {
            try {
                sortedDates.add(LocalDate.parse(day, storageDate));
            } catch (Exception ignored) {
            }
        }
        StatisticsCalculator.PauseStats pauseStats =
                StatisticsCalculator.calculatePauseStats(sortedDates);
        tvAvgPause.setText(String.format(
                Locale.getDefault(),
                "%.1f Tage",
                pauseStats.averageRestDays
        ));
        tvAvgZwischen.setText(String.format(
                Locale.getDefault(),
                "%.1f Tage",
                pauseStats.averageGapDays
        ));

        List<BarEntry> pauseEntries = new ArrayList<>();
        String[] pauseLabels = {"0–1T", "2–3T", "4–6T", "7+T"};
        for (int i = 0; i < 4; i++) {
            pauseEntries.add(new BarEntry(i, pauseStats.restDayBins[i]));
        }
        buildBarChart(chartPausenHistogramm, pauseEntries, Arrays.asList(pauseLabels), "x", false);
    }

    private void loadCardio() {
        int days = getRange("cardio");
        Date cutoff = days == Integer.MAX_VALUE ? new Date(0) : getDaysAgo(days);
        List<WorkoutStorage.CardioSession> allCardio =
                getCardioBetween(cutoff, new Date());

        Map<String, CardioAggregate> cardioSessions = new LinkedHashMap<>();
        for (WorkoutStorage.CardioSession cardio : allCardio) {
            String date = datePart(cardio.timestamp);
            String key = sessionKey(
                    cardio.sessionId,
                    date,
                    cardio.workoutType
            );
            CardioAggregate aggregate = cardioSessions.computeIfAbsent(
                    key,
                    ignored -> new CardioAggregate(date)
            );
            aggregate.minutes += cardio.minutes;
        }

        int totalMin = 0;
        int maxMin = 0;
        String maxDatum = "–";
        Map<String, Integer> artMin = new LinkedHashMap<>();
        Map<String, Integer> artCount = new LinkedHashMap<>();
        for (WorkoutStorage.CardioSession cs : allCardio) {
            String cardioLabel = workoutTypeLabel(cs.workoutType) + " · " + cs.exercise;
            artMin.put(cardioLabel, artMin.getOrDefault(cardioLabel, 0) + cs.minutes);
            artCount.put(cardioLabel, artCount.getOrDefault(cardioLabel, 0) + 1);
        }
        for (CardioAggregate aggregate : cardioSessions.values()) {
            totalMin += aggregate.minutes;
            if (aggregate.minutes > maxMin) {
                maxMin = aggregate.minutes;
                maxDatum = aggregate.date;
            }
        }

        tvCardioMinuten.setText(totalMin + " min");
        tvCardioAvg.setText(cardioSessions.isEmpty()
                ? "–"
                : String.format(
                        Locale.getDefault(),
                        "%.0f min",
                        (double) totalMin / cardioSessions.size()
                ));
        tvCardioLaengste.setText(maxMin + " min (" + maxDatum + ")");
        tvCardioSessions.setText(String.valueOf(cardioSessions.size()));

        List<String> artLabels = new ArrayList<>(artCount.keySet());
        float[] artValues = new float[artLabels.size()];
        for (int i = 0; i < artLabels.size(); i++) artValues[i] = artCount.get(artLabels.get(i));
        buildPieChart(
                chartCardioArt,
                artLabels.toArray(new String[0]),
                artValues,
                new int[]{R.color.training_gold_highlight, R.color.training_gold_pressed}
        );

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

        List<StatisticsCalculator.VolumeEntry> sessionVolumeEntries =
                new ArrayList<>();

        for (WorkoutStorage.DetailedWorkout w : all) {
            String day = datePart(w.timestamp);
            double workoutVolume = calcVolume(w.sets);
            String type = getType(w);
            double maxSetVolume = 0d;
            for (WorkoutStorage.WorkoutSet set : w.sets) {
                maxSetVolume = Math.max(maxSetVolume, set.weight * set.reps);
            }
            sessionVolumeEntries.add(new StatisticsCalculator.VolumeEntry(
                    w.sessionId,
                    day,
                    type,
                    workoutVolume,
                    maxSetVolume
            ));

            for (WorkoutStorage.WorkoutSet s : w.sets) {
                String exerciseLabel = exerciseLabel(w);
                double[] best = prMap.get(exerciseLabel);
                if (best == null || s.weight > best[0] || (s.weight == best[0] && s.reps > best[1])) {
                    prMap.put(exerciseLabel, new double[]{s.weight, s.reps, s.weight * s.reps});
                    prDate.put(exerciseLabel, day);
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
                    AppSettings.formatWeight(this, v[0], 1),
                    String.format(Locale.getDefault(), "%.0f", v[1]),
                    prDate.getOrDefault(ex, "–"));
        }

        String rekordDatum = "–";
        double rekordVol = 0;
        String rekordTyp = "–";
        for (StatisticsCalculator.SessionVolume session :
                StatisticsCalculator.aggregateSessionVolumes(sessionVolumeEntries)) {
            if (session.totalVolume > rekordVol) {
                rekordVol = session.totalVolume;
                rekordDatum = session.date;
                rekordTyp = session.workoutType;
            }
        }
        tvVolRekordDatum.setText(rekordDatum);
        tvVolRekordTyp.setText(rekordTyp);
        tvVolRekordGesamt.setText(
                AppSettings.formatVolume(this, rekordVol, 0) + " Gesamtvolumen"
        );

        tableVolumenRekord.removeAllViews();
        addTableHeader(tableVolumenRekord, "Übung", "Gewicht", "Max Wdh.", "Datum");

        Map<String, Map<Double, double[]>> repRecord = new HashMap<>();
        Map<String, Map<Double, String>> repDate = new HashMap<>();
        for (WorkoutStorage.DetailedWorkout w : all) {
            String exerciseLabel = exerciseLabel(w);
            repRecord.putIfAbsent(exerciseLabel, new TreeMap<>());
            repDate.putIfAbsent(exerciseLabel, new TreeMap<>());
            for (WorkoutStorage.WorkoutSet s : w.sets) {
                Double wKey = s.weight;
                double[] cur = repRecord.get(exerciseLabel).get(wKey);
                if (cur == null || s.reps > cur[0]) {
                    repRecord.get(exerciseLabel).put(wKey, new double[]{s.reps});
                    repDate.get(exerciseLabel).put(wKey, w.timestamp.split(" ")[0]);
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
                    AppSettings.formatWeight(this, bestW, 1),
                    String.valueOf(bestR), bDate);
        }
    }

    private void buildBarChart(BarChart chart, List<BarEntry> entries, List<String> labels, String unit, boolean hasLegend) {
        int gold = ContextCompat.getColor(this, R.color.training_gold);
        chart.setBackgroundResource(R.drawable.bg_home_metric);
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
        chart.setHighlightPerTapEnabled(true);
        chart.setOnChartValueSelectedListener(
                createIndexedChartListener(labels, unit)
        );
        if (AppSettings.animationsEnabled(this)) {
            chart.animateY(500);
        }
        chart.invalidate();
    }

    private void buildHorizontalBarChart(HorizontalBarChart chart, List<BarEntry> entries, List<String> labels) {
        int gold = ContextCompat.getColor(this, R.color.training_gold);
        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
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

        String weightUnit = AppSettings.getWeightUnit(this);
        styleYAxis(chart.getAxisLeft(), weightUnit);
        chart.getAxisRight().setEnabled(false);

        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setOnChartValueSelectedListener(
                createIndexedChartListener(labels, weightUnit)
        );
        if (AppSettings.animationsEnabled(this)) {
            chart.animateX(500);
        }
        chart.invalidate();
    }

    private int[] workoutPalette(int count) {
        int[] base = {
                R.color.training_gold_highlight,
                R.color.training_gold,
                R.color.training_gold_pressed,
                R.color.gold_light,
                R.color.gold_dark,
                R.color.text_secondary
        };
        int[] result = new int[Math.max(1, count)];
        for (int i = 0; i < result.length; i++) {
            result[i] = base[i % base.length];
        }
        return result;
    }

    private void buildPieChart(PieChart chart, String[] labels, float[] values, int[] colorRes) {
        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(false);
        chart.setUsePercentValues(true);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.black));
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);

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
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
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
        float totalPieValue = 0;
        for (float value : values) {
            totalPieValue += value;
        }
        final float pieTotal = totalPieValue;
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                if (entry instanceof PieEntry) {
                    PieEntry pieEntry = (PieEntry) entry;
                    float percentage = pieTotal <= 0
                            ? 0
                            : (pieEntry.getValue() / pieTotal) * 100f;
                    showChartDetail(String.format(
                            Locale.getDefault(),
                            "%s: %.0f%%",
                            pieEntry.getLabel(),
                            percentage
                    ));
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
        if (AppSettings.animationsEnabled(this)) {
            chart.animateY(600);
        }
        chart.invalidate();
    }

    private void buildLineChart(
            LineChart chart,
            List<Entry> entries,
            List<String> labels,
            String unit) {
        int gold = ContextCompat.getColor(this, R.color.training_gold);
        int background = ContextCompat.getColor(this, R.color.primary);

        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(true);

        if (entries.isEmpty()) {
            chart.setNoDataText("Noch keine Daten");
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            chart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(gold);
        dataSet.setCircleColor(gold);
        dataSet.setCircleHoleColor(background);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(gold);
        dataSet.setFillAlpha(28);
        dataSet.setHighLightColor(
                ContextCompat.getColor(this, R.color.training_gold_highlight)
        );
        chart.setData(new LineData(dataSet));

        styleXAxis(chart.getXAxis(), labels);
        styleYAxis(chart.getAxisLeft(), unit);
        chart.getAxisRight().setEnabled(false);
        chart.setOnChartValueSelectedListener(
                createIndexedChartListener(labels, unit)
        );
        if (AppSettings.animationsEnabled(this)) {
            chart.animateX(500);
        }
        chart.invalidate();
    }

    private OnChartValueSelectedListener createIndexedChartListener(
            List<String> labels,
            String unit) {
        return new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                int index = Math.round(entry.getX());
                String label = index >= 0 && index < labels.size()
                        ? labels.get(index)
                        : "";
                String suffix = unit == null || unit.isEmpty() ? "" : " " + unit.trim();
                showChartDetail(String.format(
                        Locale.getDefault(),
                        "%s: %.1f%s",
                        label,
                        entry.getY(),
                        suffix
                ));
            }

            @Override
            public void onNothingSelected() {
            }
        };
    }

    private void showChartDetail(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
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
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_light));
        row.setMinimumHeight(dpToPx(44));
        for (String col : cols) {
            row.addView(makeCell(col, true));
        }
        table.addView(row);
    }

    private void addTableRow(TableLayout table, String... cols) {
        TableRow row = new TableRow(this);
        boolean odd = table.getChildCount() % 2 == 0;
        row.setBackgroundColor(ContextCompat.getColor(
                this,
                odd ? R.color.primary : R.color.primary_light
        ));
        row.setMinimumHeight(dpToPx(44));
        for (String col : cols) row.addView(makeCell(col, false));
        table.addView(row);
    }

    private TextView makeCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        tv.setTextSize(isHeader ? 11f : 12f);
        tv.setTextColor(ContextCompat.getColor(
                this,
                isHeader ? R.color.training_gold_highlight : R.color.text_secondary
        ));
        if (isHeader) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private abstract static class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> p) {
        }
    }
}
