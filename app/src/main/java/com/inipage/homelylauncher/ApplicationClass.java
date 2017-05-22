package com.inipage.homelylauncher;

import android.app.Application;

import com.inipage.homelylauncher.utils.Utilities;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";

    private static ApplicationClass instance;

    public static ApplicationClass getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
      //x Fabric.with(this, new Crashlytics());
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
