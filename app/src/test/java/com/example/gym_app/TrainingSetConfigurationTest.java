package com.example.gym_app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrainingSetConfigurationTest {

    @Test
    public void countIsClampedBetweenFourAndTen() {
        assertEquals(4, TrainingSetConfiguration.clamp(1));
        assertEquals(4, TrainingSetConfiguration.clamp(4));
        assertEquals(7, TrainingSetConfiguration.clamp(7));
        assertEquals(10, TrainingSetConfiguration.clamp(10));
        assertEquals(10, TrainingSetConfiguration.clamp(15));
    }
}
