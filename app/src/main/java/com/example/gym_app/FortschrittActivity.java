package com.example.gym_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FortschrittActivity extends IronxActivity {

    private static final Integer[] RANGE_DAYS = {7, 30, 90, 180, 365, null};
    private static final String[] RANGE_LABELS = {"7T", "30T", "90T", "6M", "12M", "Gesamt"};
    private static final String[] WORKOUT_TYPES = {
            WorkoutStorage.TYPE_PUSH,
            WorkoutStorage.TYPE_PULL,
            WorkoutStorage.TYPE_LEG
    };
    private static final String[] MUSCLE_GROUPS = {
            "Brust", "Rücken", "Schultern", "Arme", "Core", "Beine"
    };
    private static final String PREFS_APP = "AppSettings";
    private static final String PREFS_MUSCLES = "ExerciseMuscleMappings";
    private static final String KEY_UNITS = "units";
    private static final double KG_TO_LBS = 2.2046226218;
    private static final DateTimeFormatter STORAGE_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY);
    private static final DateTimeFormatter CHART_DATE =
            DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY);
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
    private static final Map<String, List<String>> DEFAULT_MUSCLE_MAP =
            createDefaultMuscleMap();

    private Spinner spinnerRange;
    private Spinner spinnerExercise;
    private Spinner spinnerMuscleExercise;
    private ArrayAdapter<ExerciseOption> exerciseAdapter;
    private ArrayAdapter<ExerciseOption> muscleExerciseAdapter;

    private LinearLayout tabKraft;
    private LinearLayout tabMuskeln;
    private LinearLayout tabKonsistenz;
    private LinearLayout tabKoerper;
    private View sectionKraft;
    private View sectionMuskeln;
    private View sectionKonsistenz;
    private View sectionKoerper;
    private String currentTab = "kraft";

    private View kpiBest1Rm;
    private View kpiRelativeStrength;
    private View kpiStrengthVolume;
    private View kpiPrCount;
    private TextView tvKraftComparisonPeriod;
    private LineChart chart1RM;
    private LineChart chartMaxGewicht;
    private LineChart chartVolumen;
    private LineChart chartWiederholungen;

    private View kpiMuscleVolume;
    private View kpiMuscleBalance;
    private TextView tvMuscleComparison;
    private TextView tvMuscleMapping;
    private BarChart chartMuskelVolumen;
    private PieChart chartPushPull;
    private PieChart chartOberUnter;
    private TextView tvDysbalance;

    private View kpiCurrentStreak;
    private View kpiBestStreak;
    private View kpiWeeklyAverage;
    private View kpiGoalAchievement;
    private TextView tvConsistencyComparison;
    private BarChart chartWeeklySessions;
    private PieChart chartGoalAchievement;
    private TextView tvGoalStatus;

    private View kpiCurrentWeight;
    private View kpiWeightChange;
    private View kpiTargetWeight;
    private View kpiBodyCardio;
    private TextView tvWeightGoalStatus;
    private TextView tvProgressFocusHint;
    private TextView tvPersonalizedAssessment;
    private TextView tvWeeklyGoalConfigured;
    private LineChart chartBodyWeight;

    private List<ExerciseOption> exerciseOptions = new ArrayList<>();
    private SharedPreferences appPreferences;
    private SharedPreferences musclePreferences;
    private boolean displayLbs;
    private String weightUnit;
    private double weightFactor;
    private ProfileRepository profileRepository;
    private ProfileRepository.Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fortschritt);
        applyWindowInsets();

        appPreferences = getSharedPreferences(PREFS_APP, MODE_PRIVATE);
        profileRepository = new ProfileRepository(this);
        profile = profileRepository.load();
        musclePreferences = getSharedPreferences(PREFS_MUSCLES, MODE_PRIVATE);
        displayLbs = getString(R.string.settings_unit_lbs).equals(
                appPreferences.getString(KEY_UNITS, getString(R.string.settings_unit_kg))
        );
        weightUnit = displayLbs ? "lbs" : "kg";
        weightFactor = displayLbs ? KG_TO_LBS : 1.0;

        bindViews();
        setupTabs();
        setupRangeSpinner();
        setupExerciseSpinners();
        setupWeeklyGoalDisplay();
        setupMuscleMapping();
        updateGoalSummary();

        findViewById(R.id.btnBackFortschritt).setOnClickListener(v -> finish());
        showSection(getDefaultTab());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profileRepository == null) {
            return;
        }
        profile = profileRepository.load();
        updateGoalSummary();
        updateWeeklyGoalDisplay();
        if (sectionKraft != null) {
            loadCurrentSection();
        }
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootFortschrittLayout);
        int left = rootLayout.getPaddingLeft();
        int top = rootLayout.getPaddingTop();
        int right = rootLayout.getPaddingRight();
        int bottom = rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + systemBars.left,
                    top + systemBars.top,
                    right + systemBars.right,
                    bottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(rootLayout);
    }

    private void bindViews() {
        spinnerRange = findViewById(R.id.spinnerProgressRange);
        spinnerExercise = findViewById(R.id.spinnerFortschrittExercise);
        spinnerMuscleExercise = findViewById(R.id.spinnerMuscleExercise);

        tabKraft = findViewById(R.id.tabKraft);
        tabMuskeln = findViewById(R.id.tabMuskeln);
        tabKonsistenz = findViewById(R.id.tabKonsistenz);
        tabKoerper = findViewById(R.id.tabKoerper);
        sectionKraft = findViewById(R.id.sectionKraft);
        sectionMuskeln = findViewById(R.id.sectionMuskeln);
        sectionKonsistenz = findViewById(R.id.sectionKonsistenz);
        sectionKoerper = findViewById(R.id.sectionKoerper);
        tvProgressFocusHint = findViewById(R.id.tvProgressFocusHint);
        tvPersonalizedAssessment = findViewById(R.id.tvPersonalizedAssessment);
        tvWeeklyGoalConfigured = findViewById(R.id.tvWeeklyGoalConfigured);

        kpiBest1Rm = findViewById(R.id.kpiBest1Rm);
        kpiRelativeStrength = findViewById(R.id.kpiRelativeStrength);
        kpiStrengthVolume = findViewById(R.id.kpiStrengthVolume);
        kpiPrCount = findViewById(R.id.kpiPrCount);
        tvKraftComparisonPeriod = findViewById(R.id.tvKraftComparisonPeriod);
        chart1RM = findViewById(R.id.chart1RM);
        chartMaxGewicht = findViewById(R.id.chartMaxGewicht);
        chartVolumen = findViewById(R.id.chartVolumen);
        chartWiederholungen = findViewById(R.id.chartWiederholungen);

        kpiMuscleVolume = findViewById(R.id.kpiMuscleVolume);
        kpiMuscleBalance = findViewById(R.id.kpiMuscleBalance);
        tvMuscleComparison = findViewById(R.id.tvMuscleComparison);
        tvMuscleMapping = findViewById(R.id.tvMuscleMapping);
        chartMuskelVolumen = findViewById(R.id.chartMuskelVolumen);
        chartPushPull = findViewById(R.id.chartPushPull);
        chartOberUnter = findViewById(R.id.chartOberUnter);
        tvDysbalance = findViewById(R.id.tvDysbalance);

        kpiCurrentStreak = findViewById(R.id.kpiCurrentStreak);
        kpiBestStreak = findViewById(R.id.kpiBestStreak);
        kpiWeeklyAverage = findViewById(R.id.kpiWeeklyAverage);
        kpiGoalAchievement = findViewById(R.id.kpiGoalAchievement);
        tvConsistencyComparison = findViewById(R.id.tvConsistencyComparison);
        chartWeeklySessions = findViewById(R.id.chartWeeklySessions);
        chartGoalAchievement = findViewById(R.id.chartGoalAchievement);
        tvGoalStatus = findViewById(R.id.tvGoalStatus);

        kpiCurrentWeight = findViewById(R.id.kpiCurrentWeight);
        kpiWeightChange = findViewById(R.id.kpiWeightChange);
        kpiTargetWeight = findViewById(R.id.kpiTargetWeight);
        kpiBodyCardio = findViewById(R.id.kpiBodyCardio);
        tvWeightGoalStatus = findViewById(R.id.tvWeightGoalStatus);
        chartBodyWeight = findViewById(R.id.chartBodyWeight);
    }

    private void setupTabs() {
        ((TextView) tabKraft.getChildAt(0)).setText("Kraft");
        ((TextView) tabMuskeln.getChildAt(0)).setText("Muskeln");
        ((TextView) tabKonsistenz.getChildAt(0)).setText("Konsistenz");
        ((TextView) tabKoerper.getChildAt(0)).setText(R.string.progress_tab_body);

        tabKraft.setOnClickListener(v -> showSection("kraft"));
        tabMuskeln.setOnClickListener(v -> showSection("muskeln"));
        tabKonsistenz.setOnClickListener(v -> showSection("konsistenz"));
        tabKoerper.setOnClickListener(v -> showSection("koerper"));
    }

    private void setupRangeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_stats,
                RANGE_LABELS
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_stats);
        spinnerRange.setAdapter(adapter);
        spinnerRange.setSelection(RANGE_LABELS.length - 1);
        spinnerRange.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                loadCurrentSection();
            }
        });
    }

    private void setupExerciseSpinners() {
        exerciseOptions = collectExerciseOptions();
        exerciseAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_stats,
                exerciseOptions
        );
        exerciseAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_stats);
        spinnerExercise.setAdapter(exerciseAdapter);
        spinnerExercise.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                if ("kraft".equals(currentTab)) {
                    loadKraftData();
                }
            }
        });

        muscleExerciseAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_stats,
                exerciseOptions
        );
        muscleExerciseAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_stats);
        spinnerMuscleExercise.setAdapter(muscleExerciseAdapter);
        spinnerMuscleExercise.setOnItemSelectedListener(new SimpleItemSelected() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {
                updateMuscleMappingText();
            }
        });
    }

    private void setupWeeklyGoalDisplay() {
        findViewById(R.id.btnEditWeeklyGoal).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileGoalsActivity.class))
        );
        updateWeeklyGoalDisplay();
    }

    private void updateWeeklyGoalDisplay() {
        if (tvWeeklyGoalConfigured == null || profile == null) {
            return;
        }
        StringBuilder text = new StringBuilder()
                .append(getWeeklyGoal())
                .append(getWeeklyGoal() == 1
                        ? " Trainingstag pro Woche"
                        : " Trainingstage pro Woche");
        String preferredDays = formatPreferredDays(profile.preferredDays);
        if (!preferredDays.isEmpty()) {
            text.append("\nBevorzugt: ").append(preferredDays);
        }
        tvWeeklyGoalConfigured.setText(text);
    }

    private void setupMuscleMapping() {
        findViewById(R.id.btnEditMuscleMapping)
                .setOnClickListener(v -> showMuscleMappingDialog());
    }

    private void showSection(String tab) {
        currentTab = tab;
        sectionKraft.setVisibility("kraft".equals(tab) ? View.VISIBLE : View.GONE);
        sectionMuskeln.setVisibility("muskeln".equals(tab) ? View.VISIBLE : View.GONE);
        sectionKonsistenz.setVisibility(
                "konsistenz".equals(tab) ? View.VISIBLE : View.GONE
        );
        sectionKoerper.setVisibility("koerper".equals(tab) ? View.VISIBLE : View.GONE);

        int active = ContextCompat.getColor(this, R.color.training_gold_highlight);
        int inactive = ContextCompat.getColor(this, R.color.text_tertiary);
        updateTab(tabKraft, "kraft".equals(tab), active, inactive);
        updateTab(tabMuskeln, "muskeln".equals(tab), active, inactive);
        updateTab(tabKonsistenz, "konsistenz".equals(tab), active, inactive);
        updateTab(tabKoerper, "koerper".equals(tab), active, inactive);
        loadCurrentSection();
    }

    private void updateTab(
            LinearLayout tab,
            boolean selected,
            int activeColor,
            int inactiveColor) {
        ((TextView) tab.getChildAt(0)).setTextColor(
                selected ? activeColor : inactiveColor
        );
        tab.getChildAt(1).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadCurrentSection() {
        if ("kraft".equals(currentTab)) {
            loadKraftData();
        } else if ("muskeln".equals(currentTab)) {
            loadMuscleData();
        } else if ("konsistenz".equals(currentTab)) {
            loadKonsistenzData();
        } else {
            loadBodyData();
        }
        updatePersonalizedAssessment();
    }

    private void loadKraftData() {
        ExerciseOption option = getSelectedExercise(spinnerExercise);
        if (option == null) {
            clearStrengthCharts();
            return;
        }

        List<WorkoutRecord> allRecords = getWorkoutRecords(
                option.workoutType,
                option.exercise
        );
        LocalDate today = LocalDate.now();
        LocalDate start = getPeriodStart(extractDates(allRecords), today);
        TreeMap<LocalDate, DayMetric> allDays = aggregateStrength(
                allRecords,
                null,
                today
        );
        TreeMap<LocalDate, DayMetric> selectedDays = aggregateStrength(
                allRecords,
                start,
                today
        );

        ProgressCalculator.Period comparisonCurrent =
                getComparisonCurrentPeriod(start, today);
        ProgressCalculator.Period previous = ProgressCalculator.previousPeriod(
                comparisonCurrent.start,
                comparisonCurrent.end
        );
        StrengthSummary selectedSummary = summarizeStrength(selectedDays);
        StrengthSummary comparisonSummary = summarizeStrength(aggregateStrength(
                allRecords,
                comparisonCurrent.start,
                comparisonCurrent.end
        ));
        StrengthSummary previousSummary = summarizeStrength(aggregateStrength(
                allRecords,
                previous.start,
                previous.end
        ));

        Set<LocalDate> oneRmPrDays = detectPrDays(allDays, MetricType.ONE_RM);
        Set<LocalDate> weightPrDays = detectPrDays(allDays, MetricType.MAX_WEIGHT);
        Set<LocalDate> volumePrDays = detectPrDays(allDays, MetricType.VOLUME);
        Set<LocalDate> repsPrDays = detectPrDays(allDays, MetricType.AVG_REPS);
        Set<LocalDate> selectedPrDays = new LinkedHashSet<>(oneRmPrDays);
        selectedPrDays.addAll(weightPrDays);
        selectedPrDays.retainAll(selectedDays.keySet());

        setKpi(
                kpiBest1Rm,
                "BESTES 1RM",
                formatWeight(selectedSummary.bestOneRm),
                comparisonTrend(
                        comparisonSummary.bestOneRm,
                        previousSummary.bestOneRm
                )
        );
        setKpi(
                kpiRelativeStrength,
                "SCHWERSTES GEWICHT",
                formatWeight(selectedSummary.maxWeight),
                comparisonTrend(
                        comparisonSummary.maxWeight,
                        previousSummary.maxWeight
                )
        );
        setKpi(
                kpiStrengthVolume,
                "GESAMTVOLUMEN",
                formatVolume(selectedSummary.totalVolume),
                comparisonTrend(
                        comparisonSummary.totalVolume,
                        previousSummary.totalVolume
                )
        );
        setKpi(
                kpiPrCount,
                "ECHTE PRs",
                String.valueOf(selectedPrDays.size()),
                "Allzeit-Rekorde im Zeitraum"
        );
        tvKraftComparisonPeriod.setText(getComparisonLabel());

        List<String> labels = new ArrayList<>();
        List<Entry> oneRmEntries = new ArrayList<>();
        List<Entry> maxWeightEntries = new ArrayList<>();
        List<Entry> volumeEntries = new ArrayList<>();
        List<Entry> repsEntries = new ArrayList<>();
        List<Entry> oneRmPrEntries = new ArrayList<>();
        List<Entry> weightPrEntries = new ArrayList<>();
        List<Entry> volumePrEntries = new ArrayList<>();
        List<Entry> repsPrEntries = new ArrayList<>();

        int index = 0;
        for (Map.Entry<LocalDate, DayMetric> entry : selectedDays.entrySet()) {
            LocalDate date = entry.getKey();
            DayMetric metric = entry.getValue();
            labels.add(date.format(CHART_DATE));
            addChartEntry(oneRmEntries, oneRmPrEntries, index,
                    metric.bestOneRm * weightFactor, oneRmPrDays.contains(date));
            addChartEntry(maxWeightEntries, weightPrEntries, index,
                    metric.maxWeight * weightFactor, weightPrDays.contains(date));
            addChartEntry(volumeEntries, volumePrEntries, index,
                    metric.volume * weightFactor, volumePrDays.contains(date));
            addChartEntry(repsEntries, repsPrEntries, index,
                    metric.averageReps(), repsPrDays.contains(date));
            index++;
        }

        buildLineChart(
                chart1RM,
                oneRmEntries,
                oneRmPrEntries,
                labels,
                weightUnit
        );
        double exerciseGoalKg = profile.getStrengthGoalKg(
                option.workoutType,
                option.exercise
        );
        addGoalLine(
                chart1RM,
                exerciseGoalKg > 0
                        ? (float) (exerciseGoalKg * weightFactor)
                        : null
        );
        buildLineChart(
                chartMaxGewicht,
                maxWeightEntries,
                weightPrEntries,
                labels,
                weightUnit
        );
        buildLineChart(
                chartVolumen,
                volumeEntries,
                volumePrEntries,
                labels,
                weightUnit
        );
        buildLineChart(
                chartWiederholungen,
                repsEntries,
                repsPrEntries,
                labels,
                "Wdh."
        );
        if (ProfileRepository.GOAL_STRENGTH.equals(profile.goalId)) {
            updateStrengthAssessment(
                    option,
                    summarizeStrength(allDays).bestOneRm,
                    exerciseGoalKg
            );
        }
    }

    private void loadMuscleData() {
        List<WorkoutRecord> records = getWorkoutRecords(null, null);
        LocalDate today = LocalDate.now();
        LocalDate start = getPeriodStart(extractDates(records), today);
        ProgressCalculator.Period comparisonCurrent =
                getComparisonCurrentPeriod(start, today);
        ProgressCalculator.Period previous = ProgressCalculator.previousPeriod(
                comparisonCurrent.start,
                comparisonCurrent.end
        );

        MuscleSummary selected = summarizeMuscles(records, start, today);
        MuscleSummary comparison = summarizeMuscles(
                records,
                comparisonCurrent.start,
                comparisonCurrent.end
        );
        MuscleSummary previousSummary = summarizeMuscles(
                records,
                previous.start,
                previous.end
        );

        setKpi(
                kpiMuscleVolume,
                "GESAMTVOLUMEN",
                formatVolume(selected.totalVolume),
                comparisonTrend(
                        comparison.totalVolume,
                        previousSummary.totalVolume
                )
        );

        double pushPullTotal = selected.pushVolume + selected.pullVolume;
        double pushPercent = pushPullTotal > 0
                ? selected.pushVolume * 100.0 / pushPullTotal
                : 0;
        double pullPercent = pushPullTotal > 0 ? 100.0 - pushPercent : 0;
        setKpi(
                kpiMuscleBalance,
                "PUSH / PULL",
                String.format(
                        Locale.GERMANY,
                        "%.0f / %.0f",
                        pushPercent,
                        pullPercent
                ),
                "Anteil am Oberkörpervolumen"
        );
        String muscleComparison = getComparisonLabel();
        if (profile.weeklyVolumeGoalKg > 0) {
            LocalDate weekStart = LocalDate.now().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            double currentWeekVolume = calculateVolume(
                    records,
                    weekStart,
                    LocalDate.now()
            );
            int volumeProgress = Math.max(
                    0,
                    Math.min(
                            100,
                            Math.round((float) (
                                    currentWeekVolume * 100.0
                                            / profile.weeklyVolumeGoalKg
                            ))
                    )
            );
            muscleComparison += "\nWochenvolumen: "
                    + formatVolume(currentWeekVolume)
                    + " / "
                    + formatVolume(profile.weeklyVolumeGoalKg)
                    + " · "
                    + volumeProgress
                    + "%";
        }
        tvMuscleComparison.setText(muscleComparison);

        List<BarEntry> muscleEntries = new ArrayList<>();
        List<String> muscleLabels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : selected.muscleVolume.entrySet()) {
            muscleEntries.add(new BarEntry(
                    index++,
                    (float) (entry.getValue() * weightFactor)
            ));
            muscleLabels.add(entry.getKey());
        }
        buildBarChart(
                chartMuskelVolumen,
                muscleEntries,
                muscleLabels,
                weightUnit,
                null
        );
        buildPieChart(
                chartPushPull,
                new String[]{"Push", "Pull"},
                new float[]{
                        (float) selected.pushVolume,
                        (float) selected.pullVolume
                },
                new int[]{R.color.training_gold, R.color.card_background_light}
        );
        buildPieChart(
                chartOberUnter,
                new String[]{"Oberkörper", "Unterkörper"},
                new float[]{
                        (float) selected.upperVolume,
                        (float) selected.lowerVolume
                },
                new int[]{R.color.training_gold_highlight, R.color.training_gold_pressed}
        );
        updateDysbalance(selected);
        updateMuscleMappingText();
    }

    private void loadKonsistenzData() {
        Set<LocalDate> activeDays = getAllActiveDays();
        LocalDate today = LocalDate.now();
        LocalDate start = getPeriodStart(activeDays, today);
        List<LocalDate> filtered = ProgressCalculator.filterDates(
                activeDays,
                start,
                today
        );
        Set<LocalDate> filteredSet = new LinkedHashSet<>(filtered);
        ProgressCalculator.StreakStats streaks =
                ProgressCalculator.calculateStreaks(
                        activeDays,
                        start,
                        today,
                        today
                );
        List<ProgressCalculator.WeekSummary> weeks =
                ProgressCalculator.buildWeeklySummaries(
                        activeDays,
                        start,
                        today
                );

        int goal = getWeeklyGoal();
        double average = ProgressCalculator.weeklyAverage(
                filtered.size(),
                start,
                today
        );
        int achievement = ProgressCalculator.goalAchievement(average, goal);

        ProgressCalculator.Period comparisonCurrent =
                getComparisonCurrentPeriod(start, today);
        ProgressCalculator.Period previous = ProgressCalculator.previousPeriod(
                comparisonCurrent.start,
                comparisonCurrent.end
        );
        int currentComparisonCount = ProgressCalculator.filterDates(
                activeDays,
                comparisonCurrent.start,
                comparisonCurrent.end
        ).size();
        int previousComparisonCount = ProgressCalculator.filterDates(
                activeDays,
                previous.start,
                previous.end
        ).size();
        double currentAverage = ProgressCalculator.weeklyAverage(
                currentComparisonCount,
                comparisonCurrent.start,
                comparisonCurrent.end
        );
        double previousAverage = ProgressCalculator.weeklyAverage(
                previousComparisonCount,
                previous.start,
                previous.end
        );

        setKpi(
                kpiCurrentStreak,
                "AKTUELLE SERIE",
                streaks.currentStreak + " Tage",
                "Heute oder gestern fortgeführt"
        );
        setKpi(
                kpiBestStreak,
                "BESTE SERIE",
                streaks.bestStreak + " Tage",
                formatStreakPeriod(streaks)
        );
        setKpi(
                kpiWeeklyAverage,
                "Ø PRO WOCHE",
                String.format(Locale.GERMANY, "%.1fx", average),
                comparisonTrend(currentAverage, previousAverage)
        );
        setKpi(
                kpiGoalAchievement,
                "ZIELERREICHUNG",
                achievement + "%",
                goal + " Trainingstage pro Woche"
        );
        tvConsistencyComparison.setText(getComparisonLabel());

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (ProgressCalculator.WeekSummary week : weeks) {
            entries.add(new BarEntry(index++, week.sessionDays));
            labels.add(week.label);
        }
        buildBarChart(
                chartWeeklySessions,
                entries,
                labels,
                "x",
                (float) goal
        );
        buildDonutChart(chartGoalAchievement, achievement);
        updateGoalStatus(achievement, goal, activeDays);
    }

    private void loadBodyData() {
        List<ProfileRepository.WeightEntry> allEntries =
                profileRepository.getWeightHistory();
        LocalDate today = LocalDate.now();
        Set<LocalDate> availableDates = new LinkedHashSet<>();
        for (ProfileRepository.WeightEntry entry : allEntries) {
            availableDates.add(entry.date);
        }
        LocalDate start = getPeriodStart(availableDates, today);
        List<ProfileRepository.WeightEntry> selected = new ArrayList<>();
        for (ProfileRepository.WeightEntry entry : allEntries) {
            if (isWithin(entry.date, start, today)) {
                selected.add(entry);
            }
        }

        double currentWeight = profile.currentWeightKg;
        if (currentWeight <= 0 && !allEntries.isEmpty()) {
            currentWeight = allEntries.get(allEntries.size() - 1).weightKg;
        }
        double startWeight = selected.isEmpty()
                ? currentWeight
                : selected.get(0).weightKg;
        double change = currentWeight > 0 && startWeight > 0
                ? currentWeight - startWeight
                : 0;

        setKpi(
                kpiCurrentWeight,
                "AKTUELLES GEWICHT",
                currentWeight > 0 ? formatWeight(currentWeight) : "–",
                allEntries.size() + " Messpunkte"
        );
        setKpi(
                kpiWeightChange,
                "VERÄNDERUNG",
                currentWeight > 0
                        ? String.format(
                                Locale.GERMANY,
                                "%+.1f %s",
                                change * weightFactor,
                                weightUnit
                        )
                        : "–",
                selected.isEmpty()
                        ? "Noch kein Verlauf"
                        : "Seit " + selected.get(0).date.format(DISPLAY_DATE)
        );
        setKpi(
                kpiTargetWeight,
                "ZIELGEWICHT",
                profile.targetWeightKg > 0
                        ? formatWeight(profile.targetWeightKg)
                        : "–",
                profile.targetDate == null
                        ? "Kein Zieltermin"
                        : "Bis " + profile.targetDate.format(DISPLAY_DATE)
        );

        int cardioMinutes = calculateCardioMinutes(start, today);
        setKpi(
                kpiBodyCardio,
                "CARDIO",
                cardioMinutes + " min",
                getComparisonLabel()
        );

        updateWeightGoalStatus(currentWeight);

        List<Entry> chartEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            ProfileRepository.WeightEntry entry = selected.get(i);
            chartEntries.add(new Entry(
                    i,
                    (float) (entry.weightKg * weightFactor)
            ));
            labels.add(entry.date.format(CHART_DATE));
        }
        buildLineChart(
                chartBodyWeight,
                chartEntries,
                Collections.emptyList(),
                labels,
                weightUnit
        );
        addGoalLine(
                chartBodyWeight,
                profile.targetWeightKg > 0
                        ? (float) (profile.targetWeightKg * weightFactor)
                        : null
        );
    }

    private TreeMap<LocalDate, DayMetric> aggregateStrength(
            List<WorkoutRecord> records,
            LocalDate start,
            LocalDate end) {
        TreeMap<LocalDate, DayMetric> result = new TreeMap<>();
        for (WorkoutRecord record : records) {
            if (!isWithin(record.date, start, end)) {
                continue;
            }
            DayMetric day = result.computeIfAbsent(
                    record.date,
                    ignored -> new DayMetric()
            );
            for (WorkoutStorage.WorkoutSet set : record.sets) {
                double estimatedOneRm = set.weight * (1.0 + set.reps / 30.0);
                day.bestOneRm = Math.max(day.bestOneRm, estimatedOneRm);
                day.maxWeight = Math.max(day.maxWeight, set.weight);
                day.volume += set.weight * set.reps;
                day.totalReps += set.reps;
                day.setCount++;
            }
        }
        return result;
    }

    private StrengthSummary summarizeStrength(
            TreeMap<LocalDate, DayMetric> days) {
        StrengthSummary result = new StrengthSummary();
        for (DayMetric day : days.values()) {
            result.bestOneRm = Math.max(result.bestOneRm, day.bestOneRm);
            result.maxWeight = Math.max(result.maxWeight, day.maxWeight);
            result.totalVolume += day.volume;
            result.totalReps += day.totalReps;
            result.totalSets += day.setCount;
        }
        return result;
    }

    private double calculateVolume(
            List<WorkoutRecord> records,
            LocalDate start,
            LocalDate end) {
        double volume = 0;
        for (WorkoutRecord record : records) {
            if (!isWithin(record.date, start, end)) {
                continue;
            }
            for (WorkoutStorage.WorkoutSet set : record.sets) {
                volume += set.weight * set.reps;
            }
        }
        return volume;
    }

    private MuscleSummary summarizeMuscles(
            List<WorkoutRecord> records,
            LocalDate start,
            LocalDate end) {
        MuscleSummary result = new MuscleSummary();
        for (WorkoutRecord record : records) {
            if (!isWithin(record.date, start, end)) {
                continue;
            }
            double volume = 0;
            for (WorkoutStorage.WorkoutSet set : record.sets) {
                volume += set.weight * set.reps;
            }
            result.totalVolume += volume;

            if (WorkoutStorage.TYPE_PUSH.equals(record.workoutType)) {
                result.pushVolume += volume;
                result.upperVolume += volume;
            } else if (WorkoutStorage.TYPE_PULL.equals(record.workoutType)) {
                result.pullVolume += volume;
                result.upperVolume += volume;
            } else if (WorkoutStorage.TYPE_LEG.equals(record.workoutType)) {
                result.lowerVolume += volume;
            }

            List<String> groups = getMuscleGroups(
                    record.workoutType,
                    record.exercise
            );
            if (groups.isEmpty()) {
                groups = Collections.singletonList("Nicht zugeordnet");
            }
            double allocatedVolume = volume / groups.size();
            for (String group : groups) {
                result.muscleVolume.put(
                        group,
                        result.muscleVolume.getOrDefault(group, 0.0)
                                + allocatedVolume
                );
            }
        }
        return result;
    }

    private Set<LocalDate> detectPrDays(
            TreeMap<LocalDate, DayMetric> days,
            MetricType metricType) {
        Set<LocalDate> result = new LinkedHashSet<>();
        double record = Double.NEGATIVE_INFINITY;
        for (Map.Entry<LocalDate, DayMetric> entry : days.entrySet()) {
            double value = getMetricValue(entry.getValue(), metricType);
            if (value > 0 && value > record) {
                result.add(entry.getKey());
                record = value;
            }
        }
        return result;
    }

    private double getMetricValue(DayMetric metric, MetricType type) {
        switch (type) {
            case ONE_RM:
                return metric.bestOneRm;
            case MAX_WEIGHT:
                return metric.maxWeight;
            case VOLUME:
                return metric.volume;
            case AVG_REPS:
                return metric.averageReps();
            default:
                return 0;
        }
    }

    private void addChartEntry(
            List<Entry> entries,
            List<Entry> prEntries,
            int index,
            double value,
            boolean isPr) {
        Entry entry = new Entry(index, (float) value);
        entries.add(entry);
        if (isPr) {
            prEntries.add(new Entry(index, (float) value));
        }
    }

    private void updateDysbalance(MuscleSummary summary) {
        List<String> messages = new ArrayList<>();
        double pushPull = summary.pushVolume + summary.pullVolume;
        if (pushPull > 0) {
            double push = summary.pushVolume * 100.0 / pushPull;
            double pull = 100.0 - push;
            if (push > 65) {
                messages.add(String.format(
                        Locale.GERMANY,
                        "Push überwiegt Pull um %.0f Prozentpunkte.",
                        push - pull
                ));
            } else if (pull > 65) {
                messages.add(String.format(
                        Locale.GERMANY,
                        "Pull überwiegt Push um %.0f Prozentpunkte.",
                        pull - push
                ));
            }
        }

        double body = summary.upperVolume + summary.lowerVolume;
        if (body > 0) {
            double upper = summary.upperVolume * 100.0 / body;
            double lower = 100.0 - upper;
            if (upper > 65) {
                messages.add(String.format(
                        Locale.GERMANY,
                        "Oberkörper überwiegt Unterkörper um %.0f Prozentpunkte.",
                        upper - lower
                ));
            } else if (lower > 65) {
                messages.add(String.format(
                        Locale.GERMANY,
                        "Unterkörper überwiegt Oberkörper um %.0f Prozentpunkte.",
                        lower - upper
                ));
            }
        }

        if (messages.isEmpty()) {
            tvDysbalance.setVisibility(View.GONE);
        } else {
            tvDysbalance.setText(String.join("\n", messages));
            tvDysbalance.setVisibility(View.VISIBLE);
        }
    }

    private void showMuscleMappingDialog() {
        ExerciseOption option = getSelectedExercise(spinnerMuscleExercise);
        if (option == null) {
            return;
        }
        List<String> current = getMuscleGroups(
                option.workoutType,
                option.exercise
        );
        boolean[] checked = new boolean[MUSCLE_GROUPS.length];
        for (int i = 0; i < MUSCLE_GROUPS.length; i++) {
            checked[i] = current.contains(MUSCLE_GROUPS[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle(option.exercise)
                .setMultiChoiceItems(
                        MUSCLE_GROUPS,
                        checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked
                )
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < MUSCLE_GROUPS.length; i++) {
                        if (checked[i]) {
                            selected.add(MUSCLE_GROUPS[i]);
                        }
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Mindestens eine Muskelgruppe auswählen",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                    musclePreferences.edit()
                            .putString(
                                    getMusclePreferenceKey(option),
                                    String.join("|", selected)
                            )
                            .apply();
                    updateMuscleMappingText();
                    loadMuscleData();
                })
                .show();
    }

    private void updateMuscleMappingText() {
        ExerciseOption option = getSelectedExercise(spinnerMuscleExercise);
        if (option == null || tvMuscleMapping == null) {
            return;
        }
        List<String> groups = getMuscleGroups(
                option.workoutType,
                option.exercise
        );
        tvMuscleMapping.setText(
                groups.isEmpty()
                        ? "Noch keine Muskelgruppe zugeordnet"
                        : String.join(" · ", groups)
        );
    }

    private List<String> getMuscleGroups(String workoutType, String exercise) {
        ExerciseOption option = new ExerciseOption(exercise, workoutType);
        String stored = musclePreferences.getString(
                getMusclePreferenceKey(option),
                null
        );
        if (stored != null) {
            List<String> result = new ArrayList<>();
            for (String item : stored.split("\\|")) {
                if (!item.trim().isEmpty()) {
                    result.add(item.trim());
                }
            }
            return result;
        }
        List<String> defaults = DEFAULT_MUSCLE_MAP.get(exercise);
        return defaults == null
                ? Collections.emptyList()
                : new ArrayList<>(defaults);
    }

    private String getMusclePreferenceKey(ExerciseOption option) {
        return option.workoutType + "|" + option.exercise;
    }

    private void buildLineChart(
            LineChart chart,
            List<Entry> entries,
            List<Entry> prEntries,
            List<String> labels,
            String unit) {
        int gold = ContextCompat.getColor(this, R.color.training_gold);
        int highlight = ContextCompat.getColor(
                this,
                R.color.training_gold_highlight
        );
        int background = ContextCompat.getColor(this, R.color.primary);

        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(true);

        if (entries.isEmpty()) {
            setNoData(chart);
            return;
        }

        LineDataSet values = new LineDataSet(entries, "Werte");
        values.setColor(gold);
        values.setCircleColor(gold);
        values.setCircleHoleColor(background);
        values.setLineWidth(2f);
        values.setCircleRadius(3f);
        values.setDrawValues(false);
        values.setMode(LineDataSet.Mode.LINEAR);
        values.setDrawFilled(true);
        values.setFillColor(gold);
        values.setFillAlpha(24);
        values.setHighLightColor(highlight);

        LineData data = new LineData(values);
        if (!prEntries.isEmpty()) {
            LineDataSet prs = new LineDataSet(prEntries, "PR");
            prs.setColor(Color.TRANSPARENT);
            prs.setCircleColor(highlight);
            prs.setCircleHoleColor(background);
            prs.setCircleRadius(6f);
            prs.setCircleHoleRadius(2.5f);
            prs.setLineWidth(0f);
            prs.setDrawValues(false);
            prs.setDrawFilled(false);
            prs.setHighLightColor(highlight);
            data.addDataSet(prs);
        }
        chart.setData(data);
        styleXAxis(chart.getXAxis(), labels);
        styleYAxis(chart.getAxisLeft(), unit);
        chart.getAxisRight().setEnabled(false);
        Set<Integer> prIndexes = new LinkedHashSet<>();
        for (Entry prEntry : prEntries) {
            prIndexes.add(Math.round(prEntry.getX()));
        }
        chart.setOnChartValueSelectedListener(
                createIndexedChartListener(labels, unit, prIndexes)
        );
        if (AppSettings.animationsEnabled(this)) {
            chart.animateX(450);
        }
        chart.invalidate();
    }

    private void addGoalLine(LineChart chart, Float target) {
        chart.getAxisLeft().removeAllLimitLines();
        if (target == null || target <= 0 || chart.getData() == null) {
            chart.invalidate();
            return;
        }
        LimitLine goalLine = new LimitLine(target, "Ziel");
        goalLine.setLineColor(ContextCompat.getColor(
                this,
                R.color.training_gold_highlight
        ));
        goalLine.setTextColor(ContextCompat.getColor(
                this,
                R.color.text_secondary
        ));
        goalLine.setLineWidth(1.2f);
        chart.getAxisLeft().addLimitLine(goalLine);
        chart.invalidate();
    }

    private void buildBarChart(
            BarChart chart,
            List<BarEntry> entries,
            List<String> labels,
            String unit,
            Float target) {
        int gold = ContextCompat.getColor(this, R.color.training_gold);
        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        if (entries.isEmpty()) {
            setNoData(chart);
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(gold);
        dataSet.setDrawValues(false);
        chart.setData(new BarData(dataSet));
        chart.getBarData().setBarWidth(0.58f);
        styleXAxis(chart.getXAxis(), labels);
        styleYAxis(chart.getAxisLeft(), unit);
        chart.getAxisLeft().removeAllLimitLines();
        if (target != null) {
            LimitLine goalLine = new LimitLine(target, "Ziel");
            goalLine.setLineColor(ContextCompat.getColor(
                    this,
                    R.color.training_gold_highlight
            ));
            goalLine.setTextColor(ContextCompat.getColor(
                    this,
                    R.color.text_secondary
            ));
            goalLine.setLineWidth(1.2f);
            chart.getAxisLeft().addLimitLine(goalLine);
        }
        chart.getAxisRight().setEnabled(false);
        chart.setOnChartValueSelectedListener(
                createIndexedChartListener(
                        labels,
                        unit,
                        Collections.emptySet()
                )
        );
        if (AppSettings.animationsEnabled(this)) {
            chart.animateY(450);
        }
        chart.invalidate();
    }

    private void buildPieChart(
            PieChart chart,
            String[] labels,
            float[] values,
            int[] colorResources) {
        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(48f);
        chart.setHoleColor(ContextCompat.getColor(this, R.color.primary));
        chart.setUsePercentValues(true);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.text_primary));
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        float total = 0;
        for (int i = 0; i < labels.length; i++) {
            total += values[i];
            if (values[i] > 0) {
                entries.add(new PieEntry(values[i], labels[i]));
                colors.add(ContextCompat.getColor(
                        this,
                        colorResources[Math.min(i, colorResources.length - 1)]
                ));
            }
        }
        if (entries.isEmpty()) {
            setNoData(chart);
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
                return String.format(Locale.GERMANY, "%.0f%%", value);
            }
        });
        Legend legend = chart.getLegend();
        legend.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        legend.setTextSize(11f);
        chart.setData(new PieData(dataSet));
        final float totalValue = total;
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                if (entry instanceof PieEntry) {
                    PieEntry pieEntry = (PieEntry) entry;
                    float percentage = totalValue > 0
                            ? pieEntry.getValue() * 100f / totalValue
                            : 0;
                    showChartDetail(String.format(
                            Locale.GERMANY,
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
            chart.animateY(500);
        }
        chart.invalidate();
    }

    private void buildDonutChart(PieChart chart, int achievement) {
        int capped = Math.max(0, Math.min(100, achievement));
        int color = achievement >= 100
                ? R.color.success
                : achievement >= 75
                ? R.color.training_gold
                : R.color.error;

        chart.setBackgroundResource(R.drawable.bg_home_metric);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(62f);
        chart.setHoleColor(ContextCompat.getColor(this, R.color.primary));
        chart.setDrawCenterText(true);
        chart.setCenterText(achievement + "%");
        chart.setCenterTextColor(ContextCompat.getColor(this, color));
        chart.setCenterTextSize(23f);
        chart.getLegend().setEnabled(false);
        chart.setEntryLabelColor(Color.TRANSPARENT);
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        List<PieEntry> entries = Arrays.asList(
                new PieEntry(capped, ""),
                new PieEntry(100 - capped, "")
        );
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                ContextCompat.getColor(this, color),
                ContextCompat.getColor(this, R.color.divider)
        );
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(1f);
        chart.setData(new PieData(dataSet));
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                showChartDetail("Zielerreichung: " + achievement + "%");
            }

            @Override
            public void onNothingSelected() {
            }
        });
        if (AppSettings.animationsEnabled(this)) {
            chart.animateY(500);
        }
        chart.invalidate();
    }

    private OnChartValueSelectedListener createIndexedChartListener(
            List<String> labels,
            String unit,
            Set<Integer> prIndexes) {
        return new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                int index = Math.round(entry.getX());
                String label = index >= 0 && index < labels.size()
                        ? labels.get(index)
                        : "";
                String suffix = unit == null || unit.isEmpty()
                        ? ""
                        : " " + unit;
                boolean isPr = prIndexes.contains(index);
                showChartDetail(String.format(
                        Locale.GERMANY,
                        "%s: %.1f%s%s",
                        label,
                        entry.getY(),
                        suffix,
                        isPr ? " · PR" : ""
                ));
            }

            @Override
            public void onNothingSelected() {
            }
        };
    }

    private void styleXAxis(XAxis axis, List<String> labels) {
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        axis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        axis.setDrawAxisLine(false);
        axis.setValueFormatter(new IndexAxisValueFormatter(labels));
        axis.setGranularity(1f);
        axis.setLabelCount(Math.min(labels.size(), 6), false);
    }

    private void styleYAxis(YAxis axis, String unit) {
        axis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        axis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        axis.setDrawAxisLine(false);
        axis.setAxisMinimum(0f);
        axis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String formatted = Math.abs(value - Math.round(value)) < 0.05f
                        ? String.valueOf(Math.round(value))
                        : String.format(Locale.GERMANY, "%.1f", value);
                return unit == null || unit.isEmpty()
                        ? formatted
                        : formatted + " " + unit;
            }
        });
    }

    private void setNoData(com.github.mikephil.charting.charts.Chart<?> chart) {
        chart.setNoDataText("Noch keine Daten");
        chart.setNoDataTextColor(ContextCompat.getColor(
                this,
                R.color.text_tertiary
        ));
        chart.clear();
        chart.invalidate();
    }

    private void clearStrengthCharts() {
        setNoData(chart1RM);
        setNoData(chartMaxGewicht);
        setNoData(chartVolumen);
        setNoData(chartWiederholungen);
    }

    private List<ExerciseOption> collectExerciseOptions() {
        Map<String, ExerciseOption> options = new LinkedHashMap<>();
        addCatalogExercises(
                options,
                WorkoutStorage.TYPE_PUSH,
                R.array.push_exercises
        );
        addCatalogExercises(
                options,
                WorkoutStorage.TYPE_PULL,
                R.array.pull_exercises
        );
        addCatalogExercises(
                options,
                WorkoutStorage.TYPE_LEG,
                R.array.leg_exercises
        );
        for (String type : WORKOUT_TYPES) {
            for (WorkoutStorage.DetailedWorkout workout
                    : WorkoutStorage.getDetailedWorkouts(this, type)) {
                addExerciseOption(options, workout.exercise, workout.workoutType);
            }
        }
        return new ArrayList<>(options.values());
    }

    private void addCatalogExercises(
            Map<String, ExerciseOption> options,
            String type,
            int defaultArray) {
        for (String exercise : ExerciseCatalog.getExercises(
                this,
                defaultArray,
                type
        )) {
            addExerciseOption(options, exercise, type);
        }
    }

    private void addExerciseOption(
            Map<String, ExerciseOption> options,
            String exercise,
            String type) {
        if (exercise == null || exercise.trim().isEmpty()) {
            return;
        }
        String safeType = isKnownType(type) ? type : "";
        String key = safeType + "|" + exercise.trim().toLowerCase(Locale.ROOT);
        options.putIfAbsent(key, new ExerciseOption(exercise.trim(), safeType));
    }

    private List<WorkoutRecord> getWorkoutRecords(
            String requiredType,
            String requiredExercise) {
        List<WorkoutRecord> records = new ArrayList<>();
        for (String type : WORKOUT_TYPES) {
            for (WorkoutStorage.DetailedWorkout workout
                    : WorkoutStorage.getDetailedWorkouts(this, type)) {
                String storedType = isKnownType(workout.workoutType)
                        ? workout.workoutType
                        : type;
                if (requiredType != null && !requiredType.equals(storedType)) {
                    continue;
                }
                if (requiredExercise != null
                        && !requiredExercise.equals(workout.exercise)) {
                    continue;
                }
                LocalDate date = parseDate(workout.timestamp);
                if (date != null && workout.sets != null) {
                    records.add(new WorkoutRecord(
                            date,
                            storedType,
                            workout.exercise,
                            workout.sets
                    ));
                }
            }
        }
        records.sort(Comparator.comparing(record -> record.date));
        return records;
    }

    private Set<LocalDate> getAllActiveDays() {
        Set<LocalDate> result = new LinkedHashSet<>();
        for (String type : WORKOUT_TYPES) {
            for (WorkoutStorage.DetailedWorkout workout
                    : WorkoutStorage.getDetailedWorkouts(this, type)) {
                LocalDate date = parseDate(workout.timestamp);
                if (date != null) {
                    result.add(date);
                }
            }
            for (WorkoutStorage.CardioSession session
                    : WorkoutStorage.getCardioSessions(this, type)) {
                LocalDate date = parseDate(session.timestamp);
                if (date != null) {
                    result.add(date);
                }
            }
        }
        return result;
    }

    private Set<LocalDate> extractDates(List<WorkoutRecord> records) {
        Set<LocalDate> result = new LinkedHashSet<>();
        for (WorkoutRecord record : records) {
            result.add(record.date);
        }
        return result;
    }

    private LocalDate getPeriodStart(
            Set<LocalDate> availableDates,
            LocalDate end) {
        Integer days = getSelectedDays();
        if (days != null) {
            return end.minusDays(days - 1L);
        }
        return availableDates.stream()
                .min(LocalDate::compareTo)
                .orElse(end);
    }

    private Integer getSelectedDays() {
        int position = spinnerRange.getSelectedItemPosition();
        return position >= 0 && position < RANGE_DAYS.length
                ? RANGE_DAYS[position]
                : null;
    }

    private ProgressCalculator.Period getComparisonCurrentPeriod(
            LocalDate selectedStart,
            LocalDate end) {
        if (getSelectedDays() == null) {
            return new ProgressCalculator.Period(end.minusDays(29), end);
        }
        return new ProgressCalculator.Period(selectedStart, end);
    }

    private String getComparisonLabel() {
        return getSelectedDays() == null
                ? "Vergleich: letzte 30 Tage gegenüber den 30 Tagen davor"
                : "Vergleich: gewählter Zeitraum gegenüber dem Vorzeitraum";
    }

    private ExerciseOption getSelectedExercise(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected instanceof ExerciseOption
                ? (ExerciseOption) selected
                : null;
    }

    private LocalDate parseDate(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestamp, STORAGE_TIMESTAMP).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private boolean isWithin(
            LocalDate date,
            LocalDate start,
            LocalDate end) {
        return (start == null || !date.isBefore(start))
                && (end == null || !date.isAfter(end));
    }

    private boolean isKnownType(String type) {
        return WorkoutStorage.TYPE_PUSH.equals(type)
                || WorkoutStorage.TYPE_PULL.equals(type)
                || WorkoutStorage.TYPE_LEG.equals(type);
    }

    private int getWeeklyGoal() {
        return Math.max(1, Math.min(7, profile.weeklyTrainingGoal));
    }

    private void updateGoalSummary() {
        TextView summary = findViewById(R.id.tvProgressGoalSummary);
        summary.setText(String.format(
                Locale.GERMANY,
                "%s · %d Trainingstage pro Woche",
                getGoalLabel(profile.goalId),
                getWeeklyGoal()
        ));
        updateFocusHint();
    }

    private void updateFocusHint() {
        if (tvProgressFocusHint == null || profile == null) {
            return;
        }
        int textRes;
        if (ProfileRepository.GOAL_STRENGTH.equals(profile.goalId)) {
            textRes = R.string.progress_focus_strength;
        } else if (ProfileRepository.GOAL_WEIGHT_LOSS.equals(profile.goalId)) {
            textRes = R.string.progress_focus_weight_loss;
        } else if (ProfileRepository.GOAL_FITNESS.equals(profile.goalId)) {
            textRes = R.string.progress_focus_fitness;
        } else {
            textRes = R.string.progress_focus_muscle;
        }
        ProfileInsights.SessionRecommendation recommendation =
                ProfileInsights.recommendSessions(
                        profile.activityLevelId,
                        profile.experienceId,
                        getWeeklyGoal()
                );
        String recommendationStatus;
        if (recommendation.difference < 0) {
            recommendationStatus = "Dein Wochenziel liegt darunter.";
        } else if (recommendation.difference > 0) {
            recommendationStatus =
                    "Dein Wochenziel liegt darüber; plane zusätzliche Regeneration ein.";
        } else {
            recommendationStatus = "Dein Wochenziel liegt im empfohlenen Bereich.";
        }
        tvProgressFocusHint.setText(
                getString(textRes)
                        + "\n"
                        + getExperienceLabel(profile.experienceId)
                        + " · "
                        + getActivityLabel(profile.activityLevelId)
                        + "\nEmpfehlung aus Trainingsstand und Alltagsaktivität: "
                        + formatSessionRange(recommendation.min, recommendation.max)
                        + ". "
                        + recommendationStatus
        );
    }

    private String formatSessionRange(int min, int max) {
        if (min == max) {
            return min + (min == 1
                    ? " Trainingstag pro Woche"
                    : " Trainingstage pro Woche");
        }
        return min + "–" + max + " Trainingstage pro Woche";
    }

    private void updatePersonalizedAssessment() {
        if (tvPersonalizedAssessment == null || profile == null) {
            return;
        }
        if (ProfileRepository.GOAL_STRENGTH.equals(profile.goalId)) {
            ExerciseOption option = getSelectedExercise(spinnerExercise);
            if (option == null) {
                tvPersonalizedAssessment.setText(
                        "Kraftbewertung: Wähle eine Übung aus."
                );
                return;
            }
            double bestOneRm = summarizeStrength(aggregateStrength(
                    getWorkoutRecords(option.workoutType, option.exercise),
                    null,
                    LocalDate.now()
            )).bestOneRm;
            updateStrengthAssessment(
                    option,
                    bestOneRm,
                    profile.getStrengthGoalKg(option.workoutType, option.exercise)
            );
        } else if (ProfileRepository.GOAL_WEIGHT_LOSS.equals(profile.goalId)) {
            updateWeightLossAssessment();
        } else if (ProfileRepository.GOAL_FITNESS.equals(profile.goalId)) {
            updateFitnessAssessment();
        } else {
            updateMuscleAssessment();
        }
    }

    private void updateStrengthAssessment(
            ExerciseOption option,
            double bestOneRm,
            double targetOneRm) {
        if (targetOneRm <= 0) {
            tvPersonalizedAssessment.setText(
                    "Kraftbewertung · "
                            + option.exercise
                            + ": Noch kein übungsbezogenes 1RM-Ziel festgelegt."
            );
            return;
        }
        if (bestOneRm <= 0) {
            tvPersonalizedAssessment.setText(
                    "Kraftbewertung · "
                            + option.exercise
                            + ": Ziel "
                            + formatWeight(targetOneRm)
                            + ". Noch keine auswertbaren Sätze vorhanden."
            );
            return;
        }

        int progress = ProfileInsights.progressPercent(bestOneRm, targetOneRm);
        StringBuilder text = new StringBuilder()
                .append("Kraftbewertung · ")
                .append(option.exercise)
                .append(": ")
                .append(progress)
                .append("% des 1RM-Ziels · ")
                .append(formatWeight(bestOneRm))
                .append(" von ")
                .append(formatWeight(targetOneRm));
        if (bestOneRm >= targetOneRm) {
            text.append("\nZiel erreicht. Lege bei Bedarf ein neues Ziel fest.");
        } else if (hasFutureTargetDate()) {
            double weeksRemaining = targetWeeksRemaining();
            double requiredPerWeek = (targetOneRm - bestOneRm) / weeksRemaining;
            text.append(String.format(
                    Locale.GERMANY,
                    "\nBis %s sind durchschnittlich +%.2f %s 1RM pro Woche erforderlich.",
                    profile.targetDate.format(DISPLAY_DATE),
                    requiredPerWeek * weightFactor,
                    weightUnit
            ));
        } else {
            text.append("\nMit einem Zieltermin wird die nötige Steigerung pro Woche berechnet.");
        }
        tvPersonalizedAssessment.setText(text);
    }

    private void updateMuscleAssessment() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate previousWeekStart = weekStart.minusWeeks(1);
        LocalDate previousWeekEnd = weekStart.minusDays(1);
        List<WorkoutRecord> records = getWorkoutRecords(null, null);
        double currentVolume = calculateVolume(records, weekStart, today);
        double previousVolume = calculateVolume(
                records,
                previousWeekStart,
                previousWeekEnd
        );
        int activeDays = ProgressCalculator.filterDates(
                getAllActiveDays(),
                weekStart,
                today
        ).size();

        StringBuilder text = new StringBuilder("Muskelaufbau-Bewertung: ")
                .append(activeDays)
                .append("/")
                .append(getWeeklyGoal())
                .append(" Trainingstage");
        if (profile.weeklyVolumeGoalKg > 0) {
            int progress = ProfileInsights.progressPercent(
                    currentVolume,
                    profile.weeklyVolumeGoalKg
            );
            text.append(" · ")
                    .append(progress)
                    .append("% des Wochenvolumens (")
                    .append(formatVolume(currentVolume))
                    .append(" von ")
                    .append(formatVolume(profile.weeklyVolumeGoalKg))
                    .append(")");
        } else {
            text.append("\nLege ein Wochenvolumen-Ziel fest, um progressive Steigerung zu bewerten.");
        }
        if (previousVolume > 0) {
            text.append("\nVolumentrend zur Vorwoche: ")
                    .append(StatisticsCalculator.formatChange(
                            currentVolume,
                            previousVolume
                    ));
        }
        if (profile.weeklyVolumeGoalKg > 0 && hasFutureTargetDate()) {
            double fourWeekAverage = calculateVolume(
                    records,
                    today.minusDays(27),
                    today
            ) / 4.0;
            double requiredWeeklyDevelopment =
                    (profile.weeklyVolumeGoalKg - fourWeekAverage)
                            / targetWeeksRemaining();
            if (requiredWeeklyDevelopment > 0) {
                text.append(String.format(
                        Locale.GERMANY,
                        "\nBis %s sollte dein Wochenvolumen im Mittel um %.0f %s je Woche steigen.",
                        profile.targetDate.format(DISPLAY_DATE),
                        requiredWeeklyDevelopment * weightFactor,
                        weightUnit
                ));
            }
        }
        tvPersonalizedAssessment.setText(text);
    }

    private void updateWeightLossAssessment() {
        List<ProfileRepository.WeightEntry> entries =
                profileRepository.getWeightHistory();
        double currentWeight = profile.currentWeightKg;
        ProfileInsights.WeightProjection projection =
                buildWeightProjection(entries, currentWeight);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        int activeDays = ProgressCalculator.filterDates(
                getAllActiveDays(),
                weekStart,
                today
        ).size();
        int cardioMinutes = calculateCardioMinutes(today.minusDays(29), today);

        StringBuilder text = new StringBuilder("Abnehmen-Bewertung: ")
                .append(activeDays)
                .append("/")
                .append(getWeeklyGoal())
                .append(" Trainingstage diese Woche · ")
                .append(cardioMinutes)
                .append(" Cardio-Minuten in 30 Tagen.");
        if (profile.targetWeightKg <= 0) {
            text.append("\nLege ein Zielgewicht fest, um die notwendige Entwicklung zu berechnen.");
        } else if (!projection.hasTrend) {
            text.append("\nFür eine Trendbewertung werden mindestens zwei Messungen an verschiedenen Tagen benötigt.");
        } else if (profile.targetWeightKg < currentWeight
                && projection.actualPerWeek < 0) {
            text.append(String.format(
                    Locale.GERMANY,
                    "\nAktueller Gewichtstrend: %.2f %s pro Woche.",
                    projection.actualPerWeek * weightFactor,
                    weightUnit
            ));
        } else {
            text.append("\nDer aktuelle Gewichtstrend bewegt sich noch nicht in Richtung Zielgewicht.");
        }
        tvPersonalizedAssessment.setText(text);
    }

    private void updateFitnessAssessment() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        int activeDays = ProgressCalculator.filterDates(
                getAllActiveDays(),
                weekStart,
                today
        ).size();
        int strengthDays = extractDates(getWorkoutRecords(null, null)).stream()
                .filter(date -> isWithin(date, today.minusDays(29), today))
                .collect(java.util.stream.Collectors.toSet())
                .size();
        int cardioMinutes = calculateCardioMinutes(today.minusDays(29), today);
        int weeklyProgress = TrainingGoalPlanner.progressPercent(
                activeDays,
                getWeeklyGoal()
        );
        boolean balanced = strengthDays >= 2 && cardioMinutes >= 60;

        StringBuilder text = new StringBuilder("Fitness-Bewertung: ")
                .append(weeklyProgress)
                .append("% des Wochenziels · ")
                .append(strengthDays)
                .append(" Krafttage und ")
                .append(cardioMinutes)
                .append(" Cardio-Minuten in 30 Tagen.");
        text.append(balanced
                ? "\nKraft und Cardio sind aktuell ausgewogen vertreten."
                : "\nFür mehr Ausgewogenheit sollten Krafttraining und Cardio regelmäßig vorkommen.");
        if (hasFutureTargetDate()) {
            text.append("\nBis zum Zieltermin bleiben ")
                    .append(ChronoUnit.DAYS.between(
                            LocalDate.now(),
                            profile.targetDate
                    ))
                    .append(" Tage.");
        }
        tvPersonalizedAssessment.setText(text);
    }

    private boolean hasFutureTargetDate() {
        return profile.targetDate != null
                && profile.targetDate.isAfter(LocalDate.now());
    }

    private double targetWeeksRemaining() {
        if (!hasFutureTargetDate()) {
            return 1;
        }
        return Math.max(
                1.0 / 7.0,
                ChronoUnit.DAYS.between(LocalDate.now(), profile.targetDate) / 7.0
        );
    }

    private String formatPreferredDays(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return "";
        }
        String[] labels = {"MO", "DI", "MI", "DO", "FR", "SA", "SO"};
        List<DayOfWeek> sorted = new ArrayList<>(days);
        sorted.sort(Comparator.comparingInt(DayOfWeek::getValue));
        List<String> result = new ArrayList<>();
        for (DayOfWeek day : sorted) {
            result.add(labels[day.getValue() - 1]);
        }
        return String.join(", ", result);
    }

    private String getDefaultTab() {
        if (ProfileRepository.GOAL_STRENGTH.equals(profile.goalId)) {
            return "kraft";
        }
        if (ProfileRepository.GOAL_WEIGHT_LOSS.equals(profile.goalId)) {
            return "koerper";
        }
        if (ProfileRepository.GOAL_FITNESS.equals(profile.goalId)) {
            return "konsistenz";
        }
        return "muskeln";
    }

    private String getGoalLabel(String goalId) {
        if (ProfileRepository.GOAL_STRENGTH.equals(goalId)) {
            return getString(R.string.settings_goal_strength);
        }
        if (ProfileRepository.GOAL_WEIGHT_LOSS.equals(goalId)) {
            return getString(R.string.settings_goal_weight_loss);
        }
        if (ProfileRepository.GOAL_FITNESS.equals(goalId)) {
            return getString(R.string.settings_goal_fitness);
        }
        return getString(R.string.settings_goal_muscle);
    }

    private String getExperienceLabel(String experienceId) {
        if (ProfileRepository.EXPERIENCE_INTERMEDIATE.equals(experienceId)) {
            return getString(R.string.profile_experience_intermediate);
        }
        if (ProfileRepository.EXPERIENCE_EXPERIENCED.equals(experienceId)) {
            return getString(R.string.profile_experience_experienced);
        }
        return getString(R.string.profile_experience_beginner);
    }

    private String getActivityLabel(String activityId) {
        if (ProfileRepository.ACTIVITY_LOW.equals(activityId)) {
            return getString(R.string.profile_activity_low);
        }
        if (ProfileRepository.ACTIVITY_HIGH.equals(activityId)) {
            return getString(R.string.profile_activity_high);
        }
        if (ProfileRepository.ACTIVITY_VERY_HIGH.equals(activityId)) {
            return getString(R.string.profile_activity_very_high);
        }
        return getString(R.string.profile_activity_moderate);
    }

    private String comparisonTrend(double current, double previous) {
        return getComparisonLabel().startsWith("Vergleich: letzte")
                ? "30T ggü. davor " + StatisticsCalculator.formatChange(
                        current,
                        previous
                )
                : "Zum Vorzeitraum " + StatisticsCalculator.formatChange(
                        current,
                        previous
                );
    }

    private String formatWeight(double kilograms) {
        return String.format(
                Locale.GERMANY,
                "%.1f %s",
                kilograms * weightFactor,
                weightUnit
        );
    }

    private String formatVolume(double kilogramVolume) {
        return String.format(
                Locale.GERMANY,
                "%.0f %s",
                kilogramVolume * weightFactor,
                weightUnit
        );
    }

    private String formatStreakPeriod(
            ProgressCalculator.StreakStats streaks) {
        if (streaks.bestStart == null || streaks.bestEnd == null) {
            return "Noch keine Trainingstage";
        }
        return streaks.bestStart.format(DISPLAY_DATE)
                + " – "
                + streaks.bestEnd.format(DISPLAY_DATE);
    }

    private void updateGoalStatus(
            int achievement,
            int goal,
            Set<LocalDate> activeDays) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate weekEnd = weekStart.plusDays(6);
        int completedThisWeek = ProgressCalculator.filterDates(
                activeDays,
                weekStart,
                weekEnd
        ).size();
        int remaining = Math.max(0, goal - completedThisWeek);
        LocalDate nextPreferred = TrainingGoalPlanner.nextPreferredTrainingDate(
                today,
                profile.preferredDays,
                activeDays,
                goal
        );

        if (completedThisWeek >= goal) {
            tvGoalStatus.setText(
                    "Wochenziel erreicht · Regeneration bleibt eingeplant"
            );
        } else if (nextPreferred != null) {
            String weekday = nextPreferred.format(
                    DateTimeFormatter.ofPattern(
                            "EEEE",
                            getResources().getConfiguration().getLocales().get(0)
                    )
            );
            tvGoalStatus.setText(
                    remaining
                            + (remaining == 1
                                    ? " Trainingstag"
                                    : " Trainingstage")
                            + " offen · Nächster geplanter Tag: "
                            + weekday
            );
        } else {
            tvGoalStatus.setText(
                    remaining
                            + (remaining == 1
                                    ? " Trainingstag"
                                    : " Trainingstage")
                            + " offen · Ruhetage werden nicht negativ bewertet"
            );
        }
    }

    private int calculateCardioMinutes(LocalDate start, LocalDate end) {
        int minutes = 0;
        for (String type : WORKOUT_TYPES) {
            for (WorkoutStorage.CardioSession session
                    : WorkoutStorage.getCardioSessions(this, type)) {
                LocalDate date = parseDate(session.timestamp);
                if (date != null && isWithin(date, start, end)) {
                    minutes += session.minutes;
                }
            }
        }
        return minutes;
    }

    private void updateWeightGoalStatus(double currentWeight) {
        List<ProfileRepository.WeightEntry> entries =
                profileRepository.getWeightHistory();
        ProfileInsights.WeightProjection projection =
                buildWeightProjection(entries, currentWeight);
        String status;
        if (currentWeight <= 0) {
            status = "Füge im Profil eine Gewichtsmessung hinzu, um den Verlauf zu starten.";
        } else if (profile.targetWeightKg <= 0) {
            status = "Die Messungen werden als Verlauf gespeichert. Ergänze optional ein Zielgewicht.";
        } else {
            double difference = profile.targetWeightKg - currentWeight;
            if (Math.abs(difference) < 0.1) {
                status = "Zielgewicht erreicht.";
            } else {
                status = String.format(
                        Locale.GERMANY,
                        "Noch %.1f %s bis zum Zielgewicht",
                        Math.abs(difference) * weightFactor,
                        weightUnit
                );
            }
            if (hasFutureTargetDate()) {
                status += String.format(
                        Locale.GERMANY,
                        "\nBis %s erforderlich: %+.2f %s pro Woche",
                        profile.targetDate.format(DISPLAY_DATE),
                        projection.requiredPerWeek * weightFactor,
                        weightUnit
                );
            } else {
                status += "\nLege einen zukünftigen Zieltermin für die erforderliche Wochenrate fest.";
            }
            if (projection.hasTrend) {
                status += String.format(
                        Locale.GERMANY,
                        "\nTatsächlicher Trend: %+.2f %s pro Woche",
                        projection.actualPerWeek * weightFactor,
                        weightUnit
                );
                if (projection.projectedDate != null) {
                    status += " · Prognose: "
                            + projection.projectedDate.format(DISPLAY_DATE);
                } else if (Math.abs(difference) >= 0.1) {
                    status += " · Der Trend führt aktuell nicht zum Ziel.";
                }
            } else {
                status += "\nMindestens zwei Messungen an verschiedenen Tagen ergeben eine Prognose.";
            }
        }
        String details = buildBodyProfileDetails();
        tvWeightGoalStatus.setText(
                details.isEmpty() ? status : status + "\n" + details
        );
    }

    private ProfileInsights.WeightProjection buildWeightProjection(
            List<ProfileRepository.WeightEntry> entries,
            double currentWeight) {
        ProfileRepository.WeightEntry first =
                entries.isEmpty() ? null : entries.get(0);
        ProfileRepository.WeightEntry latest =
                entries.isEmpty() ? null : entries.get(entries.size() - 1);
        return ProfileInsights.projectWeightGoal(
                currentWeight,
                profile.targetWeightKg,
                LocalDate.now(),
                profile.targetDate,
                first == null ? null : first.date,
                first == null ? 0 : first.weightKg,
                latest == null ? null : latest.date,
                latest == null ? 0 : latest.weightKg
        );
    }

    private String buildBodyProfileDetails() {
        List<String> details = new ArrayList<>();
        if (profile.birthYear > 0) {
            details.add(
                    Math.max(0, LocalDate.now().getYear() - profile.birthYear)
                            + " Jahre"
            );
        }
        if (profile.heightCm > 0) {
            details.add(profile.heightCm + " cm");
        }
        if (profile.bodyFatPercent > 0) {
            details.add(String.format(
                    Locale.GERMANY,
                    "%.1f%% KFA",
                    profile.bodyFatPercent
            ));
        }
        return String.join(" · ", details);
    }

    private void setKpi(
            View card,
            String label,
            String value,
            String trend) {
        ((TextView) card.findViewById(R.id.kpiLabel)).setText(label);
        ((TextView) card.findViewById(R.id.kpiValue)).setText(value);
        ((TextView) card.findViewById(R.id.kpiTrend)).setText(trend);
    }

    private void showChartDetail(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private static Map<String, List<String>> createDefaultMuscleMap() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        putMuscles(result, "Flys", "Brust");
        putMuscles(result, "Brustpresse", "Brust", "Schultern", "Arme");
        putMuscles(result, "Schrägbank", "Brust", "Schultern", "Arme");
        putMuscles(result, "Frontheben", "Schultern");
        putMuscles(result, "Seitheben", "Schultern");
        putMuscles(result, "Triceps", "Arme");
        putMuscles(result, "Latzug eng", "Rücken", "Arme");
        putMuscles(result, "Latzug breit", "Rücken", "Arme");
        putMuscles(result, "Rudern", "Rücken", "Arme");
        putMuscles(result, "Unterer Rücken", "Rücken", "Core");
        putMuscles(result, "Bizeps", "Arme");
        putMuscles(result, "Hammer Curls", "Arme");
        putMuscles(result, "Bauch", "Core");
        putMuscles(result, "Butterfly Reverse", "Rücken", "Schultern");
        putMuscles(result, "Beinbeuger", "Beine");
        putMuscles(result, "Beine innen", "Beine");
        putMuscles(result, "Waden sitzend", "Beine");
        putMuscles(result, "Beinstrecker", "Beine");
        putMuscles(result, "Beinpresse", "Beine");
        putMuscles(result, "Hip Thrust", "Beine", "Core");
        putMuscles(result, "Hyperextension", "Beine", "Rücken", "Core");
        return result;
    }

    private static void putMuscles(
            Map<String, List<String>> map,
            String exercise,
            String... groups) {
        map.put(exercise, Arrays.asList(groups));
    }

    private enum MetricType {
        ONE_RM,
        MAX_WEIGHT,
        VOLUME,
        AVG_REPS
    }

    private static final class ExerciseOption {
        final String exercise;
        final String workoutType;

        ExerciseOption(String exercise, String workoutType) {
            this.exercise = exercise;
            this.workoutType = workoutType;
        }

        @Override
        public String toString() {
            String typeLabel;
            if (WorkoutStorage.TYPE_PUSH.equals(workoutType)) {
                typeLabel = "Push";
            } else if (WorkoutStorage.TYPE_PULL.equals(workoutType)) {
                typeLabel = "Pull";
            } else if (WorkoutStorage.TYPE_LEG.equals(workoutType)) {
                typeLabel = "Leg";
            } else {
                typeLabel = "Sonstige";
            }
            return exercise + " · " + typeLabel;
        }
    }

    private static final class WorkoutRecord {
        final LocalDate date;
        final String workoutType;
        final String exercise;
        final List<WorkoutStorage.WorkoutSet> sets;

        WorkoutRecord(
                LocalDate date,
                String workoutType,
                String exercise,
                List<WorkoutStorage.WorkoutSet> sets) {
            this.date = date;
            this.workoutType = workoutType;
            this.exercise = exercise;
            this.sets = sets;
        }
    }

    private static final class DayMetric {
        double bestOneRm;
        double maxWeight;
        double volume;
        double totalReps;
        int setCount;

        double averageReps() {
            return setCount == 0 ? 0 : totalReps / setCount;
        }
    }

    private static final class StrengthSummary {
        double bestOneRm;
        double maxWeight;
        double totalVolume;
        double totalReps;
        int totalSets;
    }

    private static final class MuscleSummary {
        final Map<String, Double> muscleVolume = new LinkedHashMap<>();
        double totalVolume;
        double pushVolume;
        double pullVolume;
        double upperVolume;
        double lowerVolume;
    }

    private abstract static class SimpleItemSelected
            implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
