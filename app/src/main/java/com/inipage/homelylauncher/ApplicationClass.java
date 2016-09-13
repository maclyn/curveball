package com.inipage.homelylauncher;

import android.app.Application;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";

    private static ApplicationClass instance;

    public static ApplicationClass getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
     //   Fabric.with(this, new Crashlytics());
        instance = this;
    }
}
