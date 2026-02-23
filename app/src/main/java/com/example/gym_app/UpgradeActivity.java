package com.example.gym_app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

public class UpgradeActivity extends AppCompatActivity {

    private static final String PLAN_MONTHLY = "monthly";
    private static final String PLAN_YEARLY = "yearly";

    private String selectedPlan = PLAN_YEARLY;

    private LinearLayout monthlyPlanCard;
    private LinearLayout yearlyPlanCard;
    private TextView monthlyCheck;
    private TextView yearlyCheck;
    private TextView yearlySaving;
    private TextView ctaTitle;
    private TextView ctaSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        bindViews();
        bindEvents();
        applyPlanSelection();
    }

    private void bindViews() {
        monthlyPlanCard = findViewById(R.id.monthlyPlanCard);
        yearlyPlanCard = findViewById(R.id.yearlyPlanCard);
        monthlyCheck = findViewById(R.id.monthlyCheck);
        yearlyCheck = findViewById(R.id.yearlyCheck);
        yearlySaving = findViewById(R.id.yearlySaving);
        ctaTitle = findViewById(R.id.ctaTitle);
        ctaSubtitle = findViewById(R.id.ctaSubtitle);

        View backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void bindEvents() {
        monthlyPlanCard.setOnClickListener(v -> {
            selectedPlan = PLAN_MONTHLY;
            applyPlanSelection();
        });

        yearlyPlanCard.setOnClickListener(v -> {
            selectedPlan = PLAN_YEARLY;
            applyPlanSelection();
        });

        View ctaButton = findViewById(R.id.ctaButton);
        ctaButton.setOnClickListener(this::handleCtaClick);
    }

    private void applyPlanSelection() {
        boolean isYearly = PLAN_YEARLY.equals(selectedPlan);

        monthlyPlanCard.setBackgroundResource(isYearly ? R.drawable.bg_plan_card : R.drawable.bg_plan_card_active);
        yearlyPlanCard.setBackgroundResource(isYearly ? R.drawable.bg_plan_card_active : R.drawable.bg_plan_card);

        monthlyCheck.setVisibility(isYearly ? View.GONE : View.VISIBLE);
        yearlyCheck.setVisibility(isYearly ? View.VISIBLE : View.GONE);
        yearlySaving.setVisibility(isYearly ? View.VISIBLE : View.GONE);

        if (isYearly) {
            ctaTitle.setText(R.string.upgrade_cta_yearly);
            ctaSubtitle.setText(R.string.upgrade_cta_yearly_sub);
        } else {
            ctaTitle.setText(R.string.upgrade_cta_monthly);
            ctaSubtitle.setText(R.string.upgrade_cta_monthly_sub);
        }
    }

    private void handleCtaClick(View view) {
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.95f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.95f);
        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.95f, 1f);
        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.95f, 1f);

        AnimatorSet pressAnim = new AnimatorSet();
        AnimatorSet down = new AnimatorSet();
        down.playTogether(scaleXDown, scaleYDown);
        down.setDuration(80);

        AnimatorSet up = new AnimatorSet();
        up.playTogether(scaleXUp, scaleYUp);
        up.setDuration(120);

        pressAnim.playSequentially(down, up);
        pressAnim.start();

        Toast.makeText(
                this,
                getString(R.string.upgrade_purchase_placeholder, selectedPlan),
                Toast.LENGTH_SHORT
        ).show();
    }
}
