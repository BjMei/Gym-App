package com.example.gym_app;

final class ActiveDurationTracker {

    private long accumulatedMs;
    private long segmentStartedAtMs = -1L;

    void restore(long durationMs) {
        accumulatedMs = Math.max(0L, durationMs);
        segmentStartedAtMs = -1L;
    }

    void start(long nowMs) {
        if (segmentStartedAtMs < 0L) {
            segmentStartedAtMs = Math.max(0L, nowMs);
        }
    }

    long pause(long nowMs) {
        if (segmentStartedAtMs >= 0L) {
            accumulatedMs += Math.max(0L, nowMs - segmentStartedAtMs);
            segmentStartedAtMs = -1L;
        }
        return accumulatedMs;
    }

    long elapsed(long nowMs) {
        if (segmentStartedAtMs < 0L) {
            return accumulatedMs;
        }
        return accumulatedMs + Math.max(0L, nowMs - segmentStartedAtMs);
    }
}
