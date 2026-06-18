package com.example.gym_app;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileInputValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 18);

    @Test
    public void targetDateMustBeStrictlyInFuture() {
        assertFalse(ProfileInputValidator.isTargetDateAllowed(TODAY, TODAY));
        assertFalse(ProfileInputValidator.isTargetDateAllowed(
                TODAY.minusDays(1),
                TODAY
        ));
        assertTrue(ProfileInputValidator.isTargetDateAllowed(
                TODAY.plusDays(1),
                TODAY
        ));
        assertTrue(ProfileInputValidator.isTargetDateAllowed(null, TODAY));
    }

    @Test
    public void measurementAndBirthDatesCannotBeInFuture() {
        assertTrue(ProfileInputValidator.isBirthDateAllowed(TODAY, TODAY));
        assertFalse(ProfileInputValidator.isBirthDateAllowed(
                TODAY.plusDays(1),
                TODAY
        ));
        assertTrue(ProfileInputValidator.isMeasurementDateAllowed(TODAY, TODAY));
        assertFalse(ProfileInputValidator.isMeasurementDateAllowed(
                TODAY.plusDays(1),
                TODAY
        ));
    }
}
