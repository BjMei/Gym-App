package com.example.gym_app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.WeakHashMap;

public final class TrainingTimerAnimator {

    private static final long PULSE_HALF_CYCLE_MS = 2_400L;
    private static final WeakHashMap<View, PulseController> PULSES =
            new WeakHashMap<>();
    private static final WeakHashMap<ImageButton, ButtonStateController> BUTTONS =
            new WeakHashMap<>();

    private TrainingTimerAnimator() {
    }

    public static void setRunning(@NonNull View timerCard, boolean running) {
        if (!AppSettings.animationsEnabled(timerCard.getContext())) {
            release(timerCard);
            return;
        }

        if (running) {
            PulseController controller = PULSES.get(timerCard);
            if (controller == null) {
                controller = new PulseController(timerCard);
                PULSES.put(timerCard, controller);
            }
            controller.start();
        } else {
            PulseController controller = PULSES.get(timerCard);
            if (controller != null) {
                controller.stopSmoothly();
            }
        }
    }

    public static void suspend(@NonNull View timerCard) {
        PulseController controller = PULSES.get(timerCard);
        if (controller != null) {
            controller.stopImmediately();
        }
    }

    public static void release(@NonNull View timerCard) {
        PulseController controller = PULSES.remove(timerCard);
        if (controller != null) {
            controller.stopImmediately();
        }
    }

    public static void changeButtonState(
            @NonNull ImageButton button,
            @DrawableRes int targetIcon) {
        if (!AppSettings.animationsEnabled(button.getContext())) {
            button.setImageResource(targetIcon);
            button.setImageAlpha(255);
            return;
        }
        ButtonStateController controller = BUTTONS.get(button);
        if (controller == null) {
            controller = new ButtonStateController(button);
            BUTTONS.put(button, controller);
        }
        controller.play(targetIcon);
    }

    private static final class PulseController {

        private final View timerCard;
        private final Drawable originalBackground;
        private final Drawable activeBackground;
        private final Drawable originalForeground;
        private final PulseBorderDrawable pulseBorder;
        private ValueAnimator animator;
        private float progress;

        PulseController(View timerCard) {
            this.timerCard = timerCard;
            originalBackground = timerCard.getBackground();
            activeBackground = ContextCompat.getDrawable(
                    timerCard.getContext(),
                    R.drawable.bg_stopwatch_card_active
            );
            originalForeground = timerCard.getForeground();
            pulseBorder = new PulseBorderDrawable(
                    ContextCompat.getColor(
                            timerCard.getContext(),
                            R.color.timer_active_green
                    ),
                    timerCard.getResources().getDisplayMetrics().density
            );
        }

