package com.example.gym_app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkoutTypeRepositoryTest {

    @Test
    public void acceptsBuiltInAndGeneratedCustomTypeIds() {
        assertTrue(WorkoutTypeRepository.isSafeTypeId(WorkoutStorage.TYPE_PUSH));
        assertTrue(WorkoutTypeRepository.isSafeTypeId("custom_1234abcd"));
        assertFalse(WorkoutTypeRepository.isSafeTypeId("custom Oberkörper"));
        assertFalse(WorkoutTypeRepository.isSafeTypeId("../custom_bad"));
        assertFalse(WorkoutTypeRepository.isSafeTypeId(""));
    }
}
