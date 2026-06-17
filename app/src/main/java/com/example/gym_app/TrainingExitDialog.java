package com.example.gym_app;

import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

final class TrainingExitDialog {

    private TrainingExitDialog() {
    }

    static void show(
            @NonNull IronxActivity activity,
            @NonNull String sessionId,
            @NonNull String workoutType,
            @NonNull String workoutStartedTimestamp,
            long workoutStartedEpochMs,
            boolean hasUnsavedInput,
            @NonNull Runnable onContinue,
            @NonNull Runnable onFinished) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_end_training);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.78f;
            window.setAttributes(attributes);
        }

        TextView unsavedWarning = dialog.findViewById(R.id.tvUnsavedWarning);
        unsavedWarning.setVisibility(hasUnsavedInput ? View.VISIBLE : View.GONE);

        dialog.findViewById(R.id.btnContinueTraining).setOnClickListener(view -> {
            dialog.dismiss();
            onContinue.run();
        });
        dialog.findViewById(R.id.btnEndTraining).setOnClickListener(view -> {
            if (!WorkoutStorage.hasTrainingSessionItems(
                    activity,
                    workoutType,
                    sessionId
            )) {
                unsavedWarning.setText(R.string.training_exit_empty_warning);
                unsavedWarning.setVisibility(View.VISIBLE);
                return;
            }
            long workoutDurationMs = Math.max(
                    1L,
                    System.currentTimeMillis() - workoutStartedEpochMs
            );
            if (!WorkoutStorage.saveTrainingSession(
                    activity,
                    sessionId,
                    workoutType,
                    workoutStartedTimestamp,
                    workoutDurationMs
            )) {
                unsavedWarning.setText(R.string.training_save_failed);
                unsavedWarning.setVisibility(View.VISIBLE);
                return;
            }
            showSummary(
                    dialog,
                    activity,
                    sessionId,
                    workoutType,
                    workoutDurationMs
            );
        });
        dialog.findViewById(R.id.btnDiscardTraining).setOnClickListener(view -> {
            if (sessionId.trim().isEmpty()) {
                dialog.dismiss();
                onFinished.run();
                return;
            }
            if (!WorkoutStorage.discardTrainingSession(activity, workoutType, sessionId)) {
                unsavedWarning.setText(R.string.training_discard_failed);
                unsavedWarning.setVisibility(View.VISIBLE);
                return;
            }
            dialog.dismiss();
            onFinished.run();
        });
        dialog.findViewById(R.id.btnSummaryDone).setOnClickListener(view -> {
            dialog.dismiss();
            onFinished.run();
        });

        dialog.show();
        if (window != null) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = (int) (
                    activity.getResources().getDisplayMetrics().widthPixels * 0.92f
            );
            window.setAttributes(attributes);
        }
    }

    private static void showSummary(
            Dialog dialog,
            IronxActivity activity,
            String sessionId,
            String workoutType,
            long elapsedMs) {
        List<WorkoutStorage.DetailedWorkout> workouts =
                WorkoutStorage.getDetailedWorkouts(activity, workoutType);
        List<WorkoutStorage.CardioSession> cardio =
                WorkoutStorage.getCardioSessions(activity, workoutType);
        TrainingSummaryCalculator.Summary summary =
                TrainingSummaryCalculator.calculate(workouts, cardio, sessionId);

        long storedDuration =
                WorkoutStorage.getTrainingSessionDuration(activity, sessionId);
        long displayedDuration = Math.max(elapsedMs, storedDuration);

        setText(dialog, R.id.tvExitChip, R.string.training_exit_summary_chip);
        setText(dialog, R.id.tvExitTitle, R.string.training_exit_summary_title);
        setText(
                dialog,
                R.id.tvExitSubtitle,
                R.string.training_exit_summary_subtitle
        );
        setText(dialog, R.id.tvSummaryWorkoutType, workoutTitle(workoutType));
        setText(
                dialog,
                R.id.tvSummaryDuration,
                formatDuration(displayedDuration)
        );
        setText(
                dialog,
                R.id.tvSummaryExercises,
                activity.getResources().getQuantityString(
                        R.plurals.history_exercise_count,
                        summary.exerciseCount,
                        summary.exerciseCount
                )
        );
        setText(
                dialog,
                R.id.tvSummarySets,
                activity.getResources().getQuantityString(
                        R.plurals.history_set_count,
                        summary.setCount,
                        summary.setCount
                )
        );
        setText(
                dialog,
                R.id.tvSummaryBestSet,
                formatBestSet(activity, summary.bestSet)
        );
        configureRecordText(dialog, activity, summary.recordExercises);
        setText(
                dialog,
                R.id.tvSummaryVolume,
                formatVolume(activity, summary, workoutType)
        );
        setText(
                dialog,
                R.id.tvSummaryCardio,
                formatCardio(activity, summary.cardioSessions)
        );

        View warning = dialog.findViewById(R.id.tvUnsavedWarning);
        View actions = dialog.findViewById(R.id.confirmationActions);
        View summarySection = dialog.findViewById(R.id.summarySection);
        View done = dialog.findViewById(R.id.btnSummaryDone);

        warning.setVisibility(View.GONE);
        actions.animate()
                .alpha(0f)
                .setDuration(140L)
                .withEndAction(() -> {
                    actions.setVisibility(View.GONE);
                    summarySection.setAlpha(0f);
                    summarySection.setTranslationY(18f);
                    summarySection.setVisibility(View.VISIBLE);
                    done.setAlpha(0f);
                    done.setVisibility(View.VISIBLE);
                    summarySection.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(240L)
                            .start();
                    done.animate()
                            .alpha(1f)
                            .setStartDelay(80L)
                            .setDuration(220L)
                            .start();
                })
                .start();
    }

    private static String formatBestSet(
            IronxActivity activity,
            TrainingSummaryCalculator.BestSet bestSet) {
        if (bestSet == null) {
            return activity.getString(R.string.training_summary_no_sets);
        }
        return activity.getString(
                R.string.training_summary_best_set_value,
                bestSet.exercise,
                AppSettings.formatWeight(activity, bestSet.weight, 1),
                bestSet.reps
        );
    }

    private static void configureRecordText(
            Dialog dialog,
            IronxActivity activity,
            List<String> recordExercises) {
        TextView record = dialog.findViewById(R.id.tvSummaryRecord);
        if (recordExercises.isEmpty()) {
            record.setText(R.string.training_summary_no_record);
            record.setTextColor(ContextCompat.getColor(
                    activity,
                    R.color.text_tertiary
            ));
            return;
        }

        String recordNames;
        if (recordExercises.size() <= 2) {
            recordNames = joinRecordNames(recordExercises);
        } else {
            recordNames = activity.getString(
                    R.string.training_summary_record_more,
                    recordExercises.get(0),
                    recordExercises.get(1),
                    recordExercises.size() - 2
            );
        }
        record.setText(activity.getString(
                R.string.training_summary_record,
                recordNames
        ));
        record.setTextColor(ContextCompat.getColor(activity, R.color.success));
    }

    private static String joinRecordNames(List<String> recordExercises) {
        StringBuilder names = new StringBuilder();
        for (String exercise : recordExercises) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(exercise);
        }
        return names.toString();
    }

    private static String formatVolume(
            IronxActivity activity,
            TrainingSummaryCalculator.Summary summary,
            String workoutType) {
        if (summary.setCount == 0) {
            return activity.getString(R.string.training_summary_no_sets);
        }
        String volume = AppSettings.formatVolume(activity, summary.volume, 0);
        if (!summary.hasPreviousVolume) {
            return activity.getString(
                    R.string.training_summary_first_comparison,
                    volume
            );
        }

        double change = summary.previousVolume == 0d
                ? 0d
                : (summary.volume - summary.previousVolume)
                        / summary.previousVolume * 100d;
        String changeText = String.format(
                Locale.getDefault(),
                "%+.0f %%",
                change
        );
        return activity.getString(
                R.string.training_summary_volume_change,
                volume,
                changeText,
                workoutTitle(workoutType)
        );
    }

    private static String formatCardio(
            IronxActivity activity,
            List<WorkoutStorage.CardioSession> sessions) {
        if (sessions.isEmpty()) {
            return activity.getString(R.string.training_summary_no_cardio);
        }
        StringBuilder result = new StringBuilder();
        for (WorkoutStorage.CardioSession session : sessions) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(session.exercise)
                    .append(" · ")
                    .append(activity.getString(
                            R.string.training_minutes_value,
                            session.minutes
                    ));
        }
        return result.toString();
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
            );
        }
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    private static String workoutTitle(String workoutType) {
        if (WorkoutStorage.TYPE_PUSH.equals(workoutType)) {
            return "PUSH DAY";
        }
        if (WorkoutStorage.TYPE_PULL.equals(workoutType)) {
            return "PULL DAY";
        }
        if (WorkoutStorage.TYPE_LEG.equals(workoutType)) {
            return "LEG DAY";
        }
        return workoutType == null
                ? ""
                : workoutType.toUpperCase(Locale.getDefault());
    }

    private static void setText(Dialog dialog, int viewId, int stringId) {
        ((TextView) dialog.findViewById(viewId)).setText(stringId);
    }

    private static void setText(Dialog dialog, int viewId, String value) {
        ((TextView) dialog.findViewById(viewId)).setText(value);
    }
}
