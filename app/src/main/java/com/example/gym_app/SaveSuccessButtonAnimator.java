package com.example.gym_app;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public final class SaveSuccessButtonAnimator {

    private static final long EXIT_DELAY_MS = 1_180L;

    private SaveSuccessButtonAnimator() {
    }

    public static void play(
            @NonNull MaterialButton button,
            @Nullable Runnable onFinished) {
        if (!AppSettings.animationsEnabled(button.getContext())) {
            button.setEnabled(false);
            button.post(() -> {
                button.setEnabled(true);
                finish(onFinished);
            });
            return;
        }
        if (!button.isLaidOut()) {
            button.post(() -> play(button, onFinished));
            return;
        }

        button.setEnabled(false);
        button.animate().cancel();

        int startWidth = button.getWidth();
        int startHeight = button.getHeight();
        int targetWidth = Math.max(dp(button, 98), startHeight + dp(button, 40));
        CharSequence originalText = button.getText();
        CharSequence originalContentDescription = button.getContentDescription();
        ColorStateList originalTextColors = button.getTextColors();
        int originalTextColor = button.getCurrentTextColor();
        ColorStateList originalBackgroundTint = button.getBackgroundTintList();
        Drawable originalIcon = button.getIcon();
        ColorStateList originalIconTint = button.getIconTint();
        int originalIconSize = button.getIconSize();
        int originalIconPadding = button.getIconPadding();
        int originalIconGravity = button.getIconGravity();
        int goldStart = ContextCompat.getColor(
                button.getContext(),
                R.color.training_gold_highlight
        );
        int goldSuccess = ContextCompat.getColor(
                button.getContext(),
                R.color.training_gold_pressed
        );
        int particleColor = ContextCompat.getColor(
                button.getContext(),
                R.color.training_gold_highlight
        );

        ViewGroup.LayoutParams params = button.getLayoutParams();
        int originalWidth = params.width;
        int originalGravity = params instanceof LinearLayout.LayoutParams
                ? ((LinearLayout.LayoutParams) params).gravity
                : Gravity.NO_GRAVITY;
        params.width = startWidth;
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).gravity = Gravity.CENTER_HORIZONTAL;
        }
        button.setLayoutParams(params);
        button.setPivotX(startWidth / 2f);
        button.setPivotY(startHeight / 2f);
        button.setContentDescription(
                button.getContext().getString(R.string.training_saved_confirmation)
        );

        AnimatedCheckDrawable checkDrawable =
                new AnimatedCheckDrawable(Color.parseColor("#11100B"));
        checkDrawable.setAlpha(0);
        button.setIcon(checkDrawable);
        button.setIconTint(null);
        button.setIconSize(dp(button, 32));
        button.setIconPadding(0);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);

        GlowParticleDrawable particles = new GlowParticleDrawable(particleColor);
        particles.setBounds(0, 0, startWidth, startHeight);
        button.getOverlay().add(particles);

        ObjectAnimator pressX =
                ObjectAnimator.ofFloat(button, View.SCALE_X, 1f, 0.955f, 1f);
        ObjectAnimator pressY =
                ObjectAnimator.ofFloat(button, View.SCALE_Y, 1f, 0.955f, 1f);
        pressX.setDuration(190L);
        pressY.setDuration(190L);
        pressX.setInterpolator(new AccelerateDecelerateInterpolator());
        pressY.setInterpolator(new AccelerateDecelerateInterpolator());

        ValueAnimator textFade = ValueAnimator.ofInt(255, 0);
        textFade.setStartDelay(120L);
        textFade.setDuration(220L);
        textFade.setInterpolator(new DecelerateInterpolator());
        textFade.addUpdateListener(animation -> {
            int alpha = (int) animation.getAnimatedValue();
            button.setTextColor(withAlpha(originalTextColor, alpha));
            if (alpha < 12 && button.length() > 0) {
                button.setText("");
            }
        });

        ValueAnimator widthMorph = ValueAnimator.ofInt(startWidth, targetWidth);
        widthMorph.setStartDelay(235L);
        widthMorph.setDuration(330L);
        widthMorph.setInterpolator(new AccelerateDecelerateInterpolator());
        widthMorph.addUpdateListener(animation -> {
            int width = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = button.getLayoutParams();
            layoutParams.width = width;
            button.setLayoutParams(layoutParams);
            particles.setBounds(0, 0, width, button.getHeight());
        });

        ValueAnimator backgroundMorph = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                goldStart,
                goldSuccess
        );
        backgroundMorph.setStartDelay(255L);
        backgroundMorph.setDuration(330L);
        backgroundMorph.addUpdateListener(animation -> button.setBackgroundTintList(
                ColorStateList.valueOf((int) animation.getAnimatedValue())
        ));

        ValueAnimator checkAnimator = ValueAnimator.ofFloat(0f, 1f);
        checkAnimator.setStartDelay(330L);
        checkAnimator.setDuration(390L);
        checkAnimator.setInterpolator(new DecelerateInterpolator());
        checkAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            checkDrawable.setProgress(progress);
            checkDrawable.setAlpha((int) (255 * Math.min(1f, progress * 1.45f)));
        });

        ValueAnimator particleAnimator = ValueAnimator.ofFloat(0f, 1f);
        particleAnimator.setStartDelay(535L);
        particleAnimator.setDuration(430L);
        particleAnimator.setInterpolator(new DecelerateInterpolator());
        particleAnimator.addUpdateListener(animation ->
                particles.setProgress((float) animation.getAnimatedValue())
        );

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                pressX,
                pressY,
                textFade,
                widthMorph,
                backgroundMorph,
                checkAnimator,
                particleAnimator
        );
        animatorSet.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> button.animate()
                .translationY(-dp(button, 12))
                .scaleX(0.92f)
                .scaleY(0.92f)
                .alpha(0f)
                .setDuration(220L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    button.getOverlay().remove(particles);
                    resetButton(
                            button,
                            originalText,
                            originalContentDescription,
                            originalTextColors,
                            originalBackgroundTint,
                            originalIcon,
                            originalIconTint,
                            originalIconSize,
                            originalIconPadding,
                            originalIconGravity,
                            originalWidth,
                            originalGravity
                    );
                    finish(onFinished);
                })
                .start(), EXIT_DELAY_MS);
    }

    private static void resetButton(
            @NonNull MaterialButton button,
            CharSequence originalText,
            CharSequence originalContentDescription,
            ColorStateList originalTextColors,
            ColorStateList originalBackgroundTint,
            Drawable originalIcon,
            ColorStateList originalIconTint,
            int originalIconSize,
            int originalIconPadding,
            int originalIconGravity,
            int originalWidth,
            int originalGravity) {
        button.animate().cancel();
        button.setAlpha(1f);
        button.setScaleX(1f);
        button.setScaleY(1f);
        button.setTranslationX(0f);
        button.setTranslationY(0f);
        button.setText(originalText);
        button.setTextColor(originalTextColors);
        button.setContentDescription(originalContentDescription);
        button.setBackgroundTintList(originalBackgroundTint);
        button.setIcon(originalIcon);
        button.setIconTint(originalIconTint);
        button.setIconSize(originalIconSize);
        button.setIconPadding(originalIconPadding);
        button.setIconGravity(originalIconGravity);

        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.width = originalWidth;
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).gravity = originalGravity;
        }
        button.setLayoutParams(params);
        button.setEnabled(true);
    }

    private static void finish(@Nullable Runnable onFinished) {
        if (onFinished != null) {
            onFinished.run();
        }
    }

    private static int dp(@NonNull View view, int value) {
        return Math.round(
                value * view.getResources().getDisplayMetrics().density
        );
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private static final class AnimatedCheckDrawable extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;
        private int alpha = 255;

        AnimatedCheckDrawable(int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        void setProgress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            float scale = size / 48f;
            float centerX = bounds.centerX();
            float centerY = bounds.centerY();

            paint.setStrokeWidth(7.2f * scale);
            paint.setAlpha(alpha);

            Path checkPath = new Path();
            checkPath.moveTo(centerX - 16f * scale, centerY + scale);
            checkPath.lineTo(centerX - 4f * scale, centerY + 13f * scale);
            checkPath.lineTo(centerX + 18f * scale, centerY - 12f * scale);

            PathMeasure measure = new PathMeasure(checkPath, false);
            Path visiblePath = new Path();
            measure.getSegment(
                    0f,
                    measure.getLength() * progress,
                    visiblePath,
                    true
            );
            canvas.drawPath(visiblePath, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = Math.max(0, Math.min(255, alpha));
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class GlowParticleDrawable extends Drawable {

        private static final int PARTICLE_COUNT = 12;
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;

        GlowParticleDrawable(int color) {
            particlePaint.setColor(color);
            particlePaint.setStyle(Paint.Style.FILL);
            glowPaint.setColor(color);
            glowPaint.setStyle(Paint.Style.STROKE);
        }

        void setProgress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (progress <= 0f || progress >= 1f) {
                return;
            }

            Rect bounds = getBounds();
            float centerX = bounds.centerX();
            float centerY = bounds.centerY();
            float size = Math.min(bounds.width(), bounds.height());
            float eased = easeOut(progress);
            float radius = size * (0.18f + 0.48f * eased);

            glowPaint.setStrokeWidth(Math.max(1.5f, size * 0.025f));
            glowPaint.setAlpha((int) (95 * (1f - progress)));
            canvas.drawCircle(centerX, centerY, radius * 0.72f, glowPaint);

            particlePaint.setAlpha((int) (145 * (1f - progress)));
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                double angle = Math.PI * 2.0 * i / PARTICLE_COUNT;
                float x = centerX + (float) Math.cos(angle) * radius;
                float y = centerY + (float) Math.sin(angle) * radius;
                float dotSize = Math.max(
                        1.5f,
                        size * 0.028f * (1f - progress * 0.4f)
                );
                canvas.drawCircle(x, y, dotSize, particlePaint);
            }
        }

        private float easeOut(float value) {
            return 1f - (float) Math.pow(1f - value, 3);
        }

        @Override
        public void setAlpha(int alpha) {
            particlePaint.setAlpha(alpha);
            glowPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            particlePaint.setColorFilter(colorFilter);
            glowPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
