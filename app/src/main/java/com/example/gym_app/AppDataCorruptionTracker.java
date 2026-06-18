package com.example.gym_app;

import android.util.Log;

final class AppDataCorruptionTracker {

    private static final String TAG = "IronxData";
    private static boolean corruptionDetected;
    private static boolean corruptionNoticeShown;

    private AppDataCorruptionTracker() {
    }

    static synchronized void record(String source, Exception exception) {
        corruptionDetected = true;
        Log.e(
                TAG,
                "Beschädigte App-Daten in " + source
                        + " werden weder verwendet noch überschrieben.",
                exception
        );
    }

    static synchronized boolean consumeNotice() {
        if (!corruptionDetected || corruptionNoticeShown) {
            return false;
        }
        corruptionNoticeShown = true;
        return true;
    }
}
