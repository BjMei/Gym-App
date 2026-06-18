package com.example.gym_app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppContextInstrumentedTest {

    @Test
    public void applicationContextUsesConfiguredPackage() {
        Context appContext =
                InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.example.gym_app", appContext.getPackageName());
    }

    @Test
    public void customWorkoutCrudKeepsArchivedAndActiveNamesDistinct() {
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences preferences = context.getSharedPreferences(
                WorkoutTypeRepository.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        boolean hadStoredTypes = preferences.contains("custom_types");
        String originalTypes = preferences.getString("custom_types", "[]");

        try {
            assertTrue(preferences.edit().putString("custom_types", "[]").commit());
            WorkoutTypeRepository.WorkoutType archived =
                    WorkoutTypeRepository.create(
                            context,
                            "Test Oberkörper",
                            "",
                            WorkoutTypeRepository.FOCUS_UPPER
                    );
            assertNotNull(archived);
            assertTrue(WorkoutTypeRepository.update(
                    context,
                    archived.id,
                    "Test Split",
                    "Beschreibung",
                    WorkoutTypeRepository.FOCUS_FULL
            ));
            assertTrue(WorkoutTypeRepository.archive(context, archived.id));

            WorkoutTypeRepository.WorkoutType active =
                    WorkoutTypeRepository.create(
                            context,
                            "Test Split",
                            "",
                            WorkoutTypeRepository.FOCUS_UPPER
                    );
            assertNotNull(active);
            assertNotEquals(
                    WorkoutTypeRepository.label(context, archived.id),
                    WorkoutTypeRepository.label(context, active.id)
            );
        } finally {
            SharedPreferences.Editor restore = preferences.edit();
            if (hadStoredTypes) {
                restore.putString("custom_types", originalTypes);
            } else {
                restore.remove("custom_types");
            }
            assertTrue(restore.commit());
        }
    }
}
