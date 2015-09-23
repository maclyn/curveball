package com.inipage.homelylauncher;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Pair;

import java.util.List;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";

    private static ApplicationClass instance;

    private List<Pair<String, String>> pairs;

    public void storePairs(List<Pair<String, String>> pairs){
        this.pairs = pairs;
    }

    public List<Pair<String, String>> getPairs(){
        return this.pairs;
    }

    public static ApplicationClass getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
