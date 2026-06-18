package com.example.gym_app;

final class TrainingSetConfiguration {

    static final int MIN_SETS = 4;
    static final int MAX_SETS = 10;

    private TrainingSetConfiguration() {
    }

    static int clamp(int count) {
        return Math.max(MIN_SETS, Math.min(MAX_SETS, count));
    }
}
