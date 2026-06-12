package com.example.gym_app;

import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

public final class TrainingSaveFeedback {

    private static final long CHECK_VISIBLE_MS = 950L;

    private TrainingSaveFeedback() {
    }

    public static void show(TextView button) {
        if (!AppSettings.animationsEnabled(button.getContext())) {
            return;
        }

        CharSequence originalText = button.getText();
        CharSequence originalContentDescription = button.getContentDescription();
        Drawable[] originalDrawables = button.getCompoundDrawablesRelative();
        Drawable originalForeground = button.getForeground();
        int originalForegroundGravity = button.getForegroundGravity();

        button.setEnabled(false);
        button.animate().cancel();
        button.animate()
                .scaleX(0.86f)
                .scaleY(0.86f)
                .alpha(0.72f)
                .setDuration(90L)
                .withEndAction(() -> {
                    button.setText("");
                    button.setContentDescription(
                            button.getContext().getString(R.string.training_saved_confirmation)
                    );
                    button.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    button.setForeground(
                            button.getContext().getDrawable(R.drawable.ic_ironx_check)
                    );
                    button.setForegroundGravity(Gravity.CENTER);
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(190L)
                            .setInterpolator(new OvershootInterpolator(1.7f))
                            .start();
                })
                .start();

        button.postDelayed(() -> button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0.68f)
                .setDuration(90L)
                .withEndAction(() -> {
                    button.setText(originalText);
                    button.setContentDescription(originalContentDescription);
                    button.setForeground(originalForeground);
                    button.setForegroundGravity(originalForegroundGravity);
                    button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            originalDrawables[0],
                            originalDrawables[1],
                            originalDrawables[2],
                            originalDrawables[3]
                    );
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(160L)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> button.setEnabled(true))
                            .start();
                })
                .start(), CHECK_VISIBLE_MS);
    }

    public static void revealSavedEntry(View entry) {
        if (!AppSettings.animationsEnabled(entry.getContext())) {
            return;
        }

        entry.setAlpha(0f);
        entry.setTranslationY(dp(entry.getContext(), 28));
        entry.setScaleX(0.98f);
        entry.setScaleY(0.98f);
        entry.postDelayed(() -> entry.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(320L)
                .setInterpolator(new DecelerateInterpolator())
                .start(), 190L);
    }

    private static int dp(android.content.Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }
}
