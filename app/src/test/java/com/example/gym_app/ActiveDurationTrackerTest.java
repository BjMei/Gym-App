package com.example.gym_app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActiveDurationTrackerTest {

    @Test
    public void trackerAccumulatesOnlyStartedSegments() {
        ActiveDurationTracker tracker = new ActiveDurationTracker();
        tracker.restore(1_000L);

        tracker.start(10_000L);
        assertEquals(1_500L, tracker.elapsed(10_500L));
        assertEquals(1_800L, tracker.pause(10_800L));

        assertEquals(1_800L, tracker.elapsed(50_000L));
        tracker.start(60_000L);
        assertEquals(2_200L, tracker.pause(60_400L));
    }

    @Test
    public void trackerNeverSubtractsTimeForNonMonotonicInput() {
        ActiveDurationTracker tracker = new ActiveDurationTracker();
        tracker.restore(500L);
        tracker.start(1_000L);

        assertEquals(500L, tracker.pause(900L));
    }
}
