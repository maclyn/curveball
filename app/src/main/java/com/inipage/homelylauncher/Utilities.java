package com.inipage.homelylauncher;

import android.graphics.Color;
import android.util.Log;

import com.inipage.homelylauncher.views.DockView;

public class Utilities {
    public static int colorWithMutedAlpha(int color, float alpha){
        return Color.argb((int) (alpha * 160), Color.red(color), Color.green(color), Color.blue(color));
    }
}
