package com.inipage.homelylauncher;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Pair;

import java.util.List;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";

    private static ApplicationClass instance;

    public static ApplicationClass getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
