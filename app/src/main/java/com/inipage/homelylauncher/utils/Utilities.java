package com.inipage.homelylauncher.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.inipage.homelylauncher.Constants;
import com.inipage.homelylauncher.HomeActivity;

import java.security.Permission;

public class Utilities {
    private static final String TAG = "Utilities";

    public static boolean withinView(MotionEvent ev, View v){
        int location[] = new int[2];
        v.getLocationOnScreen(location);
        float rawX = ev.getRawX();
        float rawY = ev.getRawY();
        float endX = location[0] + v.getWidth();
        float endY = location[1] + v.getHeight();
        Log.d(TAG, rawX + " " + rawY + " " + " " + location[0] + " " + location[1] + " " +
                endX + " " + endY);
        return rawX >= v.getX() && rawX <= endX && rawY >= v.getY() && rawY <= endY;
    }

    public static void grabActivity(Activity a, int requestCode){
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
        a.startActivityForResult(pickIntent, requestCode);
    }


    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static double getScreenSize(Context context){
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        return Math.sqrt(x + y);
    }

    public static boolean isSmallTablet(Context context){
        if(PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.IS_PHONE_PREFERENCE, false)) return false;

        double screenSize = getScreenSize(context);
        if(screenSize >= 6.5 && screenSize <= 8.8){
            return true;
        }
        return false;
    }

    public static boolean isLargeTablet(Context context){
        if(PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.IS_PHONE_PREFERENCE, false)) return false;

        double screenSize = getScreenSize(context);
        if(screenSize > 8.8){
            return true;
        }
        return false;
    }

    public static boolean checkMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean checkPermission(String permission, Context context) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