        void start() {
            if (animator != null && animator.isRunning()
                    && animator.getRepeatCount() == ValueAnimator.INFINITE) {
                return;
            }

            cancelAnimator();
            timerCard.setBackground(activeBackground);
            timerCard.setForeground(pulseBorder);

            animator = ValueAnimator.ofFloat(progress, 1f);
            animator.setDuration(Math.max(
                    500L,
                    Math.round(PULSE_HALF_CYCLE_MS * (1f - progress))
            ));
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> update(
                    (float) animation.getAnimatedValue()
            ));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator != animation) {
                        return;
                    }
                    startRepeatingPulse();
                }
            });
            animator.start();
        }

        void stopSmoothly() {
            cancelAnimator();
            if (progress <= 0.01f) {
                restoreAppearance();
                return;
            }

            animator = ValueAnimator.ofFloat(progress, 0f);
            animator.setDuration(520L);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> update(
                    (float) animation.getAnimatedValue()
            ));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animator == animation) {
                        restoreAppearance();
                    }
                }
            });
            animator.start();
        }

        void stopImmediately() {
            cancelAnimator();
            progress = 0f;
            pulseBorder.setProgress(0f);
            restoreAppearance();
        }

        private void startRepeatingPulse() {
            animator = ValueAnimator.ofFloat(1f, 0f);
            animator.setDuration(PULSE_HALF_CYCLE_MS);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> update(
                    (float) animation.getAnimatedValue()
            ));
            animator.start();
        }

        private void update(float value) {
            progress = value;
            pulseBorder.setProgress(value);
        }

        private void cancelAnimator() {
            if (animator != null) {
                animator.removeAllListeners();
                animator.cancel();
                animator = null;
            }
        }

        private void restoreAppearance() {
            timerCard.setBackground(originalBackground);
            timerCard.setForeground(originalForeground);
        }
    }

    private static final class PulseBorderDrawable extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private float progress;

        PulseBorderDrawable(int color, float density) {
            this.density = density;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
        }

        void setProgress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            float glowWidth = density * (4.2f + 3.8f * progress);
            float inset = glowWidth / 2f + density * 0.6f;
            RectF border = new RectF(
                    bounds.left + inset,
                    bounds.top + inset,
                    bounds.right - inset,
                    bounds.bottom - inset
            );

            paint.setStrokeWidth(glowWidth);
            paint.setAlpha(Math.round(30f + 70f * progress));
            canvas.drawRoundRect(
                    border,
                    18f * density,
                    18f * density,
                    paint
            );

            paint.setStrokeWidth(density * (1.4f + 1.5f * progress));
            paint.setAlpha(Math.round(125f + 125f * progress));
            canvas.drawRoundRect(
                    border,
                    18f * density,
                    18f * density,
                    paint
            );
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final class ButtonStateController {

        private final ImageButton button;
        private AnimatorSet animator;
        private int generation;

        ButtonStateController(ImageButton button) {
            this.button = button;
        }

        void play(@DrawableRes int targetIcon) {
            generation++;
            int currentGeneration = generation;
            cancelAnimator();

            button.animate().cancel();
            button.setPivotX(button.getWidth() / 2f);
            button.setPivotY(button.getHeight() / 2f);
            button.setImageAlpha(255);

            AnimatorSet press = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                    button,
                    View.SCALE_X,
                    button.getScaleX(),
                    0.9f
            );
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                    button,
                    View.SCALE_Y,
                    button.getScaleY(),
                    0.9f
            );
            scaleX.setDuration(85L);
            scaleY.setDuration(85L);
            scaleX.setInterpolator(new DecelerateInterpolator());
            scaleY.setInterpolator(new DecelerateInterpolator());
            press.playTogether(scaleX, scaleY);
            animator = press;
            press.addListener(new SafeEndListener(currentGeneration) {
                @Override
                void onSafeEnd() {
                    fadeOutCurrentIcon(currentGeneration, targetIcon);
                }
            });
            press.start();
        }

        private void fadeOutCurrentIcon(
                int currentGeneration,
                @DrawableRes int targetIcon) {
            ValueAnimator fadeOut = ValueAnimator.ofInt(button.getImageAlpha(), 0);
            fadeOut.setDuration(90L);
            fadeOut.setInterpolator(new DecelerateInterpolator());
            fadeOut.addUpdateListener(animation ->
                    button.setImageAlpha((int) animation.getAnimatedValue())
            );

            AnimatorSet fadeSet = new AnimatorSet();
            fadeSet.play(fadeOut);
            animator = fadeSet;
            fadeSet.addListener(new SafeEndListener(currentGeneration) {
                @Override
                void onSafeEnd() {
                    button.setImageResource(targetIcon);
                    button.setImageAlpha(0);
                    revealTargetIcon(currentGeneration);
                }
            });
            fadeSet.start();
        }

        private void revealTargetIcon(int currentGeneration) {
            ValueAnimator fadeIn = ValueAnimator.ofInt(0, 255);
            fadeIn.setDuration(155L);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            fadeIn.addUpdateListener(animation ->
                    button.setImageAlpha((int) animation.getAnimatedValue())
            );

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                    button,
                    View.SCALE_X,
                    button.getScaleX(),
                    1f
            );
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                    button,
                    View.SCALE_Y,
                    button.getScaleY(),
                    1f
            );
            scaleX.setDuration(180L);
            scaleY.setDuration(180L);
            scaleX.setInterpolator(new OvershootInterpolator(1.12f));
            scaleY.setInterpolator(new OvershootInterpolator(1.12f));

            AnimatorSet reveal = new AnimatorSet();
            reveal.playTogether(fadeIn, scaleX, scaleY);
            animator = reveal;
            reveal.addListener(new SafeEndListener(currentGeneration) {
                @Override
                void onSafeEnd() {
                    button.setImageAlpha(255);
                    button.setScaleX(1f);
                    button.setScaleY(1f);
                    animator = null;
                }
            });
            reveal.start();
        }

        private void cancelAnimator() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            button.setScaleX(1f);
            button.setScaleY(1f);
            button.setImageAlpha(255);
        }

        private abstract class SafeEndListener extends AnimatorListenerAdapter {

            private final int expectedGeneration;
            private boolean cancelled;

            SafeEndListener(int expectedGeneration) {
                this.expectedGeneration = expectedGeneration;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled && generation == expectedGeneration) {
                    onSafeEnd();
                }
            }

            abstract void onSafeEnd();
        }
    }
}
