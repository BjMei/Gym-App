package com.example.gym_app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Locale;

final class InfoDialogHelper {

    private InfoDialogHelper() {
    }

    static void bind(View infoButton, String title, String message) {
        if (infoButton == null || title == null || message == null) {
            return;
        }
        infoButton.setVisibility(View.VISIBLE);
        infoButton.setContentDescription("Info: " + title);
        infoButton.setOnClickListener(v -> show(v.getContext(), title, message));
    }

    static void bindKpi(View card, String label) {
        Info info = kpiInfo(label);
        View button = card == null ? null : card.findViewById(R.id.kpiInfoButton);
        if (button == null) {
            return;
        }
        if (info == null) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        bind(button, info.title, info.message);
    }

    static void insertInfoRowAfter(View anchor, String title, String message) {
        if (anchor == null || !(anchor.getParent() instanceof LinearLayout)) {
            return;
        }
        LinearLayout parent = (LinearLayout) anchor.getParent();
        View row = createInfoRow(parent.getContext(), title, message);
        ViewGroup.LayoutParams anchorParams = anchor.getLayoutParams();
        ViewGroup.LayoutParams infoParams = row.getLayoutParams();
        if (anchorParams instanceof ViewGroup.MarginLayoutParams
                && infoParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams source =
                    (ViewGroup.MarginLayoutParams) anchorParams;
            ViewGroup.MarginLayoutParams target =
                    (ViewGroup.MarginLayoutParams) infoParams;
            target.leftMargin = source.leftMargin;
            target.rightMargin = source.rightMargin;
            target.setMarginStart(source.getMarginStart());
            target.setMarginEnd(source.getMarginEnd());
            row.setLayoutParams(target);
        }
        int index = parent.indexOfChild(anchor);
        parent.addView(row, Math.max(0, index + 1));
    }

    static void insertInfoRowAtTop(LinearLayout container, String title, String message) {
        if (container == null) {
            return;
        }
        container.addView(createInfoRow(container.getContext(), title, message), 0);
    }

