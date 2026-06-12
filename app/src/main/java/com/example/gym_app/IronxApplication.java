package com.example.gym_app;

import android.app.Application;

public class IronxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppSettings.wrapContext(this);
    }
}
