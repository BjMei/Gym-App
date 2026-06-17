package com.example.gym_app;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public abstract class IronxActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSettings.animationsEnabled(this)) {
            getWindow().setWindowAnimations(0);
        }
        if (AppSettings.keepScreenOn(this) && isWorkoutScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View content = findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            View root = ((ViewGroup) content).getChildAt(0);
            if (AppSettings.isOledMode(this)) {
                applyOledBackground(root);
            }
            applyHapticSetting(root, AppSettings.hapticsEnabled(this));
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (!AppSettings.animationsEnabled(this)) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (WorkoutStorage.consumeCorruptionNotice() && !isFinishing()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.storage_corruption_title)
                    .setMessage(R.string.storage_corruption_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private boolean isWorkoutScreen() {
        return this instanceof PushActivity
                || this instanceof PullActivity
                || this instanceof LegActivity;
    }

    private void applyHapticSetting(View view, boolean enabled) {
        view.setHapticFeedbackEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyHapticSetting(group.getChildAt(i), enabled);
            }
        }
    }

    private void applyOledBackground(View view) {
        if (view.getId() != View.NO_ID) {
            try {
                String resourceName = getResources().getResourceEntryName(view.getId());
                if (resourceName.startsWith("root") && resourceName.endsWith("Layout")) {
                    view.setBackgroundColor(Color.BLACK);
                }
            } catch (Exception ignored) {
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyOledBackground(group.getChildAt(i));
            }
        }
    }
}
