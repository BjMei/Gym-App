package com.example.gym_app;

import java.time.LocalDate;

final class ProfileInputValidator {

    private ProfileInputValidator() {
    }

    static boolean isBirthDateAllowed(LocalDate date, LocalDate today) {
        return date != null && !date.isAfter(today);
    }

    static boolean isTargetDateAllowed(LocalDate date, LocalDate today) {
        return date == null || date.isAfter(today);
    }

    static boolean isMeasurementDateAllowed(LocalDate date, LocalDate today) {
        return date != null && !date.isAfter(today);
    }
}
