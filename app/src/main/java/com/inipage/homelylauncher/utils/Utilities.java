package com.inipage.homelylauncher.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;

import com.inipage.homelylauncher.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Random;

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
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    public static Float floatRange(float min, float max) {
        if(max <  min) throw new RuntimeException("max < min. gawsh.");
        return (new Random().nextFloat() * (max - min)) + min;
    }

    //Densities apps will likely have icons at
    private static final int[] DENSITY_LIST = new int[] {
            DisplayMetrics.DENSITY_LOW, DisplayMetrics.DENSITY_MEDIUM, DisplayMetrics.DENSITY_HIGH,
            DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_XXHIGH, DisplayMetrics.DENSITY_XXXHIGH
    };

    public static Drawable getDrawableForSize(Resources resources, int icon, int size) {
        float minDpi = (size / 48F) * 160F;
        minDpi -= 0.1F;

        int densityIndex = DENSITY_LIST.length - 1;
        for(int i = 0; i < DENSITY_LIST.length; i++) {
            if(minDpi <= DENSITY_LIST[i]){
                densityIndex = i;
                break;
            }
        }

        //TODO: Choose density appropriately
        for(int i = 0; i < DENSITY_LIST.length; i++) {
            Drawable d = resources.getDrawableForDensity(icon, DENSITY_LIST[densityIndex + i % DENSITY_LIST.length]);
            if(d != null) return d;
        }
        return null;
    }

    public enum LogLevel {
		/** Some background task (e.g. icon fetching) has occured. **/
		BACKGROUND_TASK,
		/** A system hook has caused us to do something (e.g. packages changed.) **/
		SYS_BG_TASK,
		/** Some state has changed (e.g. onCreate(...), onPause(...)). */
		STATE_CHANGE,
        /** Some obvious state of error has occurred. **/
        ERROR_STATE,
		/** Some specific user action as occured (e.g. swipe to show drawer). **/
		USER_ACTION;

		public String getRepr(){
			switch(this){
				case BACKGROUND_TASK:
					return "BGTSK";
				case SYS_BG_TASK:
					return "SBGTK";
				case STATE_CHANGE:
					return "STCHG";
				case USER_ACTION:
					return "USACT";
                case ERROR_STATE:
                    return "ERRST";
                default:
                    return "?????";
			}
		}
	}

	private static File LOG_FILE = null;
    private static FileInputStream LOG_FILE_IN = null;
    private static FileOutputStream LOG_FILE_OUT = null;

	public static void openLog(Context context){
        try {
            File filesDir = context.getFilesDir();
            LOG_FILE = new File(filesDir + "/logfile.txt");
            LOG_FILE_OUT = new FileOutputStream(LOG_FILE);
            LOG_FILE_IN = new FileInputStream(LOG_FILE);
        } catch (Exception fileIoEx){
            Log.e(TAG, "Error opening log!", fileIoEx);
        }
	}

    public static void closeLog() {
        try {
            LOG_FILE_OUT.close();
        } catch (Exception ignored){
        }
    }

	public static void clearLog(){
        try {
            LOG_FILE_OUT.flush();
            FileChannel channel = LOG_FILE_OUT.getChannel();
            channel.truncate(0);
        } catch (Exception ignored) {}
	}

    public static String dumpLog(){
        String result = "";
        byte[] buf = new byte[1024];
        int read = 0;
        int off = 0;
        try {
            while ((read = LOG_FILE_IN.read(buf, off, 1024)) > 0) {
                result += new String(buf, StandardCharsets.UTF_8);
            }
            LOG_FILE_IN = new FileInputStream(LOG_FILE);
            return result;
        } catch (Exception e){
            return "Log cannot be read! Welp.";
        }
    }

	/**
	 * Append to an internal log of meaningful state changes.
	 */
	public static void logEvent(LogLevel level, String message){
        try {
            StringBuilder event = new StringBuilder(5 + message.length() + 3 + 10);
            event.append(level.getRepr());
            event.append("|");
            event.append(message.replace("|", "I"));
            event.append("|");
            event.append(System.currentTimeMillis());
            event.append("\n");

            LOG_FILE_OUT.write(event.toString().getBytes(StandardCharsets.UTF_8));
            LOG_FILE_OUT.flush();
        } catch (Exception badWrite) {
            Log.w(TAG, "Error logging event...");
        }
    }

    public static void logError(String str, Exception err){
        logEvent(LogLevel.ERROR_STATE, str);

        //See: Log.e(...)
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        err.printStackTrace(pw);
        pw.flush();

        String[] lines = sw.toString().split("\n");
        for(String s : lines){
            logEvent(LogLevel.ERROR_STATE, s);
        }
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    public static int colorWithMutedAlpha(int color, float alpha){
        return Color.argb((int) (alpha * 160), Color.red(color), Color.green(color), Color.blue(color));
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

    public static boolean checkPermission(String permission, Context context) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isRunningOutOfHeap(){
        return Runtime.getRuntime().freeMemory() < (2 * 1024 * 1024); //2MB
    }

    public static void throwNotImplemented(){
        throw new RuntimeException("Not implemented!");
    }

    public interface ScaleAnimation {
        void onComplete();
    }

    public static void animateAlphaChange(final View child, final ScaleAnimation listener, long time, float start, float end){
        ObjectAnimator oa = ObjectAnimator.ofFloat(child, "alpha", start, end);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(time);
        set.playTogether(oa);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                listener.onComplete();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                listener.onComplete();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    public static void animateScaleChange(final View child, final ScaleAnimation listener, long time, float start, float end){
        ObjectAnimator oa = ObjectAnimator.ofFloat(child, "scaleX", start, end);
        ObjectAnimator oa2 = ObjectAnimator.ofFloat(child, "scaleY", start, end);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(time);
        set.playTogether(oa, oa2);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                listener.onComplete();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                listener.onComplete();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    public static void animateAbsoluteLayoutChange(final View alChild, final AbsoluteLayout.LayoutParams newParams, long duration){
        AbsoluteLayout.LayoutParams oldParams = (AbsoluteLayout.LayoutParams) alChild.getLayoutParams();
        ValueAnimator animation = ValueAnimator.ofObject(new TypeEvaluator<AbsoluteLayout.LayoutParams>() {
            @Override
            public AbsoluteLayout.LayoutParams evaluate(float fraction,
                                                        AbsoluteLayout.LayoutParams startValue,
                                                        AbsoluteLayout.LayoutParams endValue) {
                int currX = (int) (startValue.x + ((endValue.x - startValue.x) * fraction));
                int currY = (int) (startValue.y + ((endValue.y - startValue.y) * fraction));
                int currWidth = (int) ((startValue.width) + ((endValue.width - startValue.width) * fraction));
                int currHeight = (int) ((startValue.height) + ((endValue.height - startValue.height) * fraction));
                return new AbsoluteLayout.LayoutParams(currWidth, currHeight, currX, currY);
            }
        }, oldParams, newParams);
        animation.setTarget(alChild);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                alChild.setLayoutParams((AbsoluteLayout.LayoutParams) animation.getAnimatedValue());
            }
        });
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                alChild.setLayoutParams(newParams);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                alChild.setLayoutParams(newParams);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animation.setDuration(duration);
        animation.start();
    }
}