    private static View createInfoRow(Context context, String title, String message) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 30));
        row.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("Info: " + title);
        row.setOnClickListener(v -> show(context, title, message));

        TextView icon = createIcon(context);
        row.addView(icon);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(context, 2);
        rowParams.bottomMargin = dp(context, 2);
        row.setLayoutParams(rowParams);
        return row;
    }

    private static TextView createIcon(Context context) {
        TextView icon = new TextView(context);
        icon.setText("i");
        icon.setTextColor(ContextCompat.getColor(context, R.color.training_gold_highlight));
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        icon.setTextSize(13);
        icon.setBackgroundResource(R.drawable.bg_info_button);
        icon.setIncludeFontPadding(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(context, 22),
                dp(context, 22)
        );
        icon.setLayoutParams(params);
        return icon;
    }

    private static void show(Context context, String title, String message) {
        Dialog dialog = new Dialog(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 22), dp(context, 20), dp(context, 22), dp(context, 18));
        content.setBackgroundResource(R.drawable.bg_dialog_surface);

        TextView chip = new TextView(context);
        chip.setText("INFO");
        chip.setTextColor(ContextCompat.getColor(context, R.color.gold_dark));
        chip.setTextSize(9);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setLetterSpacing(0.14f);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.bg_home_chip);
        chip.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        content.addView(chip, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        titleView.setTextSize(20);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(context, 12);
        content.addView(titleView, titleParams);

        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        messageView.setTextSize(13);
        messageView.setLineSpacing(dp(context, 3), 1.0f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dp(context, 10);
        content.addView(messageView, messageParams);

        TextView close = new TextView(context);
        close.setText("VERSTANDEN");
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setTextSize(11);
        close.setLetterSpacing(0.08f);
        close.setBackgroundResource(R.drawable.bg_dialog_primary_action);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 48)
        );
        closeParams.topMargin = dp(context, 18);
        content.addView(close, closeParams);

        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shownWindow.setLayout(
                    context.getResources().getDisplayMetrics().widthPixels - dp(context, 44),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private static Info kpiInfo(String label) {
        if (label == null) {
            return null;
        }
        String key = label.toUpperCase(Locale.GERMANY)
                .replace("Ø", "OE")
                .replace("∅", "OE");
        if (key.contains("1RM")) {
            return new Info(
                    "Geschätztes 1RM",
                    "1RM ist das geschätzte Gewicht, das du ungefähr einmal sauber bewegen könntest.\n\nFormel: Gewicht × (1 + Wiederholungen / 30). Beispiel: 80 kg × (1 + 8 / 30) = 101,3 kg."
            );
        }
        if (key.contains("PR")) {
            return new Info(
                    "Persönlicher Rekord",
                    "Ein PR ist ein neuer Bestwert im Vergleich zu allen gespeicherten Trainings. Markiert werden echte Allzeit-Bestwerte, nicht nur der beste Wert im aktuellen Zeitraum."
            );
        }
        if (key.contains("VOLUMEN")) {
            return new Info(
                    "Trainingsvolumen",
                    "Volumen zeigt die bewegte Gesamtlast.\n\nFormel pro Satz: Gewicht × Wiederholungen. Gesamtvolumen ist die Summe aller Satzvolumen."
            );
        }
        if (key.contains("GEWICHT")) {
            return new Info(
                    "Gewichtswerte",
                    "Bei Übungen ist damit das eingetragene Satzgewicht gemeint. Beim Körpergewicht werden nur deine separaten Gewichtsmessungen aus Profil & Ziele verwendet."
            );
        }
        if (key.contains("VERÄNDERUNG")) {
            return new Info(
                    "Gewichtsveränderung",
                    "Die Veränderung ist die Differenz zwischen dem ersten und dem neuesten Gewicht im gewählten Zeitraum. Ein Minus bedeutet Gewichtsverlust, ein Plus Gewichtszunahme."
            );
        }
        if (key.contains("PUSH / PULL") || key.contains("BALANCE")) {
            return new Info(
                    "Trainingsbalance",
                    "Die Balance vergleicht das zugeordnete Trainingsvolumen der Bereiche. 50 / 50 bedeutet gleich viel Volumen; eine Abweichung ist ein Hinweis und keine medizinische Bewertung."
            );
        }
        if (key.contains("WIEDERHOL")) {
            return new Info(
                    "Wiederholungen",
                    "Wiederholungen sind die sauber ausgeführten Wiederholungen pro Satz. Durchschnittswerte werden über gespeicherte Sätze im gewählten Zeitraum gebildet."
            );
        }
        if (key.contains("DAUER") || key.contains("STUNDEN")) {
            return new Info(
                    "Trainingsdauer",
                    "Die Workout-Dauer startet automatisch beim Öffnen von Push, Pull oder Leg und endet beim bestätigten Beenden. Stunden sind die Summe dieser abgeschlossenen Workouts; Ø Dauer ist die Gesamtzeit geteilt durch deren Anzahl."
            );
        }
        if (key.contains("PRO WOCHE") || key.contains("ZIELERREICHUNG")) {
            return new Info(
                    "Wochenziel",
                    "Gezählt werden Trainingstage im ausgewählten Zeitraum. Das persönliche Wochenziel bearbeitest du zentral unter Profil & Ziele."
            );
        }
        if (key.contains("SERIE")) {
            return new Info(
                    "Trainingsserie",
                    "Eine Serie zählt aufeinanderfolgende aktive Trainingstage. Wenn du heute nicht trainiert hast, kann die Serie trotzdem weiter gelten, wenn gestern trainiert wurde."
            );
        }
        if (key.contains("CARDIO")) {
            return new Info(
                    "Cardio",
                    "Cardio zählt die gespeicherten Cardio-Minuten und Einheiten. Cardio-only-Tage werden in Wochenziel und Konsistenz mitgezählt."
            );
        }
        if (key.contains("TRAININGS")) {
            return new Info(
                    "Trainings",
                    "Gezählt werden aktive Tage mit Krafttraining oder Cardio. Mehrere Workouts am selben Tag bleiben für Wochenziel und Frequenz ein Trainingstag."
            );
        }
        return null;
    }

    static final class Texts {
        private Texts() {
        }

        static String profileWeight() {
            return "Trage hier dein Körpergewicht als Messwert mit Datum ein. Der neueste gespeicherte Messwert wird automatisch als aktuelles Gewicht im Profil und in Auswertungen verwendet. Wenn es für ein Datum bereits einen Wert gibt, wird dieser ersetzt.";
        }

        static String profileBodyData() {
            return "Der Name wird nur für die Begrüßung verwendet. Körpergröße wird zusammen mit Körpergewicht für BMI und Ziel-BMI genutzt. Das Geburtsdatum liefert eine genaue altersbezogene Einordnung für Zieltempo und Regeneration. Der Körperfettanteil kommt aus datierten Messwerten und wird mit dem aktuellen Gewicht als Fettmasse und fettfreie Masse im Körper-Fortschritt angezeigt.";
        }

        static String profileTarget() {
            return "Zielgewicht und Zieltermin werden mit deinem aktuellen Gewichtstrend verglichen. Daraus berechnet die App die noch nötige Veränderung und eine erforderliche durchschnittliche Entwicklung pro Woche. Das ist eine Prognose, keine medizinische Vorgabe.";
        }

        static String profileRecommendationBasis() {
            return "Das Aktivitätsniveau beschreibt deinen Alltag außerhalb des Trainings. Der Trainingsstand beschreibt deine Erfahrung im Krafttraining. Beides beeinflusst Hinweise zu Umfang, Steigerung und Regeneration.";
        }

        static String profileTrainingGoal() {
            return "Das Wochenziel ist die Anzahl geplanter Trainingstage pro Woche. Es wird auf der Startseite und in den Fortschritt-Auswertungen verwendet. Bevorzugte Trainingstage helfen der App, Ruhetage sinnvoll zu berücksichtigen.";
        }

        static String profilePerformanceGoals() {
            return "Kraftziele gelten pro Übung und beziehen sich auf das geschätzte 1RM. Das Wochenvolumen-Ziel ist die bewegte Gesamtlast pro Woche: Gewicht × Wiederholungen über alle Sätze.";
        }

        static String trainingSets() {
            return "Trage pro Satz Gewicht und Wiederholungen ein. Satzvolumen = Gewicht × Wiederholungen. Die App nutzt diese Werte später für Volumen, 1RM, PRs und Trends.";
        }

        static String cardio() {
            return "Wähle die Cardio-Art und trage die Minuten ein. Cardio wird separat gespeichert und zählt in Übersicht, Konsistenz und Cardio-Auswertung.";
        }

        static String statisticsOverview() {
            return "Die Übersicht zählt aktive Trainingstage und die Trainingsfrequenz. Die Dauer startet automatisch bei der Auswahl von Push, Pull oder Leg und endet beim bestätigten Beenden. Stunden und Ø Dauer verwenden ausschließlich diese exakt erfassten, abgeschlossenen Workouts.";
        }

        static String statisticsVolume() {
            return "Gesamtvolumen = Summe aus Gewicht × Wiederholungen über alle gespeicherten Sätze. Satzvolumen ist diese Rechnung für einen einzelnen Satz.";
        }

        static String statisticsStructure() {
            return "Struktur zeigt Verteilung nach Push, Pull und Leg sowie Pausen zwischen Trainingstagen. Cardio-only-Tage zählen als aktive Tage.";
        }

        static String statisticsRecords() {
            return "Rekorde vergleichen alle gespeicherten Werte: schwerster Satz, höchstes Gesamtvolumen und beste Wiederholungsleistungen je Übung.";
        }

        static String progressStrength() {
            return "1RM wird mit Gewicht × (1 + Wiederholungen / 30) geschätzt. Volumen ist Gewicht × Wiederholungen über alle Sätze. Ein PR-Marker kennzeichnet einen neuen Allzeit-Bestwert. Tippe einen Diagrammpunkt an, um Datum und Wert zu sehen.";
        }

        static String progressMuscles() {
            return "Das Volumen wird anhand der hinterlegten Muskelgruppen verteilt. Eigene Übungen kannst du hier zuordnen. Push/Pull und Oberkörper/Unterkörper zeigen Anteile am erfassten Volumen, keine anatomische Belastungsmessung.";
        }

        static String progressConsistency() {
            return "Aktive Tage enthalten Krafttraining und Cardio. Serien zählen aufeinanderfolgende aktive Tage; die aktuelle Serie bleibt bestehen, wenn zuletzt gestern trainiert wurde. Zielerreichung vergleicht den Wochendurchschnitt mit deinem Profilziel.";
        }

        static String progressBody() {
            return "Der Verlauf nutzt datierte Gewichtsmessungen und datierte Körperfettmessungen aus Profil & Ziele. Veränderung = neuester minus erster Wert im Zeitraum. Körpergröße ergänzt BMI und Ziel-BMI, Körperfett ergänzt Fettmasse und fettfreie Masse, Geburtsdatum ergänzt eine genaue altersbezogene Einordnung. Cardio summiert die gespeicherten Minuten.";
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Info {
        final String title;
        final String message;

        Info(String title, String message) {
            this.title = title;
            this.message = message;
        }
    }
}
