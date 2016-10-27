package com.inipage.homelylauncher;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.inipage.homelylauncher.utils.Utilities;

import io.fabric.sdk.android.Fabric;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";

    private static ApplicationClass instance;

    public static ApplicationClass getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        instance = this;

        Utilities.openLog(this);
        Utilities.logEvent(Utilities.LogLevel.STATE_CHANGE, "Application started");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Utilities.logEvent(Utilities.LogLevel.STATE_CHANGE, "Application terminated");
        Utilities.closeLog();
    }
}
